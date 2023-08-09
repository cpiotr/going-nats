package pl.ciruk.goingnats.ex02;

import io.nats.client.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.time.Duration;

public class OptionsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    void shouldConnectIdentifyingWithConnectionName() throws IOException, InterruptedException {
        final var options = Options.builder()
                .connectionName("client-1@" + InetAddress.getLocalHost().getHostName())
                .server("nats://localhost:4222")
                .build();
        final var connection = Nats.connect(options);
        Thread.sleep(Duration.ofHours(1));
    }

    @Test
    void shouldHandleConnectionEvents() throws IOException, InterruptedException {
        final var options = Options.builder()
                .connectionName("client-2@" + InetAddress.getLocalHost().getHostName())
                .server("nats://localhost:4222")
                .errorListener(new ErrorListener() {
                    @Override
                    public void exceptionOccurred(Connection conn, Exception exp) {
                        LOGGER.error("Exception occurred", exp);
                    }
                })
                .connectionListener(new ConnectionListener() {
                    @Override
                    public void connectionEvent(Connection connection, Events events) {
                        LOGGER.info("Events: {}", events);
                    }
                })
                .build();
        final var connection = Nats.connect(options);
        Thread.sleep(Duration.ofHours(1));
    }

    @Test
    void shouldHandleConnectionEventsButSloooowly() throws IOException, InterruptedException {
        final var options = Options.builder()
                .connectionName("client-2@" + InetAddress.getLocalHost().getHostName())
                .server("nats://localhost:4222")
                .connectionListener(new ConnectionListener() {
                    @Override
                    public void connectionEvent(Connection connection, Events events) {
                        LOGGER.info("Events: {}", events);
                        try {
                            Thread.sleep(Duration.ofSeconds(10));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .build();
        final var connection = Nats.connect(options);
        Thread.sleep(Duration.ofHours(1));
    }
}
