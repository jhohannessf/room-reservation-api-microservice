package br.com.jhohannesfreitas.booking_ms.dto;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;

public record StatusSalaRequest(
        StatusSala status
) {
}
