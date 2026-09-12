package br.com.jhohannesfreitas.user_ms.dto;

import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import jakarta.validation.constraints.NotNull;

public record PerfilRequest(
        @NotNull
        PerfilNome perfilNome
) {
}
