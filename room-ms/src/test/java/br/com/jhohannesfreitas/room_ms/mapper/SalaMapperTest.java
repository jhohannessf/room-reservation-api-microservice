package br.com.jhohannesfreitas.room_ms.mapper;

import br.com.jhohannesfreitas.room_ms.domain.entity.Sala;
import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class SalaMapperTest {

    @Test
    @DisplayName("Deveria mapear SalaRequest para entidade Sala corretamente")
    void deveriaMapearSalaRequestParaEntidadeQuandoDadosForemCorretos() {
        // Arrange
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );

        // ACT
        Sala sala = SalaMapper.toEntity(salaRequest);

        // ASSERT
        assertAll(
                () -> assertEquals(salaRequest.numero(), sala.getNumero()),
                () -> assertEquals(salaRequest.capacidade(), sala.getCapacidade()),
                () -> assertEquals(StatusSala.LIVRE, sala.getStatus()),
                () -> assertNull(sala.getId()) // ainda não persistida, id só existe após o save()
        );
    }

    @Test
    @DisplayName("Deveria mapear entidade Sala para SalaResponse corretamente")
    void deveriaMapearSalaParaResponseQuandoDadosForemCorretos() {
        // ARRANGE
        Sala sala = new Sala(
                1,
                20
        );
        sala.setStatus(StatusSala.OCUPADA);
        ReflectionTestUtils.setField(sala, "id", 100L);

        // ACT
        SalaResponse salaResponse = SalaMapper.toDTO(sala);

        // ASSERT
        assertAll(
                () -> assertEquals(sala.getId(), salaResponse.id()),
                () -> assertEquals(sala.getNumero(), salaResponse.numero()),
                () -> assertEquals(sala.getCapacidade(), salaResponse.capacidade()),
                () -> assertEquals(sala.getStatus(), salaResponse.status())
        );

    }
}