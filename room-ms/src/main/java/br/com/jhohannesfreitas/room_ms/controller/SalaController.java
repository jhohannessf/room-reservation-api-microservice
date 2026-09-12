package br.com.jhohannesfreitas.room_ms.controller;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import br.com.jhohannesfreitas.room_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.room_ms.service.SalaService;
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
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public ResponseEntity<List<SalaResponse>> listarSalas() {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.listarSalas());
    }

    @GetMapping("/listar-paginado")
    public ResponseEntity<Page<SalaResponse>> listarPorPagina(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(salaService.listarSalasPorPagina(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.buscarSalaPorId(id));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<SalaResponse> cadastrar(@RequestBody SalaRequest salaRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaService.cadastrar(salaRequest));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<SalaResponse> atualizar(@PathVariable Long id, @RequestBody @Valid SalaRequest salaRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.atualizar(id, salaRequest));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<SalaResponse> deletar(@PathVariable Long id) {
        salaService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/alterar-status/{id}")
    public ResponseEntity<SalaResponse> alterarStatus(@PathVariable Long id, @RequestBody @Valid StatusSalaRequest statusSalaRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(salaService.alterarStatus(id, statusSalaRequest.status()));
    }
}
