package br.com.jhohannesfreitas.user_ms.mapper;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioMapperTest {

    @Test
    @DisplayName("Deveria mapear UsuarioRequest para entidade Usuario corretamente")
    void deveriaMapearUsuarioRequestParaEntidadeQuandoDadosForemCorretos() {
        // Arrange
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "teste",
                "teste@gmail.com",
                "123"
        );
        String senhaHash = "123@123";

        // ACT
        Usuario usuario = UsuarioMapper.toEntity(usuarioRequest, senhaHash);

        // ASSERT
        assertAll(
                () -> assertEquals(usuarioRequest.nome(), usuario.getNome()),
                () -> assertEquals(usuarioRequest.email(), usuario.getEmail()),
                () -> assertEquals(senhaHash, usuario.getPassword()),
                () -> assertNull(usuario.getId()) // ainda não persistida, id só existe após o save()

        );
    }

    @Test
    @DisplayName("Deveria mapear entidade Usuario para UsuarioResponse corretamente")
    void deveriaMapearUsuarioParaResponseQuandoDadosForemCorretos() {
        // ARRANGE
        Usuario usuario = new Usuario(
                "teste",
                "teste@gmail.com",
                "123"
        );
        ReflectionTestUtils.setField(usuario, "id", 100L);
        usuario.adicionarPerfil(new Perfil(PerfilNome.ESTUDANTE));

        // ACT
        UsuarioResponse usuarioResponse = UsuarioMapper.toDto(usuario);

        // ASSERT
        assertAll(
                () -> assertEquals(usuario.getId(), usuarioResponse.id()),
                () -> assertEquals(usuario.getNome(), usuarioResponse.nome()),
                () -> assertEquals(usuario.getEmail(), usuarioResponse.email()),
                () -> assertEquals(usuario.getAuthorities(), usuarioResponse.authorities()),
                () -> assertFalse(usuarioResponse.authorities().isEmpty()) // prova que não é uma lista vazia por acidente
        );
    }
}