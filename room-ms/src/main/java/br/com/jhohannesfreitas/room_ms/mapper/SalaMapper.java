package br.com.jhohannesfreitas.room_ms.mapper;

import br.com.jhohannesfreitas.room_ms.domain.entity.Sala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import org.springframework.stereotype.Component;

@Component
public class SalaMapper {

    public static Sala toEntity(SalaRequest salaRequest) {
        return new Sala(
                salaRequest.numero(),
                salaRequest.capacidade()
        );
    }

    public static SalaResponse toDTO(Sala sala) {
        return new SalaResponse(
                sala.getId(),
                sala.getNumero(),
                sala.getCapacidade(),
                sala.getStatus()
        );
    }

}
