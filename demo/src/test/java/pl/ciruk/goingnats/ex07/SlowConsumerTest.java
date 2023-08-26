package pl.ciruk.goingnats.ex07;

import io.nats.client.*;
import io.nats.client.impl.ErrorListenerLoggerImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class SlowConsumerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Container
    private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:2.6.5")
            .withExposedPorts(4222, 8222)
            .withCopyFileToContainer(MountableFile.forClasspathResource("ex07.conf"), "/etc/nats/nats-server.conf")
            .withCommand("--config", "/etc/nats/nats-server.conf")
            .withReuse(true)
            .withLogConsumer(outputFrame -> LOGGER.info("{}", outputFrame.getUtf8String()))
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    static final String SUBJECT1 = "TestSubject";

    @Test
    void shouldFailDueToSlowConsumerDroppingMessages() throws IOException, InterruptedException {
        final var options = new Options.Builder()
                .connectionName("client-3@" + InetAddress.getLocalHost().getHostName())
                .errorListener(new ErrorListener() {
                    @Override
                    public void slowConsumerDetected(Connection conn, Consumer consumer) {
                        LOGGER.info("I'm a slow consumer :( Dropped {} messages from {}", consumer.getDroppedCount(), conn.getServerInfo());
                    }
                })
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .build();
        final var connection = Nats.connect(options);
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        var latch = new CountDownLatch(1_000_000);
        var counter = new AtomicInteger();
        final var slowDispatcher = connection.createDispatcher(message -> {
            final var count = counter.incrementAndGet();
            if (count % 1_000 == 0) {
                LOGGER.info("Received: {} messages", count);
            }
            Thread.sleep(Duration.ofSeconds(1));
            latch.countDown();
        });
        slowDispatcher.subscribe(SUBJECT1);

        for (int i = 0; i < 1_000_000; i++) {
            connection.publish(SUBJECT1, "Message%d".formatted(i).getBytes(StandardCharsets.UTF_8));
        }

        assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
    }

    @Test
    void shouldNotDropMessages() throws IOException, InterruptedException {
        final var options = new Options.Builder()
                .connectionName("client-3@" + InetAddress.getLocalHost().getHostName())
                .errorListener(new ErrorListener() {
                    @Override
                    public void slowConsumerDetected(Connection conn, Consumer consumer) {
                        LOGGER.info("I'm a slow consumer :( Dropped {} messages from {}", consumer.getDroppedCount(), conn.getServerInfo().getHost());
                    }
                })
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .build();
        final var connection = Nats.connect(options);
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        var latch = new CountDownLatch(1_000_000);
        var counter = new AtomicInteger();
        final var slowDispatcher = connection.createDispatcher(message -> {
            final var count = counter.incrementAndGet();
            if (count % 100 == 0) {
                LOGGER.info("Received: {} messages", count);
            }
            Thread.sleep(Duration.ofSeconds(1));
            latch.countDown();
        });
        slowDispatcher.setPendingLimits(0, 0);
        slowDispatcher.subscribe(SUBJECT1);

        for (int i = 1; i <= 10_000_000; i++) {
            connection.publish(SUBJECT1, "Message%d".formatted(i).getBytes(StandardCharsets.UTF_8));
            if (i % 1_000_000 == 0) {
                LOGGER.info("Published {}M messages", i / 1_000_000);
            }
        }

        assertThat(latch.await(2, TimeUnit.MINUTES)).isTrue();
    }

    @Test
    void shouldGetDisconnectedByServer() throws IOException, InterruptedException {
        final var localPort = 14222;
        SlowProxy.start(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222), localPort, 1);

        final var listener = new ErrorListenerLoggerImpl();
        final var options = new Options.Builder()
                .connectionName("slooooow")
                .errorListener(listener)
                .connectionListener(new ConnectionListener() {
                    @Override
                    public void connectionEvent(Connection connection, Events events) {
                        LOGGER.info("{}", events);
                    }
                })
                .server("nats://%s:%s".formatted("localhost", localPort))
                .build();
        final var connection = Nats.connect(options);

        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        final var limit = 1_000_000;
        var latch = new CountDownLatch(limit);
        var counter = new AtomicInteger();
        final var slowDispatcher = connection.createDispatcher(message -> {
            final var count = counter.incrementAndGet();
            if (count % 100 == 0) {
                LOGGER.info("Received: {} messages", count);
            }
            latch.countDown();
        });
        slowDispatcher.subscribe(SUBJECT1);

        final var publisherOptions = Options.builder()
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .build();
        final var publishingConnection = Nats.connect(publisherOptions);
        for (int i = 1; i <= limit; i++) {
            publishingConnection.publish(SUBJECT1, "Message%d".formatted(i).getBytes(StandardCharsets.UTF_8));
            if (i % (limit / 100) == 0) {
                LOGGER.info("Published {} messages", i);
            }
        }

        assertThat(latch.await(5, TimeUnit.MINUTES)).isTrue();
    }
}
