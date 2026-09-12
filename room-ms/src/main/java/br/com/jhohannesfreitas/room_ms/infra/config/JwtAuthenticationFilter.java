package br.com.jhohannesfreitas.room_ms.infra.config;

// Classe responsável por fazer o filtro das requisições

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Pra cada requisição será feito um filtro

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Pegar o token JWT que veio no cabeçalho "Authorization" da requisição
        String token = recuperarToken(request);

        // 2. Se tiver token e a assinatura for válida(a chave secreta bater)
        if (token != null && tokenProvider.isTokenValid(token)) {

            // 3. Extrai as informações de dentro do Token (NÃO VAMOS NO BANCO)
            String email = tokenProvider.getUsernameFromToken(token);
            List<String> perfis = tokenProvider.getAuthoritiesFromToken(token);

            // 4. Converte a lista de textos (ROLE_ESTUDANTE) para a classe que o Spring entende
            var authorities = perfis.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            // 5. Cria o "usuário autenticado" apenas em memória e salva no contexto
            var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. Manda a requisição continuar (se não tiver token, barra na frente)
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return token;
        }
        return null;
    }
}