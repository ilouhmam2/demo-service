package fr.francetv.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DemoApplicationTests {

    @Container
    static GenericContainer<?> natsContainer =
            new GenericContainer<>(DockerImageName.parse("nats:2-alpine"))
                    .withExposedPorts(4222);

    @DynamicPropertySource
    static void natsProperties(DynamicPropertyRegistry registry) {
        registry.add("foundation.nats.server-url",
                () -> "nats://" + natsContainer.getHost() + ":" + natsContainer.getMappedPort(4222));
    }

    @Test
    void contextLoads() {
    }
}
