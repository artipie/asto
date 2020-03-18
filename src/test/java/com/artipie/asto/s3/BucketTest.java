/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto.s3;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Tests for {@link Bucket}.
 *
 * @since 0.15
 */
class BucketTest {

    /**
     * Mock S3 server.
     */
    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Bucket name to use in tests.
     */
    private String name;

    @BeforeEach
    void setUp(final AmazonS3 client) {
        this.name = UUID.randomUUID().toString();
        client.createBucket(this.name);
    }

    @Test
    void shouldUploadPartAndCompleteMultipartUpload(final AmazonS3 client) throws Exception {
        final String key = "multipart";
        final String id = client.initiateMultipartUpload(
            new InitiateMultipartUploadRequest(this.name, key)
        ).getUploadId();
        final byte[] data = "data".getBytes();
        final Bucket bucket = this.bucket();
        bucket.uploadPart(
            UploadPartRequest.builder()
                .key(key)
                .uploadId(id)
                .partNumber(1)
                .contentLength((long) data.length)
                .build(),
            AsyncRequestBody.fromPublisher(AsyncRequestBody.fromBytes(data))
        ).thenCompose(
            ignored -> bucket.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .key(key)
                    .uploadId(id)
                    .build()
            )
        ).join();
        final byte[] downloaded;
        try (S3Object s3Object = client.getObject(this.name, key)) {
            downloaded = ByteStreams.toByteArray(s3Object.getObjectContent());
        }
        MatcherAssert.assertThat(downloaded, Matchers.equalTo(data));
    }

    @Test
    void shouldAbortMultipartUploadWhenFailedToReadContent(final AmazonS3 client) {
        final String key = "abort";
        final String id = client.initiateMultipartUpload(
            new InitiateMultipartUploadRequest(this.name, key)
        ).getUploadId();
        this.bucket().abortMultipartUpload(
            AbortMultipartUploadRequest.builder()
                .key(key)
                .uploadId(id)
                .build()
        ).join();
        final List<MultipartUpload> uploads = client.listMultipartUploads(
            new ListMultipartUploadsRequest(this.name)
        ).getMultipartUploads();
        MatcherAssert.assertThat(uploads, new IsEmptyIterable<>());
    }

    private Bucket bucket() {
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .endpointOverride(
                URI.create(String.format("http://localhost:%d", MOCK.getHttpPort()))
            )
            .build();
        return new Bucket(client, this.name);
    }
}
