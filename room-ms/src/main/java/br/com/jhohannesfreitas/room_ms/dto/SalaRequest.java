package br.com.jhohannesfreitas.room_ms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SalaRequest(
        @NotNull
        Integer numero,

        @NotNull
        @Positive
        Integer capacidade
) {
}
