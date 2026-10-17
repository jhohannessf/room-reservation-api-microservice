package br.com.jhohannesfreitas.user_ms.repository;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PerfilRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PerfilRepository perfilRepository;

    @AfterEach
    void tearDown() {
        perfilRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria encontrar perfil pelo nome")
    void findByPerfilNome() {
        // ARRANGE - Não precisa criar o perfil, pois a tabela já é criada com valores definidos ao subir o projeto

        // ACT
        Optional<Perfil> resultado = perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE);

        // ASSERT
        assertTrue(resultado.isPresent());
        assertEquals(PerfilNome.ESTUDANTE, resultado.get().getPerfilNome());

    }
}