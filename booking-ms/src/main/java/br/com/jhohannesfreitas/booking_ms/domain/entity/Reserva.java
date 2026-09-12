package br.com.jhohannesfreitas.booking_ms.domain.entity;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;
    @Column(nullable = false)
    private LocalTime horaInicial;
    @Column(nullable = false)
    private LocalTime horaFinal;

    @Column(nullable = false)
    private Integer quantidadePessoas;

    @Enumerated(EnumType.STRING)
    private StatusReserva status = StatusReserva.ATIVA;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Long salaId;

    public Reserva() {}

    public Reserva(LocalDate data, LocalTime horaInicial, LocalTime horaFinal, Integer quantidadePessoas) {
        this.data = data;
        this.horaInicial = horaInicial;
        this.horaFinal = horaFinal;
        this.quantidadePessoas = quantidadePessoas;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getHoraInicial() {
        return horaInicial;
    }


    public LocalTime getHoraFinal() {
        return horaFinal;
    }

    public Integer getQuantidadePessoas() {
        return quantidadePessoas;
    }


    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getSalaId() {
        return salaId;
    }

    public void setSalaId(Long salaId) {
        this.salaId = salaId;
    }

    public void alterarData(LocalDate data) {
        this.data = data;
    }

    public void alterarHorario(LocalTime horaInicial, LocalTime horaFinal) {
        this.horaInicial = horaInicial;
        this.horaFinal = horaFinal;
    }

    public void atualizar(ReservaRequest reservaRequest, Long usuarioId, Long salaId) {
        this.data = reservaRequest.data();
        this.horaInicial = reservaRequest.horaInicial();
        this.horaFinal = reservaRequest.horaFinal();
        this.quantidadePessoas = reservaRequest.quantidadePessoas();
        this.usuarioId = usuarioId;
        this.salaId = salaId;
    }
}
