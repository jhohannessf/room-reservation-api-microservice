package br.com.jhohannesfreitas.booking_ms.integration;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import br.com.jhohannesfreitas.booking_ms.service.ReservaService;
import br.com.jhohannesfreitas.booking_ms.service.SalaIntegracaoService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Testes do Circuit Breaker "atualizaSala" com o Resilience4j FUNCIONANDO DE VERDADE.
 *
 * Aqui o SalaIntegracaoService NÃO é mockado (nos outros testes ele é @MockitoBean): é o bean real,
 * com o proxy do Spring AOP e a configuração do application.properties de teste
 * (slidingWindowSize=3, minimumNumberOfCalls=2, failureRateThreshold padrão de 50%).
 * Só o SalaClient (HTTP para o room-ms) é simulado, para podermos "derrubar" o room-ms à vontade.
 *
 * Esses testes são a rede de proteção do bug da auto-invocação: se alguém mover a chamada anotada
 * de volta para dentro do ReservaService, o @CircuitBreaker deixa de funcionar e estes testes quebram.
 *
 * Determinismo: o estado do circuito vive no CircuitBreakerRegistry, compartilhado por todo o contexto,
 * então ele é resetado antes e depois de cada teste.
 */
public class SalaIntegracaoCircuitBreakerIntegrationTest extends AbstractIntegrationTest {

    private static final String CIRCUIT_BREAKER = "atualizaSala";
    private static final Long SALA_ID = 10L;
    private static final Long USUARIO_ID = 1L;
    private static final LocalDate DATA = LocalDate.now().plusDays(1);

    @MockitoBean
    private SalaClient salaClient;

    @MockitoBean
    private UsuarioClient usuarioClient;

    @Autowired // bean REAL, com proxy do Resilience4j
    private SalaIntegracaoService salaIntegracaoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER);
        circuitBreaker.reset(); // volta para CLOSED e zera as métricas
    }

    @AfterEach
    void tearDown() {
        circuitBreaker.reset();
        reservaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria carregar a configuração do Circuit Breaker definida nas propriedades de teste")
    void deveriaCarregarConfiguracaoDoCircuitBreaker() {
        // Se este teste falhar, os demais não estão testando o que parecem testar
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();

        assertEquals(3, config.getSlidingWindowSize());
        assertEquals(2, config.getMinimumNumberOfCalls());
    }

    @Test
    @DisplayName("Deveria retornar true e manter o circuito fechado quando o room-ms responde")
    void deveriaRetornarTrueEManterCircuitoFechadoQuandoRoomMsResponde() {
        roomMsResponde();

        assertTrue(salaIntegracaoService.marcarSalaOcupada(SALA_ID));

        assertEquals(State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    @DisplayName("Deveria acionar o fallback (false) numa falha isolada, sem abrir o circuito")
    void deveriaAcionarFallbackSemAbrirCircuitoNumaFalhaIsolada() {
        roomMsForaDoAr();

        assertFalse(salaIntegracaoService.marcarSalaOcupada(SALA_ID));

        // Só 1 chamada: ainda abaixo do minimumNumberOfCalls (2), então o circuito segue fechado
        assertEquals(State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    @DisplayName("Deveria abrir o circuito após falhas seguidas e parar de chamar o room-ms")
    void deveriaAbrirCircuitoEPararDeChamarRoomMs() {
        roomMsForaDoAr();

        // 2 falhas = minimumNumberOfCalls atingido com 100% de falha (> 50%) -> OPEN
        assertFalse(salaIntegracaoService.marcarSalaOcupada(SALA_ID));
        assertFalse(salaIntegracaoService.marcarSalaOcupada(SALA_ID));
        assertEquals(State.OPEN, circuitBreaker.getState());
        then(salaClient).should(times(2)).alterarStatusSala(anyLong(), any(StatusSalaRequest.class));

        // 3ª chamada: o circuito aberto barra a requisição ANTES de chegar no Feign, e o fallback responde
        clearInvocations(salaClient);
        assertFalse(salaIntegracaoService.marcarSalaOcupada(SALA_ID));

        then(salaClient).shouldHaveNoInteractions();
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
    }

    @Test
    @DisplayName("Deveria fechar o circuito de novo quando o room-ms volta e os testes em HALF_OPEN dão certo")
    void deveriaFecharCircuitoQuandoRoomMsVolta() {
        abrirCircuito();

        // Em produção isso acontece sozinho após waitDurationInOpenState (50s); aqui forçamos a transição
        circuitBreaker.transitionToHalfOpenState();
        roomMsResponde();

        int chamadasDeTeste = circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState();
        for (int i = 0; i < chamadasDeTeste; i++) {
            assertTrue(salaIntegracaoService.marcarSalaOcupada(SALA_ID));
        }

        assertEquals(State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Deveria reabrir o circuito quando o room-ms continua fora do ar em HALF_OPEN")
    void deveriaReabrirCircuitoQuandoRoomMsContinuaForaDoAr() {
        abrirCircuito();

        circuitBreaker.transitionToHalfOpenState();
        // room-ms continua fora do ar (o stub de falha segue valendo)
        int chamadasDeTeste = circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState();
        for (int i = 0; i < chamadasDeTeste; i++) {
            assertFalse(salaIntegracaoService.marcarSalaOcupada(SALA_ID));
        }

        assertEquals(State.OPEN, circuitBreaker.getState());
    }

    // ----------------------------------------------- efeito na regra de negócio (ReservaService)

    @Test
    @DisplayName("Deveria cadastrar a reserva como ATIVA_SEM_INTEGRACAO quando o room-ms está fora do ar")
    void deveriaCadastrarReservaAtivaSemIntegracaoQuandoRoomMsForaDoAr() {
        prepararValidacoesDeCadastro();
        roomMsForaDoAr();

        ReservaResponse resposta = reservaService.cadastrar(criarReservaRequest(), USUARIO_ID);

        // A reserva NÃO é perdida nem rejeitada: fica pendente de integração, e isso vale no banco
        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, resposta.status());
        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO,
                reservaRepository.findById(resposta.id()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Deveria cadastrar como ATIVA_SEM_INTEGRACAO sem nem tentar o room-ms quando o circuito já está aberto")
    void deveriaCadastrarSemChamarRoomMsQuandoCircuitoJaEstaAberto() {
        prepararValidacoesDeCadastro();
        abrirCircuito();
        clearInvocations(salaClient);

        ReservaResponse resposta = reservaService.cadastrar(criarReservaRequest(), USUARIO_ID);

        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, resposta.status());
        // buscarPorId (validação, sem Circuit Breaker) continua sendo chamado; a atualização de status não
        then(salaClient).should(never()).alterarStatusSala(anyLong(), any(StatusSalaRequest.class));
    }

    // ---------------------------------------------------------------------------------- helpers

    private void roomMsResponde() {
        // Estilo "willReturn(...).given(mock)": não executa o mock durante o stub, então pode sobrescrever
        // um stub anterior de falha sem estourar a exceção antiga.
        willReturn(new SalaRequest(SALA_ID, 1, 20, StatusSala.OCUPADA))
                .given(salaClient).alterarStatusSala(anyLong(), any(StatusSalaRequest.class));
    }

    private void roomMsForaDoAr() {
        willThrow(new RuntimeException("room-ms fora do ar"))
                .given(salaClient).alterarStatusSala(anyLong(), any(StatusSalaRequest.class));
    }

    private void abrirCircuito() {
        roomMsForaDoAr();
        salaIntegracaoService.marcarSalaOcupada(SALA_ID);
        salaIntegracaoService.marcarSalaOcupada(SALA_ID);
        assertEquals(State.OPEN, circuitBreaker.getState(), "Pré-condição: o circuito deveria estar aberto");
    }

    private void prepararValidacoesDeCadastro() {
        given(usuarioClient.buscarPorId(USUARIO_ID))
                .willReturn(new UsuarioRequest(USUARIO_ID, "teste", "teste@gmail.com"));
        given(salaClient.buscarPorId(SALA_ID))
                .willReturn(new SalaRequest(SALA_ID, 1, 20, StatusSala.LIVRE));
    }

    private ReservaRequest criarReservaRequest() {
        return new ReservaRequest(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), 5);
    }
}
