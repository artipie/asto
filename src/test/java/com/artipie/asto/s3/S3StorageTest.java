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
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.google.common.io.ByteStreams;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Tests for {@link S3Storage}.
 *
 * @since 0.15
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (3 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
class S3StorageTest {

    /**
     * Mock S3 server.
     */
    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Bucket to use in tests.
     */
    private String bucket;

    @BeforeEach
    void setUp(final AmazonS3 client) {
        this.bucket = UUID.randomUUID().toString();
        client.createBucket(this.bucket);
    }

    @Test
    void shouldUploadObjectWhenSave(final AmazonS3 client) throws Exception {
        final byte[] data = "data2".getBytes();
        final String key = "a/b/c";
        this.storage().save(new Key.From(key), new Content.From(data)).join();
        MatcherAssert.assertThat(this.download(client, key), Matchers.equalTo(data));
    }

    @Test
    @Timeout(5)
    void shouldUploadObjectWhenSaveContentOfUnknownSize(final AmazonS3 client) throws Exception {
        final byte[] data = "data?".getBytes();
        final String key = "unknown/size";
        this.storage().save(
            new Key.From(key),
            new Content.From(new OneOffPublisher(ByteBuffer.wrap(data)))
        ).join();
        MatcherAssert.assertThat(this.download(client, key), Matchers.equalTo(data));
    }

    @Test
    @Timeout(15)
    void shouldUploadObjectWhenSaveLargeContent(final AmazonS3 client) throws Exception {
        final int size = 20 * 1024 * 1024;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final String key = "big/data";
        this.storage().save(
            new Key.From(key),
            new Content.From(new OneOffPublisher(ByteBuffer.wrap(data)))
        ).join();
        MatcherAssert.assertThat(this.download(client, key), Matchers.equalTo(data));
    }

    @Test
    void shouldAbortMultipartUploadWhenFailedToReadContent(final AmazonS3 client) {
        this.storage().save(
            new Key.From("abort"),
            new Content.From(Flowable.error(new IllegalStateException()))
        ).exceptionally(ignore -> null).join();
        final List<MultipartUpload> uploads = client.listMultipartUploads(
            new ListMultipartUploadsRequest(this.bucket)
        ).getMultipartUploads();
        MatcherAssert.assertThat(uploads, new IsEmptyIterable<>());
    }

    @Test
    void shouldExistForSavedObject(final AmazonS3 client) {
        final byte[] data = "content".getBytes();
        final String key = "some/existing/key";
        client.putObject(this.bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        final boolean exists = new BlockingStorage(this.storage())
            .exists(new Key.From(key));
        MatcherAssert.assertThat(
            exists,
            Matchers.equalTo(true)
        );
    }

    @Test
    void shouldListKeysInOrder(final AmazonS3 client) {
        final byte[] data = "some data!".getBytes();
        Arrays.asList(
            new Key.From("1"),
            new Key.From("a", "b", "c", "1"),
            new Key.From("a", "b", "2"),
            new Key.From("a", "z"),
            new Key.From("z")
        ).forEach(
            key -> client.putObject(
                this.bucket,
                key.string(),
                new ByteArrayInputStream(data),
                new ObjectMetadata()
            )
        );
        final Collection<String> keys = new BlockingStorage(this.storage())
            .list(new Key.From("a", "b"))
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
        );
    }

    @Test
    void shouldGetObjectWhenLoad(final AmazonS3 client) {
        final byte[] data = "data".getBytes();
        final String key = "some/key";
        client.putObject(this.bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        final byte[] value = new BlockingStorage(this.storage())
            .value(new Key.From(key));
        MatcherAssert.assertThat(
            value,
            new IsEqual<>(data)
        );
    }

    @Test
    void shouldCopyObjectWhenMoved(final AmazonS3 client) throws Exception {
        final byte[] original = "something".getBytes();
        final String source = "source";
        client.putObject(
            this.bucket,
            source, new ByteArrayInputStream(original),
            new ObjectMetadata()
        );
        final String destination = "destination";
        new BlockingStorage(this.storage()).move(
            new Key.From(source),
            new Key.From(destination)
        );
        try (S3Object s3Object = client.getObject(this.bucket, destination)) {
            MatcherAssert.assertThat(
                ByteStreams.toByteArray(s3Object.getObjectContent()),
                new IsEqual<>(original)
            );
        }
    }

    @Test
    void shouldDeleteOriginalObjectWhenMoved(final AmazonS3 client) throws Exception {
        final String source = "src";
        client.putObject(
            this.bucket,
            source,
            new ByteArrayInputStream("some data".getBytes()),
            new ObjectMetadata()
        );
        new BlockingStorage(this.storage()).move(
            new Key.From(source),
            new Key.From("dest")
        );
        MatcherAssert.assertThat(
            client.doesObjectExist(this.bucket, source),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldDeleteObject(final AmazonS3 client) {
        final byte[] data = "to be deleted".getBytes();
        final String key = "to/be/deleted";
        client.putObject(this.bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        new BlockingStorage(this.storage()).delete(new Key.From(key));
        MatcherAssert.assertThat(
            client.doesObjectExist(this.bucket, key),
            new IsEqual<>(false)
        );
    }

    private byte[] download(final AmazonS3 client, final String key) throws IOException {
        try (S3Object s3Object = client.getObject(this.bucket, key)) {
            return ByteStreams.toByteArray(s3Object.getObjectContent());
        }
    }

    private S3Storage storage() {
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .endpointOverride(
                URI.create(String.format("http://localhost:%d", MOCK.getHttpPort()))
            )
            .build();
        return new S3Storage(client, this.bucket);
    }

    /**
     * Publisher that produces value only once for first subscription.
     *
     * @since 0.19
     */
    private static class OneOffPublisher implements Publisher<ByteBuffer> {

        /**
         * Data for subscriber.
         */
        private final ByteBuffer data;

        /**
         * Flag for completion.
         */
        private final AtomicBoolean complete;

        OneOffPublisher(final ByteBuffer data) {
            this.data = data;
            this.complete = new AtomicBoolean(false);
        }

        @Override
        public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
            if (!this.complete.getAndSet(true)) {
                subscriber.onNext(this.data);
                subscriber.onComplete();
            }
        }
    }
}
