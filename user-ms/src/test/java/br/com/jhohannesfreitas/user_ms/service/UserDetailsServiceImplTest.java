package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Deveria retornar usuário quando e-mail existir")
    void deveriaRetornarUsuarioQuandoEmailExistir() {
        // ARRANGE
        String email = "teste@email.com";
        Usuario usuario = new Usuario();

        given(usuarioRepository.findByEmailIgnoreCase(email))
                .willReturn(Optional.of(usuario));

        // ACT
        Usuario resposta = (Usuario) userDetailsService.loadUserByUsername(email);

        // ASSERT
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should()
                .findByEmailIgnoreCase(email);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertSame(usuario, resposta);
    }

    @Test
    @DisplayName("Deveria lançar exceção quando usuário não existir")
    void deveriaLancarExcecaoQuandoUsuarioNaoExistir() {
        // ARRANGE
        String email = "inexistente@email.com";

        given(usuarioRepository.findByEmailIgnoreCase(email))
                .willReturn(Optional.empty());

        // ACT + ASSERT
        UsernameNotFoundException excecao = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email)
        );

        // ASSERT
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should()
                .findByEmailIgnoreCase(email);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertEquals(
                "O usuário não foi encontrado!",
                excecao.getMessage()
        );

    }
}