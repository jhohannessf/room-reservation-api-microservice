package br.com.jhohannesfreitas.room_ms.repository;

import br.com.jhohannesfreitas.room_ms.domain.entity.Sala;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    Optional<Sala> findByNumero(Integer numero);

    boolean existsByNumero(Integer numero);

    Optional<Sala> findByNumeroAndIdNot(Integer numero, Long id); // Existe uma sala com este número e cujo ID é diferente
}
