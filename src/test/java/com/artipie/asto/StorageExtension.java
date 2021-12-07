/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */

package com.artipie.asto;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.artipie.asto.etcd.EtcdStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.fs.VertxFileStorage;
import com.artipie.asto.memory.BenchmarkStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.s3.S3Storage;
import com.github.dockerjava.api.DockerClient;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.launcher.EtcdContainer;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.testcontainers.DockerClientFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * JUnit5 test template extension running test methods with all Storage types.
 *
 * @since 0.15
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
final class StorageExtension
    implements TestTemplateInvocationContextProvider, BeforeAllCallback, AfterAllCallback {

    /**
     * S3 mock server extension.
     */
    private final S3MockExtension mock = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Etcd cluster.
     */
    private final EtcdClusterExtension etcd = new EtcdClusterExtension(
        "test-etcd",
        1,
        false,
        "--data-dir",
        "/data.etcd0"
    );

    /**
     * Vert.x file System.
     */
    private Vertx vertx;

    @Override
    public void beforeAll(final ExtensionContext extension) throws Exception {
        if (!SystemUtils.IS_OS_WINDOWS) {
            final DockerClient client = DockerClientFactory.instance().client();
            client.pullImageCmd(EtcdContainer.ETCD_DOCKER_IMAGE_NAME)
                .start()
                .awaitCompletion();
            this.etcd.beforeAll(extension);
        }
        this.mock.beforeAll(extension);
        this.vertx = Vertx.vertx();
    }

    @Override
    public void afterAll(final ExtensionContext extension) throws Exception {
        if (!SystemUtils.IS_OS_WINDOWS) {
            this.etcd.afterAll(extension);
        }
        this.mock.afterAll(extension);
        this.vertx.close();
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
            storages = new LinkedList<>(
                Arrays.asList(
                    new InMemoryStorage(),
                    this.s3Storage(),
                    new SubStorage(new Key.From("mem-prefix"), new InMemoryStorage()),
                    new SubStorage(
                        new Key.From("file-prefix"),
                        new FileStorage(Files.createTempDirectory("pref-sub"))
                    ),
                    new SubStorage(Key.ROOT, new InMemoryStorage()),
                    new SubStorage(Key.ROOT, new FileStorage(Files.createTempDirectory("sub"))),
                    new FileStorage(Files.createTempDirectory("junit")),
                    new VertxFileStorage(Files.createTempDirectory("vtxjunit"), this.vertx),
                    new BenchmarkStorage(new InMemoryStorage())
                )
            );
            if (!SystemUtils.IS_OS_WINDOWS) {
                storages.add(this.etcdStorage());
                storages.add(
                    new SubStorage(
                        new Key.From("etcd-prefix"),
                        this.etcdStorage()
                    )
                );
                storages.add(
                    new SubStorage(Key.ROOT, this.etcdStorage())
                );
            }
        // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception ex) {
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
     * Creates {@link EtcdStorage} instance.
     *
     * @return Etcd instance.
     */
    private EtcdStorage etcdStorage() {
        return new EtcdStorage(
            Client.builder()
                .endpoints(this.etcd.getClientEndpoints())
                .build()
        );
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
