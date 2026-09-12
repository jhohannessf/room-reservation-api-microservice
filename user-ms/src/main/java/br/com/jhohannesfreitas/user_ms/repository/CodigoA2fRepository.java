package br.com.jhohannesfreitas.user_ms.repository;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoA2fRepository extends JpaRepository<CodigoA2f, Long> {

    Optional<CodigoA2f> findByUsuarioIdAndCodigoAndUtilizadoFalse(Long usuarioId, String codigo);
}
