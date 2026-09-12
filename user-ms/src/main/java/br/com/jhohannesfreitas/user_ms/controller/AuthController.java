package br.com.jhohannesfreitas.user_ms.controller;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.dto.*;
import br.com.jhohannesfreitas.user_ms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid UsuarioRequest usuarioRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(usuarioRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.login(loginRequest));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verificarA2f(@RequestBody @Valid CodigoA2fRequest codigoA2fRequest) {
        LoginResponse loginResponse = authService.verificarA2f(codigoA2fRequest);

        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("2fa/totp/setup")
    public ResponseEntity<TotpActivationResponse> iniciarTotp(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        TotpActivationResponse response = authService.iniciarAtivacaoTotp(usuario.getId());

        return ResponseEntity.ok(response); // Forma resumida
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/totp/confirm")
    public ResponseEntity<Void> confirmarTotp(@RequestBody TotpConfirmRequest totpConfirmRequest, Authentication authentication) {
        System.out.println("AUTHENTICATION: " + authentication);
        Usuario usuario = (Usuario) authentication.getPrincipal();
        authService.confirmarAtivacaoTotp(usuario.getId(), totpConfirmRequest.codigo());

        return ResponseEntity.noContent().build(); // Forma resumida
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
