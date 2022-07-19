/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.third.party.factory.second;

import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.StorageConfig;
import com.artipie.asto.factory.StorageFactory;
import com.artipie.asto.memory.InMemoryStorage;

/**
 * Test storage factory.
 *
 * @since 1.13.0
 */
@ArtipieStorageFactory("test-second")
public final class TestSecondStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final StorageConfig cfg) {
        return new InMemoryStorage();
    }
}
