package br.com.jhohannesfreitas.user_ms.infra.config;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.domain.enums.ProvedorLogin;
import br.com.jhohannesfreitas.user_ms.repository.PerfilRepository;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient;

    public OAuth2LoginSuccessHandler(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            TokenProvider tokenProvider,
            PasswordEncoder passwordEncoder,
            OAuth2AuthorizedClientService authorizedClientService,
            RestClient.Builder restClientBuilder) {

        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.authorizedClientService = authorizedClientService;

        this.restClient = restClientBuilder
                .baseUrl("https://api.github.com")
                .build();
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // 1. Identificar o provedor
        String registrationId = obterRegistrationId(authentication);

        // 2. Obter os dados do usuário OAuth2
        OAuth2User oauth2User = obterOAuth2User(authentication);

        // 3. Obter nome
        String nome = obterNome(oauth2User);

        // 4. Obter e-mail
        String email = obterEmail(authentication, oauth2User, registrationId);

        // 5. Definir o provedor do login
        ProvedorLogin provedorLogin =
                obterProvedorLogin(registrationId);

        // 6. Buscar ou criar usuário
        Usuario usuario = buscarOuCriarUsuario(
                nome,
                email,
                provedorLogin
        );

        // 7. Criar Authentication usando o Usuario da aplicação
        Authentication usuarioAuthentication =
                criarAuthentication(usuario);

        // 8. Gerar o seu JWT
        String token =
                tokenProvider.gerarToken(usuarioAuthentication);

        // 9. Retornar o JWT
        enviarToken(response, token);
    }

    // Identificar provedor
    private String obterRegistrationId(Authentication authentication) {
        OAuth2AuthenticationToken oauth2Authentication =
                (OAuth2AuthenticationToken) authentication;

        return oauth2Authentication
                .getAuthorizedClientRegistrationId();
    }

    // Obter OAuth2User
    private OAuth2User obterOAuth2User(Authentication authentication) {
        OAuth2AuthenticationToken oauth2Authentication =
                (OAuth2AuthenticationToken) authentication;

        return oauth2Authentication.getPrincipal();
    }

    // Obter nome
    private String obterNome(OAuth2User oauth2User) {
        String nome = oauth2User.getAttribute("name");

        if (nome == null || nome.isBlank()) {
            nome = oauth2User.getAttribute("login");
        }

        if (nome == null || nome.isBlank()) {
            throw new IllegalStateException(
                    "Não foi possível obter o nome do usuário."
            );
        }

        return nome;
    }

    // Obter e-mail
    private String obterEmail(
            Authentication authentication,
            OAuth2User oauth2User,
            String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> obterEmailGoogle(oauth2User);
            case "github" -> obterEmailGitHub(authentication);
            default -> throw new IllegalArgumentException(
                    "Provedor OAuth2 não suportado: " + registrationId
            );
        };
    }

    // E-mail do Google
    private String obterEmailGoogle(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "Não foi possível obter o e-mail do Google."
            );
        }

        return email;
    }

    // E-mail do GitHub
    private String obterEmailGitHub(Authentication authentication) {
        OAuth2AuthenticationToken oauth2Authentication =
                (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient(
                        "github",
                        oauth2Authentication.getName()
                );

        if (authorizedClient == null) {
            throw new IllegalStateException(
                    "Não foi possível obter o cliente autorizado do GitHub."
            );
        }

        String accessToken =
                authorizedClient
                        .getAccessToken()
                        .getTokenValue();

        return buscarEmailGitHub(accessToken);
    }

    private String buscarEmailGitHub(String accessToken) {
        List<Map<String, Object>> emails = restClient
                .get()
                .uri("/user/emails")
                .headers(headers ->
                        headers.setBearerAuth(accessToken))
                .retrieve()
                .body(List.class);

        if (emails == null || emails.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhum e-mail foi encontrado na conta do GitHub."
            );
        }

        return emails.stream()
                .filter(email ->
                        Boolean.TRUE.equals(email.get("primary")))
                .filter(email ->
                        Boolean.TRUE.equals(email.get("verified")))
                .map(email ->
                        (String) email.get("email"))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Não foi encontrado um e-mail principal e verificado no GitHub."
                        ));
    }

    // Identificar provedor
    private ProvedorLogin obterProvedorLogin(String registrationId) {
        return switch (registrationId.toLowerCase()) {

            case "google" -> ProvedorLogin.GOOGLE;
            case "github" -> ProvedorLogin.GITHUB;
            default -> throw new IllegalArgumentException(
                    "Provedor OAuth2 não suportado: " + registrationId
            );
        };
    }

    // Buscar ou criar usuário
    private Usuario buscarOuCriarUsuario(
            String nome,
            String email,
            ProvedorLogin provedorLogin) {

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        if (usuario == null) {
            usuario = criarUsuario(
                    nome,
                    email,
                    provedorLogin
            );
        }

        return usuario;
    }

    // Criar usuário
    private Usuario criarUsuario(
            String nome,
            String email,
            ProvedorLogin provedorLogin) {

        String senhaAleatoria = UUID.randomUUID().toString();

        Usuario usuario = new Usuario(
                nome,
                email,
                passwordEncoder.encode(senhaAleatoria)
        );

        usuario.setProvedorLogin(provedorLogin);

        Perfil perfilEstudante = perfilRepository
                .findByPerfilNome(PerfilNome.ESTUDANTE)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Perfil ESTUDANTE não encontrado."
                        ));

        usuario.adicionarPerfil(perfilEstudante);

        return usuarioRepository.save(usuario);
    }

    // Criar Authentication da sua aplicação
    private Authentication criarAuthentication(Usuario usuario) {

        return new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
        );
    }

    // Enviar JWT
    private void enviarToken(
            HttpServletResponse response,
            String token) throws IOException {

        response.setContentType("application/json");

        response.getWriter().write("""
                {
                    "token": "%s"
                }
                """.formatted(token));
    }
}
