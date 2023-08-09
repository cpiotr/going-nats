package pl.ciruk.goingnats.ex06;

import io.nats.client.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.base.Strings;
import org.testcontainers.shaded.com.google.common.util.concurrent.Uninterruptibles;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class FastProducerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Container
    private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:2.6.5")
            .withExposedPorts(4222, 8222)
            .withReuse(true)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    static final String SUBJECT1 = "TestSubject";

    @Test
    void shouldThrowWhenOutgoingQueueFull() throws IOException, InterruptedException {
        final var options = new Options.Builder()
                .connectionName("client-3@" + InetAddress.getLocalHost().getHostName())
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .maxMessagesInOutgoingQueue(5_000) // default
                .build();
        final var connection = Nats.connect(options);
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        final var messageCount = 100_000;
        var latch = new CountDownLatch(messageCount);
        var counter = new AtomicInteger();
        final var dispatcher = connection.createDispatcher(message -> {
            latch.countDown();
            final var count = counter.incrementAndGet();
            if (count % 100 == 0) {
                LOGGER.info("Received {} messages", counter);
            }
        });
        dispatcher.subscribe(SUBJECT1);
        var dispatchers = new ArrayList<Dispatcher>();
        var blackHole = new LongAdder();
        for (int i = 0; i < 100; i++) {
            final var newDispatcher = connection.createDispatcher(message -> blackHole.add(message.consumeByteCount()));
            dispatchers.add(newDispatcher);
            newDispatcher.subscribe(SUBJECT1);
        }

        for (int i = 0; i < messageCount; i++) {
            final var message = Strings.repeat("Zażółć gęślą jaźń " + i, 1000);
            connection.publish(SUBJECT1, message.getBytes(StandardCharsets.UTF_8));
        }

        assertThat(latch.await(100, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void shouldDiscardMessagesWhenOutgoingQueueFull() throws IOException, InterruptedException {
        final var options = new Options.Builder()
                .connectionName("client-3@" + InetAddress.getLocalHost().getHostName())
                .server("nats://%s:%s".formatted(NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(4222)))
                .maxMessagesInOutgoingQueue(5_000) // default
                .discardMessagesWhenOutgoingQueueFull()
                .errorListener(new ErrorListener() {
                    AtomicInteger counter = new AtomicInteger();
                    @Override
                    public void messageDiscarded(Connection conn, Message msg) {
                        final var count = counter.incrementAndGet();
                        if (count % 1_000 == 0) {
                            LOGGER.info("Discarded {} messages", count);
                        }
                    }
                })
                .build();
        final var connection = Nats.connect(options);
        LOGGER.info("http://{}:{}/connz", NATS_SERVER.getHost(), NATS_SERVER.getMappedPort(8222));

        final var messageCount = 100_000;
        var latch = new CountDownLatch(messageCount);
        var counter = new AtomicInteger();
        final var dispatcher = connection.createDispatcher(message -> {
            latch.countDown();
            final var count = counter.incrementAndGet();
            if (count % 100 == 0) {
                LOGGER.info("Received {} messages", counter);
            }
        });
        dispatcher.subscribe(SUBJECT1);
        var dispatchers = new ArrayList<Dispatcher>();
        var blackHole = new LongAdder();
        for (int i = 0; i < 100; i++) {
            final var newDispatcher = connection.createDispatcher(message -> blackHole.add(message.consumeByteCount()));
            dispatchers.add(newDispatcher);
            newDispatcher.subscribe(SUBJECT1);
        }

        for (int i = 0; i < messageCount; i++) {
            final var message = Strings.repeat("Zażółć gęślą jaźń " + i, 1000);
            connection.publish(SUBJECT1, message.getBytes(StandardCharsets.UTF_8));
        }

        assertThat(latch.await(100, TimeUnit.SECONDS)).isTrue();
    }
}
