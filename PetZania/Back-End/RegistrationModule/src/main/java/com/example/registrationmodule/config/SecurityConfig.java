package com.example.registrationmodule.config;

import com.example.registrationmodule.filter.JWTFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JWTFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // disable the need for CSRF Token since it will be stateless.
        http.csrf(AbstractHttpConfigurer::disable);

        // enabling cross-origin resource sharing
        http.cors(Customizer.withDefaults());

        // make every request needs an authorization.
        http.authorizeHttpRequests(request ->
                request.requestMatchers("/api/user/auth/signup",
                                "/api/user/auth/sendResetPasswordOTP",
                                "/api/user/auth/verifyResetOTP",
                                "/api/user/auth/login",
                                "/api/user/auth/refresh-token",
                                "/api/user/auth/verify",
                                "/api/user/auth/resendOTP",
                                "/api/user/auth/resetPassword",
                                "/api/admin/login",
                                "/api/admin/refresh-token",
                                "/api/swagger-ui/**",
                                "/api/admin")
                        .permitAll()

                        .requestMatchers("/api/admin/create", "/api/admin/delete/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/payment/create").hasRole("USER")
                        .requestMatchers("/api/payment/refund").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/user/auth/block",
                                "/api/user/auth/unblock",
                                "/api/user/auth/deleteAll").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .requestMatchers("/api/user/auth/delete", "/api/user/auth/users").hasAnyRole("ADMIN", "USER", "SUPER_ADMIN")
                        .requestMatchers("/api/payment/**").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/user/auth/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN").anyRequest().authenticated());

        // Enable the form login.
        // http.formLogin(Customizer.withDefaults());

        // Enable it to be able to sign in from postman.
        http.httpBasic(Customizer.withDefaults());

        // This makes the session Stateless so we no longer need the CSRF token.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // we will add a filter before the UserPasswordAuthentication filter.
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        UserDetails user1 = User
//                .withDefaultPasswordEncoder()
//                .username("admin")
//                .password("1234")
//                .roles("USER").build();
//
//        return new InMemoryUserDetailsManager(user1);
//    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // your frontend origin
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // allow cookies/auth headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
