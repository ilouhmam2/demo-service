package fr.francetv.demo.infrastructure.config;

import fr.francetv.foundation.nats.properties.NatsProperties;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * Overrides the default NATS {@link Connection} bean from {@code foundation-nats-starter}
 * with a resilient connection that survives NATS being unavailable at startup.
 *
 * <p>Key behaviours:
 * <ul>
 *   <li><b>No startup crash</b> — uses {@code connectReconnectOnConnect} which returns a
 *       {@link Connection} in {@code CONNECTING} state rather than throwing when NATS is
 *       unreachable at boot time.</li>
 *   <li><b>Unlimited automatic reconnect</b> — retries every 2 seconds indefinitely.</li>
 *   <li><b>Connection events logged</b> — state transitions visible at INFO level.</li>
 * </ul>
 *
 * <p>Because this is a user-defined {@code @Configuration} class it is processed before
 * the starter auto-configuration. The starter's {@code @ConditionalOnMissingBean}
 * on the {@link Connection} bean then skips its own definition.
 */
@Configuration
public class NatsConnectionConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsConnectionConfig.class);

    @Bean(destroyMethod = "close")
    public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException {
        Options options = new Options.Builder()
                .server(properties.serverUrl())
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(2))
                .connectionTimeout(Duration.ofSeconds(5))
                .errorListener(new io.nats.client.ErrorListener() {
                    @Override
                    public void exceptionOccurred(Connection conn, Exception exp) {
                        log.warn("NATS error: {}", exp.getMessage());
                    }
                })
                .connectionListener((conn, type) ->
                        log.info("NATS connection event: {} status={}", type, conn.getStatus()))
                .build();
        try {
            return Nats.connectReconnectOnConnect(options);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}

