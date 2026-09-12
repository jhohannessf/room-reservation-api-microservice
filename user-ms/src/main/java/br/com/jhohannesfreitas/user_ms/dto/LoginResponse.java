package br.com.jhohannesfreitas.user_ms.dto;

import br.com.jhohannesfreitas.user_ms.domain.enums.TipoA2f;

import java.time.LocalDateTime;

public record LoginResponse(
        String token,
        LocalDateTime expirationTime,
        Boolean a2fRequired,
        TipoA2f tipoA2f
) {
}
