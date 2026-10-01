package br.com.jhohannesfreitas.booking_ms.repository;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ReservaRepository reservaRepository;

    @AfterEach
    void tearDown() {
        reservaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deveria buscar reservas por salaId, data e status")
    void findBySalaIdAndDataAndStatus() {
        // ARRANGE
        Reserva reserva = criarReserva(LocalDate.now(), 20);
        reserva.setUsuarioId(1L);
        reserva.setSalaId(10L);
        reserva.setStatus(StatusReserva.ATIVA);

        // Salva
        Reserva reservaPersistida = reservaRepository.save(reserva);

        // ACT
        List<Reserva> reservas = reservaRepository.findBySalaIdAndDataAndStatus(reservaPersistida.getSalaId(), reservaPersistida.getData(), reservaPersistida.getStatus());

        // ASSERT
        assertThat(reservas)
                .hasSize(1)
                .first()
                .usingRecursiveComparison()
                .isEqualTo(reservaPersistida);
    }

    @Test
    @DisplayName("Deveria buscar reservas por salaId, data e status. Retornando lista vazia.")
    void findBySalaIdAndDataAndStatusReturnEmptyList() {
        // ARRANGE
        // ACT
        List<Reserva> reservas = reservaRepository.findBySalaIdAndDataAndStatus(
                999L,
                LocalDate.now(),
                StatusReserva.ATIVA
        );
        // ASSERT
        assertThat(reservas).isEmpty();
    }

    @Test
    @DisplayName("Deveria buscar reservas por salaId, data e status, ignorando o próprio ID")
    void findBySalaIdAndDataAndStatusAndIdNot() {
        // ARRANGE
        Reserva reserva = criarReserva(LocalDate.now(), 20);
        reserva.setUsuarioId(1L);
        reserva.setSalaId(10L);
        reserva.setStatus(StatusReserva.ATIVA);

        // Salva
        Reserva reservaPersistida = reservaRepository.save(reserva);

        // ACT
        List<Reserva> reservas = reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(reservaPersistida.getSalaId(), reservaPersistida.getData(), reservaPersistida.getStatus(), reservaPersistida.getId());

        // ASSERT

        assertAll(
                () -> assertEquals(0, reservas.size()),
                () -> assertEquals(reserva.getId(), reservaPersistida.getId()),
                () -> assertEquals(reserva.getUsuarioId(), reservaPersistida.getUsuarioId()),
                () -> assertEquals(reserva.getSalaId(), reservaPersistida.getSalaId()),
                () -> assertEquals(reserva.getData(), reservaPersistida.getData()),
                () -> assertEquals(reserva.getQuantidadePessoas(), reservaPersistida.getQuantidadePessoas()),
                () -> assertEquals(reserva.getStatus(), reservaPersistida.getStatus())
        );
    }

    @Test
    @DisplayName("Deveria buscar reservas por salaId, data e status, ignorando o próprio ID e retornando outra reserva.")
    void findBySalaIdAndDataAndStatusAndIdNotReturnOtherBooking() {
        // ARRANGE
        Reserva reserva = criarReserva(LocalDate.now(), 20);
        reserva.setUsuarioId(1L);
        reserva.setSalaId(10L);
        reserva.setStatus(StatusReserva.ATIVA);

        Reserva reserva2 = criarReserva(LocalDate.now(), 20);
        reserva2.setUsuarioId(1L);
        reserva2.setSalaId(10L);
        reserva2.setStatus(StatusReserva.ATIVA);

        // Salva
        Reserva reservaPersistida = reservaRepository.save(reserva);
        Reserva reservaPersistida2 = reservaRepository.save(reserva2);

        // ACT
        List<Reserva> reservas =
                reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(
                        reservaPersistida.getSalaId(),
                        reservaPersistida.getData(),
                        reservaPersistida.getStatus(),
                        reservaPersistida.getId()
                );
        // ASSERT
        assertThat(reservas)
                .hasSize(1)
                .extracting(Reserva::getId)
                .containsExactly(reservaPersistida2.getId());
    }

    @Test
    @DisplayName("Deveria buscar reservas da sala dentro do intervalo de datas")
    void findBySalaIdAndDataBetween() {
        // ARRANGE
        Reserva reserva1 = criarReserva(LocalDate.of(2026, 10,1), 20);
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        reserva1.setStatus(StatusReserva.ATIVA);

        Reserva reserva2 = criarReserva(LocalDate.of(2026, 10, 5), 20);
        reserva2.setUsuarioId(1L);
        reserva2.setSalaId(10L);
        reserva2.setStatus(StatusReserva.ATIVA);

        Reserva reserva3 = criarReserva(LocalDate.of(2026, 10, 10), 20);
        reserva3.setUsuarioId(1L);
        reserva3.setSalaId(10L);
        reserva3.setStatus(StatusReserva.ATIVA);

        // Salvando todas as reservas no banco
        reservaRepository.saveAll(List.of(reserva1, reserva2, reserva3));

        // Paginação
        Pageable pageable = PageRequest.of(0, 10);

        // ACT
        Page<Reserva> resultado = reservaRepository.findBySalaIdAndDataBetween(
                10L,
                LocalDate.of(2026,10, 1),
                LocalDate.of(2026,10, 10),
                pageable
        );

        // ASSERT
        assertThat(resultado.getContent())
            .hasSize(3);
    }

    @Test
    @DisplayName("Deveria buscar reservas da sala dentro do intervalo de datas respeitando a paginação")
    void findBySalaIdAndDataBetweenConfirmationPagination() {
        // ARRANGE
        Reserva reserva1 = criarReserva(LocalDate.of(2026, 10,1), 20);
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        reserva1.setStatus(StatusReserva.ATIVA);

        Reserva reserva2 = criarReserva(LocalDate.of(2026, 10, 5), 20);
        reserva2.setUsuarioId(1L);
        reserva2.setSalaId(10L);
        reserva2.setStatus(StatusReserva.ATIVA);

        Reserva reserva3 = criarReserva(LocalDate.of(2026, 10, 10), 20);
        reserva3.setUsuarioId(1L);
        reserva3.setSalaId(10L);
        reserva3.setStatus(StatusReserva.ATIVA);

        // Salvando todas as reservas no banco
        reservaRepository.saveAll(List.of(reserva1, reserva2, reserva3));

        // Paginação
        Pageable pageable = PageRequest.of(0, 2);

        // ACT
        Page<Reserva> resultado = reservaRepository.findBySalaIdAndDataBetween(
                10L,
                LocalDate.of(2026,10, 1),
                LocalDate.of(2026,10, 10),
                pageable
        );

        // ASSERT
        assertThat(resultado.getContent())
                .hasSize(2);
        assertThat(resultado.getTotalElements())
                .isEqualTo(3);
        assertThat(resultado.getTotalPages())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Deveria buscar reserva no banco por Status")
    void findByStatus() {
        // ARRANGE
        Reserva reserva1 = criarReserva(LocalDate.now(), 20);
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        reserva1.setStatus(StatusReserva.ATIVA);

        Reserva reserva2 = criarReserva(LocalDate.now(), 20);
        reserva2.setUsuarioId(1L);
        reserva2.setSalaId(10L);
        reserva2.setStatus(StatusReserva.CANCELADA);

        // Salva
        Reserva reservaPersistida1 = reservaRepository.save(reserva1);
        Reserva reservaPersistida2 = reservaRepository.save(reserva2);

        // ACT
        List<Reserva> reservas =
                reservaRepository.findByStatus(reservaPersistida1.getStatus());
        // ASSERT
        assertThat(reservas)
                .hasSize(1)
                .extracting(Reserva::getId)
                .containsExactly(reservaPersistida1.getId());
    }

    @Test
    @DisplayName("Deveria retornar lista vazia quando não houver reservas com o status informado")
    void findByStatusReturnEmpty() {
        // ACT
        List<Reserva> reservas =
                reservaRepository.findByStatus(StatusReserva.ATIVA);

        // ASSERT
        assertThat(reservas).isEmpty();
    }

    private Reserva criarReserva(LocalDate data, Integer quantidadePessoas) {
        return new Reserva(
                data, LocalTime.of(10,0), LocalTime.of(11,0), quantidadePessoas
        );
    }
}