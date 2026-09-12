package br.com.jhohannesfreitas.user_ms.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoA2f(String email, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Código de autenticação - A2F");
        message.setText("""
                Olá!

                Seu código de autenticação em dois fatores é:

                %s

                Esse código é válido por 5 minutos.

                Se você não solicitou esse código, ignore este e-mail.
                """.formatted(codigo));

        mailSender.send(message);
    }
}
