package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FeignErrorDecoderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FeignErrorDecoder decoder = new FeignErrorDecoder(objectMapper);

    @ParameterizedTest
    @CsvSource({"400,BAD_REQUEST", "403,FORBIDDEN", "404,NOT_FOUND", "409,CONFLICT"})
    @DisplayName("Deveria converter ErrorResponse do room-ms em RegraNegocioException com o mesmo status")
    void deveriaConverterErrorResponseEmRegraNegocioException(int codigo, HttpStatus esperado) {
        String json = """
                {"status":%d,"error":"x","message":"Sala com id 1 não encontrada.","timestamp":"2026-10-10T10:00:00"}
                """.formatted(codigo);

        Exception ex = decoder.decode("SalaClient#buscarPorId", montarResposta(codigo, json));

        // ACT + ASSERT
        RegraNegocioException regra = assertInstanceOf(RegraNegocioException.class, ex);

        assertAll(
                () -> assertEquals("Sala com id 1 não encontrada.", regra.getMessage()),
                () -> assertEquals(esperado, regra.getStatus())
        );
    }

    @Test
    @DisplayName("Deveria cair no decoder padrão do Feign quando o corpo não for um ErrorResponse")
    void deveriaCairNoDecoderPadraoQuandoCorpoNaoForErrorResponse() {
        String html = "<html>Bad Gateway</html>";

        Exception ex = decoder.decode("SalaClient#buscarPorId", montarResposta(502, html));

        // ACT + ASSERT
        FeignException regra = assertInstanceOf(FeignException.class, ex);

        assertEquals(502, regra.status());
    }

    @Test
    @DisplayName("Deveria cair no decoder padrão do Feign quando o corpo estiver null")
    void deveriaCairNoDecoderPadraoQuandoSemCorpo() {
        Exception ex = decoder.decode("SalaClient#buscarPorId", montarResposta(503, null));

        // ACT + ASSERT
        FeignException feign = assertInstanceOf(FeignException.class, ex);

        assertEquals(503, feign.status());
    }

    @Test
    @DisplayName("Deveria usar 500 quando o status da resposta não for reconhecido")
    void deveriaUsar500QuandoStatusNaoForReconhecido() {
        String json = """
            {"status":599,"error":"x","message":"Sala com id 1 não encontrada.","timestamp":"2026-10-10T10:00:00"}
            """;

        Exception ex = decoder.decode("SalaClient#buscarPorId", montarResposta(599, json));

        // ACT + ASSERT
        RegraNegocioException regra = assertInstanceOf(RegraNegocioException.class, ex);

        assertAll(
                () -> assertEquals("Sala com id 1 não encontrada.", regra.getMessage()),
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, regra.getStatus())
        );
    }

    private Response montarResposta(int status, String corpo) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/salas/1",
                Map.of(), null, StandardCharsets.UTF_8, null);

        Response.Builder builder = Response.builder()
                .status(status)
                .reason("qualquer")
                .request(request)      // o builder exige
                .headers(Map.of());

        if (corpo != null) {
            builder.body(corpo, StandardCharsets.UTF_8);
        }
        return builder.build();
    }


}
