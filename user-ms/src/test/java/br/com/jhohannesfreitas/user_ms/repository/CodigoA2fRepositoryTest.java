package br.com.jhohannesfreitas.user_ms.repository;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import br.com.jhohannesfreitas.user_ms.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CodigoA2fRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private CodigoA2fRepository codigoA2fRepository;

    @AfterEach
    void tearDown() {
        codigoA2fRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria retornar o código a2f quando buscar por UsuarioId e utilizado false")
    void findByUsuarioIdAndCodigoAndUtilizadoFalse() {
        // ARRANGE
        CodigoA2f codigoA2f = new CodigoA2f(
                10L,
                "123456",
                LocalDateTime.now().plusMinutes(60)
        );
        codigoA2fRepository.save(codigoA2f);

        // ACT
        Optional<CodigoA2f> resultado = codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(codigoA2f.getUsuarioId(), codigoA2f.getCodigo());

        // ASSERT
        assertTrue(resultado.isPresent());
        assertEquals(10L, resultado.get().getUsuarioId());
        assertEquals("123456", resultado.get().getCodigo());
    }

    @Test
    @DisplayName("Não deveria retornar código quando o código informado for inválido")
    void findByUsuarioIdAndCodigoAndUtilizadoFalseComCodigoInvalido() {
        // ARRANGE
        CodigoA2f codigoA2f = new CodigoA2f(
                10L,
                "123456",
                LocalDateTime.now().plusMinutes(60)
        );
        codigoA2fRepository.save(codigoA2f);

        // ACT
        Optional<CodigoA2f> resultado = codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(codigoA2f.getUsuarioId(), "CODIGO_INVALIDO");

        // ASSERT
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Não deveria encontrar código com usuarioId incorreto")
    void findByUsuarioIdAndCodigoAndUtilizadoFalseComUsuarioIdIncorreto() {
        // ARRANGE
        CodigoA2f codigoA2f = new CodigoA2f(
                10L,
                "ABC123",
                LocalDateTime.now().plusMinutes(60)
        );
        codigoA2fRepository.save(codigoA2f);

        // ACT
        Optional<CodigoA2f> resultado =
                codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(
                        20L, "ABC123"
                );

        // ASSERT
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Não deveria encontrar código já utilizado")
    void findByUsuarioIdAndCodigoAndUtilizadoFalseComCodigoJaUtilizado() {
        // ARRANGE
        CodigoA2f codigoA2f = new CodigoA2f(
                10L,
                "ABC123",
                LocalDateTime.now().plusMinutes(60)
        );
        codigoA2f.setUtilizado(true);
        codigoA2fRepository.save(codigoA2f);

        // ACT
        Optional<CodigoA2f> resultado =
                codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(
                        10L, "ABC123"
                );

        // ASSERT
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Não deveria encontrar código inexistente")
    void findByUsuarioIdAndCodigoAndUtilizadoFalseComCodigoInexistente() {
        // ACT
        Optional<CodigoA2f> resultado =
                codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(
                        10L, "XYZ789"
                );

        // ASSERT
        assertTrue(resultado.isEmpty());
    }
}