/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieException;
import com.artipie.asto.etcd.EtcdStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.s3.S3Storage;
import com.third.party.factory.first2.TestFirst2StorageFactory;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for Storages.
 *
 * @since 1.13.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class StoragesTest {

    @Test
    void shouldCreateFileStorage() {
        MatcherAssert.assertThat(
            new Storages()
                .newStorage(
                    "fs",
                    Yaml.createYamlMappingBuilder()
                        .add("path", "")
                        .build()
                ),
            new IsInstanceOf(FileStorage.class)
        );
    }

    /**
     * Test for S3 storage factory.
     *
     * @checkstyle MethodNameCheck (3 lines)
     */
    @Test
    void shouldCreateS3Storage() {
        MatcherAssert.assertThat(
            new Storages()
                .newStorage(
                    "s3",
                    Yaml.createYamlMappingBuilder()
                        .add("region", "us-east-1")
                        .add("bucket", "aaa")
                        .add("endpoint", "http://localhost")
                        .add(
                            "credentials",
                            Yaml.createYamlMappingBuilder()
                                .add("type", "basic")
                                .add("accessKeyId", "foo")
                                .add("secretAccessKey", "bar")
                                .build()
                        )
                        .build()
                ),
            new IsInstanceOf(S3Storage.class)
        );
    }

    @Test
    void shouldCreateEtcdStorage() {
        MatcherAssert.assertThat(
            new Storages()
                .newStorage(
                    "etcd",
                    Yaml.createYamlMappingBuilder()
                        .add(
                            "connection",
                            Yaml.createYamlMappingBuilder()
                                .add(
                                    "endpoints",
                                    Yaml.createYamlSequenceBuilder()
                                        .add("http://localhost")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                ),
            new IsInstanceOf(EtcdStorage.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenTypeIsWrong() {
        Assertions.assertThrows(
            StorageNotFoundException.class,
            () -> new Storages()
                .newStorage(
                    "wrong-storage-type",
                    Yaml.createYamlMappingBuilder().build()
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenReadTwoFactoryWithTheSameName() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new Storages(
                Collections.singletonMap(
                    Storages.SCAN_PACK,
                    "com.third.party.factory.first;com.third.party.factory.first2"
                )
            ),
            String.format(
                "Storage factory with type 'test-first' already exists [class=%s].",
                TestFirst2StorageFactory.class.getSimpleName()
            )
        );
    }

    @Test
    void shouldScanAdditionalPackageFromEnv() {
        MatcherAssert.assertThat(
            new Storages(
                Collections.singletonMap(
                    Storages.SCAN_PACK,
                    "com.third.party.factory.first"
                )
            ).types(),
            Matchers.containsInAnyOrder("fs", "s3", "etcd", "test-first")
        );
    }

    @Test
    void shouldScanSeveralPackagesFromEnv() {
        MatcherAssert.assertThat(
            new Storages(
                Collections.singletonMap(
                    Storages.SCAN_PACK,
                    "com.third.party.factory.first;com.third.party.factory.second"
                )
            ).types(),
            Matchers.containsInAnyOrder("fs", "s3", "etcd", "test-first", "test-second")
        );
    }
}
