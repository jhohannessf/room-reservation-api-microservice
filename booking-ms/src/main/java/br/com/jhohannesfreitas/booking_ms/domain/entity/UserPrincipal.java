package br.com.jhohannesfreitas.booking_ms.domain.entity;

public class UserPrincipal {
    private final Long id;
    private final String email;

    public UserPrincipal(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
