package fr.francetv.demo.infrastructure.adapter.in.web;

import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.out.ExternalItemPort;
import fr.francetv.demo.infrastructure.adapter.out.persistence.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for POST /api/items/sync.
 *
 * <p>ExternalItemPort is mocked to isolate the controller → use case → repository
 * pipeline from the actual HTTP client (tested separately in ExternalItemRestAdapterTest).
 *
 * <ul>
 *   <li>Happy path: port returns items → 200, items saved to DB</li>
 *   <li>Empty list: port returns [] → 200, synced=0</li>
 *   <li>External error (4xx/5xx) → 502 Bad Gateway</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SyncExternalItemsIT {

    static final GenericContainer<?> natsContainer;

    static {
        natsContainer = new GenericContainer<>(DockerImageName.parse("nats:2-alpine"))
                .withExposedPorts(4222);
        natsContainer.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("foundation.nats.server-url",
                () -> "nats://" + natsContainer.getHost() + ":" + natsContainer.getMappedPort(4222));
    }

    @MockitoBean
    ExternalItemPort externalItemPort;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ItemRepository itemRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        itemRepository.deleteAll();
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    void sync_portReturnsItems_savesThem_andReturns200() throws Exception {
        given(externalItemPort.fetchAll()).willReturn(List.of(
                new Item(null, "Widget A", "Description A", null, null),
                new Item(null, "Widget B", "Description B", null, null)
        ));

        mockMvc.perform(post("/api/items/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(2));

        assertThat(itemRepository.count()).isEqualTo(2);
    }

    @Test
    void sync_portReturnsEmptyList_returns200WithZeroCount() throws Exception {
        given(externalItemPort.fetchAll()).willReturn(List.of());

        mockMvc.perform(post("/api/items/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(0));

        assertThat(itemRepository.count()).isZero();
    }

    // ── Error mapping ────────────────────────────────────────────────────────

    @Test
    void sync_portThrowsBadGateway_returns502() throws Exception {
        given(externalItemPort.fetchAll())
                .willThrow(new ExternalServiceException(
                        ExternalServiceException.Kind.BAD_GATEWAY,
                        "External service error: 500"));

        mockMvc.perform(post("/api/items/sync"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void sync_portThrowsServiceUnavailable_returns503() throws Exception {
        given(externalItemPort.fetchAll())
                .willThrow(new ExternalServiceException(
                        ExternalServiceException.Kind.SERVICE_UNAVAILABLE,
                        "External service unreachable"));

        mockMvc.perform(post("/api/items/sync"))
                .andExpect(status().isServiceUnavailable());
    }
}
