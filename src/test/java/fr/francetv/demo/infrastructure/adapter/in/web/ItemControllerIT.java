package fr.francetv.demo.infrastructure.adapter.in.web;

import fr.francetv.demo.infrastructure.adapter.out.persistence.ItemRepository;
import fr.francetv.foundation.core.CorrelationIdFilter;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying the ItemController DoD:
 * <ul>
 *   <li>POST /api/items valid body → 201 + Location header</li>
 *   <li>POST /api/items invalid body → 400 with structured errors array</li>
 *   <li>GET /api/items/999 → 404 standard JSON</li>
 *   <li>X-Correlation-Id generated in every response</li>
 *   <li>X-Correlation-Id from request echoed unchanged</li>
 *   <li>Response is DTO, not JPA entity</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ItemControllerIT {

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
    WebApplicationContext wac;

    @Autowired
    CorrelationIdFilter correlationIdFilter;

    @Autowired
    ItemRepository itemRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .addFilter(correlationIdFilter)
                .build();
        itemRepository.deleteAll();
    }

    // ── DoD 1: POST valid body → 201 Created + Location ──────────────────────

    @Test
    void create_validBody_returns201WithLocationHeader() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Widget\",\"description\":\"A test widget\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/items/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.description").value("A test widget"));
    }

    // ── DoD 2: POST invalid body → 400 with structured errors ────────────────

    @Test
    void create_blankName_returns400WithStructuredErrors() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty())
                .andExpect(jsonPath("$.errors[0]", containsString("name")));
    }

    @Test
    void create_missingName_returns400WithStructuredErrors() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    // ── DoD 3: GET non-existent ID → 404 standard JSON ───────────────────────

    @Test
    void getById_nonExistentId_returns404WithStandardJson() throws Exception {
        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/items/999"));
    }

    // ── DoD 4: X-Correlation-Id generated in every response ──────────────────

    @Test
    void response_alwaysHasCorrelationIdHeader_whenNoneProvided() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void errorResponse_alsoIncludesCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    // ── DoD 5: Custom X-Correlation-Id echoed unchanged ──────────────────────

    @Test
    void response_echoesCustomCorrelationId() throws Exception {
        String customId = "trace-abc-123";
        mockMvc.perform(get("/api/items")
                        .header("X-Correlation-Id", customId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", customId));
    }

    @Test
    void errorResponse_echoesCustomCorrelationIdInBodyAndHeader() throws Exception {
        String customId = "trace-xyz-456";
        mockMvc.perform(get("/api/items/999")
                        .header("X-Correlation-Id", customId))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", customId))
                .andExpect(jsonPath("$.correlationId").value(customId));
    }

    // ── DoD 6: Response is ItemResponse DTO — not the JPA entity ─────────────

    @Test
    void create_responseIsItemResponseDto_notJpaEntity() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DtoCheck\",\"description\":\"verify shape\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("DtoCheck"))
                .andExpect(jsonPath("$.description").value("verify shape"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                // Exactly 5 fields — no JPA-internal properties leaked
                .andExpect(jsonPath("$.*", hasSize(5)));
    }

    // ── Phase 5 DoD: Actuator & Observability ─────────────────────────────────

    @Test
    void actuator_health_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuator_liveness_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuator_readiness_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuator_prometheus_containsItemsCreatedCounter() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PrometheusCheck\",\"description\":\"metrics test\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/actuator/prometheus")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("items_total")));
    }
}
