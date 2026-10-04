package br.com.jhohannesfreitas.booking_ms.service;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.booking_ms.integration.AbstractIntegrationTest;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class ReservaServiceIntegrationConflitosHorariosTest extends AbstractIntegrationTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long SALA_ID = 10L;
    private static final Long OUTRA_SALA_ID = 11L;

    // Sempre no futuro, para nunca cair na validação de "data no passado"
    private static final LocalDate DATA = LocalDate.now().plusDays(1);

    @MockitoBean
    private UsuarioClient usuarioClient;

    @MockitoBean
    private SalaClient salaClient;

    @MockitoBean
    private SalaIntegracaoService salaIntegracaoService;

    @MockitoSpyBean
    private KafkaTemplate kafkaTemplate;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @BeforeEach
    void setUp() {
        // Os spies são compartilhados entre as classes de teste: zera o histórico
        clearInvocations(kafkaTemplate, rabbitTemplate);

        given(usuarioClient.buscarPorId(USUARIO_ID))
                .willReturn(new UsuarioRequest(USUARIO_ID, "teste", "teste@gmail.com"));

        // Qualquer sala existe, é LIVRE e comporta 20 pessoas
        given(salaClient.buscarPorId(anyLong()))
                .willAnswer(inv -> new SalaRequest(inv.<Long>getArgument(0), 1, 20, StatusSala.LIVRE));
        given(salaIntegracaoService.marcarSalaOcupada(anyLong())).willReturn(true);
    }

    @AfterEach
    void tearDown() {
        reservaRepository.deleteAll();
    }

    // Forma de executar o mesmo teste várias vezes, mudando apenas os dados de entrada
    @ParameterizedTest(name = "[{index}] novo {0}-{1} conflita com existente 10:00-11:00")
    // É onde você fornece os valores que serão usados em cada execução
    @CsvSource({
            "10:00, 11:00", // exatamente o mesmo horário
            "09:30, 10:30", // invade o início da existente
            "10:30, 11:30", // invade o fim da existente
            "09:00, 12:00", // envolve a existente
            "10:15, 10:45"  // está dentro da existente
    })

    @DisplayName("Deveria lançar conflito 409 e não gravar nem publicar quando o horário se sobrepõe a uma reserva ATIVA")
    void deveriaLancarConflitoQuandoHorarioSeSobrepoe(LocalTime horaInicial, LocalTime horaFinal) {
        // ARRANGE
        persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA);
        ReservaRequest novaReserva = new ReservaRequest(SALA_ID, DATA, horaInicial, horaFinal, 5);

        // ACT + ASSERT
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> reservaService.cadastrar(novaReserva, USUARIO_ID));

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, ex.getStatus()),
                () -> assertTrue(ex.getMessage().contains("Conflito de horário")),
                // Estado: nada novo foi gravado
                () -> assertEquals(1, reservaRepository.count())
        );
        
        // Comportamento: nada foi publicado e a sala não foi marcada como ocupada
        verify(kafkaTemplate, never()).send(eq("booking-created"), any(ReservaRequest.class));
        then(salaIntegracaoService).shouldHaveNoInteractions();
        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "[{index}] reservas adjacentes {0}-{1} não conflitam com 10:00-11:00")
    @CsvSource({
            "09:00, 10:00", // termina quando a existente começa
            "11:00, 12:00"  // começa quando a existente termina
    })

    @DisplayName("Deveria permitir reservas adjacentes, cujo horário apenas encosta no da existente")
    void devePermitirReservasAdjacentes(LocalTime horaInicial, LocalTime horaFinal) {
        persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA);
        ReservaRequest novaReserva = new ReservaRequest(SALA_ID, DATA, horaInicial, horaFinal, 5);

        // Eu espero que esse código NÃO lance nenhuma exceção
        assertDoesNotThrow(() -> reservaService.cadastrar(novaReserva, USUARIO_ID));

        assertEquals(2, reservaRepository.count());
        verify(kafkaTemplate, times(1)).send("booking-created", novaReserva);
    }

    @Test
    @DisplayName("Deveria permitir o mesmo horário em outra sala")
    void devePermitirMesmoHorarioEmOutraSala() {
        persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA);
        ReservaRequest outraSala = new ReservaRequest(OUTRA_SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), 5);

        assertDoesNotThrow(() -> reservaService.cadastrar(outraSala, USUARIO_ID));

        assertEquals(2, reservaRepository.count());
    }

    @Test
    @DisplayName("Deveria permitir o mesmo horário em outra data")
    void devePermitirMesmoHorarioEmOutraData() {
        persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA);
        ReservaRequest outraData = new ReservaRequest(SALA_ID, DATA.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), 5);

        assertDoesNotThrow(() -> reservaService.cadastrar(outraData, USUARIO_ID));

        assertEquals(2, reservaRepository.count());
    }

    @Test
    //@Disabled("Decisão de negócio pendente: hoje só reservas ATIVA bloqueiam; ATIVA_SEM_INTEGRACAO NÃO bloqueia. "
            //+ "Habilite este teste depois de decidir (e corrigir o validarConflitoHorarioCadastro, se for o caso).")
    @DisplayName("Deveria lançar conflito também quando a reserva existente está ATIVA_SEM_INTEGRACAO")
    void deveriaLancarConflitoQuandoExistenteEstaSemIntegracao() {
        persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA_SEM_INTEGRACAO);
        ReservaRequest novaReserva = new ReservaRequest(SALA_ID, DATA, LocalTime.of(10, 30), LocalTime.of(11, 30), 5);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> reservaService.cadastrar(novaReserva, USUARIO_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(1, reservaRepository.count());
    }

    // ---------------------------------------------------------------- atualizar

    @Test
    @DisplayName("Deveria permitir atualizar a reserva mantendo o próprio horário (ela não conflita consigo mesma)")
    void devePermitirAtualizarSemConflitarComSiMesma() {
        Reserva existente = persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA);
        // Mesmo horário, só muda a quantidade de pessoas
        ReservaRequest mesmaReservaComMaisPessoas = new ReservaRequest(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), 15);

        assertDoesNotThrow(() -> reservaService.atualizar(existente.getId(), mesmaReservaComMaisPessoas, USUARIO_ID));

        Reserva atualizada = reservaRepository.findById(existente.getId()).orElseThrow();
        assertEquals(15, atualizada.getQuantidadePessoas());
    }

    @Test
    @DisplayName("Deveria lançar conflito 409 ao atualizar uma reserva para um horário ocupado por outra")
    void deveriaLancarConflitoAoAtualizarParaHorarioOcupado() {
        persistirReserva(SALA_ID, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), StatusReserva.ATIVA);
        Reserva reservaDaManha = persistirReserva(SALA_ID, DATA, LocalTime.of(8, 0), LocalTime.of(9, 0), StatusReserva.ATIVA);
        // Tenta mover a reserva das 8h para 10:30-11:30, que está ocupado
        ReservaRequest movimentoComConflito = new ReservaRequest(SALA_ID, DATA, LocalTime.of(10, 30), LocalTime.of(11, 30), 5);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> reservaService.atualizar(reservaDaManha.getId(), movimentoComConflito, USUARIO_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        // O banco mantém o horário original da reserva
        Reserva continuaIgual = reservaRepository.findById(reservaDaManha.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(LocalTime.of(8, 0), continuaIgual.getHoraInicial()),
                () -> assertEquals(LocalTime.of(9, 0), continuaIgual.getHoraFinal())
        );
    }

    private Reserva persistirReserva(Long salaId, LocalDate data, LocalTime inicio, LocalTime fim, StatusReserva status) {
        Reserva reserva = new Reserva(data, inicio, fim, 5);
        reserva.setUsuarioId(USUARIO_ID);
        reserva.setSalaId(salaId);
        reserva.setStatus(status);
        return reservaRepository.save(reserva);
    }
}
