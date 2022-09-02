/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.factory.Storages;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for EtcdStorageFactory.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class EtcdStorageFactoryTest {
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
}
