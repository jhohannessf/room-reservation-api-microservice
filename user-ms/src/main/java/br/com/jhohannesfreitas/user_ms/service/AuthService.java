package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.domain.enums.TipoA2f;
import br.com.jhohannesfreitas.user_ms.dto.*;
import br.com.jhohannesfreitas.user_ms.infra.config.TokenProvider;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.mapper.UsuarioMapper;
import br.com.jhohannesfreitas.user_ms.repository.CodigoA2fRepository;
import br.com.jhohannesfreitas.user_ms.repository.PerfilRepository;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final CodigoA2fService codigoA2fService;
    private final EmailService emailService;
    private final CodigoA2fRepository codigoA2fRepository;
    private final TotpService totpService;

    public AuthService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenProvider tokenProvider, CodigoA2fService codigoA2fService, EmailService emailService, CodigoA2fRepository codigoA2fRepository, TotpService totpService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.codigoA2fService = codigoA2fService;
        this.emailService = emailService;
        this.codigoA2fRepository = codigoA2fRepository;
        this.totpService = totpService;
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioRequest usuarioRequest) {
        // Verificar se o email já está cadastrado no banco
        if (usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email())) {
            throw new RegraNegocioException("Usuário já cadastrado com este e-mail",
                    HttpStatus.CONFLICT);
        }
        // Faz o Hash da Senha
        var senha = passwordEncoder.encode(usuarioRequest.senha());
        //var senha = usuarioRequest.senha(); // provisório

        // Transformar o DTO em Entity
        var usuarioDto = UsuarioMapper.toEntity(usuarioRequest, senha);

        // Adiciona Perfil Padrão
        var perfil = perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE).orElseThrow();
        usuarioDto.adicionarPerfil(perfil);

        // Salvar usuario no banco
        var usuarioEntity = usuarioRepository.save(usuarioDto);

        // Retorna o DTO de resposta para o usuário.
        return UsuarioMapper.toDto(usuarioEntity);

    }

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // authentication provider -> userDetailsService -> passwordEncoder.matches() -> autenticado
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.senha()));

            Usuario usuario = (Usuario) authentication.getPrincipal();

            // A2F ativada e TipoA2f E-mail
            if (usuario.isA2fAtiva() && usuario.getTipoA2f().equals(TipoA2f.EMAIL)) {
                CodigoA2f codigoA2f = codigoA2fService.gerarCodigo(usuario.getId());
                emailService.enviarCodigoA2f(usuario.getEmail(), codigoA2f.getCodigo());

                return new LoginResponse(
                        null,
                        null,
                        true,
                        TipoA2f.EMAIL
                );
            }
            // A2F ativada e TipoA2f Authenticator
            if (usuario.isA2fAtiva() && usuario.getTipoA2f().equals(TipoA2f.AUTHENTICATOR)) {
                return new LoginResponse(
                        null,
                        null,
                        true,
                        TipoA2f.AUTHENTICATOR
                );
            }

            // A2F desativada → Gera JWT imediatamente
            String token = tokenProvider.gerarToken(authentication);
            LocalDateTime expirationTime = tokenProvider.getExpirationDateToken();

            return new LoginResponse(
                    token,
                    expirationTime,
                    false,
                    TipoA2f.DESATIVADA);

        } catch (BadCredentialsException e) {
            throw new RegraNegocioException("Credenciais inválidas", HttpStatus.BAD_REQUEST);
        }
    }

    public LoginResponse verificarA2f(CodigoA2fRequest codigoA2fRequest) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(codigoA2fRequest.email())
                .orElseThrow(() ->
                        new RegraNegocioException(
                                "Usuário não encontrado.",
                                HttpStatus.NOT_FOUND
                        ));

        if (!usuario.isA2fAtiva()) {
            throw new RegraNegocioException(
                    "A2F não está ativa para este usuário.",
                    HttpStatus.BAD_REQUEST
            );
        }

        boolean codigoValido;

        if (TipoA2f.EMAIL.equals(usuario.getTipoA2f())) {

            codigoValido = validarCodigoEmail(
                    usuario.getId(),
                    codigoA2fRequest.codigo()
            );

        } else if (TipoA2f.AUTHENTICATOR.equals(usuario.getTipoA2f())) {

            codigoValido = totpService.validarCodigo(
                    usuario.getA2fSecret(),
                    codigoA2fRequest.codigo()
            );

        } else {
            throw new RegraNegocioException(
                    "Tipo de A2F inválido.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!codigoValido) {
            throw new RegraNegocioException(
                    "Código de A2F inválido.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );

        String token = tokenProvider.gerarToken(authentication);

        LocalDateTime expirationTime =
                tokenProvider.getExpirationDateToken();

        return new LoginResponse(token, expirationTime, false, null);
    }

    public TotpActivationResponse iniciarAtivacaoTotp(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new RegraNegocioException(
                                "Usuário não encontrado.",
                                HttpStatus.NOT_FOUND
                        ));

        var credentials = totpService.gerarCredenciais();

        String secret = credentials.getKey();

        usuario.setA2fSecret(secret);
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);
        usuario.setA2fAtiva(false);

        usuarioRepository.save(usuario);

        String qrCodeUrl = totpService.gerarQrCodeUrl(
                "Booking MS",
                usuario.getEmail(),
                credentials
        );

        return new TotpActivationResponse(
                qrCodeUrl,
                secret
        );
    }

    public void confirmarAtivacaoTotp(Long usuarioId, String codigo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new RegraNegocioException(
                                "Usuário não encontrado.",
                                HttpStatus.NOT_FOUND
                        ));

        boolean valido = totpService.validarCodigo(
                usuario.getA2fSecret(),
                codigo
        );
        if (!valido) {
            throw new RegraNegocioException(
                    "Código TOTP inválido.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        usuario.setA2fAtiva(true);
        usuario.setTipoA2f(TipoA2f.AUTHENTICATOR);

        usuarioRepository.save(usuario);
    }

    private boolean validarCodigoEmail(Long usuarioId, String codigo) {

        CodigoA2f codigoA2f = codigoA2fRepository
                .findByUsuarioIdAndCodigoAndUtilizadoFalse(
                        usuarioId,
                        codigo
                )
                .orElseThrow(() ->
                        new RegraNegocioException(
                                "Código inválido.",
                                HttpStatus.UNAUTHORIZED
                        ));

        if (codigoA2f.getExpiracao().isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException(
                    "Código expirado.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        codigoA2f.setUtilizado(true);
        codigoA2fRepository.save(codigoA2f);

        return true;
    }
}
