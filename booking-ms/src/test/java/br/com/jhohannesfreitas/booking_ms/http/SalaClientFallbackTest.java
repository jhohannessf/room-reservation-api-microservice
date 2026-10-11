package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

public class SalaClientFallbackTest {

    private final SalaClientFallback fallback = new SalaClientFallback();

    @Test
    @DisplayName("Deveria lançar 503 ao buscar sala quando o room-ms estiver indisponível")
    void deveriaLancar503AoBuscarSalaPorId() {
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> fallback.buscarPorId(1L));

        assertAll(
                () -> assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus()),
                () -> assertEquals("O Serviço room-ms está indisponível no momento.", ex.getMessage())
        );
    }

    @Test
    @DisplayName("Deveria lançar 503 ao alterar status da sala quando o room-ms estiver indisponível")
    void deveriaLancar503AoAlterarStatusSala() {
        // ARRANGE
        Long salaId = 1L;
        StatusSalaRequest statusSalaRequest = new StatusSalaRequest(StatusSala.LIVRE);

        // ACT + ASSERT
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
            () -> fallback.alterarStatusSala(salaId, statusSalaRequest));

        assertAll(
                () -> assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus()),
                () -> assertEquals("O Serviço room-ms está indisponível no momento.", ex.getMessage())
        );
    }
}

