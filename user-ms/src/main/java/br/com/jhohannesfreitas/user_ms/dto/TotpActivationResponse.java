package br.com.jhohannesfreitas.user_ms.dto;

public record TotpActivationResponse(
        String qrCodeUrl,
        String secret
) {
}
