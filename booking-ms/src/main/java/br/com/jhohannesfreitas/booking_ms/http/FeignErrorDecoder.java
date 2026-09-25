package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.infra.exception.ErrorResponse;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

public class FeignErrorDecoder implements ErrorDecoder {

    // Decoder padrão do próprio Feign — usado como "rede de segurança"
    // para qualquer erro que não conseguirmos traduzir.
    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper;

    public FeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());

        if (response.body() != null) {
            try (InputStream bodyStream = response.body().asInputStream()) {
                ErrorResponse errorResponse = objectMapper.readValue(bodyStream, ErrorResponse.class);
                return new RegraNegocioException(
                        errorResponse.message(),
                        status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR
                );
            } catch (IOException e) {
                // Corpo não veio no formato ErrorResponse esperado (ex: erro de
                // infraestrutura do próprio serviço remoto, não um RegraNegocioException dele).
                // Cai pro decoder padrão em vez de mascarar um problema desconhecido.
            }
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
