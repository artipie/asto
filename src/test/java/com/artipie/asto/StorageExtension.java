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
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.s3.S3Storage;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * JUnit5 test template extension running test methods with all Storage types.
 *
 * @since 0.15
 */
final class StorageExtension
    implements TestTemplateInvocationContextProvider, BeforeAllCallback, AfterAllCallback {

    /**
     * S3 mock server extension.
     */
    private final S3MockExtension mock = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Vert.x instance to use in tests.
     */
    private Vertx vertx;

    @Override
    public void beforeAll(final ExtensionContext extension) {
        this.mock.beforeAll(extension);
        this.vertx = Vertx.vertx();
    }

    @Override
    public void afterAll(final ExtensionContext extension) {
        this.mock.afterAll(extension);
        if (this.vertx != null) {
            this.vertx.rxClose().blockingAwait();
        }
    }

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
        return context.getTestMethod().isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
        final ExtensionContext context) {
        final Collection<Storage> storages;
        try {
            storages = Arrays.asList(
                new InMemoryStorage(),
                new InMemoryStorage().transaction(Collections.emptyList()).get(),
                this.s3Storage(),
                new FileStorage(
                    Files.createTempDirectory("junit"),
                    this.vertx
                )
            );
        } catch (final InterruptedException | ExecutionException | IOException ex) {
            throw new IllegalStateException("Failed to generate storage", ex);
        }
        return storages.stream().map(StorageContext::new);
    }

    /**
     * Creates {@link S3Storage} instance with mock endpoint and random bucket.
     *
     * @return S3Storage instance.
     * @checkstyle MethodNameCheck (2 line)
     */
    private S3Storage s3Storage() {
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .endpointOverride(
                URI.create(String.format("http://localhost:%d", this.mock.getHttpPort()))
            )
            .build();
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(CreateBucketRequest.builder().bucket(bucket).build()).join();
        return new S3Storage(client, bucket);
    }

    /**
     * Test template context with bound storage.
     *
     * @since 0.15
     */
    private static class StorageContext implements TestTemplateInvocationContext {

        /**
         * Storage instance to be used in test.
         */
        private final Storage storage;

        /**
         * Ctor.
         *
         * @param storage Storage instance to be used in test.
         */
        StorageContext(final Storage storage) {
            this.storage = storage;
        }

        @Override
        public String getDisplayName(final int index) {
            return String.format("[%s]", this.storage.getClass().getSimpleName());
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return Collections.singletonList(new Resolver());
        }

        /**
         * Resolver for {@link Storage} parameter.
         *
         * @since 0.15
         */
        private class Resolver implements ParameterResolver {

            @Override
            public boolean supportsParameter(
                final ParameterContext parameter,
                final ExtensionContext extension) throws ParameterResolutionException {
                return parameter.getParameter().getType().equals(Storage.class);
            }

            @Override
            public Object resolveParameter(
                final ParameterContext parameter,
                final ExtensionContext extension) throws ParameterResolutionException {
                return StorageContext.this.storage;
            }
        }
    }
}
