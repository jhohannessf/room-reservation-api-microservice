package br.com.jhohannesfreitas.user_ms.domain.entity;

import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "perfis")
public class Perfil implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PerfilNome perfilNome;

    public Perfil() {}

    public Perfil(PerfilNome perfilNome) {
        this.perfilNome = perfilNome;
    }

    public Long getId() {
        return id;
    }

    public PerfilNome getPerfilNome() {
        return perfilNome;
    }

    @Override
    public @Nullable String getAuthority() {
        return "ROLE_" + perfilNome;
    }
}
