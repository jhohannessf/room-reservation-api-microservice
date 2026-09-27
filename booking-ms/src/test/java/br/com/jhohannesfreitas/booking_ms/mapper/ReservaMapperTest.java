package br.com.jhohannesfreitas.booking_ms.mapper;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import br.com.jhohannesfreitas.booking_ms.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservaMapperTest {

    @Test
    @DisplayName("Deveria mapear ReservaRequest para entidade Reserva corretamente")
    void deveriaMapearReservaRequestParaEntidadeQuandoDadosForemCorretos() {
        // Arrange
        Long usuarioId = 1L;
        Long salaId = 2L;
        ReservaRequest reservaRequest = new ReservaRequest(
                salaId,
                LocalDate.now(),
                LocalTime.of(9,0),
                LocalTime.of(10,0),
                20
        );

        // ACT
        Reserva reserva = ReservaMapper.toEntity(reservaRequest, usuarioId, salaId);

        // ASSERT
        assertAll(
                () -> assertEquals(reservaRequest.data(), reserva.getData()),
                () -> assertEquals(reservaRequest.horaInicial(), reserva.getHoraInicial()),
                () -> assertEquals(reservaRequest.horaFinal(), reserva.getHoraFinal()),
                () -> assertEquals(reservaRequest.quantidadePessoas(), reserva.getQuantidadePessoas()),
                () -> assertEquals(usuarioId, reserva.getUsuarioId()),
                () -> assertEquals(salaId, reserva.getSalaId()),
                () -> assertEquals(StatusReserva.ATIVA, reserva.getStatus()),
                () -> assertNull(reserva.getId()) // ainda não persistida, id só existe após o save()
        );
    }

    @Test
    @DisplayName("Deveria mapear entidade Reserva para ReservaResponse corretamente")
    void deveriaMapearReservaParaResponseQuandoDadosForemCorretos() {
        // ARRANGE
        Reserva reserva = new Reserva(
                LocalDate.now(),
                LocalTime.of(9,0),
                LocalTime.of(10,0),
                20
        );
        reserva.setUsuarioId(1L);
        reserva.setSalaId(10L);
        ReflectionTestUtils.setField(reserva, "id", 100L);

        // ACT
        ReservaResponse reservaResponse = ReservaMapper.toDto(reserva);

        // ASSERT
        assertAll(
                () -> assertEquals(reserva.getId(), reservaResponse.id()),
                () -> assertEquals(reserva.getUsuarioId(), reservaResponse.usuarioId()),
                () -> assertEquals(reserva.getSalaId(), reservaResponse.salaId()),
                () -> assertEquals(reserva.getData(), reservaResponse.data()),
                () -> assertEquals(reserva.getHoraInicial(), reservaResponse.horaInicial()),
                () -> assertEquals(reserva.getHoraFinal(), reservaResponse.horaFinal()),
                () -> assertEquals(reserva.getQuantidadePessoas(), reservaResponse.quantidadePessoas()),
                () -> assertEquals(reserva.getStatus(), reservaResponse.status())
        );

    }
}