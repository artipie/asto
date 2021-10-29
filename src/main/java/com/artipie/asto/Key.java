/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.ArtipieException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Storage key.
 *
 * @since 0.6
 * @todo #341:30min Add others key operations (exclusion by index, insertion,..).
 *  We should exclude a part from a storage key by index. We should also insert
 *  a part to a key by specifying the position (Like we do with {@code List.add(Obj, index)}).
 *  We also want to get parts of a storage key as list or stream.
 */
public interface Key {
    /**
     * Comparator for key values by their string representation.
     */
    Comparator<Key> CMP_STRING = Comparator.comparing(Key::string);

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
         * Key from two keys.
         * @param first First key
         * @param second Second key
         */
        public From(final Key first, final Key second) {
            this(
                Stream.concat(
                    new From(first).parts.stream(),
                    new From(second).parts.stream()
                ).collect(Collectors.toList())
            );
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
                    throw new ArtipieException("Empty parts are not allowed");
                }
                if (part.contains(From.DELIMITER)) {
                    throw new ArtipieException(String.format("Invalid part: '%s'", part));
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

    /**
     * Key that excludes first occurrence of part.
     * @since 1.8.1
     */
    final class ExcludeFirst extends Wrap {

        /**
         * Ctor.
         * @param key Key
         * @param part Part to exclude
         */
        public ExcludeFirst(final Key key, final String part) {
            super(
                new Key.From(ExcludeFirst.exclude(key, part))
            );
        }

        /**
         * Excludes first occurrence of part.
         * @param key Key
         * @param part Part to exclude
         * @return List of parts
         */
        private static List<String> exclude(final Key key, final String part) {
            final List<String> parts = new LinkedList<>();
            boolean isfound = false;
            for (final String prt : new From(key.string()).parts) {
                if (prt.equals(part) && !isfound) {
                    isfound = true;
                    continue;
                }
                parts.add(prt);
            }
            return parts;
        }
    }

    /**
     * Key that excludes all occurrences of part found.
     * @since 1.8.1
     */
    final class ExcludeAll extends Wrap {

        /**
         * Ctor.
         * @param key Key
         * @param part Part to exclude
         */
        public ExcludeAll(final Key key, final String part) {
            super(
                new Key.From(
                    new From(key.string())
                        .parts.stream()
                        .filter(p -> !p.equals(part))
                        .collect(Collectors.toList())
                )
            );
        }
    }
}
