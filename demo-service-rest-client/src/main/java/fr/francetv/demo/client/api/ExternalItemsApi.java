package fr.francetv.demo.client.api;

import fr.francetv.demo.client.model.ExternalItem;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Thin WebClient-based API client for the External Items API.
 * Hand-crafted for Spring Framework 7.x compatibility.
 */
public class ExternalItemsApi {

    private final WebClient webClient;

    public ExternalItemsApi(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<ExternalItem> getExternalItems() {
        return webClient.get()
                .uri("/api/external/items")
                .retrieve()
                .bodyToFlux(ExternalItem.class);
    }
}
