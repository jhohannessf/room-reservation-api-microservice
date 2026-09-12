package br.com.jhohannesfreitas.user_ms.dto;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Collection<? extends GrantedAuthority> authorities
) {
}
