package br.com.jhohannesfreitas.user_ms.infra.config;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TokenProviderTest {

    // HMAC-SHA exige no mínimo 32 bytes; com menos, o JJWT lança WeakKeyException
    private static final String CHAVE = "chave-de-teste-com-mais-de-32-caracteres-ok";
    private static final String OUTRA_CHAVE = "outra-chave-de-teste-com-mais-de-32-caracteres";
    private static final String EMAIL = "teste@email.com";

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "key", CHAVE);
        ReflectionTestUtils.setField(tokenProvider, "expirationTime", 900_000L);
    }

    @Test
    @DisplayName("Deveria considerar válido um token bem assinado e dentro do prazo")
    void deveriaConsiderarValidoTokenCorreto() {
        // ARRANGE
        Long usuarioId = 42L;
        String token = gerarToken(CHAVE, daquiAUmaHora(), usuarioId, "ROLE_ESTUDANTE");

        // ACT + ASSERT
        assertTrue(tokenProvider.isTokenValid(token));
    }

    @Test
    @DisplayName("Deveria extrair e-mail, id do usuário e perfis de um token válido")
    void deveriaExtrairDadosDeTokenValido() {
        // ARRANGE
        Long usuarioId = 42L;
        String token = gerarToken(CHAVE, daquiAUmaHora(), usuarioId, "ROLE_ESTUDANTE");

        // ACT + ASSERT

        assertAll(
                () -> assertEquals(EMAIL, tokenProvider.getUsernameFromToken(token)),
                () -> assertEquals(List.of("ROLE_ESTUDANTE"), tokenProvider.getAuthoritiesFromToken(token))
        );
    }

    @Test
    @DisplayName("Deveria considerar inválido um token expirado")
    void deveriaConsiderarInvalidoTokenExpirado() {
        // ARRANGE
        Long usuarioId = 42L;
        String token = gerarToken(CHAVE, haUmaHora(), usuarioId, "ROLE_ESTUDANTE");

        // ACT + ASSERT
        assertFalse(tokenProvider.isTokenValid(token));
    }

    @Test
    @DisplayName("Deveria considerar inválido um token assinado com outra chave")
    void deveriaConsiderarInvalidoTokenAssinadoComOutraChave() {
        // ARRANGE
        Long usuarioId = 42L;
        String token = gerarToken(OUTRA_CHAVE, daquiAUmaHora(), usuarioId, "ROLE_ESTUDANTE");

        // ACT + ASSERT
        assertFalse(tokenProvider.isTokenValid(token));
    }

    @Test
    @DisplayName("Deveria considerar inválido um token com payload trocado e assinatura original")
    void deveriaConsiderarInvalidoTokenAdulterado() {
        // ARRANGE
        // Token A: o do usuário comum, com assinatura legítima
        String tokenA = gerarToken(CHAVE, daquiAUmaHora(), 1L, "ROLE_ESTUDANTE");

        // Token B: o que o atacante gostaria de ter (admin), também assinado com a chave certa
        String tokenB = gerarToken(CHAVE, daquiAUmaHora(), 99L, "ROLE_ADMINISTRADOR");

        String[] partesA = tokenA.split("\\.");   // [cabeçalho, payload, assinatura]
        String[] partesB = tokenB.split("\\.");

        String tokenAdulterado = partesA[0] + "." + partesB[1] + "." + partesA[2];

        // ACT + ASSERT
        assertTrue(tokenProvider.isTokenValid(tokenB));            // contraprova: B sozinho é legítimo
        assertFalse(tokenProvider.isTokenValid(tokenAdulterado));  // a mistura não é
    }

    @Test
    @DisplayName("Deveria considerar inválido um token sem assinatura")
    void deveriaConsiderarInvalidoTokenSemAssinatura() {
        // ARRANGE
        // Sem .signWith(...): o JJWT gera um token "não protegido" (alg: none),
        // com as claims de um administrador mas sem nenhuma assinatura
        String tokenSemAssinatura = Jwts.builder()
                .subject(EMAIL)
                .claim("usuarioId", 99L)
                .claim("authorities", List.of("ROLE_ADMINISTRADOR"))
                .issuedAt(new Date())
                .expiration(daquiAUmaHora())
                .compact();

        // ACT + ASSERT
        assertFalse(tokenProvider.isTokenValid(tokenSemAssinatura));
        assertTrue(tokenProvider.isTokenValid(gerarToken(CHAVE, daquiAUmaHora(), 99L, "ROLE_ADMINISTRADOR")));
    }

    @ParameterizedTest(name = "[{index}] texto = \"{0}\"")
    @ValueSource(strings = {"", "   ", "isso-nao-e-um-jwt", "a.b.c"})
    @DisplayName("Deveria considerar inválido um texto que não é um JWT")
    void deveriaConsiderarInvalidoTextoQueNaoEJwt(String textoInvalido) {
        assertFalse(tokenProvider.isTokenValid(textoInvalido));
    }

    @Test
    @DisplayName("Deveria gerar token com e-mail e authorities da autenticação")
    void deveriaGerarTokenComDadosDaAutenticacao() {
        // ARRANGE
        Usuario usuario = new Usuario(
                "teste",
                EMAIL,
                "senha"
        );
        ReflectionTestUtils.setField(usuario, "id", 42L);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE")));

        // ACT
        String token = tokenProvider.gerarToken(authentication);

        // ASSERT
        assertAll(
                () -> assertTrue(tokenProvider.isTokenValid(token)),
                () -> assertEquals(EMAIL, tokenProvider.getUsernameFromToken(token)),
                () -> assertEquals(
                        List.of("ROLE_ESTUDANTE"),
                        tokenProvider.getAuthoritiesFromToken(token)),
                () -> assertEquals(
                        42,
                        Jwts.parser()
                                .verifyWith(Keys.hmacShaKeyFor(CHAVE.getBytes()))
                                .build()
                                .parseSignedClaims(token)
                                .getPayload()
                                .get("usuarioId", Integer.class)
                )
        );
    }

    @Test
    @DisplayName("Deveria retornar a data de expiração do token")
    void deveriaRetornarDataDeExpiracaoDoToken() {
        // ARRANGE
        LocalDateTime antes = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));

        // ACT
        LocalDateTime expiracao = tokenProvider.getExpirationDateToken();

        // ASSERT
        LocalDateTime esperado = antes.plusMinutes(15);

        assertAll(
                () -> assertFalse(expiracao.isBefore(esperado.minusSeconds(1))),
                () -> assertFalse(expiracao.isAfter(esperado.plusSeconds(1)))
        );
    }

    private String gerarToken(String chave, Date expiracao, Long usuarioId, String perfil) {
        Usuario usuario = new Usuario(
                "teste",
                EMAIL,
                "senha"
        );
        ReflectionTestUtils.setField(usuario, "id", 42L);
        return Jwts.builder()
                .subject("teste@email.com")
                .claim("usuarioId", usuario.getId())
                .claim("authorities", List.of(perfil))
                .issuedAt(new Date())
                .expiration(expiracao)
                .signWith(Keys.hmacShaKeyFor(chave.getBytes()))
                .compact();
    }

    private Date daquiAUmaHora() {
        return new Date(System.currentTimeMillis() + 3_600_000); }

    private Date haUmaHora() {
        return new Date(System.currentTimeMillis() - 3_600_000); }

}
