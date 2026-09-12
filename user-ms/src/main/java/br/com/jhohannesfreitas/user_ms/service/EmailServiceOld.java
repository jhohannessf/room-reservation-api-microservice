package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailServiceOld {

    @Value("${spring.mail.username}")
    private String EMAIL_ORIGEM;
    private static final String NOME_ENVIADOR = "Teste";
    private static final String URL_SITE = "http://localhost:8080";

    private final JavaMailSender enviadorEmail;
    private final UsuarioRepository usuarioRepository;

    public EmailServiceOld(JavaMailSender enviadorEmail, UsuarioRepository usuarioRepository) {
        this.enviadorEmail = enviadorEmail;
        this.usuarioRepository = usuarioRepository;
    }

    private void enviarEmail(String emailUsuario, String assunto, String conteudo) {
        MimeMessage message = enviadorEmail.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        try {
            helper.setFrom(EMAIL_ORIGEM, NOME_ENVIADOR);
            helper.setTo(emailUsuario);
            helper.setSubject(assunto);
            helper.setText(conteudo, true);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RegraNegocioException("Erro ao enviar email",
                    HttpStatus.BAD_REQUEST);
        }

        enviadorEmail.send(message);
    }

    public void enviarEmailVerificacao(Usuario usuario, CodigoA2f codigoA2f) {
        String assunto = "Aqui está seu link para verificar o email";
        String conteudo = gerarConteudoEmail("Olá [[name]],<br>"
                + "Por favor clique no link abaixo para verificar sua conta:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFICAR</a></h3>"
                + "Obrigado,<br>"
                + "Teste :).", usuario.getNome(), URL_SITE + "/api/v1/usuario/verificar-conta?token=" + codigoA2f.getCodigo());

        enviarEmail(usuario.getUsername(), assunto, conteudo);
    }

    public void enviarEmailA2f(Usuario usuario, String codigoString) {
        String assunto = "Aqui está seu código de verificação";
        String conteudo = gerarConteudoEmail("Olá [[name]],<br>"
                + "Seu código de verificação é<br>"
                + "<h3>[[URL]]</h3><br>"
                + "Obrigado,<br>"
                + "Fórum Hub :).", usuario.getNome(), codigoString);

        enviarEmail(usuario.getUsername(), assunto, conteudo);
    }

    private String gerarConteudoEmail(String template, String nome, String url) {
        return template.replace("[[name]]", nome).replace("[[URL]]", url);
    }
}
