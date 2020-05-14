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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage key.
 *
 * @since 0.6
 */
public interface Key {

    /**
     * Root key.
     */
    Key ROOT = new Key.From(Collections.emptyList());

    /**
     * Key.
     * @return Key string
     */
    String string();

    /**
     * Parent key.
     * @return Parent key or Optional.empty if the current key is ROOT
     */
    Optional<Key> parent();

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

        @Override
        public Optional<Key> parent() {
            return this.origin.parent();
        }

        @Override
        public final String toString() {
            return this.string();
        }
    }

    /**
     * Key from something.
     * @since 0.6
     */
    final class From implements Key {

        /**
         * Delimiter used to split string into parts and join parts into string.
         */
        private static final String DELIMITER = "/";

        /**
         * Parts.
         */
        private final List<String> parts;

        /**
         * Ctor.
         * @param parts Parts delimited by `/` symbol
         */
        public From(final String parts) {
            this(parts.split(From.DELIMITER));
        }

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
                    new From(base.string()).parts.stream(),
                    Arrays.stream(parts)
                ).collect(Collectors.toList())
            );
        }

        /**
         * Ctor.
         * @param parts Parts
         */
        @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
        public From(final List<String> parts) {
            if (parts.size() == 1 && parts.get(0).isEmpty()) {
                this.parts = Collections.emptyList();
            } else {
                this.parts = parts.stream()
                    .flatMap(part -> Arrays.stream(part.split("/")))
                    .collect(Collectors.toList());
            }
        }

        @Override
        public String string() {
            for (final String part : this.parts) {
                if (part.isEmpty()) {
                    throw new IllegalStateException("Empty parts are not allowed");
                }
                if (part.contains(From.DELIMITER)) {
                    throw new IllegalStateException(String.format("Invalid part: '%s'", part));
                }
            }
            return String.join(From.DELIMITER, this.parts);
        }

        @Override
        public Optional<Key> parent() {
            final Optional<Key> parent;
            if (this.parts.isEmpty()) {
                parent = Optional.empty();
            } else {
                parent = Optional.of(
                    new Key.From(this.parts.subList(0, this.parts.size() - 1))
                );
            }
            return parent;
        }

        @Override
        public boolean equals(final Object another) {
            if (this == another) {
                return true;
            }
            if (another == null || getClass() != another.getClass()) {
                return false;
            }
            final From from = (From) another;
            return Objects.equals(this.parts, from.parts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.parts);
        }

        @Override
        public String toString() {
            return this.string();
        }
    }
}
