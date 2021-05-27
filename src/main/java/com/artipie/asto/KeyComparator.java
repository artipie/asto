/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import java.util.Comparator;

/**
 * Comparator for key values by their string representation.
 * @param <T> Type of key
 * @since 1.1.0
 */
public final class KeyComparator<T extends Key> implements Comparator<T> {
    @Override
    public int compare(final T first, final T second) {
        return first.string().compareTo(second.string());
    }
}
