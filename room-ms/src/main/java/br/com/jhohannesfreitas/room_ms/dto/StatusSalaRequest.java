package br.com.jhohannesfreitas.room_ms.dto;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;

public record StatusSalaRequest(
        StatusSala status
) {
}
