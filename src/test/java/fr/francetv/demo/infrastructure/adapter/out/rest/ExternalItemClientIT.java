package fr.francetv.demo.infrastructure.adapter.out.rest;

import fr.francetv.demo.client.api.ExternalItemsApi;
import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.foundation.httpclient.filter.CorrelationIdExchangeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link ExternalItemRestAdapter} using an embedded Reactor Netty server.
 *
 * <p>Uses Reactor Netty {@link HttpServer} to avoid Jetty/WireMock version conflicts
 * while still verifying real HTTP wire-level behaviour (same pattern as
 * {@code CorrelationIdFilterTest} in foundation-http-client-starter).
 *
 * <p>DoD coverage:
 * <ul>
 *   <li>fetchAll() returns mapped items from a 200 response</li>
 *   <li>Empty array is handled correctly</li>
 *   <li>X-Correlation-Id from MDC is propagated to the outgoing HTTP request</li>
 *   <li>X-Correlation-Id is auto-generated when MDC is empty</li>
 *   <li>5xx response maps to 502 Bad Gateway</li>
 *   <li>4xx response maps to 502 Bad Gateway</li>
 * </ul>
 *
 * <p>No {@code @SpringBootTest} — the adapter is instantiated directly with a stub-backed
 * WebClient. No PostgreSQL or NATS containers are required.
 */
class ExternalItemClientIT {

    static final String EXTERNAL_ITEMS_PATH = "/api/external/items";

    DisposableServer server;
    ExternalItemRestAdapter adapter;

    @AfterEach
    void tearDown() {
        MDC.remove("correlationId");
        if (server != null) {
            server.disposeNow(Duration.ofSeconds(5));
        }
    }

    private ExternalItemRestAdapter adapterFor(DisposableServer srv) {
        WebClient webClient = WebClient.builder()
                .filter(new CorrelationIdExchangeFilter())
                .baseUrl("http://localhost:" + srv.port())
                .build();
        return new ExternalItemRestAdapter(new ExternalItemsApi(webClient));
    }

    // ── Success path ──────────────────────────────────────────────────────────

    @Test
    void fetchAll_success_returnsMappedItems() {
        server = HttpServer.create().port(0)
                .handle((req, res) -> res.status(200)
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just(
                                "[{\"id\":\"1\",\"name\":\"Widget A\",\"description\":\"Desc A\"}," +
                                "{\"id\":\"2\",\"name\":\"Widget B\",\"description\":null}]")))
                .bindNow();

        List<Item> items = adapterFor(server).fetchAll();

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getName()).isEqualTo("Widget A");
        assertThat(items.get(0).getDescription()).isEqualTo("Desc A");
        assertThat(items.get(1).getName()).isEqualTo("Widget B");
        assertThat(items.get(1).getDescription()).isNull();
    }

    @Test
    void fetchAll_emptyArray_returnsEmptyList() {
        server = HttpServer.create().port(0)
                .handle((req, res) -> res.status(200)
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just("[]")))
                .bindNow();

        assertThat(adapterFor(server).fetchAll()).isEmpty();
    }

    // ── Correlation ID propagation ────────────────────────────────────────────

    @Test
    void fetchAll_propagatesCorrelationIdFromMdc() {
        AtomicReference<String> receivedHeader = new AtomicReference<>();

        server = HttpServer.create().port(0)
                .handle((req, res) -> {
                    receivedHeader.set(req.requestHeaders().get("X-Correlation-Id"));
                    return res.status(200)
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just("[]"));
                })
                .bindNow();

        MDC.put("correlationId", "test-correlation-xyz-789");
        adapterFor(server).fetchAll();

        assertThat(receivedHeader.get()).isEqualTo("test-correlation-xyz-789");
    }

    @Test
    void fetchAll_generatesCorrelationIdWhenMdcEmpty() {
        AtomicReference<String> receivedHeader = new AtomicReference<>();

        server = HttpServer.create().port(0)
                .handle((req, res) -> {
                    receivedHeader.set(req.requestHeaders().get("X-Correlation-Id"));
                    return res.status(200)
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just("[]"));
                })
                .bindNow();

        MDC.remove("correlationId");
        adapterFor(server).fetchAll();

        assertThat(receivedHeader.get())
                .isNotBlank()
                .matches("[0-9a-f\\-]{36}");
    }

    // ── Error mapping ─────────────────────────────────────────────────────────

    @Test
    void fetchAll_serverError_mapsToBadGateway() {
        server = HttpServer.create().port(0)
                .handle((req, res) -> res.status(500).send())
                .bindNow();

        assertThatThrownBy(() -> adapterFor(server).fetchAll())
                .isInstanceOf(ExternalServiceException.class)
                .satisfies(e -> assertThat(((ExternalServiceException) e).getKind())
                        .isEqualTo(ExternalServiceException.Kind.BAD_GATEWAY));
    }

    @Test
    void fetchAll_clientError_mapsToBadGateway() {
        server = HttpServer.create().port(0)
                .handle((req, res) -> res.status(404).send())
                .bindNow();

        assertThatThrownBy(() -> adapterFor(server).fetchAll())
                .isInstanceOf(ExternalServiceException.class)
                .satisfies(e -> assertThat(((ExternalServiceException) e).getKind())
                        .isEqualTo(ExternalServiceException.Kind.BAD_GATEWAY));
    }
}
