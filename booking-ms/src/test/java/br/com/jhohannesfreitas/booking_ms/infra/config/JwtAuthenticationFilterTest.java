package br.com.jhohannesfreitas.booking_ms.infra.config;

import br.com.jhohannesfreitas.booking_ms.domain.entity.UserPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JwtAuthenticationFilterTest {

    private static final String CHAVE = "chave-de-teste-com-mais-de-32-caracteres-ok";
    private static final String EMAIL = "teste@email.com";

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        TokenProvider tokenProvider = new TokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "key", CHAVE);
        ReflectionTestUtils.setField(tokenProvider, "expirationTime", 900_000L);

        filter = new JwtAuthenticationFilter(tokenProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();   // o contexto é estático: não pode vazar para outros testes
    }

    @Test
    @DisplayName("Deveria preencher o contexto de segurança com id, e-mail e perfis de um token válido")
    void deveriaAutenticarComTokenValido() throws Exception {
        // ARRANGE
        String token = gerarToken(CHAVE, daquiAUmaHora(), 99L, "ROLE_ESTUDANTE");
        request.addHeader("Authorization", "Bearer " + token);

        // ACT
        filter.doFilter(request, response, chain);

        // ASSERT
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacao);

        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, autenticacao.getPrincipal());

        assertAll(
                () -> assertEquals(99L, principal.getId()),
                () -> assertEquals(EMAIL, principal.getEmail()),
                () -> assertEquals(token, autenticacao.getCredentials()),
                () -> assertTrue(autenticacao.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ESTUDANTE")))
        );

        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Deveria seguir a cadeia sem autenticar quando não houver cabeçalho Authorization")
    void deveriaSeguirSemAutenticarSemCabecalho() throws Exception {
        // ACT
        filter.doFilter(request, response, chain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Deveria seguir a cadeia sem autenticar quando token estiver expirado")
    void deveriaSeguirSemAutenticarQuandoTokenEstiverExpirado() throws Exception {
        // ARRANGE
        String token = gerarToken(CHAVE, haUmaHora(), 42L, "ROLE_ESTUDANTE");
        request.addHeader("Authorization", "Bearer " + token);

        // ACT
        filter.doFilter(request, response, chain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Deveria seguir a cadeia sem autenticar quando token estiver vazio")
    void deveriaSeguirSemAutenticarQuandoTokenEstiverVazio() throws Exception {
        // ARRANGE
        request.addHeader("Authorization", "Bearer ");

        // ACT
        filter.doFilter(request, response, chain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Deveria seguir a cadeia sem autenticar quando Bearer estiver ausente")
    void deveriaSeguirSemAutenticarQuandoBearerEstiverAusente() throws Exception {
        // ARRANGE
        String token = gerarToken(CHAVE, daquiAUmaHora(), 42L, "ROLE_ESTUDANTE");
        request.addHeader("Authorization", token);

        // ACT
        filter.doFilter(request, response, chain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Basic abc",
            "tokenCru",
            "bearer token"
    })
    @DisplayName("Deveria seguir a cadeia sem autenticar quando o formato do token for inválido")
    void deveriaSeguirSemAutenticarQuandoFormatoTokenForInvalido(String authorization) throws Exception {
        // ARRANGE
        request.addHeader("Authorization", authorization);

        // ACT
        filter.doFilter(request, response, chain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    private String gerarToken(String chave, Date expiracao, Long usuarioId, String perfil) {
        return Jwts.builder()
                .subject(EMAIL)
                .claim("usuarioId", usuarioId)
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
