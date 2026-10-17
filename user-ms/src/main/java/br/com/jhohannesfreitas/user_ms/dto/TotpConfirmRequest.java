package br.com.jhohannesfreitas.user_ms.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpConfirmRequest(
        @NotBlank
        String codigo
) {
}
