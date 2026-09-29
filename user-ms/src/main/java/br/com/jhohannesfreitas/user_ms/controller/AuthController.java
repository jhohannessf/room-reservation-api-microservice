package br.com.jhohannesfreitas.user_ms.controller;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.dto.*;
import br.com.jhohannesfreitas.user_ms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autorização", description = "Operações relacionadas à autorização do sistema.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Registrar usuário para a autenticação",
            description = """
                Registra um novo usuário no sistema.

                Regras de negócio:
                - O e-mail deve ser único.
                - Não é permitido cadastrar dois usuários com o mesmo e-mail.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "409", description = "Já existe um usuário cadastrado com este e-mail")
    })
    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid UsuarioRequest usuarioRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(usuarioRequest));
    }

    @Operation(
            summary = "Faz login do usuário para a geração do token",
            description = """
                Faz login do usuário no sistema.

                Regras de negócio:
                - O e-mail deve existir.
                - A senha deve ser válida.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "409", description = "Já existe um usuário cadastrado com este e-mail")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.login(loginRequest));
    }

    @Operation(
            summary = "Faz a verificação do tipo de A2F",
            description = """
                Verifica se a A2F está ativa e qual o tipo de A2F para o usuário no sistema.

                Regras de negócio:
                - O e-mail do usuário deve existir.
                - A2F deve estar ativa no cadastro do usuário
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
    })
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verificarA2f(@RequestBody @Valid CodigoA2fRequest codigoA2fRequest) {
        LoginResponse loginResponse = authService.verificarA2f(codigoA2fRequest);

        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    @Operation(
            summary = "Faz a inicialização da autenticação por TOTP",
            description = """
                Faz a inicialização da autenticação por TOTP para o usuário no sistema.

                Regras de negócio:
                - O usuário deve existir.
                - A2F deve estar desativada no cadastro do usuário
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("2fa/totp/setup")
    public ResponseEntity<TotpActivationResponse> iniciarTotp(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        TotpActivationResponse response = authService.iniciarAtivacaoTotp(usuario.getId());

        return ResponseEntity.ok(response); // Forma resumida
    }

    @Operation(
            summary = "Faz a confirmação da autenticação por TOTP",
            description = """
                Faz a confirmação da autenticação por TOTP para o usuário no sistema.

                Regras de negócio:
                - O usuário deve existir.
                - Deve ser passado apenas o código no body da requisição, como String.
                - Necessário autenticação do usuário com token JWT.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/totp/confirm")
    public ResponseEntity<Void> confirmarTotp(@RequestBody TotpConfirmRequest totpConfirmRequest, Authentication authentication) {
        System.out.println("AUTHENTICATION: " + authentication);
        Usuario usuario = (Usuario) authentication.getPrincipal();
        authService.confirmarAtivacaoTotp(usuario.getId(), totpConfirmRequest.codigo());

        return ResponseEntity.noContent().build(); // Forma resumida
    }

    @Operation(
            summary = "Faz o logout(saída) do usuário.",
            description = """
               Faz o logout(saída) do usuário.
                
                Regras de negócio:
                - O usuário autenticado deve estar logado.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
