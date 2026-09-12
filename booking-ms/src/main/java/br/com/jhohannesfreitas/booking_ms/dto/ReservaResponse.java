package br.com.jhohannesfreitas.booking_ms.dto;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaResponse(
        Long id,
        Long usuarioId,
        Long salaId,
        LocalDate data,
        LocalTime horaInicial,
        LocalTime horaFinal,
        Integer quantidadePessoas,
        StatusReserva status
) {
}
