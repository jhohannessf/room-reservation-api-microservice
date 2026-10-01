package br.com.jhohannesfreitas.room_ms.integration;

import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.room_ms.messaging.kafka.ReservaListenerKafka;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class ReservaListenerKafkaIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    ReservaListenerKafka reservaListenerKafka;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper; // Injetamos o conversor de Json


    @Test
    @DisplayName("Deveria ouvir mensagem kafka quando cadastro de reserva for concluído")
    public void deveriaOuvirMensagemKafkaComSucessoQuandoCadastroReservaForConcluido() throws JsonProcessingException {
        // ARRANGE
        ReservaRequest reservaRequest = criarReservaRequest(LocalDate.now(), 20);

        // Converte o objeto Java para uma String em formato Json
        String jsonPayload = objectMapper.writeValueAsString(reservaRequest);

        // ACT - Neste caso, enviamos a mensagem para o Tópico (O carteiro)
        kafkaTemplate.send("booking-created", jsonPayload);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então kafkaTemplate, você DEVERIA verificar se foi enviado mensagens
        // Aguardamos até 5 segundos para garantir que a aplicação consumiu a mensagem!
        verify(reservaListenerKafka, timeout(5000))
                .recebeMensagem(Mockito.argThat( // Verifique se o argumento passado para esse método atende a esta condição.
                        argument ->
                                argument.salaId().equals(reservaRequest.salaId()) &&
                                argument.data().equals(reservaRequest.data()) &&
                                argument.horaInicial().equals(reservaRequest.horaInicial()) &&
                                argument.horaFinal().equals(reservaRequest.horaFinal()) &&
                                argument.quantidadePessoas().equals(reservaRequest.quantidadePessoas())

                        ));

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    private ReservaRequest criarReservaRequest(LocalDate data, Integer quantidadePessoas) {
        return new ReservaRequest(
                10L, data, LocalTime.of(10,0), LocalTime.of(11,0), quantidadePessoas
        );
    }

}
