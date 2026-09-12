package br.com.jhohannesfreitas.user_ms.dto;

public record LoginRequest(
        String email,
        String senha
) {
}
