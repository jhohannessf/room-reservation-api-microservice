package br.com.jhohannesfreitas.booking_ms.repository;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findBySalaIdAndDataAndStatusIn(Long salaId, LocalDate data, List<StatusReserva> statusReserva);

    List<Reserva> findBySalaIdAndDataAndStatusInAndIdNot(Long salaId, LocalDate data, List<StatusReserva> statusReserva, Long idReserva); // IdNot significa "ID diferente de"

    List<Reserva> findByStatus(StatusReserva statusReserva);

    Page<Reserva> findBySalaIdAndDataBetween(Long salaId, LocalDate inicio, LocalDate fim, Pageable pageable);
}
