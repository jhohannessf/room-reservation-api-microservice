package br.com.jhohannesfreitas.booking_ms.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeingConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {

            Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if (authentication != null && authentication.getCredentials() != null) {

                String token = authentication.getCredentials().toString();
                requestTemplate.header("Authorization", "Bearer " + token);
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new FeignErrorDecoder(objectMapper);
    }
}
