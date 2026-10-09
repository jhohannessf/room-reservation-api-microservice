package br.com.jhohannesfreitas.room_ms.dto;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import jakarta.validation.constraints.NotNull;

public record StatusSalaRequest(
        @NotNull(message = "Status é obrigatório.")
        StatusSala status
) {
}
