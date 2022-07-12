/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.artipie.ArtipieException;

/**
 * Exception indicating that {@link StorageFactory} cannot be found.
 *
 * @since 1.13.0
 */
public class StorageFactoryNotFoundException extends ArtipieException {

    private static final long serialVersionUID = 0L;

    /**
     * Ctor.
     *
     * @param type Storage type
     */
    public StorageFactoryNotFoundException(final String type) {
        super(String.format("Storage with type '%s' is not found.", type));
    }
}
