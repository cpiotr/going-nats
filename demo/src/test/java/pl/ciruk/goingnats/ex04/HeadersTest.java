package pl.ciruk.goingnats.ex04;

import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.impl.Headers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class HeadersTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    void shouldPublishHeaders() throws Exception {
        final var options = Options.builder()
                .connectionName("client-1@" + InetAddress.getLocalHost().getHostName())
                .server("nats://localhost:4222")
                .build();
        final var connection = Nats.connect(options);

        final var dispatcher = connection.createDispatcher(message -> {
            message.getHeaders().forEach((key, values) -> LOGGER.info("Header: {}->{}", key, values));
        });
        dispatcher.subscribe("Subject1");


        final var headers = new Headers();
        headers.add("Key1", "Value1");
        headers.add("Key2", "3.14159263");
        connection.publish("Subject1", headers, "This is a sample body".getBytes(StandardCharsets.UTF_8));
    }
}
