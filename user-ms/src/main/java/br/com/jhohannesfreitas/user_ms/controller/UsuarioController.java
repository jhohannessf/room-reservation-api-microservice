package br.com.jhohannesfreitas.user_ms.controller;

import br.com.jhohannesfreitas.user_ms.dto.PerfilRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;
import br.com.jhohannesfreitas.user_ms.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuários", description = "Operações relacionadas aos usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(
            summary = "Listar usuários",
            description = """
                Retorna todas as salas cadastradas no sistema.

                Regras de negócio:
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listarTodosUsuarios());
    }

    @Operation(
            summary = "Listar usuários por páginas",
            description = """
                Retorna uma lista paginada de usuários cadastrados.

                É possível controlar a paginação utilizando os parâmetros:
                - page: número da página (inicia em 0);
                - size: quantidade de registros por página;
                - sort: campo utilizado para ordenação, seguido da direção (asc ou desc).
                - Necessário autenticação do usuário com token JWT.

                Exemplo:
                GET /api/v1/usuarios/listar-paginado?page=0&size=10&sort=nome,desc
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso por página"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/listar-paginado")
    public ResponseEntity<Page<UsuarioResponse>> listarPaginado(Pageable pageable){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listarUsuariosPorPagina(pageable));
    }

    @Operation(
            summary = "Busca usuários por id",
            description = """
                Busca um usuário pelo identificador informado.

                Regras de negócio:
                - O usuário deve existir.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.buscarUsuarioPorId(id));
    }

    @Operation(
            summary = "Cadastrar usuário",
            description = """
                Cadastra um novo usuário no sistema.

                Regras de negócio:
                - O e-mail deve ser único.
                - Não é permitido cadastrar dois usuários com o mesmo e-mail.
                - Necessário autenticação do usuário com token JWT. Se for primeiro cadastro, use o endpoint de auth/registrar.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "409", description = "Já existe um usuário cadastrado com este e-mail")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioRequest usuarioRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.cadastrar(usuarioRequest));
    }

    @Operation(
            summary = "Atualizar usuário",
            description = """
                Atualiza os dados de um usuário.

                Regras de negócio:
                - O usuário deve existir.
                - O e-mail deve continuar sendo único.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Já existe outro usuário cadastrado com este e-mail")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest usuarioRequest){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.atualizar(id, usuarioRequest));
    }

    @Operation(
            summary = "Excluir usuário",
            description = """
                Remove um usuário do sistema.

                Regras de negócio:
                - O usuário deve existir.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMINISTRADOR')") //O id recebido na URL deve ser igual ao ID do usuário autenticado OU OU o usuário possui a role ADMINISTRADOR.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id){
        usuarioService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Adicional perfil de usuário",
            description = """
                Adiciona um perfil já cadastrado ao usuário do sistema.

                Regras de negócio:
                - O perfil deve estar cadastrado, ou seja, deve existir.
                - Necessário autenticação do usuário com token JWT.
                - Apenas perfil administrador para usuários podem adicionar perfis.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Perfil no usuário adicionado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/adicionar-perfil/{id}")
    public ResponseEntity<UsuarioResponse> adicionarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest perfilRequest){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.adicionarPerfil(id, perfilRequest));
    }

    @Operation(
            summary = "Excluir perfil de usuário",
            description = """
                Remove um perfil no usuário do sistema.

                Regras de negócio:
                - O perfil no usuário deve existir.
                - Necessário autenticação do usuário com token JWT.
                - Apenas perfil administrador para usuários podem remover perfis.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Perfil no usuário removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/remover-perfil/{id}")
    public ResponseEntity<UsuarioResponse> removerPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest perfilRequest){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(usuarioService.removerPerfil(id, perfilRequest));
    }

    @Operation(
            summary = "Ativa a A2F para o usuário",
            description = """
                Ativa a A2F para o usuário do sistema.

                Regras de negócio:
                - O usuário deve existir.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "A2F foi adicionado com sucesso para o usuário"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMINISTRADOR')")
    @PatchMapping("/ativar-a2f/{id}")
    public ResponseEntity<Void> ativarA2f(@PathVariable Long id) {
        usuarioService.ativarA2f(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
