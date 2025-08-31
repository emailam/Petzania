package com.example.registrationmodule.filter;

import com.example.registrationmodule.exception.authenticationAndVerificattion.InvalidToken;
import com.example.registrationmodule.model.entity.AdminPrincipal;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.repository.RevokedRefreshTokenRepository;
import com.example.registrationmodule.service.impl.JWTService;
import com.example.registrationmodule.service.impl.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

import static com.example.registrationmodule.constant.Constants.*;

@AllArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final ApplicationContext context;
    private final RevokedRefreshTokenRepository revokedRefreshTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // you get the token from the client side in the following format "Bearer Token"
        String authenticationHeader = request.getHeader(AUTHORIZATION_HEADER);
        String token = null;
        String email = null;
        String role = null;


        if (authenticationHeader != null && authenticationHeader.startsWith(STARTING_WITH_STRING)) {
            // System.out.println("I entered the JWT filter");
            token = authenticationHeader.substring(START_INDEX);
            email = jwtService.extractEmail(token);
            role = jwtService.extractRole(token);


            // if user tries to send refresh token to access the resources
            if (revokedRefreshTokenRepository.findByToken(token).isPresent()) {
                throw new InvalidToken("User uses refresh token to access resources");
            }
        }

        if (token != null && email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (Objects.equals(role, ROLE_USER)) {
                // User authentication
                UserPrincipal userDetails = (UserPrincipal) context.getBean(MyUserDetailsService.class).loadUserByEmail(email);
                userDetails.setGrantedAuthority(new SimpleGrantedAuthority(role));

                System.out.println("this is the user details " + userDetails.getEmail());
                if (jwtService.validateTokenForUser(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else if (Objects.equals(role, ROLE_ADMIN) || Objects.equals(role, ROLE_SUPER_ADMIN)) {
                // Admin authentication
                AdminPrincipal adminDetails = (AdminPrincipal) context.getBean(MyUserDetailsService.class).loadAdminByUsername(email);
                adminDetails.setGrantedAuthority(new SimpleGrantedAuthority(role));

                System.out.println("this is the admin details " + adminDetails.getUsername());
                if (jwtService.validateTokenForAdmin(token, adminDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
