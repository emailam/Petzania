package com.example.registrationmodule.config;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PayPalConfig {
    @Value("${paypal.client-id}")
    private String clientId;
    @Value("${paypal.client-secret}")
    private String clientSecret;
    @Value("${paypal.mode}")
    private String mode;
    @Value("${paypal.intent}")
    private String intent;
    @Value("${paypal.landing-page}")
    private String landingPage;
    @Value("${paypal.user-action}")
    private String userAction;
    @Value("${paypal.brand-name}")
    private String brandName;
    @Value("${paypal.cancel-url}")
    private String cancelUrl;
    @Value("${paypal.return-url}")
    private String returnUrl;

    @Bean
    public PayPalHttpClient payPalHttpClient() {
        PayPalEnvironment environment;

        if ("live".equalsIgnoreCase(mode)) {
            environment = new PayPalEnvironment.Live(clientId, clientSecret);
        } else {
            environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        }

        return new PayPalHttpClient(environment);

    }

}

