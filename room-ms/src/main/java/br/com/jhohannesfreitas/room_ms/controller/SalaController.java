package br.com.jhohannesfreitas.room_ms.controller;

import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import br.com.jhohannesfreitas.room_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.room_ms.service.SalaService;
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
@RequestMapping("/api/v1/salas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Salas", description = "Operações relacionadas às salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @Operation(
            summary = "Listar salas",
            description = """
                Retorna todas as salas cadastradas no sistema.

                Regras de negócio:
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de salas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
    })
    @GetMapping
    public ResponseEntity<List<SalaResponse>> listar() {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.listar());
    }

    @Operation(
            summary = "Listar salas por páginas",
            description = """
                Retorna uma lista paginada de salas cadastradas.

                É possível controlar a paginação utilizando os parâmetros:
                - page: número da página (inicia em 0);
                - size: quantidade de registros por página;
                - sort: campo utilizado para ordenação, seguido da direção (asc ou desc).
                - Necessário autenticação do usuário com token JWT.

                Exemplo:
                GET /api/v1/salas/listar-paginado?page=0&size=10&sort=numero,desc
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de salas retornada com sucesso por página"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @GetMapping("/listar-paginado")
    public ResponseEntity<Page<SalaResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(salaService.listarPaginado(pageable));
    }

    @Operation(
            summary = "Buscar sala por ID",
            description = """
                Busca uma sala pelo identificador informado.

                Regras de negócio:
                - A sala deve existir.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sala encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SalaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.buscarSalaPorId(id));
    }

    @Operation(
            summary = "Cadastrar sala",
            description = """
                Cadastra uma nova sala.

                Regras de negócio:
                - O número da sala deve ser único.
                - A capacidade deve ser maior que zero.
                - Necessário autenticação do usuário com token JWT.
                - Apenas perfil administrador para usuários podem cadastrar salas.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sala cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "409", description = "Já existe uma sala cadastrada com este número")
    })
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<SalaResponse> cadastrar(@RequestBody @Valid SalaRequest salaRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaService.cadastrar(salaRequest));
    }

    @Operation(
            summary = "Atualizar sala",
            description = """
                Atualiza os dados de uma sala.

                Regras de negócio:
                - A sala deve existir.
                - O número da sala deve continuar sendo único.
                - A capacidade deve ser maior que zero.
                - Necessário autenticação do usuário com token JWT.
                - Apenas perfil administrador para usuários podem atualizar salas.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sala atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada"),
            @ApiResponse(responseCode = "409", description = "Já existe outra sala cadastrada com este número")
    })
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<SalaResponse> atualizar(@PathVariable Long id, @RequestBody @Valid SalaRequest salaRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.atualizar(id, salaRequest));
    }

    @Operation(
            summary = "Excluir sala",
            description = """
                Remove uma sala do sistema.

                Regras de negócio:
                - A sala deve existir.
                - Necessário autenticação do usuário com token JWT.
                - Apenas perfil administrador para usuários podem atualizar salas.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sala removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        salaService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Alterar Status",
            description = """
                Alterar Status de uma sala.

                Regras de negócio:
                - A sala deve existir.
                - O Status informado deve estar cadastrado.
                - Necessário autenticação do usuário com token JWT.
                - Apenas perfil administrador para usuários podem atualizar salas.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sala atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada"),
    })
    @PatchMapping("/alterar-status/{id}")
    public ResponseEntity<SalaResponse> alterarStatus(@PathVariable Long id, @RequestBody @Valid StatusSalaRequest statusSalaRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.alterarStatus(id, statusSalaRequest.status()));
    }
}
