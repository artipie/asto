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
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.s3.S3Storage;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
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

    @Test
    void shouldListKeysInOrder(final AmazonS3 client) {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final byte[] data = "some data!".getBytes();
        Arrays.asList(
            new Key.From("1"),
            new Key.From("a", "b", "c", "1"),
            new Key.From("a", "b", "2"),
            new Key.From("a", "z"),
            new Key.From("z")
        ).forEach(
            key -> client.putObject(
                bucket,
                key.string(),
                new ByteArrayInputStream(data),
                new ObjectMetadata()
            )
        );
        final Collection<String> keys = new BlockingStorage(this.storage(bucket))
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
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final byte[] data = "data".getBytes();
        final String key = "some/key";
        client.putObject(bucket, key, new ByteArrayInputStream(data), new ObjectMetadata());
        final byte[] value = new BlockingStorage(this.storage(bucket)).value(new Key.From(key));
        MatcherAssert.assertThat(
            "Storage should read object stored on S3",
            value,
            new IsEqual<>(data)
        );
    }

    @Test
    void shouldCopyObjectWhenMoved(final AmazonS3 client) throws Exception {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final byte[] original = "something".getBytes();
        final String source = "source";
        client.putObject(bucket, source, new ByteArrayInputStream(original), new ObjectMetadata());
        final String destination = "destination";
        new BlockingStorage(this.storage(bucket)).move(
            new Key.From(source),
            new Key.From(destination)
        );
        try (S3Object s3Object = client.getObject(bucket, destination)) {
            MatcherAssert.assertThat(
                ByteStreams.toByteArray(s3Object.getObjectContent()),
                new IsEqual<>(original)
            );
        }
    }

    @Test
    void shouldDeleteOriginalObjectWhenMoved(final AmazonS3 client) throws Exception {
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        final String source = "src";
        client.putObject(
            bucket,
            source,
            new ByteArrayInputStream("some data".getBytes()),
            new ObjectMetadata()
        );
        new BlockingStorage(this.storage(bucket)).move(
            new Key.From(source),
            new Key.From("dest")
        );
        MatcherAssert.assertThat(
            client.doesObjectExist(bucket, source),
            new IsEqual<>(false)
        );
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
