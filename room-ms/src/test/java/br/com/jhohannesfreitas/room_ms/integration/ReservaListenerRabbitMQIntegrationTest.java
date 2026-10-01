package br.com.jhohannesfreitas.room_ms.integration;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.room_ms.messaging.rabbitmq.ReservaListenerRabbitMQ;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class ReservaListenerRabbitMQIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    ReservaListenerRabbitMQ reservaListenerRabbitMQ;

    @Autowired
    RabbitTemplate rabbitTemplate;


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

    private ReservaRequest criarReservaRequest(LocalDate data, Integer quantidadePessoas) {
        return new ReservaRequest(
                10L, data, LocalTime.of(10,0), LocalTime.of(11,0), quantidadePessoas
        );
    }

}
