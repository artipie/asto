/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.asto;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage key.
 *
 * @since 0.6
 */
public interface Key {

    /**
     * Key.
     * @return Key string
     */
    String string();

    /**
     * Creates child key with given name.
     * @param name Child name.
     * @return Child key.
     */
    Key child(String name);

    /**
     * Default decorator.
     * @since 0.7
     */
    abstract class Wrap implements Key {

        /**
         * Origin key.
         */
        private final Key origin;

        /**
         * Ctor.
         * @param origin Origin key
         */
        protected Wrap(final Key origin) {
            this.origin = origin;
        }

        @Override
        public final String string() {
            return this.origin.string();
        }
    }

    /**
     * Key from something.
     * @since 0.6
     */
    final class From implements Key {

        /**
         * Parts.
         */
        private final Iterable<String> parts;

        /**
         * Ctor.
         * @param parts Parts
         */
        public From(final String... parts) {
            this(Arrays.asList(parts));
        }

        /**
         * From base path and parts.
         * @param base Base path
         * @param parts Parts
         */
        public From(final Key base, final String... parts) {
            this(
                Stream.concat(
                    Arrays.asList(base.string().split("/")).stream(),
                    Arrays.asList(parts).stream()
                ).collect(Collectors.toList())
            );
        }

        /**
         * Ctor.
         * @param parts Parts
         */
        public From(final Iterable<String> parts) {
            this.parts = Lists.newArrayList(parts);
        }

        @Override
        public String string() {
            return String.join("/", this.parts);
        }

        @Override
        public Key child(final String name) {
            return new From(Iterables.concat(this.parts, Collections.singleton(name)));
        }
    }
}
