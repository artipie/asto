/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.factory.Storages;
import com.artipie.asto.fs.VertxFileStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for Storages.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class StoragesTest {

    @Test
    void shouldCreateVertxFileStorage() {
        MatcherAssert.assertThat(
            new Storages()
                .newStorage(
                    "vertx-file",
                    Yaml.createYamlMappingBuilder()
                        .add("path", "")
                        .build()
                ),
            new IsInstanceOf(VertxFileStorage.class)
        );
    }
}
