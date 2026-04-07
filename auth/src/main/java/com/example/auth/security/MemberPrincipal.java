package com.example.auth.security;

import com.example.member.domain.MemberRole;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MemberPrincipal implements UserDetails {

    @Getter
    private final Long id;

    private final String email;

    @Getter
    private final List<MemberRole> roles;

    public MemberPrincipal(Long id, String email, MemberRole role) {
        this(id, email, List.of(role));
    }

    public MemberPrincipal(Long id, String email, List<MemberRole> roles) {
        this.id = id;
        this.email = email;
        this.roles = List.copyOf(roles);
    }

    public MemberRole getRole() {
        return roles.isEmpty() ? null : roles.get(0);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return JwtTokenProvider.toGrantedAuthorities(roles);
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
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
