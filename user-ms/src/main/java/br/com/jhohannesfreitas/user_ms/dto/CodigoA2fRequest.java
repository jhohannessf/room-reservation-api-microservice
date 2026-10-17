package br.com.jhohannesfreitas.user_ms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CodigoA2fRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String codigo
) {
}
