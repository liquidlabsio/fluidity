package com.logscapeng.uploader.aws;


import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Local testing container for Dynalite and DynamoDbLocal
 */
public class LocalDynamoDbContainer extends GenericContainer<LocalDynamoDbContainer> {

//    private static final String IMAGE_NAME = "quay.io/testcontainers/dynalite:v1.2.1-1";
//    private static final int MAPPED_PORT = 4567;
    private static final String IMAGE_NAME = "amazon/dynamodb-local:1.11.477";
    private static final int MAPPED_PORT = 8000;


    public LocalDynamoDbContainer() {
        this(IMAGE_NAME);
        withExposedPorts(MAPPED_PORT);
    }

    public LocalDynamoDbContainer(String imageName) {
        super(imageName);
    }

    /**
     * Gets a preconfigured {@link AmazonDynamoDB} client object for connecting to this
     * container.
     *
     * @return preconfigured client
     */
    public AmazonDynamoDB getClient() {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(getEndpointConfiguration())
                .withCredentials(getCredentials())
                .build();
    }

    /**
     * Create a standard AWS v2 SDK client pointing to the local DynamoDb instance
     *
     * @return A DynamoDbClient pointing to the local DynamoDb instance
     */
    public DynamoDbClient getAws2Client(String host, int port) {

        System.out.println("Getting client connection:" + host + ":" + port);

        ApacheHttpClient.Builder builder = ApacheHttpClient.builder();

        String endpoint = String.format("http://%s:%d", host, port);
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .httpClientBuilder(builder)
                // The region is meaningless for local DynamoDb but required for client builder validation
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy-key", "dummy-secret")))
                .build();
    }


    /**
     * Gets {@link AwsClientBuilder.EndpointConfiguration}
     * that may be used to connect to this container.
     *
     * @return endpoint configuration
     */
    public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://" +
                this.getContainerIpAddress() + ":" +
                this.getMappedPort(MAPPED_PORT), null);
    }

    /**
     * Gets an {@link AWSCredentialsProvider} that may be used to connect to this container.
     *
     * @return dummy AWS credentials
     */
    public AWSCredentialsProvider getCredentials() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy"));
    }
}