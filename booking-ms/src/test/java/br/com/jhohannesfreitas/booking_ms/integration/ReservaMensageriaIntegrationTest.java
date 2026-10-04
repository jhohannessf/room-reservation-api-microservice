package br.com.jhohannesfreitas.booking_ms.integration;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import br.com.jhohannesfreitas.booking_ms.service.ReservaService;
import br.com.jhohannesfreitas.booking_ms.service.SalaIntegracaoService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Testes de integração da PUBLICAÇÃO de mensagens (lado produtor do booking-ms).
 *
 * Diferente dos testes com @MockitoSpyBean (que só provam que o método send/convertAndSend foi chamado),
 * aqui as mensagens são LIDAS DE VERDADE do Kafka e do RabbitMQ rodando em containers.
 * Assim o teste falha se o tópico, a exchange, a routing key ou a serialização estiverem errados.
 *
 * Determinismo: cada teste usa um salaId aleatório (para filtrar apenas os seus eventos no tópico
 * compartilhado) e uma fila RabbitMQ própria, que é removida no @AfterEach.
 */
public class ReservaMensageriaIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPICO_BOOKING_CREATED = "booking-created";
    private static final String EXCHANGE_STATUS_SALA = "reserva.direct.ex";
    private static final String ROUTING_KEY_STATUS_SALA = "reserva.detalhes-status-sala";

    private static final Long USUARIO_ID = 1L;
    // Sempre no futuro, para nunca cair na validação de "data no passado"
    private static final LocalDate DATA = LocalDate.now().plusDays(1);

    @MockitoBean
    private UsuarioClient usuarioClient;

    @MockitoBean
    private SalaClient salaClient;

    @MockitoBean
    private SalaIntegracaoService salaIntegracaoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Long salaId;
    private String filaDeTeste;

    @BeforeEach
    void setUp() {
        salaId = 100_000L + ThreadLocalRandom.current().nextLong(900_000L);
        filaDeTeste = "teste.status-sala." + UUID.randomUUID();

        given(usuarioClient.buscarPorId(USUARIO_ID))
                .willReturn(new UsuarioRequest(USUARIO_ID, "teste", "teste@gmail.com"));
        given(salaClient.buscarPorId(anyLong()))
                .willAnswer(inv -> new SalaRequest(inv.<Long>getArgument(0), 1, 20, StatusSala.LIVRE));
        given(salaIntegracaoService.marcarSalaOcupada(anyLong())).willReturn(true);
    }

    @AfterEach
    void tearDown() {
        reservaRepository.deleteAll();
        rabbitAdmin.deleteQueue(filaDeTeste); // não falha se a fila nunca foi declarada
    }

    // ------------------------------------------------------------------ Kafka

    @Test
    @DisplayName("Deveria publicar no tópico Kafka booking-created o evento da reserva cadastrada")
    void deveriaPublicarEventoNoKafkaAoCadastrarReserva() {
        // ARRANGE
        ReservaRequest request = new ReservaRequest(salaId, DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), 5);

        // ACT
        reservaService.cadastrar(request, USUARIO_ID);

        // ASSERT
        // Estado: a reserva foi persistida
        assertEquals(1, reservaRepository.count());
        // Comportamento: o evento chegou ao tópico, com o conteúdo desserializado igual ao enviado
        List<ReservaRequest> eventos = lerEventosDoTopico(salaId, Duration.ofSeconds(15), true);
        assertEquals(List.of(request), eventos);
    }

    @Test
    @DisplayName("Não deveria publicar evento no Kafka quando o cadastro falha por conflito de horário")
    void naoDeveriaPublicarEventoQuandoHaConflitoDeHorario() {
        // ARRANGE - reserva existente 10:00-11:00 (gravada direto no banco: não gera evento)
        persistirReserva(StatusReserva.ATIVA);
        ReservaRequest conflitante = new ReservaRequest(salaId, DATA, LocalTime.of(10, 30), LocalTime.of(11, 30), 5);

        // ACT
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> reservaService.cadastrar(conflitante, USUARIO_ID));

        // ASSERT
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(1, reservaRepository.count());
        // Nada chegou ao tópico para essa sala (espera 3s antes de concluir que realmente não veio)
        assertTrue(lerEventosDoTopico(salaId, Duration.ofSeconds(3), false).isEmpty());
    }

    // --------------------------------------------------------------- RabbitMQ

    @Test
    @DisplayName("Deveria publicar o status OCUPADA na exchange direta ao confirmar reserva sem integração")
    void deveriaPublicarStatusNoRabbitMQAoConfirmarReservaSemIntegracao() {
        // ARRANGE
        // A exchange/fila reais pertencem ao room-ms. Aqui declaramos uma fila de teste ligada à mesma
        // exchange e routing key, para conseguir LER o que o booking-ms publicou.
        declararFilaDeTesteLigadaNaExchange();
        Reserva reserva = persistirReserva(StatusReserva.ATIVA_SEM_INTEGRACAO);

        // ACT
        reservaService.confirmarReservaSemIntegracao(reserva.getId(), USUARIO_ID);

        // ASSERT
        // Comportamento: chamada HTTP ao room-ms e mensagem de fato roteada até a fila
        then(salaClient).should().alterarStatusSala(eq(salaId), any(StatusSalaRequest.class));
        Message mensagem = rabbitTemplate.receive(filaDeTeste, 5_000);
        assertNotNull(mensagem, "Nenhuma mensagem chegou à fila: confira exchange, routing key e binding");
        assertTrue(new String(mensagem.getBody(), StandardCharsets.UTF_8).contains("OCUPADA"));
        // Estado: a reserva foi promovida para ATIVA
        assertEquals(StatusReserva.ATIVA, reservaRepository.findById(reserva.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Não deveria publicar no RabbitMQ quando outro usuário tenta confirmar a reserva")
    void naoDeveriaPublicarNoRabbitMQQuandoOutroUsuarioTentaConfirmar() {
        // ARRANGE
        declararFilaDeTesteLigadaNaExchange();
        Reserva reserva = persistirReserva(StatusReserva.ATIVA_SEM_INTEGRACAO);

        // ACT
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> reservaService.confirmarReservaSemIntegracao(reserva.getId(), 999L));

        // ASSERT
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertNull(rabbitTemplate.receive(filaDeTeste, 1_000), "Não deveria haver mensagem na fila");
        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO,
                reservaRepository.findById(reserva.getId()).orElseThrow().getStatus());
    }

    // ---------------------------------------------------------------- helpers

    private void declararFilaDeTesteLigadaNaExchange() {
        DirectExchange exchange = new DirectExchange(EXCHANGE_STATUS_SALA);
        Queue fila = new Queue(filaDeTeste, false, false, true);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(fila);
        rabbitAdmin.declareBinding(BindingBuilder.bind(fila).to(exchange).with(ROUTING_KEY_STATUS_SALA));
    }

    private Reserva persistirReserva(StatusReserva status) {
        Reserva reserva = new Reserva(DATA, LocalTime.of(10, 0), LocalTime.of(11, 0), 5);
        reserva.setUsuarioId(USUARIO_ID);
        reserva.setSalaId(salaId);
        reserva.setStatus(status);
        return reservaRepository.save(reserva);
    }

    /**
     * Lê o tópico desde o início com um consumidor de teste (group.id único, então não interfere em
     * ninguém) e devolve só os eventos da sala informada.
     *
     * @param pararAoEncontrar true: retorna assim que achar um evento; false: espera o timeout inteiro
     *                         (usado para provar que NENHUM evento foi publicado).
     */
    private List<ReservaRequest> lerEventosDoTopico(Long salaIdProcurado, Duration timeout, boolean pararAoEncontrar) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "teste-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Mesma família de (de)serializador usada pelo producer da aplicação (JsonSerializer)
        JacksonJsonDeserializer<ReservaRequest> valueDeserializer =
                new JacksonJsonDeserializer<>(ReservaRequest.class, false);
        valueDeserializer.addTrustedPackages("*");

        List<ReservaRequest> encontrados = new ArrayList<>();
        long limite = System.nanoTime() + timeout.toNanos();

        try (KafkaConsumer<String, ReservaRequest> consumer =
                     new KafkaConsumer<>(props, new StringDeserializer(), valueDeserializer)) {
            consumer.subscribe(List.of(TOPICO_BOOKING_CREATED));
            while (System.nanoTime() < limite && !(pararAoEncontrar && !encontrados.isEmpty())) {
                for (ConsumerRecord<String, ReservaRequest> registro : consumer.poll(Duration.ofMillis(300))) {
                    if (registro.value() != null && salaIdProcurado.equals(registro.value().salaId())) {
                        encontrados.add(registro.value());
                    }
                }
            }
        }
        return encontrados;
    }
}