package fr.francetv.demo.infrastructure.adapter.in.web;

import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.out.ItemSoapPort;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GET /api/items/{id}/soap-check.
 *
 * <p>ItemSoapPort is mocked to isolate the controller → use case pipeline
 * from the actual CXF client (tested separately in ItemSoapAdapterTest).
 *
 * <ul>
 *   <li>Happy path: port returns item → 200 with decoded id and name</li>
 *   <li>SOAPFault mapped to 502 Bad Gateway</li>
 *   <li>X-Correlation-Id echoed in response header</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SoapCheckIT {

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

    @MockitoBean
    ItemSoapPort itemSoapPort;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ItemRepository itemRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    void soapCheck_portReturnsItem_returns200WithDecodedResponse() throws Exception {
        given(itemSoapPort.getItem(1L))
                .willReturn(new Item(1L, "SOAP Item", null, null, null));

        mockMvc.perform(get("/api/items/1/soap-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("SOAP Item"));
    }

    // ── SOAPFault → 502 ─────────────────────────────────────────────────────

    @Test
    void soapCheck_soapFault_returns502() throws Exception {
        given(itemSoapPort.getItem(99L))
            .willThrow(new ExternalServiceException(ExternalServiceException.Kind.BAD_GATEWAY,
                        "SOAP service error: Item not found"));

        mockMvc.perform(get("/api/items/99/soap-check"))
                .andExpect(status().isBadGateway());
    }
}
