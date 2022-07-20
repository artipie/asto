/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

/**
 * Storage that extends {@link Storage.Wrap}.
 *
 * <p>
 * It's to ensure that {@link Storage.Wrap} implements all methods of {@link Storage}.
 * </p>
 * @since 1.11
 */
final class StorageWrapped extends Storage.Wrap implements Storage {
    /**
     * Ctor.
     * @param storage Storage to wrap
     */
    protected StorageWrapped(final Storage storage) {
        super(storage);
    }
}
