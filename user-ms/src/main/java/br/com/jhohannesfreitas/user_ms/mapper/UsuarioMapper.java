package br.com.jhohannesfreitas.user_ms.mapper;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;

public class UsuarioMapper {

    // Mapper de entrada: Transforma uma DTO em uma Entity
    public static Usuario toEntity(UsuarioRequest dto, String senhaHash) {
        return new Usuario(
                dto.nome(),
                dto.email(),
                senhaHash
        );
    }

    // Mapper de saída: Transforma uma Entity em um DTO
    public static UsuarioResponse toDto(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getAuthorities()
        );
    }
}
