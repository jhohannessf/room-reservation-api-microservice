package br.com.jhohannesfreitas.booking_ms.mapper;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import org.springframework.stereotype.Component;

@Component
public class ReservaMapper {

    public static Reserva toEntity(ReservaRequest reservaRequest, Long usuarioId, Long salaId) {
        Reserva reserva = new Reserva(
                reservaRequest.data(),
                reservaRequest.horaInicial(),
                reservaRequest.horaFinal(),
                reservaRequest.quantidadePessoas()
        );
        reserva.setUsuarioId(usuarioId);
        reserva.setSalaId(salaId);

        return reserva;
    }

    public static ReservaResponse toDto(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuarioId(),
                reserva.getSalaId(),
                reserva.getData(),
                reserva.getHoraInicial(),
                reserva.getHoraFinal(),
                reserva.getQuantidadePessoas(),
                reserva.getStatus()
        );
    }
}
