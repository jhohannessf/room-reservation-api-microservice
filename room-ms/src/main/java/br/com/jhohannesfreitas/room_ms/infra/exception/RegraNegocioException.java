package br.com.jhohannesfreitas.room_ms.infra.exception;

import org.springframework.http.HttpStatus;

public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraNegocioException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
