/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.artipie.asto.s3.S3Storage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import java.net.URI;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * S3 storage verification test.
 *
 * @checkstyle ProtectedMethodInFinalClassCheck (500 lines)
 * @since 1.14.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class S3StorageWhiteboxVerificationTest extends StorageWhiteboxVerification {

    /**
     * S3 mock server extension.
     */
    private final S3MockExtension mock = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    @Before
    public void setUp() throws Exception {
        this.mock.beforeAll(null);
    }

    @After
    public void tearDown() {
        this.mock.afterAll(null);
    }

    @Override
    protected Storage newStorage() {
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("foo", "bar")
                )
            )
            .endpointOverride(
                URI.create(String.format("http://localhost:%d", this.mock.getHttpPort()))
            )
            .build();
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(CreateBucketRequest.builder().bucket(bucket).build()).join();
        return new S3Storage(client, bucket);
    }
}
