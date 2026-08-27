package fr.francetv.demo.infrastructure.adapter.out.messaging;

import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.out.ItemEventPublisher;
import fr.francetv.foundation.common.CorrelationIdUtils;
import fr.francetv.foundation.core.CorrelationIdFilter;
import fr.francetv.foundation.nats.publisher.NatsMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * NATS adapter for {@link ItemEventPublisher}.
 *
 * <p>Publishes an {@code item.created} event on the NATS subject {@code item.created}
 * whenever an item is successfully persisted. Correlation ID is propagated from the
 * current MDC context (set by {@link CorrelationIdFilter} during HTTP request processing).
 *
 * <p>Publish failures are caught and logged as warnings so that a NATS outage never
 * prevents item creation from succeeding.
 */
@Component
public class NatsItemEventPublisher implements ItemEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NatsItemEventPublisher.class);
    private static final String SUBJECT = "item.created";
    private static final String TYPE = "item.created";

    private final NatsMessagePublisher natsPublisher;

    public NatsItemEventPublisher(NatsMessagePublisher natsPublisher) {
        this.natsPublisher = natsPublisher;
    }

    @Override
    public void publishItemCreated(Item item) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = CorrelationIdUtils.generate();
        }
        ItemCreatedPayload payload = new ItemCreatedPayload(item.getId(), item.getName());
        try {
            natsPublisher.publish(SUBJECT, TYPE, correlationId, payload);
        } catch (Exception e) {
            log.warn("Failed to publish NATS event '{}' for item id={}: {}",
                    TYPE, item.getId(), e.getMessage());
        }
    }

    record ItemCreatedPayload(Long itemId, String name) {}
}
