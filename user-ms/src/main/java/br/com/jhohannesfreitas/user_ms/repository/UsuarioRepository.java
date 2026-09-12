package br.com.jhohannesfreitas.user_ms.repository;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailAndIdNot(String email, Long id); // "Retorne verdadeiro se existir um usuário com este e-mail ONDE o ID seja diferente deste ID que estou passando".
}
