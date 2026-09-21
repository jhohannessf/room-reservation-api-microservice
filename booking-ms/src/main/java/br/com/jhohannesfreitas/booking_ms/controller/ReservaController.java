package br.com.jhohannesfreitas.booking_ms.controller;

import br.com.jhohannesfreitas.booking_ms.domain.entity.UserPrincipal;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import br.com.jhohannesfreitas.booking_ms.mapper.ReservaMapper;
import br.com.jhohannesfreitas.booking_ms.service.ReservaService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
@RequestMapping("api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final RabbitTemplate rabbitTemplate;

    public ReservaController(ReservaService reservaService, ReservaService reservaService1, RabbitTemplate rabbitTemplate) {
        this.reservaService = reservaService1;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listar() {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.listar());
    }

    @GetMapping("/listar-paginado")
    public ResponseEntity<Page<ReservaResponse>> listarPaginado(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.listarPaginado(pageable));
    }

    @GetMapping("/sala/{id}")
    public ResponseEntity<Page<ReservaResponse>> listarReservaPorSalaEIntervalo(@PathVariable Long id, @RequestParam LocalDate inicio, @RequestParam LocalDate fim, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.listarPorSalaEIntervalo(id, inicio, fim, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.buscarPorId(id));
    }

    // #usuarioId == authentication.principal.id or hasRole('ADMINISTRADOR')
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ReservaResponse> cadastrar(@RequestBody @Valid ReservaRequest reservaRequest, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.cadastrar(reservaRequest, usuarioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaResponse> atualizar(@PathVariable Long id, @RequestBody @Valid ReservaRequest reservaRequest, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        return ResponseEntity.status(HttpStatus.OK).body(reservaService.atualizar(id, reservaRequest, usuarioId));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long usuarioId = userPrincipal.getId();
        reservaService.deletar(id, usuarioId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

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

    // Teste load balance
    @GetMapping("/porta")
    public String retornarPorta(@Value("${local.server.port}") String porta) {
        return String.format("Requisição respondida pela instância executando na porta: %s", porta);
    }
}
