package br.com.jhohannesfreitas.user_ms.domain.entity;

import br.com.jhohannesfreitas.user_ms.domain.enums.ProvedorLogin;
import br.com.jhohannesfreitas.user_ms.domain.enums.TipoA2f;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuarios_perfis",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns =  @JoinColumn(name = "perfil_id"))
    private List<Perfil> perfis = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProvedorLogin provedorLogin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoA2f tipoA2f;

    @Column(nullable = false)
    private Boolean a2fAtiva;

    private String a2fSecret;

    public Usuario() {}

    public Usuario(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public Long getId() {
        return id;
    }
    
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public ProvedorLogin getProvedorLogin() {
        return provedorLogin;
    }

    public TipoA2f getTipoA2f() {
        return tipoA2f;
    }

    public Boolean isA2fAtiva() {
        return a2fAtiva;
    }

    public void setTipoA2f(TipoA2f tipoA2f) {
        this.tipoA2f = tipoA2f;
    }

    public void setA2fAtiva(Boolean a2fAtiva) {
        this.a2fAtiva = a2fAtiva;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public List<Perfil> getPerfis() {
        return perfis;
    }

    public void setProvedorLogin(ProvedorLogin provedorLogin) {
        this.provedorLogin = provedorLogin;
    }

    public String getA2fSecret() {
        return a2fSecret;
    }

    public void setA2fSecret(String a2fSecret) {
        this.a2fSecret = a2fSecret;
    }

    public void adicionarPerfil(Perfil perfil) {
        this.perfis.add(perfil);
    }

    public void removePerfil(Perfil perfil) {
        this.perfis.remove(perfil);
    }

    public void AtivarA2f() {
        this.a2fAtiva = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return perfis;
    }

    @Override
    public @Nullable String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
