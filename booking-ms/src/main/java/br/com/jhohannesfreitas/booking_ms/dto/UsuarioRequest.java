package br.com.jhohannesfreitas.booking_ms.dto;

public record UsuarioRequest(
        Long id,
        String nome,
        String email
) {
}
