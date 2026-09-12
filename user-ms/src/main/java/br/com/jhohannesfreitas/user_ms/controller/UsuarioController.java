package br.com.jhohannesfreitas.user_ms.controller;

import br.com.jhohannesfreitas.user_ms.dto.PerfilRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;
import br.com.jhohannesfreitas.user_ms.service.UsuarioService;
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
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listarTodosUsuarios());
    }

    @GetMapping("/listar-paginado")
    public ResponseEntity<Page<UsuarioResponse>> listarPaginado(Pageable pageable){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listarUsuariosPorPagina(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.buscarUsuarioPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioRequest usuarioRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.cadastrar(usuarioRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest usuarioRequest){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.atualizar(id, usuarioRequest));
    }

    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMINISTRADOR')") //O id recebido na URL deve ser igual ao ID do usuário autenticado OU OU o usuário possui a role ADMINISTRADOR.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id){
        usuarioService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/adicionar-perfil/{id}")
    public ResponseEntity<UsuarioResponse> adicionarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest perfilRequest){
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.adicionarPerfil(id, perfilRequest));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/remover-perfil/{id}")
    public ResponseEntity<UsuarioResponse> removerPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest perfilRequest){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(usuarioService.removerPerfil(id, perfilRequest));
    }

    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMINISTRADOR')")
    @PatchMapping("/ativar-a2f/{id}")
    public ResponseEntity<Void> ativarA2f(@PathVariable Long id) {
        usuarioService.ativarA2f(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
