package pl.ciruk.goingnats.ex05;

import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class MessageHandlerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Container
    private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:2.6.5")
            .withExposedPorts(4222, 8222)
            .withReuse(true)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    static final String SUBJECT1 = "TestSubject";
    static final String SUBJECT2 = "TestSubject2";

    @Test
    void shouldReceiveBigMessage() throws IOException, InterruptedException {
        final var options = new Options.Builder()
                .connectionName("client-3@" + InetAddress.getLocalHost().getHostName())
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .build();
        final var connection = Nats.connect(options);
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        var latch = new CountDownLatch(4);
        final var slowDispatcher = connection.createDispatcher(message -> {
            LOGGER.info("Slow received: {}", new String(message.getData(), StandardCharsets.UTF_8));
            Thread.sleep(Duration.ofSeconds(10));
            latch.countDown();
        });
        final var dispatcher = connection.createDispatcher(message -> {
            LOGGER.info("Received: {}", new String(message.getData(), StandardCharsets.UTF_8));

        });
        slowDispatcher.subscribe(SUBJECT1);
        slowDispatcher.subscribe(SUBJECT2);
        dispatcher.subscribe(SUBJECT1);
        dispatcher.subscribe(SUBJECT2);

        connection.publish(SUBJECT1, "Zażółć gęślą jaźń".getBytes(StandardCharsets.UTF_8));
        connection.publish(SUBJECT2, "Goedemiddag".getBytes(StandardCharsets.UTF_8));
        connection.publish(SUBJECT1, "Our fearful trip is done".getBytes(StandardCharsets.UTF_8));
        connection.publish(SUBJECT2, "Wer ein Warum hat, dem ist kein Wie zu schwer.".getBytes(StandardCharsets.UTF_8));

        assertThat(latch.await(100, TimeUnit.SECONDS)).isTrue();
    }
}
