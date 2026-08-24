package fr.francetv.demo.infrastructure.config;

import fr.francetv.demo.client.api.ExternalItemsApi;
import fr.francetv.foundation.httpclient.properties.HttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Configuration
public class ExternalItemsClientConfig {

    @Bean
    public ExternalItemsApi externalItemsApi(
            WebClient.Builder webClientBuilder,
            HttpClientProperties properties) {

        String baseUrl = Optional.ofNullable(properties.clients())
                .map(c -> c.get("external-items"))
                .map(HttpClientProperties.ClientConfig::baseUrl)
                .orElse("http://localhost:8089");

        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        return new ExternalItemsApi(webClient);
    }
}
