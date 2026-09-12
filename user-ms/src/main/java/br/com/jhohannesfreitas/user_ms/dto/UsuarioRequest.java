package br.com.jhohannesfreitas.user_ms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank
        String nome,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min = 6)
        String senha
) {
}
