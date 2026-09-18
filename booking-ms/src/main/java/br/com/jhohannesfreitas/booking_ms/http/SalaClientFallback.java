package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SalaClientFallback implements SalaClient {
    @Override
    public SalaRequest buscarPorId(Long id) {
        throw new RegraNegocioException("O Serviço room-ms está indisponível no momento.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public SalaRequest alterarStatusSala(Long id, StatusSalaRequest statusSalaRequest) {
        throw new RegraNegocioException("O Serviço room-ms está indisponível no momento.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
