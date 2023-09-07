package pl.ciruk.goingnats.ex01;

import io.nats.client.Nats;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;

@Testcontainers
public class SimpleConnectTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Container
    private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:2.6.5")
            .withExposedPorts(4222, 8222)
            .withReuse(true)
            .waitingFor(Wait.forHttp("/").forPort(8222).withStartupTimeout(Duration.ofSeconds(30)));

    @Test
    void shouldConnectToNats() throws IOException, InterruptedException {
        final var connection = Nats.connect("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)));
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));
        Thread.sleep(Duration.ofHours(1));
    }
}
