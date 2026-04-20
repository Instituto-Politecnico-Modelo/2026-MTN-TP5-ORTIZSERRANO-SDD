package com.DecanatoOrtizSerrano.OrtizSerranoTP3.security;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Administrador;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Docente;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Padre;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementación de UserDetails para Spring Security
 */
public class UserDetailsImpl implements UserDetails {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String username;
    private String email;
    
    @JsonIgnore
    private String password;
    
    private Collection<? extends GrantedAuthority> authorities;
    
    public UserDetailsImpl(Long id, String username, String email, String password,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }
    
    public static UserDetailsImpl build(Usuario usuario) {
        // Detectar el rol según el subtipo real del usuario
        String role;
        if (usuario instanceof Administrador) {
            role = "ADMINISTRADOR";
        } else if (usuario instanceof Docente) {
            role = "DOCENTE";
        } else if (usuario instanceof Estudiante) {
            role = "ESTUDIANTE";
        } else if (usuario instanceof Padre) {
            role = "PADRE";
        } else {
            role = "USUARIO";
        }

        Collection<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(role)
        );
        
        return new UserDetailsImpl(
            usuario.getIdUsuario(),
            usuario.getEmail(),
            usuario.getEmail(),
            usuario.getPassword(),
            authorities
        );
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
