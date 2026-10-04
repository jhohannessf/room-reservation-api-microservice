package br.com.jhohannesfreitas.room_ms.integration;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * Captura tudo que é impresso em System.out durante um teste (os listeners do room-ms só fazem println),
 * sem deixar de mostrar no console. Substitui o OutputCaptureExtension do Spring Boot, que dependia
 * de um ParameterResolver registrado pelo JUnit.
 *
 * Uso: start() no @BeforeEach, stop() no @AfterEach e getAll() nas asserções.
 */
public class SaidaConsole {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private PrintStream original;
    private Charset charset;

    public synchronized void start() {
        original = System.out;
        charset = original.charset();
        buffer.reset();

        OutputStream tee = new OutputStream() {
            @Override
            public void write(int b) {
                original.write(b);
                buffer.write(b);
            }

            @Override
            public void write(byte[] bytes, int off, int len) {
                original.write(bytes, off, len);
                buffer.write(bytes, off, len);
            }

            @Override
            public void flush() {
                original.flush();
            }
        };
        System.setOut(new PrintStream(tee, true, charset));
    }

    public synchronized void stop() {
        if (original != null) {
            System.setOut(original);
            original = null;
        }
    }

    public String getAll() {
        return buffer.toString(charset != null ? charset : Charset.defaultCharset());
    }
}
