package io.fluidity.services.aws;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

/**
 * Runs:
 * docker run --name test-dynamo-local --publish 8000:8000 amazon/dynamodb-local:1.11.477 -jar DynamoDBLocal.jar -inMemory -sharedDb
 * Need to add params: -jar DynamoDBLocal.jar -inMemory -sharedDb
 */
public class DynamoLocalTestContainer extends GenericContainer<DynamoLocalTestContainer> {
    /**
     * This is the internal port on running inside the container.
     * <p>
     * You can use this constant in case you want to map an explicit public port to it
     * instead of the default random port. This can be done using methods like
     * {@link #setPortBindings(java.util.List)}.
     */
    public static final int DB_PORT = 8000;

    public static final String DEFAULT_IMAGE_AND_TAG = "amazon/dynamodb-local:1.11.477";

    /**
     * Creates a new {@link DynamoLocalTestContainer} with the {@value DEFAULT_IMAGE_AND_TAG} image.
     */
    public DynamoLocalTestContainer() {
        this(DEFAULT_IMAGE_AND_TAG);
    }

    /**
     * Creates a new {@link DynamoLocalTestContainer} with the given {@code 'image'}.
     *
     * @param image the image (e.g. {@value DEFAULT_IMAGE_AND_TAG}) to use
     */
    public DynamoLocalTestContainer(@NotNull String image) {
        super(image);
        addExposedPort(DB_PORT);
        System.out.println("Starting:" + this.getClass().getCanonicalName());


    }
    /**
     * Returns the actual public port of the internal MongoDB port ({@value DB_PORT}).
     *
     * @return the public port of this container
     * @see #getMappedPort(int)
     */
    @NotNull
    public Integer getPort() {
        return getMappedPort(DB_PORT);
    }

}
