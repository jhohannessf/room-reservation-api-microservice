package br.com.jhohannesfreitas.user_ms.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

    @Mock
    private GoogleAuthenticator googleAuthenticator;

    @InjectMocks
    private TotpService totpService;


    @Test
    @DisplayName("Deveria retornar credenciais geradas pelo autenticador")
    void deveriaRetornarCredenciaisGeradasPeloAutenticador() {
        // ARRANGE - Preparar o ambiente
        GoogleAuthenticatorKey credenciais =
                new GoogleAuthenticatorKey.Builder("ABCDEFGHIJKLMNOP")
                        .build();

        given(googleAuthenticator.createCredentials())
                .willReturn(credenciais);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        GoogleAuthenticatorKey resposta = totpService.gerarCredenciais();

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(googleAuthenticator).should().createCredentials();

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertSame(credenciais, resposta);
    }

    @Test
    @DisplayName("Deveria gerar URL do QR Code com dados válidos")
    void deveriaGerarUrlQrCodeComDadosValidos() {
        // ARRANGE - Preparar o ambiente
        String nomeAplicacao = "MinhaAplicacao";
        String email = "teste@email.com";

        GoogleAuthenticatorKey credenciais =
                new GoogleAuthenticatorKey.Builder("ABCDEFGHIJKLMNOP")
                        .build();

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        String url = totpService.gerarQrCodeUrl(
                nomeAplicacao, email, credenciais
        );

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        
        assertNotNull(url);
        assertTrue(url.contains("api.qrserver.com"));
        assertTrue(url.contains("otpauth"));
        assertTrue(url.contains("secret"));
        assertTrue(url.contains("issuer"));
    }

    @Test
    @DisplayName("Deveria retornar true quando o código for válido")
    void deveriaRetornarTrueQuandoCodigoForValido() {
        // ARRANGE - Preparar o ambiente
        String secret = "ABCDEFGHIJKLMNOP";
        String codigo = "123456";

        given(googleAuthenticator.authorize(secret, 123456))
                .willReturn(true);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        boolean resposta = totpService.validarCodigo(secret, codigo);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(googleAuthenticator).should()
                .authorize(secret, 123456);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertTrue(resposta);
    }

    @Test
    @DisplayName("Deveria retornar false quando o código for inválido")
    void deveriaRetornarFalseQuandoCodigoForInvalido() {
        // ARRANGE - Preparar o ambiente
        String secret = "ABCDEFGHIJKLMNOP";
        String codigo = "123456";

        given(googleAuthenticator.authorize(secret, 123456))
                .willReturn(false);

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        boolean resposta = totpService.validarCodigo(secret, codigo);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertFalse(resposta);
    }

    @Test
    @DisplayName("Deveria retornar false quando o código não for numérico")
    void deveriaRetornarFalseQuandoCodigoNaoForNumerico() {
        // ARRANGE - Preparar o ambiente
        String secret = "ABCDEFGHIJKLMNOP";
        String codigo = "ABC123";

        // ACT (AGIR) - A ação que se deseja testar é executada (método)
        boolean resposta = totpService.validarCodigo(secret, codigo);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(googleAuthenticator).shouldHaveNoInteractions();

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertFalse(resposta);

    }
}