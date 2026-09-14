package br.com.jhohannesfreitas.booking_ms.repository;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findBySalaIdAndDataAndStatus(Long salaId, LocalDate data, StatusReserva statusReserva);

    List<Reserva> findBySalaIdAndDataAndStatusAndIdNot(Long salaId, LocalDate data, StatusReserva status, Long idReserva); // IdNot significa "ID diferente de"

    Page<Reserva> findBySalaIdAndDataBetweenAndStatus(Long salaId, LocalDate inicio, LocalDate fim, StatusReserva statusReserva, Pageable pageable);

    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Reserva> findByStatus(StatusReserva statusReserva);
}
