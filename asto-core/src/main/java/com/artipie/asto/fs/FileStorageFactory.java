/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.StorageFactory;
import com.artipie.asto.factory.StrictYamlMapping;
import java.nio.file.Paths;

/**
 * File storage factory.
 *
 * @since 1.13.0
 */
@ArtipieStorageFactory("fs")
public final class FileStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final YamlMapping cfg) {
        return new FileStorage(
            Paths.get(new StrictYamlMapping(cfg).string("path"))
        );
    }
}
