package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.enums.TipoA2f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Deveria enviar email com código A2F")
    void deveriaEnviarEmailComCodigoA2f() {
        // ARRANGE
        String email = "teste@email.com";
        String codigo = "123456";

        // ACT
        emailService.enviarCodigoA2f(email, codigo);

        // ASSERT
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // Captura a mensagem que o EmailService enviou ao mailSender e verificar seu conteúdo
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        then(mailSender).should().send(captor.capture());

        SimpleMailMessage mensagem = captor.getValue();

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertArrayEquals(
                        new String[]{email},
                        mensagem.getTo()
                ),
                () -> assertEquals(
                        "Código de autenticação - A2F",
                        mensagem.getSubject()
                ),
                () -> assertTrue(mensagem.getText().contains(codigo)),
                () -> assertTrue(mensagem.getText().contains("5 minutos"))
        );

    }

    @Test
    @DisplayName("Deveria propagar exceção quando falhar o envio do e-mail")
    void deveriaPropagarExcecaoQuandoFalharEnvioEmail() {
        // ARRANGE
        String email = "teste@email.com";
        String codigo = "123456";

        willThrow(new RuntimeException("Falha no envio"))
                .given(mailSender)
                .send(any(SimpleMailMessage.class));

        // ACT + ASSERT
        assertThrows(
                RuntimeException.class,
                () -> emailService.enviarCodigoA2f(email, codigo)
        );
    }
}