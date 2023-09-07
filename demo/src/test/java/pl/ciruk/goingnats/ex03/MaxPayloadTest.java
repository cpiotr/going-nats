package pl.ciruk.goingnats.ex03;

import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.base.Strings;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class MaxPayloadTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Container
    private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:2.6.5")
            .withExposedPorts(4222, 8222)
            .withReuse(true)
            .withCopyFileToContainer(MountableFile.forClasspathResource("ex03.conf"), "/etc/nats/nats-server.conf")
            .withCommand("--config", "/etc/nats/nats-server.conf")
            .waitingFor(Wait.forHttp("/").forPort(8222).withStartupTimeout(Duration.ofSeconds(30)));
    static final String SUBJECT = "TestSubject";

    @Test
    void shouldReceiveBigMessage() throws IOException, InterruptedException {
        final var options = new Options.Builder()
                .connectionName("client-3@" + InetAddress.getLocalHost().getHostName())
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .errorListener(new ErrorListener() {
                })
                .build();
        final var connection = Nats.connect(options);
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        var latch = new CountDownLatch(1);
        final var dispatcher = connection.createDispatcher(message -> {
            LOGGER.info("Received {} bytes", message.getData().length);
            latch.countDown();
        });
        dispatcher.subscribe(SUBJECT);
        connection.publish(SUBJECT, Strings.repeat("Zażółć gęślą jaźń", 1000).getBytes(StandardCharsets.UTF_8));

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }
}
