package br.com.jhohannesfreitas.room_ms.domain.entity;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salas")
public class Sala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false)
    @Positive
    private Integer capacidade;

    @Enumerated(EnumType.STRING)
    private StatusSala status = StatusSala.LIVRE;

    //@OneToMany(mappedBy = "sala")
    //private List<Reserva> reservas = new ArrayList<>();

    public Sala() {
    }

    public Sala(Integer numero, Integer capacidade) {
        this.numero = numero;
        this.capacidade = capacidade;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public Integer getCapacidade() {
        return capacidade;
    }


    public StatusSala getStatus() {
        return status;
    }

    public void setStatus(StatusSala status) {
        this.status = status;
    }

    public void atualizar(SalaRequest salaRequest) {
        this.numero = salaRequest.numero();
        this.capacidade = salaRequest.capacidade();
    }

    public void alterarStatus(StatusSala status) {
        this.status = status;
    }
}
