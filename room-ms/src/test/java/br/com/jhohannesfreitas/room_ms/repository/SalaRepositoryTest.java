package br.com.jhohannesfreitas.room_ms.repository;

import br.com.jhohannesfreitas.room_ms.domain.entity.Sala;
import br.com.jhohannesfreitas.room_ms.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SalaRepository salaRepository;

    @AfterEach
    void tearDown() {
        salaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria verificar se existe sala com número informado e retornar um boolean true")
    void existsByNumero() {
        // ARRANGE
        Sala sala = new Sala(
                1,
                20
        );

        // Salva
        salaRepository.save(sala);

        // ACT
        Boolean existeSala = salaRepository.existsByNumero(sala.getNumero());

        // ASSERT
        assertTrue(existeSala);

    }

    @Test
    @DisplayName("Deveria verificar se existe sala com número informado e retornar um boolean false")
    void existsByNumeroReturnFalse() {
        // ARRANGE
        Integer numero = 99;

        // ACT
        Boolean existeSala = salaRepository.existsByNumero(numero);

        // ASSERT
        assertFalse(existeSala);

    }

    @Test
    @DisplayName("Deveria buscar sala por número, ignorando o mesmo ID informado e retornando vazio")
    void findByNumeroAndIdNot() {
        // ARRANGE
        Sala sala = salaRepository.save(new Sala(1, 20));

        // ACT
        Optional<Sala> buscarSala = salaRepository.findByNumeroAndIdNot(sala.getNumero(), sala.getId());

        // ASSERT
        assertThat(buscarSala).isEmpty();

    }

    @Test
    @DisplayName("Deveria buscar sala por número quando o ID informado for diferente")
    void findByNumeroAndIdNotReturnOtherSala() {
        // ARRANGE
        Sala sala = salaRepository.save(new Sala(1, 20));

        // ACT
        Optional<Sala> buscarSala = salaRepository.findByNumeroAndIdNot(sala.getNumero(), -1L);

        // ASSERT
        assertThat(buscarSala)
                .isPresent()
                .get()
                .extracting(Sala::getId)
                .isEqualTo(sala.getId());

    }
}