/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.StorageFactory;
import com.artipie.asto.factory.StrictYamlMapping;
import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

/**
 * Factory to create S3 storage.
 *
 * @since 1.13.0
 */
@ArtipieStorageFactory("s3")
public final class S3StorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final YamlMapping cfg) {
        return new S3Storage(
            S3StorageFactory.s3Client(cfg),
            new StrictYamlMapping(cfg).string("bucket"),
            !"false".equals(cfg.string("multipart"))
        );
    }

    /**
     * Creates {@link S3AsyncClient} instance based on YAML config.
     *
     * @param cfg Storage config.
     * @return Built S3 client.
     * @checkstyle MethodNameCheck (3 lines)
     */
    private static S3AsyncClient s3Client(final YamlMapping cfg) {
        final S3AsyncClientBuilder builder = S3AsyncClient.builder();
        final String region = cfg.string("region");
        if (region != null) {
            builder.region(Region.of(region));
        }
        final String endpoint = cfg.string("endpoint");
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder
            .credentialsProvider(
                S3StorageFactory.credentials(
                    new StrictYamlMapping(cfg).yamlMapping("credentials")
                )
            )
            .build();
    }

    /**
     * Creates {@link StaticCredentialsProvider} instance based on YAML config.
     *
     * @param yaml Credentials config YAML.
     * @return Credentials provider.
     */
    private static StaticCredentialsProvider credentials(final YamlMapping yaml) {
        final String type = yaml.string("type");
        if ("basic".equals(type)) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    yaml.string("accessKeyId"),
                    yaml.string("secretAccessKey")
                )
            );
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported S3 credentials type: %s", type)
            );
        }
    }
}
