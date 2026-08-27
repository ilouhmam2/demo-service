package fr.francetv.demo.infrastructure.adapter.out.rest;

import fr.francetv.demo.client.api.ExternalItemsApi;
import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.out.ExternalItemPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Component
public class ExternalItemRestAdapter implements ExternalItemPort {

    private static final Logger log = LoggerFactory.getLogger(ExternalItemRestAdapter.class);

    private final ExternalItemsApi externalItemsApi;

    public ExternalItemRestAdapter(ExternalItemsApi externalItemsApi) {
        this.externalItemsApi = externalItemsApi;
    }

    @Override
    public List<Item> fetchAll() {
        try {
            List<fr.francetv.demo.client.model.ExternalItem> items =
                    externalItemsApi.getExternalItems()
                            .collectList()
                            .block();
            if (items == null) {
                return List.of();
            }
            return items.stream()
                    .map(this::toDomainItem)
                    .toList();
        } catch (WebClientResponseException e) {
            log.error("external.items.client.error status={}", e.getStatusCode().value());
            throw new ExternalServiceException(
                ExternalServiceException.Kind.BAD_GATEWAY,
                "External service error: " + e.getStatusCode().value(),
                e);
        } catch (WebClientRequestException e) {
            log.error("external.items.client.unreachable", e);
            throw new ExternalServiceException(
                ExternalServiceException.Kind.SERVICE_UNAVAILABLE,
                "External service unreachable",
                e);
        }
    }

    private Item toDomainItem(fr.francetv.demo.client.model.ExternalItem external) {
        return new Item(null, external.getName(), external.getDescription(), null, null);
    }
}
