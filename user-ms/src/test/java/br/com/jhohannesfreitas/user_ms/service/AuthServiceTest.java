package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.domain.enums.TipoA2f;
import br.com.jhohannesfreitas.user_ms.dto.*;
import br.com.jhohannesfreitas.user_ms.infra.config.TokenProvider;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.repository.CodigoA2fRepository;
import br.com.jhohannesfreitas.user_ms.repository.PerfilRepository;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private CodigoA2fRepository codigoA2fRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CodigoA2fService codigoA2fService;

    @Mock
    private EmailService emailService;

    @Mock
    private GoogleAuthenticator googleAuthenticator;

    @Mock
    private TotpService totpService;

    @InjectMocks
    private AuthService authService;

    private UsuarioRequest usuarioRequest;

    private Perfil perfil;

    @BeforeEach
    void setUp() {
        usuarioRequest = new UsuarioRequest(
                "Usuario Teste",
                "teste@teste.com",
                "teste@123");
    }

    @Test
    @DisplayName("Deveria registrar com sucesso quando dados forem validos")
    void deveriaRegistrarComSucessoQuandoDadosForemValidos() {
        // ARRANGE -  Configurar o ambinte

        // Simula que NÃO existe um usuário cadastrado com este e-mail, retornando false
        given(usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email())).willReturn(false);

        // Simula o retorno do banco após save(), atribuindo um ID ao usuário persistido
        Usuario usuario = criarUsuario();

        // Simula o Hash da senha
        given(passwordEncoder.encode(usuarioRequest.senha())).willReturn("senha");

        // Simula o perfil
        Perfil perfil = new Perfil(
                PerfilNome.ESTUDANTE
        );
        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.of(perfil));

        // Simula o save()
        given(usuarioRepository.save(any(Usuario.class))).willReturn(usuario);

        // ACT - A ação que se deseja testar é executada
        UsuarioResponse usuarioResponse = authService.registrar(usuarioRequest);

        // ASSERT

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        then(usuarioRepository).should().save(usuarioCaptor.capture());

        // "Então Repository, você DEVERIA verificar se foi chamado o 'existsByEmail()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().existsByEmailIgnoreCase(usuarioRequest.email());

        then(passwordEncoder).should().encode(usuarioRequest.senha());
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(1L, usuarioResponse.id()),
                () -> assertEquals(usuario.getNome(), usuarioResponse.nome()),
                () -> assertEquals(usuario.getEmail(), usuarioResponse.email())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao registrar usuário quando estiver email cadastrado")
    void deveriaLancarExcecaoAoCadastrarUsuarioQuandoEstiverEmailCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        //1- Simula que NÃO existe um usuário cadastrado com este e-mail, retornando false
        given(usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email())).willReturn(true);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.registrar(usuarioRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário já cadastrado com este e-mail")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'existsByEmail()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().existsByEmailIgnoreCase(usuarioRequest.email());

        // "Então Repository, você DEVERIA verificar se foi nunca foi chamado o 'save()'
        then(usuarioRepository).should(never()).save((any()));

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria codificar a senha antes de persistir o usuário")
    void deveriaCodificarSenhaAntesDePersistirUsuario() {
        // ARRANGE
        given(usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email()))
                .willReturn(false);

        given(passwordEncoder.encode(usuarioRequest.senha()))
                .willReturn("senha-hash");

        Perfil perfil = new Perfil(PerfilNome.ESTUDANTE);

        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE))
                .willReturn(Optional.of(perfil));

        Usuario usuarioSalvo = criarUsuario();
        usuarioSalvo.setSenha("senha-hash");

        given(usuarioRepository.save(any(Usuario.class)))
                .willReturn(usuarioSalvo);

        // ACT
        authService.registrar(usuarioRequest);

        // ASSERT
        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        then(passwordEncoder).should().encode(usuarioRequest.senha());
        then(usuarioRepository).should().save(captor.capture());

        String senhaPersistida = (String) ReflectionTestUtils.getField(
                captor.getValue(), "senha"
        );

        assertAll(
                () -> assertEquals("senha-hash", senhaPersistida),
                () -> assertNotEquals(usuarioRequest.senha(), senhaPersistida)
        );
    }

    @Test
    @DisplayName("Deveria efetuar login quando cadastro existir e A2f desativada, retornando token")
    void deveriaEfeturLoginQuandoCadastroExistirEA2fDesativadaRetornandoToken() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        LoginRequest loginRequest = new LoginRequest(
                "userautenticado@email.com",
                "senhaHash"
        );

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(false);
        usuario.setTipoA2f(TipoA2f.DESATIVADA);

        // Simula usuário autenticado
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
        );

        given(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).willReturn(authentication);

        given(tokenProvider.gerarToken(authentication))
                .willReturn("token-teste");

        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(60);

        given(tokenProvider.getExpirationDateToken())
                .willReturn(expirationTime);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        LoginResponse resultado = authService.login(loginRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(authenticationManager).should().authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ));
        then(tokenProvider).should().gerarToken(authentication);
        then(tokenProvider).should().getExpirationDateToken();

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals("token-teste", resultado.token()),
                () -> assertEquals(expirationTime, resultado.expirationTime()),
                () -> assertFalse(resultado.a2fRequired()),
                () -> assertEquals(TipoA2f.DESATIVADA, resultado.tipoA2f())
        );
    }

    @Test
    @DisplayName("Deveria efetuar login quando cadastro existir e A2f ativada para o tipo email, sem retornar token")
    void deveriaEfeturLoginQuandoCadastroExistirEA2fAtivadaParaTipoEmailSemRetornarToken() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        LoginRequest loginRequest = new LoginRequest(
                "userautenticado@email.com",
                "senhaHash"
        );

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.EMAIL);

        // Simula usuário autenticado
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
        );

        given(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).willReturn(authentication);

        String codigo = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(5);

        CodigoA2f codigoA2f = new CodigoA2f(
                usuario.getId(),
                codigo,
                expiracao
        );
        given(codigoA2fService.gerarCodigo(usuario.getId())).willReturn(codigoA2f);


        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        LoginResponse resultado = authService.login(loginRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(authenticationManager).should().authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ));
        then(codigoA2fService).should().gerarCodigo(usuario.getId());
        then(emailService).should().enviarCodigoA2f(usuario.getEmail(), codigo);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertNull(resultado.token()),
                () -> assertNull(resultado.expirationTime()),
                () -> assertTrue(resultado.a2fRequired()),
                () -> assertEquals(TipoA2f.EMAIL, resultado.tipoA2f())
        );
    }

    @Test
    @DisplayName("Deveria efetuar login quando cadastro existir e A2f ativada para o tipo authenticator, sem retornar token")
    void deveriaEfeturLoginQuandoCadastroExistirEA2fAtivadaParaTipoAuthenticatorSemRetornarToken() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        LoginRequest loginRequest = new LoginRequest(
                "userautenticado@email.com",
                "senhaHash"
        );

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);

        // Simula usuário autenticado
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
        );

        given(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).willReturn(authentication);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        LoginResponse resultado = authService.login(loginRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(authenticationManager).should().authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertNull(resultado.token()),
                () -> assertNull(resultado.expirationTime()),
                () -> assertTrue(resultado.a2fRequired()),
                () -> assertEquals(TipoA2f.AUTHENTICATOR, resultado.tipoA2f())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao efetuar login quando credenciais forem inválidas")
    void deveriaLancarExcecaoAoEfeturLoginQuandoCredenciaisForemInvalidas() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        LoginRequest loginRequest = new LoginRequest(
                "userautenticado@email.com",
                null
        );

        given(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).willThrow(new BadCredentialsException("Bad credentials"));

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Credenciais inválidas")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria verificar A2F para o tipo email quando código válido")
    void deveriaVerificarA2fParaTipoEmailQuandoCodigoValido() {
        // ARRANGE

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.EMAIL);

        // Simula que o usuário existe
        given(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).willReturn(Optional.of(usuario));

        // Simula que código
        String codigo = "abc123";
        CodigoA2f codigoA2f = new CodigoA2f(
                usuario.getId(),
                codigo,
                LocalDateTime.now().plusMinutes(5)
        );

        // Simula a validação do código informado
        given(codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(usuario.getId(), codigo)).willReturn(Optional.of(codigoA2f));

        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                usuario.getEmail(),
                codigo);

        // Simula geração de token para usuário Authenticado
        given(tokenProvider.gerarToken(any(Authentication.class)))
                .willReturn("token-teste");

        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(60);

        given(tokenProvider.getExpirationDateToken())
                .willReturn(expirationTime);

        // ACT
        LoginResponse resultado = authService.verificarA2f(codigoA2fRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // ArgumentCaptor - captura o Authentication realmente passado ao tokenProvider
        ArgumentCaptor<Authentication> captor =
                ArgumentCaptor.forClass(Authentication.class);

        then(tokenProvider).should().gerarToken(captor.capture());

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals("token-teste", resultado.token()),
                () -> assertEquals(expirationTime, resultado.expirationTime()),
                () -> assertFalse(resultado.a2fRequired()),
                () -> assertEquals(TipoA2f.EMAIL, resultado.tipoA2f()),
                () -> assertEquals(usuario, captor.getValue().getPrincipal())
        );
    }

    @Test
    @DisplayName("Deveria verificar A2F para o tipo authenticator quando código válido")
    void deveriaVerificarA2fParaTipoAuthenticatorQuandoCodigoValido() {
        // ARRANGE

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);
        usuario.setA2fSecret("secret-teste");

        // Simula que o usuário existe
        given(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).willReturn(Optional.of(usuario));

        // Código informado
        String codigo = "123456";

        // Simula autorização do código informado pelo Google Authenticator
        //given(googleAuthenticator.authorize("secret-teste", 123456)).willReturn(true);

        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                usuario.getEmail(),
                codigo);

        // Simula a validação do código pelo TotpService
        given(totpService.validarCodigo(
                usuario.getA2fSecret(),
                codigoA2fRequest.codigo()
        )).willReturn(true);

        // Simula geração de token para usuário Authenticado
        given(tokenProvider.gerarToken(any(Authentication.class)))
                .willReturn("token-teste");

        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(60);

        given(tokenProvider.getExpirationDateToken())
                .willReturn(expirationTime);

        // ACT
        LoginResponse resultado = authService.verificarA2f(codigoA2fRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // ArgumentCaptor - captura o Authentication realmente passado ao tokenProvider
        ArgumentCaptor<Authentication> captor =
                ArgumentCaptor.forClass(Authentication.class);

        then(tokenProvider).should().gerarToken(captor.capture());

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals("token-teste", resultado.token()),
                () -> assertEquals(expirationTime, resultado.expirationTime()),
                () -> assertFalse(resultado.a2fRequired()),
                () -> assertEquals(TipoA2f.AUTHENTICATOR, resultado.tipoA2f()),
                () -> assertEquals(usuario, captor.getValue().getPrincipal())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao verificar A2F quando A2f quando usuário não encontrado")
    void deveriaLancarExcecaoAoVerificarA2fQuandoUsuarioNaoEncontrado() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456"
        );

        // Simula que o usuário não foi encontrado
        given(usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())).willReturn(Optional.empty());

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.verificarA2f(codigoA2fRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao verificar A2F quando não estiver ativa")
    void deveriaLancarExcecaoAoVerificarA2fQuandoNaoEstiverAtiva() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456"
        );

        // Simula que existe um usuário, mas A2F está desativada
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(false);

        // Simula que o usuário foi encontrado, mas a A2F está desativada
        given(usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())).willReturn(Optional.of(usuario));

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.verificarA2f(codigoA2fRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A2F não está ativa para este usuário.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));
        then(tokenProvider).should(never()).gerarToken(any(Authentication.class));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao verificar A2F quando tipo de A2F for inválido")
    void deveriaLancarExcecaoAoVerificarA2fQuandoTipoA2fInvalido() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456"
        );

        // Simula que existe um usuário, mas o tipoA2f é desativada
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.DESATIVADA);

        // Simula que o usuário foi encontrado, mas o tipoA2f é Desativada
        given(usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())).willReturn(Optional.of(usuario));

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.verificarA2f(codigoA2fRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Tipo de A2F inválido.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));
        then(tokenProvider).should(never()).gerarToken(any(Authentication.class));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao verificar A2F para o tipo email quando código for inválido")
    void deveriaLancarExcecaoAoVerificarA2fParaTipoEmailQuandoCodigoInvalido() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456789"
        );

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.EMAIL);

        // Simula que o usuário foi encontrado
        given(usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())).willReturn(Optional.of(usuario));

        // Simula a validação do código informado, retornando vazio no banco
        given(codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(usuario.getId(), codigoA2fRequest.codigo())).willReturn(Optional.empty());

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.verificarA2f(codigoA2fRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Código de A2F por e-mail inválido.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));
        then(tokenProvider).should(never()).gerarToken(any(Authentication.class));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao verificar A2F para o tipo email quando código estiver expirado")
    void deveriaLancarExcecaoAoVerificarA2fParaTipoEmailQuandoCodigoEstiverExpirado() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456789"
        );

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.EMAIL);

        // Simula CodigoA2f expirado
        CodigoA2f codigoA2f = new CodigoA2f(
                usuario.getId(),
                codigoA2fRequest.codigo(),
                LocalDateTime.now().minusDays(1)
        );

        // Simula que o usuário foi encontrado
        given(usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())).willReturn(Optional.of(usuario));

        // Simula a validação do código informado, retornando vazio no banco
        given(codigoA2fRepository.findByUsuarioIdAndCodigoAndUtilizadoFalse(usuario.getId(), codigoA2fRequest.codigo())).willReturn(Optional.of(codigoA2f));

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.verificarA2f(codigoA2fRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Código expirado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));
        then(tokenProvider).should(never()).gerarToken(any(Authentication.class));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao verificar A2F para o tipo authenticator quando código for inválido")
    void deveriaLancarExcecaoAoVerificarA2fParaTipoAuthenticatorQuandoCodigoInvalido() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456789"
        );

        // Simula que existe um usuário
        Usuario usuario = criarUsuario();
        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);

        // Simula que o usuário foi encontrado
        given(usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())).willReturn(Optional.of(usuario));

        // Simula a validação do código pelo TotpService, retornando false
        given(totpService.validarCodigo(
                usuario.getA2fSecret(),
                codigoA2fRequest.codigo()
        )).willReturn(false);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.verificarA2f(codigoA2fRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Código de A2F inválido.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));
        then(tokenProvider).should(never()).gerarToken(any(Authentication.class));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria iniciar ativação Totp quando credenciais forem validas")
    void deveriaIniciarAtivacaoTotpQuandoCredenciaisForemValidas() {
        // ARRANGE
        // Simula criação da credentials do Google
        GoogleAuthenticatorKey credentials = new GoogleAuthenticator().createCredentials();
        given(totpService.gerarCredenciais()).willReturn(credentials);

        String secret = credentials.getKey();
        String qrCodeUrl = "otpauth://totp/Booking%20MS:teste@email.com"
                + "?secret=" + secret
                + "&issuer=Booking%20MS";

        // Simula que existe um usuário settando alguns dados
        Usuario usuario = criarUsuario();
        usuario.setA2fSecret(secret);
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);
        usuario.setA2fAtiva(false);

        // Simula que o usuário foi encontrado
        given(usuarioRepository.findById(usuario.getId())).willReturn(Optional.of(usuario));

        // Simula geração de QR code
        given(totpService.gerarQrCodeUrl(
                "Booking MS",
                usuario.getEmail(),
                credentials
        )).willReturn(qrCodeUrl);

        // ACT
        TotpActivationResponse totpActivationResponse = authService.iniciarAtivacaoTotp(usuario.getId());

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should().save(usuario);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(qrCodeUrl, totpActivationResponse.qrCodeUrl()),
                () -> assertEquals(secret, totpActivationResponse.secret()),
                () -> assertEquals(TipoA2f.AUTHENTICATOR, usuario.getTipoA2f()),
                () -> assertFalse(usuario.isA2fAtiva())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao iniciar ativação Totp quando usuário não encontrado")
    void deveriaLancarExcecaoAoIniciarAtivacaoTotpQuandoUsuarioNaoEncontrado() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;

        // Simula que o usuário não foi encontrado
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.iniciarAtivacaoTotp(usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria confirmar ativação Totp quando dados forem válidos")
    void deveriaConfirmarAtivacaoTotpQuandoDadosForemValidas() {
        // ARRANGE
        // Simula criação da credentials do Google
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456789"
        );

        // Simula que existe um usuário settando alguns dados
        Usuario usuario = criarUsuario();
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);
        usuario.setA2fAtiva(false);

        // Simula que o usuário foi encontrado
        given(usuarioRepository.findById(usuario.getId())).willReturn(Optional.of(usuario));

        // Simula a validação true do código pelo TotpService
        given(totpService.validarCodigo(
                usuario.getA2fSecret(),
                codigoA2fRequest.codigo()
        )).willReturn(true);

        // ACT
        authService.confirmarAtivacaoTotp(usuario.getId(), codigoA2fRequest.codigo());

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should().save(usuario);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(TipoA2f.AUTHENTICATOR, usuario.getTipoA2f()),
                () -> assertTrue(usuario.isA2fAtiva())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao confirmar ativação Totp quando código for inválido")
    void deveriaLancarExcecaoAoConfirmarAtivacaoTotpQuandoCodigoInvalido() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        CodigoA2fRequest codigoA2fRequest = new CodigoA2fRequest(
                "userautenticado@email.com",
                "123456789"
        );

        // Simula que existe um usuário settando alguns dados
        Usuario usuario = criarUsuario();
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);
        usuario.setA2fAtiva(false);

        // Simula que o usuário foi encontrado
        given(usuarioRepository.findById(usuario.getId())).willReturn(Optional.of(usuario));

        // Simula a validação do código pelo TotpService, retornando false
        given(totpService.validarCodigo(
                usuario.getA2fSecret(),
                codigoA2fRequest.codigo()
        )).willReturn(false);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.confirmarAtivacaoTotp(usuario.getId(), codigoA2fRequest.codigo()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Código TOTP inválido.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao confirmar ativação Totp quando usuário não encontrado")
    void deveriaLancarExcecaoAoConfirmarAtivacaoTotpQuandoUsuarioNaoEncontrado() {
        // ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        String codigo = "123456";

        // Simula que o usuário não foi encontrado
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> authService.confirmarAtivacaoTotp(usuarioId, codigo))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(usuarioRepository).should(never()).save((any()));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario(
                usuarioRequest.nome(),
                usuarioRequest.email(),
                usuarioRequest.senha()
        );
        ReflectionTestUtils.setField(usuario, "id", 1L); // Atribuindo um ID ao usuário persistido no banco

        return usuario;
    }
}