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
package com.artipie.asto;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.s3.S3Storage;
import com.google.common.io.ByteStreams;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Tests for {@link S3Storage}.
 *
 * @since 0.15
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class S3StorageTest {

    /**
     * Mock S3 server.
     */
    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    @Test
    void shouldUploadObjectWhenSave(final AmazonS3 client) throws Exception {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final byte[] data = "data2".getBytes();
        final String key = "a/b/c";
        this.storage(bucket).save(new Key.From(key), new Content.From(data)).join();
        final byte[] downloaded;
        try (S3Object s3Object = client.getObject(bucket, key)) {
            downloaded = ByteStreams.toByteArray(s3Object.getObjectContent());
        }
        MatcherAssert.assertThat(downloaded, Matchers.equalTo(data));
    }

    @Test
    void shouldFailToSaveMultipartUploadWhenFailedToReadContent(final AmazonS3 client) {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final String key = "fail";
        final CompletableFuture<Void> saved = this.storage(bucket).save(
            new Key.From(key),
            new Content.From(Flowable.error(new IllegalStateException()))
        );
        Assertions.assertThrows(Exception.class, saved::join);
    }

    @Test
    void shouldAbortMultipartUploadWhenFailedToReadContent(final AmazonS3 client) {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final String key = "abort";
        final CompletableFuture<Void> saved = this.storage(bucket).save(
            new Key.From(key),
            new Content.From(Flowable.error(new IllegalStateException()))
        );
        saved.exceptionally(ignore -> null).join();
        final List<MultipartUpload> uploads = client.listMultipartUploads(
            new ListMultipartUploadsRequest(bucket)
        ).getMultipartUploads();
        MatcherAssert.assertThat(uploads, new IsEmptyIterable<>());
    }

    @Test
    void shouldExistForSavedObject(final AmazonS3 client) {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final byte[] data = "content".getBytes();
        final String key = "some/existing/key";
        client.putObject(bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        final boolean exists = new BlockingStorage(this.storage(bucket)).exists(new Key.From(key));
        MatcherAssert.assertThat(exists, Matchers.equalTo(true));
    }

    @Test
    void shouldNotExistForUnknownObject(final AmazonS3 client) {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final String key = "unknown/key";
        final boolean exists = new BlockingStorage(this.storage(bucket)).exists(new Key.From(key));
        MatcherAssert.assertThat(exists, Matchers.equalTo(false));
    }

    private S3Storage storage(final String bucket) {
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .endpointOverride(
                URI.create(String.format("http://localhost:%d", MOCK.getHttpPort()))
            )
            .build();
        return new S3Storage(client, bucket);
    }
}
