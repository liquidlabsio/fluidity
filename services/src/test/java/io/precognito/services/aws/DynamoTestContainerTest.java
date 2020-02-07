package io.precognito.services.aws;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.Socket;

/**
 * This test might fail when missing the following script:
 * /usr/local/bin/docker-credential-desktop
 * It is possible to manually add a dummy script with the contents:
 * #!/bin/bash
 * echo true
 */
public class DynamoTestContainerTest {

    @Test
    public void containerStartsAndPublicPortIsAvailable() {
        try (DynamoLocalTestContainer container = new DynamoLocalTestContainer()) {
            container.start();
            assertThatPortIsAvailable(container);
        }
    }

    private void assertThatPortIsAvailable(DynamoLocalTestContainer container) {
        try {
            String containerIpAddress = container.getContainerIpAddress();
            System.out.println("Container IP address:" + containerIpAddress + " port:" + container.getPort() + " bindings:" + container.getPortBindings());
            new Socket(containerIpAddress, container.getPort());
        } catch (IOException e) {
            throw new AssertionError("The expected port " + container.getPort() + " is not available!");
        }
    }
}
