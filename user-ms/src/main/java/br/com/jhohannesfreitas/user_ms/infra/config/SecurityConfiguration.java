package br.com.jhohannesfreitas.user_ms.infra.config;

// Classe responsável por fazer a segurança das rotas

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Para poder definir também a segurança direto no método controller
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                        }))
                .authorizeHttpRequests(auth -> {
                            auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/registrar", "/api/v1/auth/login",
                                    "/api/v1/auth/2fa/verify", "/login/oauth2/**", "/oauth2/**").permitAll();
                            auth.anyRequest().authenticated();
                        }
                )
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler)) //Habilita o OAuth2 Login para as registrations configuradas
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Autenticar o Usuário
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Hierarquia de Roles
    @Bean
    public RoleHierarchy roleHierarchyVersionTwo(){
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMINISTRADOR").implies("MODERADOR")
                .role("MODERADOR").implies("ESTUDANTE", "INSTRUTOR")
                .build();
    }
}
