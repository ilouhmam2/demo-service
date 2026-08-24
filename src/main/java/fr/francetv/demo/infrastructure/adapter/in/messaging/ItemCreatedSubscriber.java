package fr.francetv.demo.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.francetv.foundation.nats.publisher.MessageEnvelope;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Subscribes to the {@code item.created} NATS subject and logs each received event.
 *
 * <p>Implements {@link SmartLifecycle} to cleanly start the NATS dispatcher after
 * context startup and stop it during shutdown. If NATS is unavailable at startup
 * the exception is caught and logged — the application continues without the subscriber.
 */
@Component
public class ItemCreatedSubscriber implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ItemCreatedSubscriber.class);
    private static final String SUBJECT = "item.created";

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    private volatile boolean running = false;
    private Dispatcher dispatcher;

    public ItemCreatedSubscriber(Connection natsConnection, ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        try {
            dispatcher = natsConnection.createDispatcher(message -> {
                try {
                    MessageEnvelope envelope = objectMapper.readValue(message.getData(), MessageEnvelope.class);
                    String correlationId = envelope.correlationId() != null ? envelope.correlationId() : "";
                    MDC.put("correlationId", correlationId);
                    log.info("Received NATS event type={} id={} source={} correlationId={}",
                            envelope.type(), envelope.id(), envelope.source(), correlationId);
                } catch (Exception e) {
                    log.error("Failed to process message on subject '{}': {}", SUBJECT, e.getMessage());
                } finally {
                    MDC.remove("correlationId");
                }
            });
            dispatcher.subscribe(SUBJECT);
            running = true;
            log.info("Subscribed to NATS subject '{}'", SUBJECT);
        } catch (Exception e) {
            log.warn("Could not subscribe to NATS subject '{}': {}", SUBJECT, e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (dispatcher != null) {
            try {
                natsConnection.closeDispatcher(dispatcher);
            } catch (Exception e) {
                log.warn("Error closing NATS dispatcher: {}", e.getMessage());
            }
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
