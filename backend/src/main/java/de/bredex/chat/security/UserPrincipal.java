package de.bredex.chat.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public UserPrincipal(UUID id, String username, String password) {
        this(id, username, password, List.of());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
