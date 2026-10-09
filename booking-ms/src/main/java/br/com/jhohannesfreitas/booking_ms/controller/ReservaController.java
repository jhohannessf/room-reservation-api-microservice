package br.com.jhohannesfreitas.booking_ms.controller;

import br.com.jhohannesfreitas.booking_ms.domain.entity.UserPrincipal;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import br.com.jhohannesfreitas.booking_ms.mapper.ReservaMapper;
import br.com.jhohannesfreitas.booking_ms.service.ReservaService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservas", description = "Operações relacionadas às reservas de salas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Operation(
            summary = "Listar reservas",
            description = """
                Retorna a lista de todas as reservas cadastradas no sistema.
                
                Observações:
                - Apenas reservas cadastradas são retornadas.
                - O retorno pode conter reservas com status ATIVA ou CANCELADA.
                - Caso não existam reservas cadastradas, será retornada uma lista vazia.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listar() {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.listar());
    }

    @Operation(
            summary = "Listar reservas por páginas.",
            description = """
                Retorna uma lista paginada de reservas cadastradas.

                É possível controlar a paginação utilizando os parâmetros:
                - page: número da página (inicia em 0);
                - size: quantidade de registros por página;
                - sort: campo utilizado para ordenação, seguido da direção (asc ou desc).
                - Necessário autenticação do usuário com token JWT.

                Exemplo:
                GET /api/v1/reservas/paginado?page=0&size=10&sort=data,desc
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas retornada com sucesso por página"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @GetMapping("/listar-paginado")
    public ResponseEntity<Page<ReservaResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.listarPaginado(pageable));
    }

    @Operation(
            summary = "Listar reservas por sala e intervalo de datas",
            description = """
                Retorna uma lista paginada de reservas ativas de uma sala
                dentro de um intervalo de datas informado.

                Parâmetros obrigatórios:
                - salaId: identificador da sala;
                - inicio: data inicial do intervalo (yyyy-MM-dd);
                - fim: data final do intervalo (yyyy-MM-dd).
                - Necessário autenticação do usuário com token JWT.

                Também é possível utilizar paginação e ordenação através dos
                parâmetros page, size e sort.

                Exemplo:
                GET /api/v1/reservas/sala/1?inicio=2026-07-01&fim=2026-07-31&page=0&size=10&sort=data,asc
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas por sala e intervalo retornada com sucesso por página"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @GetMapping("/sala/{id}")
    public ResponseEntity<Page<ReservaResponse>> listarReservaPorSalaEIntervalo(@PathVariable Long id, @RequestParam LocalDate inicio, @RequestParam LocalDate fim, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.listarPorSalaEIntervalo(id, inicio, fim, pageable));
    }

    @Operation(
            summary = "Buscar reserva por ID",
            description = """
                Busca uma reserva pelo seu identificador.
                
                Regras de negócio:
                - A reserva deve existir.
                - Caso o ID informado não exista, será retornado HTTP 404 (Not Found).
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.buscarPorId(id));
    }

    @Operation(
            summary = "Cadastrar uma nova reserva",
            description = """
                Cadastra uma nova reserva no sistema.
                
                Regras de negócio:
                
                - Não é permitido reservar datas no passado.
                - O horário da reserva deve estar entre 08:00 e 18:00.
                - A quantidade de pessoas não pode exceder a capacidade da sala.
                - Não é permitido haver reservas sobrepostas para a mesma sala.
                - Reservas utilizam intervalo semiaberto [início, fim).
                - Necessário autenticação do usuário com token JWT.
                - O usuário não pode cadastrar reservas para outros usuários, a reserva fica vinculada ao ID dele.
                
                  Exemplo:
                  Reserva existente: 10:00 às 12:00
                  Nova reserva: 12:00 às 14:00
                  Resultado: permitido.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da reserva inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuário ou sala não encontrados"),
            @ApiResponse(responseCode = "409", description = "Conflito de horário ou regra de negócio")
    })
    // #usuarioId == authentication.principal.id or hasRole('ADMINISTRADOR')
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ReservaResponse> cadastrar(@RequestBody @Valid ReservaRequest reservaRequest, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.cadastrar(reservaRequest, usuarioId));
    }

    @Operation(
            summary = "Atualizar reserva",
            description = """
                Atualiza uma reserva existente.
        
                Regras de negócio:
                - A reserva deve existir.
                - O usuário informado deve existir.
                - A sala informada deve existir.
                - A sala deve estar com status LIVRE.
                - Não é permitido alterar a reserva para uma data no passado.
                - O horário da reserva deve estar entre 08:00 e 18:00.
                - O horário inicial deve ser anterior ao horário final.
                - A quantidade de pessoas não pode exceder a capacidade da sala.
                - Não é permitido haver conflito de horários com outras reservas ATIVAS da mesma sala.
                - Na atualização, a própria reserva é desconsiderada na verificação de conflito de horários.
                - É permitido que uma reserva termine exatamente no horário em que outra se inicia (intervalo semiaberto [início, fim)).
                - Necessário autenticação do usuário com token JWT.
                - O usuário não pode atualizar a reserva de outros usuários, somente a reserva vinculada ao ID dele.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Reserva, usuário ou sala não encontrados"),
            @ApiResponse(responseCode = "409", description = "Conflito de horário ou violação de regra de negócio")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<ReservaResponse> atualizar(@PathVariable Long id, @RequestBody @Valid ReservaRequest reservaRequest, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.atualizar(id, reservaRequest, usuarioId));
    }

    @Operation(
            summary = "Excluir reserva",
            description = """
                Remove uma reserva do sistema.
                
                Regras de negócio:
                - A reserva deve existir.
                - Caso o ID informado não exista, será retornado HTTP 404 (Not Found).
                - Quando a exclusão for realizada com sucesso, será retornado HTTP 204 (No Content).
                - Necessário autenticação do usuário com token JWT.
                - O usuário não pode deletar a reserva de outros usuários, somente a reserva vinculada ao ID dele.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reserva removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        reservaService.deletar(id, usuarioId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Confirmar reserva sem integração",
            description = """
                Confirma reserva sem integração mudando o status e atualizando o status da sala como fallback.

                Regras de negócio:
                - A reserva deve existir.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada"),
    })
    @PatchMapping("/{id}")
    @CircuitBreaker(name = "atualizaSala", fallbackMethod = "salaAtualizadaComIntegracaoPendente")
    public void confirmarReservaSemIntegracao(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        reservaService.confirmarReservaSemIntegracao(id, usuarioId);
    }

    // A assinatura tem que receber os MESMOS parâmetros, mais a Exceção no final.
    public void salaAtualizadaComIntegracaoPendente(Long id, Authentication authentication, Throwable t) {
        // O Throwable 't' captura o erro
        System.out.println("Room-ms fora do ar! Motivo: " + t.getMessage());

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        assert userPrincipal != null;
        Long usuarioId = userPrincipal.getId();
        reservaService.alterarStatusReserva(id, usuarioId);
    }

    @Operation(
            summary = "Retorna mensagem com a porta",
            description = """
                Retorna uma mensagem com a porta utilizada naquele momento como teste de load balance.
                
                Observações:
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensagem com a porta retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    // Teste load balance
    @GetMapping("/porta")
    public String retornarPorta(@Value("${local.server.port}") String porta) {
        return String.format("Requisição respondida pela instância executando na porta: %s", porta);
    }
}
