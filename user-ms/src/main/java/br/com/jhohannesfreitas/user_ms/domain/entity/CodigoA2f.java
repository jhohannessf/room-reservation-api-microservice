package br.com.jhohannesfreitas.user_ms.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_a2f")
public class CodigoA2f {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(nullable = false)
    private boolean utilizado = false;

    public CodigoA2f() {}

    public CodigoA2f(Long usuarioId, String codigo, LocalDateTime expiracao) {
        this.usuarioId = usuarioId;
        this.codigo = codigo;
        this.expiracao = expiracao;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getExpiracao() {
        return expiracao;
    }

    public boolean isUtilizado() {
        return utilizado;
    }

    public void setUtilizado(boolean utilizado) {
        this.utilizado = utilizado;
    }
}
