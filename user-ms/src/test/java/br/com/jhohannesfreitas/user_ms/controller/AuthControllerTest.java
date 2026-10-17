package br.com.jhohannesfreitas.user_ms.controller;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.TipoA2f;
import br.com.jhohannesfreitas.user_ms.dto.*;
import br.com.jhohannesfreitas.user_ms.infra.config.JwtAuthenticationFilter;
import br.com.jhohannesfreitas.user_ms.infra.config.OAuth2LoginSuccessHandler;
import br.com.jhohannesfreitas.user_ms.infra.config.SecurityConfiguration;
import br.com.jhohannesfreitas.user_ms.infra.config.TokenProvider;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TokenProvider.class})
class AuthControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deveria retornar 201 CREATED ao registrar usuário quando dados forem válidos")
    void deveriaRetornar201AoRegistrarUsuarioQuandoDadosForemValidos() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste",
                "teste@email.com",
                "senha123"
        );
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        given(authService.registrar(usuarioRequest)).willReturn(usuarioResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.email").value("teste@email.com"));

    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao registrar novo usuário quando dados corpo da requisição for inválido")
    void deveriaRetornar400AoRegistrarUsuarioQuandoCorpoRequisicaoForInvalido() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                null,
                "teste@email.com",
                "senha123"
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao registrar novo usuário quando o service lançar exceção de usuário com este email já existe")
    void deveriaRetornar409AoRegistrarUsuarioQuandoEmailJaExiste() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste",
                "teste@email.com",
                "senha123"
        );

        // ACT + ASSERT
        when(authService.registrar(any(UsuarioRequest.class)))
                .thenThrow(new RegraNegocioException("Usuário já cadastrado com este e-mail",
                        HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar 200 ok com dados do token ao efetuar login quando A2F estiver desativada")
    void deveriaEfetuarloginRetornandoTokenQuandoA2fEstiverDesativada() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(
                "teste@email.com",
                "senha123"
        );
        LoginResponse loginResponse = createLoginResponseA2fDesativada();

        given(authService.login(loginRequest)).willReturn(loginResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(loginRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.a2fRequired").value(false))
                .andExpect(jsonPath("$.tipoA2f").value("DESATIVADA"));

    }

    @Test
    @DisplayName("Deveria retornar 200 ok sem dados token ao efetuar login quando A2F estiver ativada para o tipo email")
    void deveriaSolicitarAutenticaçãoPorEmailQuandoA2fEstiverAtivadaParaTipoEmail() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(
                "teste@email.com",
                "senha123"
        );
        LoginResponse loginResponse = createLoginResponseA2fAtivada(TipoA2f.EMAIL);

        given(authService.login(loginRequest)).willReturn(loginResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(loginRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(nullValue()))
                .andExpect(jsonPath("$.a2fRequired").value(true))
                .andExpect(jsonPath("$.tipoA2f").value("EMAIL"));

    }

    @Test
    @DisplayName("Deveria retornar 200 ok sem dados token ao efetuar login quando A2F estiver ativada para o tipo Authenticator")
    void deveriaSolicitarAutenticaçãoPorAuthenticatorQuandoA2fEstiverAtivadaParaTipoAuthenticator() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(
                "teste@email.com",
                "senha123"
        );
        LoginResponse loginResponse = createLoginResponseA2fAtivada(TipoA2f.AUTHENTICATOR);

        given(authService.login(loginRequest)).willReturn(loginResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(loginRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(nullValue()))
                .andExpect(jsonPath("$.a2fRequired").value(true))
                .andExpect(jsonPath("$.tipoA2f").value("AUTHENTICATOR"));

    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao efetuar login quando dados não preenchidos")
    void deveriaRetornar400AoEfetuarLoginQuandoDadosNaoPreenchidos() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(
                null,
                "senha123"
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(loginRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'email')]").exists());

    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao efetuar login quando credenciais inválidas")
    void deveriaRetornar401AoEfetuarLoginQuandoCredenciaisInvalidas() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(
                "emailerrado@email.com",
                "senhaerrada"
        );

        // ACT + ASSERT
        when(authService.login(loginRequest))
                .thenThrow(new RegraNegocioException("Credenciais inválidas",
                        HttpStatus.UNAUTHORIZED));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(loginRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("Deveria retornar 200 ok com token ao verificar A2F do tipo Email com código valido")
    void deveriaRetornar200AoVerificarA2fParaTipoEmail() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                "123456"
        );
        LoginResponse loginResponse = createLoginResponseA2fVerificada(TipoA2f.EMAIL);

        given(authService.verificarA2f(codigoA2fRequest)).willReturn(loginResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.a2fRequired").value(false))
                .andExpect(jsonPath("$.tipoA2f").value("EMAIL"));

    }

    @Test
    @DisplayName("Deveria retornar 200 ok com token ao verificar A2F do tipo Authenticator com código valido")
    void deveriaRetornar200AoVerificarA2fParaTipoAuthenticator() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                "123456789"
        );
        LoginResponse loginResponse = createLoginResponseA2fVerificada(TipoA2f.AUTHENTICATOR);

        given(authService.verificarA2f(codigoA2fRequest)).willReturn(loginResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.a2fRequired").value(false))
                .andExpect(jsonPath("$.tipoA2f").value("AUTHENTICATOR"));

    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao verificar A2F quando codigo não for preenchido")
    void deveriaRetornar400AoVerificarA2fQuandoCodigoNaoPreenchido() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                null
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao verificar A2F quando tipo da A2f for inválido")
    void deveriaRetornar400AoVerificarA2fQuandoTipoA2fInvalido() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                "123456"
        );

        // ACT + ASSERT
        when(authService.verificarA2f(codigoA2fRequest))
                .thenThrow(new RegraNegocioException("Tipo de A2F inválido.",
                        HttpStatus.BAD_REQUEST));
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao verificar A2F quando não está ativa para o usuário")
    void deveriaRetornar400AoVerificarA2fQuandoNaoAtivaParaUsuario() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                "123456"
        );

        // ACT + ASSERT
        when(authService.verificarA2f(codigoA2fRequest))
                .thenThrow(new RegraNegocioException("A2F não está ativa para este usuário.",
                        HttpStatus.BAD_REQUEST));
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao verificar A2F quando código inválido")
    void deveriaRetornar401AoVerificarA2fQuandoCodigoInvalido() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                "123"
        );

        // ACT + ASSERT
        when(authService.verificarA2f(codigoA2fRequest))
                .thenThrow(new RegraNegocioException("Código inválido.",
                        HttpStatus.UNAUTHORIZED));
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao verificar A2F quando código expirado")
    void deveriaRetornar401AoVerificarA2fQuandoCodigoExpirado() throws Exception {
        // ARRANGE
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "teste@email.com",
                "123456"
        );

        // ACT + ASSERT
        when(authService.verificarA2f(codigoA2fRequest))
                .thenThrow(new RegraNegocioException("Código expirado.",
                        HttpStatus.UNAUTHORIZED));
        mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(codigoA2fRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("Deveria retornar 200 ok quando iniciar autenticação por Totp com sucesso")
    void deveriaRetornar200AoIniciarAtivacaoTotpComSucesso() throws Exception {
        // ARRANGE
        var authentication = createAuthenticationPreAuthorizeAdministrador();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        TotpActivationResponse totpActivationResponse = createTotpActivationResponse();

        given(authService.iniciarAtivacaoTotp(usuario.getId())).willReturn(totpActivationResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/totp/setup")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value(totpActivationResponse.secret()))
                .andExpect(jsonPath("$.qrCodeUrl").value(totpActivationResponse.qrCodeUrl()));
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao iniciar ativação Totp quando não autorizado")
    void deveriaRetornar401AoIniciarAtivacaoTotpQuandoNaoAutorizado() throws Exception {
        mockMvc.perform(post("/api/v1/auth/2fa/totp/setup"))
                       .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found quando iniciar ativação por Totp para usuário não encontrado")
    void deveriaRetornar404AoIniciarAtivacaoTotpParaUsuarioNaoEncontrado() throws Exception {
        // ARRANGE
        var authentication = createAuthenticationPreAuthorizeAdministrador();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        // ACT + ASSERT
        willThrow(new RegraNegocioException(
                "Usuário não encontrado.",
                HttpStatus.NOT_FOUND
        )).given(authService)
                .iniciarAtivacaoTotp(usuario.getId());

        mockMvc.perform(post("/api/v1/auth/2fa/totp/setup")
                        .with(authentication(authentication)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 204 No Content quando confirmar ativação por Totp com sucesso")
    void deveriaRetornar204AoConfirmarAtivacaoTotpComSucesso() throws Exception {
        // ARRANGE
        var authentication = createAuthenticationPreAuthorizeAdministrador();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        String codigo = "abc123";
        TotpConfirmRequest totpConfirmRequest = new TotpConfirmRequest(
                codigo);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/totp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(totpConfirmRequest))
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        then(authService).should()
                .confirmarAtivacaoTotp(usuario.getId(), codigo);
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request quando confirmar ativação por Totp sem codigo")
    void deveriaRetornar400AoConfirmarAtivacaoTotpSemCodigo() throws Exception {
        // ARRANGE
        var authentication = createAuthenticationPreAuthorizeAdministrador();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        String codigo = null;
        TotpConfirmRequest totpConfirmRequest = new TotpConfirmRequest(
                codigo);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/totp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(totpConfirmRequest))
                        .with(authentication(authentication)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando confirmar ativação por Totp com codigo inválido")
    void deveriaRetornar401AoConfirmarAtivacaoTotpComCodigoInvalido() throws Exception {
        // ARRANGE
        var authentication = createAuthenticationPreAuthorizeAdministrador();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        String codigo = "abc@123";
        TotpConfirmRequest totpConfirmRequest = new TotpConfirmRequest(
                codigo);

        // ACT + ASSERT
        willThrow(new RegraNegocioException(
                "Código TOTP inválido.",
                HttpStatus.UNAUTHORIZED
        )).given(authService)
                .confirmarAtivacaoTotp(usuario.getId(), codigo);

        mockMvc.perform(post("/api/v1/auth/2fa/totp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(totpConfirmRequest))
                        .with(authentication(authentication)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando confirmar ativação por Totp sem autenticação")
    void deveriaRetornar401AoConfirmarAtivacaoTotpSemAutenticacao() throws Exception {
        // ARRANGE
        String codigo = "abc@123";
        TotpConfirmRequest totpConfirmRequest = new TotpConfirmRequest(
                codigo);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/auth/2fa/totp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(totpConfirmRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found quando confirmar ativação por Totp para usuário não encontrado")
    void deveriaRetornar404AoConfirmarAtivacaoTotpParaUsuarioNaoEncontrado() throws Exception {
        // ARRANGE
        var authentication = createAuthenticationPreAuthorizeAdministrador();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        String codigo = "abc@123";
        TotpConfirmRequest totpConfirmRequest = new TotpConfirmRequest(
                codigo);

        // ACT + ASSERT
        willThrow(new RegraNegocioException(
                "Usuário não encontrado.",
                HttpStatus.NOT_FOUND
        )).given(authService)
                .confirmarAtivacaoTotp(usuario.getId(), codigo);

        mockMvc.perform(post("/api/v1/auth/2fa/totp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(totpConfirmRequest))
                        .with(authentication(authentication)))
                .andExpect(status().isNotFound());
    }

    private UsuarioResponse createUsuarioResponse() {
        return new UsuarioResponse(
                42L,
                "Teste",
                "teste@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );
    }

    private LoginResponse createLoginResponseA2fDesativada() {
        return new LoginResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30",
                LocalDateTime.now().plusMinutes(60),
                false,
                TipoA2f.DESATIVADA
        );
    }

    private LoginResponse createLoginResponseA2fAtivada(TipoA2f tipoA2f) {
        return new LoginResponse(
                null,
                null,
                true,
                tipoA2f
        );
    }

    private LoginResponse createLoginResponseA2fVerificada(TipoA2f tipoA2f) {
        return new LoginResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30",
                LocalDateTime.now().plusMinutes(60),
                false,
                tipoA2f
        );
    }

    private UsuarioResponse createUsuarioResponsePreAuthorize() {
        return new UsuarioResponse(
                40L,
                "Teste 2",
                "teste2@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );
    }

    private Authentication createAuthenticationPreAuthorizeEstudante() {
        Usuario usuario = new Usuario(
                "Teste",
                "teste@email.com",
                "senha123"
        );
        // Define o ID para simular o usuário autenticado.
        ReflectionTestUtils.setField(usuario, "id", 42L);

        return new UsernamePasswordAuthenticationToken(usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE")));
    }

    private Authentication createAuthenticationPreAuthorizeAdministrador() {
        Usuario usuario = new Usuario(
                "Teste 2",
                "teste2@email.com",
                "senha1234"
        );
        // Define o ID para simular o usuário autenticado.
        ReflectionTestUtils.setField(usuario, "id", 40L);

        return new UsernamePasswordAuthenticationToken(usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
    }

    private TotpActivationResponse createTotpActivationResponse() {
        String secret = "JBSWY3DPEHPK3PXP";

        String qrCodeUrl = "otpauth://totp/Booking%20MS:teste@email.com"
                + "?secret=" + secret
                + "&issuer=Booking%20MS";

        return new TotpActivationResponse(
                qrCodeUrl,
                secret
        );
    }
}