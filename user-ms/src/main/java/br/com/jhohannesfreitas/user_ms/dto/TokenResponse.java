package br.com.jhohannesfreitas.user_ms.dto;

import java.time.LocalDateTime;

public record TokenResponse(
        String token,
        LocalDateTime expiration
) {
}
