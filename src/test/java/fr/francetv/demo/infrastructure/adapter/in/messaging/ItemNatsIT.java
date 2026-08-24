package fr.francetv.demo.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.francetv.foundation.core.CorrelationIdFilter;
import fr.francetv.demo.infrastructure.adapter.out.persistence.ItemRepository;
import fr.francetv.foundation.nats.publisher.MessageEnvelope;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for NATS event publishing on {@code POST /api/items}.
 *
 * <p>DoD coverage:
 * <ul>
 *   <li>POST /api/items → message published on {@code item.created}</li>
 *   <li>Envelope structure: id, correlationId, source, type, payload</li>
 *   <li>Subscriber receives the message and can deserialize the envelope</li>
 *   <li>correlationId is propagated from the HTTP request header</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ItemNatsIT {

    static final GenericContainer<?> natsContainer;

    static {
        natsContainer = new GenericContainer<>(DockerImageName.parse("nats:2-alpine"))
                .withExposedPorts(4222);
        natsContainer.start();
    }

    @DynamicPropertySource
    static void natsProperties(DynamicPropertyRegistry registry) {
        registry.add("foundation.nats.server-url",
                () -> "nats://" + natsContainer.getHost() + ":" + natsContainer.getMappedPort(4222));
    }

    @Autowired
    CorrelationIdFilter correlationIdFilter;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    Connection natsConnection;

    @Autowired
    ItemRepository itemRepository;

    MockMvc mockMvc;

    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .addFilter(correlationIdFilter)
                .build();
        itemRepository.deleteAll();
    }

    @Test
    void postItem_publishesItemCreatedEvent() throws Exception {
        Subscription subscription = natsConnection.subscribe("item.created");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Widget\",\"description\":\"NATS test\"}"))
                .andExpect(status().isCreated());

        Message message = subscription.nextMessage(Duration.ofSeconds(5));
        assertThat(message).as("Expected a NATS message on item.created within 5 seconds").isNotNull();

        MessageEnvelope envelope = objectMapper.readValue(message.getData(), MessageEnvelope.class);
        assertThat(envelope.id()).isNotBlank();
        assertThat(envelope.type()).isEqualTo("item.created");
        assertThat(envelope.source()).isEqualTo("demo-service");
        assertThat(envelope.correlationId()).isNotBlank();
        assertThat(envelope.timestamp()).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), Map.class);
        assertThat(payload.get("name")).isEqualTo("Widget");
        assertThat(payload.get("itemId")).isNotNull();
    }

    @Test
    void postItem_propagatesCorrelationIdIntoEnvelope() throws Exception {
        String customCorrelationId = "test-correlation-abc-123";
        Subscription subscription = natsConnection.subscribe("item.created");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CorrelationWidget\"}")
                        .header("X-Correlation-Id", customCorrelationId))
                .andExpect(status().isCreated());

        Message message = subscription.nextMessage(Duration.ofSeconds(5));
        assertThat(message).isNotNull();

        MessageEnvelope envelope = objectMapper.readValue(message.getData(), MessageEnvelope.class);
        assertThat(envelope.correlationId()).isEqualTo(customCorrelationId);
    }
}
