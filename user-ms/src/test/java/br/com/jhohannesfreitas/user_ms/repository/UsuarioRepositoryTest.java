package br.com.jhohannesfreitas.user_ms.repository;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria retornar o usuário quando o email existir")
    void findByEmail() {
        // ARRANGE
        Usuario usuario = new Usuario("Teste", "teste@email.com", "senha123");
        usuarioRepository.save(usuario);

        // ACT
        Optional<Usuario> resultado = usuarioRepository.findByEmail(usuario.getEmail());

        // ASSERT
        assertTrue(resultado.isPresent());
        assertEquals("teste@email.com", resultado.get().getEmail());
    }

    @Test
    @DisplayName("Deveria retornar vazio quando o email não existir")
    void findByEmailReturnVazio() {
        // ARRANGE
        String email = "teste@email.com";

        // ACT
        Optional<Usuario> resultado = usuarioRepository.findByEmail(email);

        // ASSERT
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deveria retornar o usuário quando o email existir ignorando maiúsculas e minúsculas")
    void findByEmailIgnoreCase() {
        // ARRANGE
        Usuario usuario = new Usuario("Teste", "teste@email.com", "senha123");
        usuarioRepository.save(usuario);

        // ACT
        Optional<Usuario> resultado = usuarioRepository.findByEmailIgnoreCase("TESTE@EMAIL.COM");

        // ASSERT
        assertTrue(resultado.isPresent());
        assertEquals("teste@email.com", resultado.get().getEmail());
    }

    @Test
    @DisplayName("Deveria retornar vazio quando o email não existir, mesmo ignorando maiúsculas e minúsculas")
    void findByEmailIgnoreCaseReturnVazio() {
        // ARRANGE
        String email = "TESTE@EMAIL.COM";

        // ACT
        Optional<Usuario> resultado = usuarioRepository.findByEmailIgnoreCase(email);

        // ASSERT
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deveria verificar se existe um usuário com email informado ignorando maiúsculas e minúsculas, retornando true")
    void existsByEmailIgnoreCase() {
        // ARRANGE
        Usuario usuario = new Usuario(
                "Teste",
                "teste@email.com",
                "senha123"
        );
        usuarioRepository.save(usuario);

        // ACT
        boolean existeUsuario = usuarioRepository.existsByEmailIgnoreCase("TESTE@EMAIL.COM");

        // ASSERT
        assertTrue(existeUsuario);
    }

    @Test
    @DisplayName("Deveria verificar se existe um usuário com email informado ignorando letras maiúsculas e minúsculas, retornando false")
    void existsByEmailIgnoreCaseReturnFalse() {
        // ARRANGE
        String email = "teste@email.com";

        // ACT
        boolean existeUsuario = usuarioRepository.existsByEmailIgnoreCase(email);

        // ASSERT
        assertFalse(existeUsuario);
    }

    @Test
    @DisplayName("Deveria retornar true quando outro usuário (id diferente) já possui o email informado")
    void existsByEmailAndIdNot() {
        // ARRANGE
        Usuario usuario = usuarioRepository.save(new Usuario("Teste", "teste@email.com", "senha123"));
        Usuario outroUsuario = usuarioRepository.save(new Usuario("Outro Teste", "outroteste@email.com", "senha123"));

        // ACT
        // outroUsuario tenta usar um e-mail que já pertence a usuario
        boolean existeUsuario = usuarioRepository.existsByEmailAndIdNot("teste@email.com", outroUsuario.getId());

        // ASSERT
        assertTrue(existeUsuario);
    }

    @Test
    @DisplayName("Deveria retornar false quando o único usuário com o email é o próprio id informado")
    void existsByEmailAndIdNotReturnFalse() {
        // ARRANGE
        Usuario usuario = usuarioRepository.save(new Usuario("Teste", "teste@email.com", "senha123"));

        // ACT
        boolean existeUsuario = usuarioRepository.existsByEmailAndIdNot(usuario.getEmail(), usuario.getId());

        // ASSERT
        assertFalse(existeUsuario);
    }
}