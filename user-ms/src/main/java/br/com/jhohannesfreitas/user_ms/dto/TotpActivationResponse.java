package br.com.jhohannesfreitas.user_ms.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpActivationResponse(
        @NotBlank
        String qrCodeUrl,
        @NotBlank
        String secret
) {
}
