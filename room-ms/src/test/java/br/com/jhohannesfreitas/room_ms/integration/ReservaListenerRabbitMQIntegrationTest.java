package br.com.jhohannesfreitas.room_ms.integration;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.room_ms.messaging.rabbitmq.ReservaListenerRabbitMQ;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class ReservaListenerRabbitMQIntegrationTest extends AbstractIntegrationTest {

    private static final String DLQ = "reserva.detalhes-sala-dlq";

    @MockitoSpyBean
    ReservaListenerRabbitMQ reservaListenerRabbitMQ;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    private final SaidaConsole console = new SaidaConsole();

    // Dados determinísticos: a DLQ é esvaziada antes e depois de cada teste
    @BeforeEach
    void prepararTeste() {
        rabbitAdmin.purgeQueue(DLQ);
        console.start();
    }

    @AfterEach
    void finalizarTeste() {
        console.stop();
        rabbitAdmin.purgeQueue(DLQ);
    }

    @Test
    @DisplayName("Deveria ouvir mensagem RabbitMQ quando o booking-ms confirmar uma reserva sem integração")
    void deveriaOuvirMensagemRabbitMQQuandoStatusDaSalaForAlterado() {
        // ARRANGE

        // ACT - Neste caso, enviamos a mensagem para o Tópico (O carteiro)
        rabbitTemplate.convertAndSend("reserva.direct.ex","reserva.detalhes-status-sala", StatusSala.OCUPADA);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então kafkaTemplate, você DEVERIA verificar se foi enviado mensagens
        // Aguardamos até 5 segundos para garantir que a aplicação consumiu a mensagem!
        verify(reservaListenerRabbitMQ, timeout(5000)).recebeMensagemAlteracaoStatus(StatusSala.OCUPADA);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria consumir mensagem de status da sala e registrar no log")
    void deveriaConsumirMensagemDeStatusDaSalaERegistrarNoLog() {
        // ACT - mesma exchange e routing key usadas pelo booking-ms
        rabbitTemplate.convertAndSend("reserva.direct.ex", "reserva.detalhes-status-sala", StatusSala.OCUPADA);

        // ASSERT
        // Comportamento: o listener recebeu o enum já convertido
        verify(reservaListenerRabbitMQ, timeout(10_000)).recebeMensagemAlteracaoStatus(StatusSala.OCUPADA);
        // Efeito colateral: log do status
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(console.getAll()).contains("da sala: OCUPADA"));
    }

    @Test
    @DisplayName("Deveria consumir os detalhes da reserva enviados pela exchange fanout e registrar no log")
    void deveriaConsumirDetalhesDaReservaERegistrarNoLog() {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                987_101L, LocalDate.now().plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 0), 8);

        // ACT - exchange fanout não usa routing key
        rabbitTemplate.convertAndSend("reserva.fanout.ex", "", reservaRequest);

        // ASSERT
        verify(reservaListenerRabbitMQ, timeout(10_000)).recebeMensagem(reservaRequest);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(console.getAll())
                        .contains("da sala: 987101")
                        .contains("Hora inicial: 14:00")
                        .contains("Quantidade de pessoas: 8"));
    }

    @Test
    @DisplayName("Deveria enviar para a Dead Letter Queue a mensagem que não pode ser convertida")
    void deveriaEnviarParaDlqQuandoMensagemForInvalida() {
        // ARRANGE - corpo que não é um JSON válido para ReservaRequest
        MessageProperties propriedades = new MessageProperties();
        propriedades.setContentType("application/json");
        Message mensagemInvalida = new Message("{isto nao e json".getBytes(StandardCharsets.UTF_8), propriedades);

        // ACT
        rabbitTemplate.send("reserva.fanout.ex", "", mensagemInvalida);

        // ASSERT - após a rejeição (sem requeue), a mensagem vai parar na DLQ, preservada
        Message naDlq = rabbitTemplate.receive(DLQ, 10_000);
        assertThat(naDlq).as("A mensagem inválida deveria ter ido para a DLQ").isNotNull();
        assertThat(new String(naDlq.getBody(), StandardCharsets.UTF_8)).isEqualTo("{isto nao e json");
    }

    private ReservaRequest criarReservaRequest(LocalDate data, Integer quantidadePessoas) {
        return new ReservaRequest(
                10L, data, LocalTime.of(10,0), LocalTime.of(11,0), quantidadePessoas
        );
    }

}
