package br.com.jhohannesfreitas.user_ms.repository;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByPerfilNome(PerfilNome perfilNome);
}
