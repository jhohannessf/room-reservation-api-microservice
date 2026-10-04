package br.com.jhohannesfreitas.booking_ms.controller;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.integration.AbstractIntegrationTest;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ReservaControllerCircuitBreakerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservaRepository reservaRepository;

    @MockitoBean
    private SalaClient salaClient;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @Value("${jwt.key}")
    private String jwtKey;

    @AfterEach
    void tearDown() {
        reservaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria acionar o fallback do Circuit Breaker quando o room-ms estiver fora do ar")
    void deveriaAcionarFallbackQuandoRoomMsEstiverForaDoAr() throws Exception {
        // ARRANGE
        Long usuarioId = 1L;
        Reserva reservaPendente = new Reserva(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), 5);
        reservaPendente.setUsuarioId(usuarioId);
        reservaPendente.setSalaId(10L);
        reservaPendente.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        Reserva reservaSalva = reservaRepository.save(reservaPendente);

        // Simula o room-ms fora do ar
        given(salaClient.alterarStatusSala(eq(10L), any()))
                .willThrow(new RuntimeException("room-ms indisponível"));

        String token = gerarTokenValido(usuarioId, "jhou@email.com");

        // ACT + ASSERT — responde 200: o fallback engole a falha, o cliente da API nunca vê o erro
        mockMvc.perform(patch("/api/v1/reservas/{id}", reservaSalva.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // O fallback chamou alterarStatusReserva(), que persiste ATIVA_SEM_INTEGRACAO
        Reserva reservaFinal = reservaRepository.findById(reservaSalva.getId()).orElseThrow();
        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, reservaFinal.getStatus());

        // A exceção interrompeu o fluxo antes da publicação no RabbitMQ
        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    private String gerarTokenValido(Long usuarioId, String email) {
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtKey.getBytes());
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + 900_000);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("authorities", List.of("ROLE_ESTUDANTE"))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(signingKey)
                .compact();
    }
}