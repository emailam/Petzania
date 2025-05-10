package com.example.friends.and.chats.module.model.principal;

import com.example.friends.and.chats.module.model.entity.Admin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class AdminPrincipal implements UserDetails {

    private Admin admin;
    private GrantedAuthority grantedAuthority;

    public AdminPrincipal(Admin admin) {
        this.admin = admin;
    }

    public void setGrantedAuthority(GrantedAuthority grantedAuthority){
        this.grantedAuthority = grantedAuthority;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(grantedAuthority == null){
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
        return admin.getUsername();
    }

}