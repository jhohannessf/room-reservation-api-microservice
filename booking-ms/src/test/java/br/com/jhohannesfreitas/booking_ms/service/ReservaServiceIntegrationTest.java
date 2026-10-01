package br.com.jhohannesfreitas.booking_ms.service;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.*;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.integration.AbstractIntegrationTest;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;

public class ReservaServiceIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private UsuarioClient usuarioClient;

    @MockitoBean
    private SalaClient salaClient;

    @MockitoBean
    private SalaIntegracaoService salaIntegracaoService;

    @Autowired //Uso para chamar a classe de verdade, com a lógica real dela.
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @MockitoSpyBean
    private KafkaTemplate kafkaTemplate;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void tearDown() {
        reservaRepository.deleteAll(); // Limpa o banco após cada teste
    }

    @Test
    @DisplayName("Deveria cadastrar reserva com sucesso quando os dados estiverem corretos")
    void deveriaCadastrarReservaComSucessoQuandoOsDadosEstiveremCorretos() {
        // ARRANGE
        UsuarioRequest usuarioRequest = criarUsuarioRequest();
        SalaRequest salaRequest = criarSalaRequest();
        ReservaRequest reservaRequest = criarReservaRequest(LocalDate.now(), 20);

        // Simula a busca por id usando o Feing
        given(usuarioClient.buscarPorId(usuarioRequest.id())).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula que a integração com sala está ok, não entra no Circuit Breaker e retorna true
        given(salaIntegracaoService.marcarSalaOcupada(salaRequest.id())).willReturn(true);

        // Limpa o histórico de interações do Spring na inicialização
        clearInvocations(rabbitTemplate);

        // ACT
        ReservaResponse reservaResponse = reservaService.cadastrar(reservaRequest, usuarioRequest.id());

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Usuario".
        then(usuarioClient).should().buscarPorId(usuarioRequest.id());
        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Sala".
        then(salaClient).should().buscarPorId(reservaRequest.salaId());
        // "Então Repository, você DEVERIA verificar se foi chamado o 'findBySalaIdAndDataAndStatus()' do objeto Reserva".
        // "Então kafkaTemplate, você DEVERIA verificar se foi enviado mensagens
        then(kafkaTemplate).should().send("booking-created", reservaRequest);
        // "Então rabbitTemplate, você DEVERIA verificar que não há mensagem interação de mensagens
        then(rabbitTemplate).shouldHaveNoInteractions();
        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Verificando se os dados retornados são iguais aos da entidade persistida
        assertAll(
                () -> assertEquals(reservaRequest.data(), reservaResponse.data()),
                () -> assertEquals(reservaRequest.horaInicial(), reservaResponse.horaInicial()),
                () -> assertEquals(reservaRequest.horaFinal(), reservaResponse.horaFinal()),
                () -> assertEquals(reservaRequest.quantidadePessoas(), reservaResponse.quantidadePessoas()),
                () -> assertEquals(usuarioRequest.id(), reservaResponse.usuarioId()),
                () -> assertEquals(salaRequest.id(), reservaResponse.salaId()),
                () -> assertEquals(StatusReserva.ATIVA, reservaResponse.status()),
                () -> assertEquals(usuarioRequest.id(), reservaResponse.usuarioId())
        );

    }

    @Test
    @DisplayName("Deveria alterar o status ao cadastrar reserva quando estiver sem integração com sala")
    void DeveriaAlterarStatusAoCadastrarReservaQuandoEstiverSemIntegracaoComSala() {
        UsuarioRequest usuarioRequest = criarUsuarioRequest();
        SalaRequest salaRequest = criarSalaRequest();
        ReservaRequest reservaRequest = criarReservaRequest(LocalDate.now(), 20);

        // Simula a busca por id usando o Feing
        given(usuarioClient.buscarPorId(usuarioRequest.id())).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula que a integração falhou, ativando o fallback que retorna false
        given(salaIntegracaoService.marcarSalaOcupada(salaRequest.id())).willReturn(false);

        // Limpa o histórico de interações do Spring na inicialização
        clearInvocations(rabbitTemplate);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        ReservaResponse reservaResponse = reservaService.cadastrar(reservaRequest, usuarioRequest.id());

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Usuario".
        then(usuarioClient).should().buscarPorId(usuarioRequest.id());
        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Sala".
        then(salaClient).should().buscarPorId(reservaRequest.salaId());
        // "Então kafkaTemplate, você DEVERIA verificar se foi enviado mensagens
        then(kafkaTemplate).should().send("booking-created", reservaRequest);
        // "Então rabbitTemplate, você DEVERIA verificar que não há mensagem interação de mensagens
        then(rabbitTemplate).shouldHaveNoInteractions();
        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Verificando se os dados retornados são iguais aos da entidade persistida
        Reserva reservaPersistida = reservaRepository.findById(reservaResponse.id()).orElseThrow();
        assertAll(
                () -> assertEquals(reservaRequest.data(), reservaResponse.data()),
                () -> assertEquals(reservaRequest.horaInicial(), reservaResponse.horaInicial()),
                () -> assertEquals(reservaRequest.horaFinal(), reservaResponse.horaFinal()),
                () -> assertEquals(reservaRequest.quantidadePessoas(), reservaResponse.quantidadePessoas()),
                () -> assertEquals(usuarioRequest.id(), reservaResponse.usuarioId()),
                () -> assertEquals(salaRequest.id(), reservaResponse.salaId()),
                () -> assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, reservaResponse.status()),
                () -> assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, reservaPersistida.getStatus()),
                () -> assertEquals(usuarioRequest.id(), reservaResponse.usuarioId())
        );
    }

    @Test
    @DisplayName("Deveria confirmar reserva sem integração e publicar no RabbitMQ quando os dados forem válidos")
    void deveriaConfirmarReservaSemIntegracaoEPublicarNoRabbitMQ() {
        // ARRANGE

        // Persiste uma reserva de verdade no banco com status ATIVA_SEM_INTEGRACAO
        Reserva reserva = new Reserva(
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5
                );
        reserva.setUsuarioId(1L);
        reserva.setSalaId(10L);
        reserva.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        Reserva reservaSalva = reservaRepository.save(reserva);

        // Limpa o histórico de interações do Spring na inicialização
        clearInvocations(rabbitTemplate);

        // ACT
        reservaService.confirmarReservaSemIntegracao(reservaSalva.getId(), reservaSalva.getUsuarioId());

        // ASSERT
        // Verificação de comportamento
        then(salaClient).should().alterarStatusSala(eq(10L), any(StatusSalaRequest.class));
        then(rabbitTemplate).should().convertAndSend("reserva.direct.ex", "reserva.detalhes-status-sala", StatusSala.OCUPADA);

        // Verificação de estado - Confirmando que o banco realmente foi atualizado
        Reserva reservaAtualiza = reservaRepository.findById(reservaSalva.getId()).orElseThrow();
        assertEquals(StatusReserva.ATIVA, reservaAtualiza.getStatus());
    }

    private ReservaRequest criarReservaRequest(LocalDate data, Integer quantidadePessoas) {
        return new ReservaRequest(
                10L, data, LocalTime.of(10,0), LocalTime.of(11,0), quantidadePessoas
        );
    }

    private UsuarioRequest criarUsuarioRequest() {
        return new UsuarioRequest(
                1L,
                "teste",
                "teste@gmail.com"
        );
    }

    private SalaRequest criarSalaRequest() {
        return new SalaRequest(
                10L,
                1,
                20,
                StatusSala.LIVRE
        );
    }
}
