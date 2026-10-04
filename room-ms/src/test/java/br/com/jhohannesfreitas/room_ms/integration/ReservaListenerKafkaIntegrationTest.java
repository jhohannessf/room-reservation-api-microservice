package br.com.jhohannesfreitas.room_ms.integration;

import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.room_ms.messaging.kafka.ReservaListenerKafka;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class ReservaListenerKafkaIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    ReservaListenerKafka reservaListenerKafka;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    private final SaidaConsole console = new SaidaConsole();

    @BeforeEach
    void iniciarCapturaDoConsole() {
        console.start();
    }

    @AfterEach
    void encerrarCapturaDoConsole() {
        console.stop();
    }


    @Test
    @DisplayName("Deveria ouvir mensagem kafka quando cadastro de reserva for concluído")
    public void deveriaOuvirMensagemKafkaComSucessoQuandoCadastroReservaForConcluido() throws JsonProcessingException {
        // ARRANGE
        ReservaRequest reservaRequest = criarReservaRequest(10L, 20);

        // JSON literal
        String jsonPayload = """
                {"salaId": %d, "data": "%s", "horaInicial": "%s", "horaFinal": "%s", "quantidadePessoas": %d}
                """.formatted(reservaRequest.salaId(), reservaRequest.data(),
                reservaRequest.horaInicial(), reservaRequest.horaFinal(), reservaRequest.quantidadePessoas());

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

    @Test
    @DisplayName("Deveria consumir mensagem Kafka e registrar os dados da reserva no log")
    void deveriaConsumirMensagemKafkaERegistrarNoLog() {
        // ARRANGE
        ReservaRequest reservaRequest = criarReservaRequest(987_001L, 20);

        // ACT - publica no tópico, como o booking-ms faz
        kafkaTemplate.send("booking-created", toJson(reservaRequest));

        // ASSERT
        // Comportamento: o listener recebeu o objeto já desserializado corretamente
        verify(reservaListenerKafka, timeout(15_000)).recebeMensagem(Mockito.argThat(argument ->
                argument.salaId().equals(reservaRequest.salaId())
                        && argument.data().equals(reservaRequest.data())
                        && argument.horaInicial().equals(reservaRequest.horaInicial())
                        && argument.horaFinal().equals(reservaRequest.horaFinal())
                        && argument.quantidadePessoas().equals(reservaRequest.quantidadePessoas())));

        // Efeito colateral: o listener registrou a reserva no log (sem acentos, por causa do encoding do console)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(console.getAll())
                        .contains("da sala: 987001")
                        .contains("Hora inicial: 10:00")
                        .contains("Hora final: 11:00")
                        .contains("Quantidade de pessoas: 20"));
    }

    @Test
    @DisplayName("Deveria processar todas as mensagens quando várias reservas forem publicadas em sequência")
    void deveriaProcessarVariasMensagensEmSequencia() {
        // ACT
        kafkaTemplate.send("booking-created", toJson(criarReservaRequest(987_002L, 3)));
        kafkaTemplate.send("booking-created", toJson(criarReservaRequest(987_003L, 4)));
        kafkaTemplate.send("booking-created", toJson(criarReservaRequest(987_004L, 5)));

        // ASSERT - as três foram consumidas (a ordem entre partições não é garantida, por isso só checamos presença)
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(console.getAll())
                        .contains("da sala: 987002")
                        .contains("da sala: 987003")
                        .contains("da sala: 987004"));
    }

    private String toJson(ReservaRequest r) {
        return """
                {"salaId": %d, "data": "%s", "horaInicial": "%s", "horaFinal": "%s", "quantidadePessoas": %d}
                """.formatted(r.salaId(), r.data(), r.horaInicial(), r.horaFinal(), r.quantidadePessoas());
    }

    private ReservaRequest criarReservaRequest(Long salaId, Integer quantidadePessoas) {
        return new ReservaRequest(
                salaId, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), quantidadePessoas
        );
    }

}
