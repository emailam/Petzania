package com.example.notificationmodule.model.principal;

import com.example.notificationmodule.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private User user;
    private GrantedAuthority grantedAuthority;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public void setGrantedAuthority(GrantedAuthority grantedAuthority) {
        this.grantedAuthority = grantedAuthority;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (grantedAuthority == null) {
            return Collections.singleton(new SimpleGrantedAuthority("NONE"));
        }
        return Collections.singleton(grantedAuthority);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public User getUser() {
        return user;
    }

    public UUID getUserId() {
        return user.getUserId();
    }
}

