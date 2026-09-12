package br.com.jhohannesfreitas.user_ms.infra.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(RegraNegocioException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {


        // Mensagem personalizada
        String mensagem;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            // Busca os enums requeridos de acordo com o getRequiredType, que retorna StatusCandidaturaEnum.class
            Object[] valores = ex.getRequiredType().getEnumConstants();

            String valoresValidos = Arrays.stream(valores)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            mensagem = "Valor '" + ex.getValue() +
                    "' inválido para o parâmetro '" + ex.getName() +
                    "'. Valores permitidos: " +
                    valoresValidos;
        } else {
            mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "'.";
        }

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Requisição inválida",
                mensagem,
                LocalDateTime.now()
               );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Recurso não autorizado",
                ex.getMessage(),
                LocalDateTime.now()
                );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Requisição inválida",
                "Perfil inválido. Valores permitidos: ESTUDANTE, ADMINISTRADOR, INSTRUTOR, MODERADOR",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno do servidor",
                ex.getMessage(),
                LocalDateTime.now()                );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
