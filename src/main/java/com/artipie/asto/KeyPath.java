/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Converts key to path.
 *
 * @since 1.11
 */
public final class KeyPath {

    /**
     * Root.
     */
    private final Path root;

    /**
     * Key to transform.
     */
    private final Key key;

    /**
     * Ctor.
     * @param root Root path in string
     * @param key Key to transform
     */
    public KeyPath(final String root, final Key key) {
        this(Paths.get(root), key);
    }

    /**
     * Ctor.
     * @param root Root
     * @param key Key to transform
     */
    public KeyPath(final Path root, final Key key) {
        this.root = root.normalize();
        this.key = key;
    }

    /**
     * Get path of the key.
     * <p>
     * Validates that the path is in root location and converts it to path.
     * Fails with {@link ArtipieIOException} if key is out of root location.
     * </p>
     *
     * @return Path future
     */
    public Path get() {
        final Path path = this.root.resolve(this.key.string()).normalize();
        if (path.startsWith(this.root)) {
            return path;
        } else {
            throw new ArtipieIOException(
                String.format("Entry path is out of root location: %s", path)
            );
        }
    }
}
