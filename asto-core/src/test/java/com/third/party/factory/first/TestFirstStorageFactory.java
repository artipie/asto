/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.third.party.factory.first;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.StorageFactory;
import com.artipie.asto.memory.InMemoryStorage;

/**
 * Test storage factory.
 *
 * @since 1.13.0
 */
@ArtipieStorageFactory("test-first")
public final class TestFirstStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final YamlMapping cfg) {
        return new InMemoryStorage();
    }
}
