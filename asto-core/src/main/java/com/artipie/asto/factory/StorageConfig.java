/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.ArtipieException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Storage config.
 *
 * @since 1.13.0
 */
public interface StorageConfig {

    /**
     * Gets string value.
     *
     * @param key Key.
     * @return Value.
     */
    String string(String key);

    /**
     * Gets sequence of values.
     *
     * @param key Key.
     * @return Sequence.
     */
    Collection<String> sequence(String key);

    /**
     * Gets subconfig.
     *
     * @param key Key.
     * @return Config.
     */
    StorageConfig config(String key);

    /**
     * Strict storage config throws {@code NullPointerException} when value is not exist.
     *
     * @since 1.13.0
     */
    class StrictStorageConfig implements StorageConfig {

        /**
         * Original config.
         */
        private final StorageConfig original;

        /**
         * Ctor.
         *
         * @param original Original config.
         */
        public StrictStorageConfig(final StorageConfig original) {
            this.original = original;
        }

        @Override
        public String string(final String key) {
            return Objects.requireNonNull(
                this.original.string(key),
                String.format("No value found for key %s", key)
            );
        }

        @Override
        public Collection<String> sequence(final String key) {
            return Objects.requireNonNull(
                this.original.sequence(key),
                String.format("No sequence found for key %s", key)
            );
        }

        @Override
        public StorageConfig config(final String key) {
            return Objects.requireNonNull(
                this.original.config(key),
                String.format("No config found for key %s", key)
            );
        }
    }

    /**
     * Storage config based on {@link YamlMapping}.
     *
     * @since 1.13.0
     */
    class YamlStorageConfig implements StorageConfig {
        /**
         * Original {@code YamlMapping}.
         */
        private final YamlMapping original;

        /**
         * Ctor.
         *
         * @param original Original {@code YamlMapping}.
         */
        public YamlStorageConfig(final YamlMapping original) {
            this.original = original;
        }

        @Override
        public String string(final String key) {
            final YamlNode node = this.original.value(key);
            String res = null;
            if (node != null) {
                switch (node.type()) {
                    case SCALAR:
                        res = node.asScalar().value();
                        break;
                    case MAPPING:
                        res = node.asMapping().toString();
                        break;
                    case STREAM:
                        res = node.asStream().toString();
                        break;
                    case SEQUENCE:
                        res = node.asSequence().toString();
                        break;
                    default:
                        throw new ArtipieException(
                            String.format("Unknown node type [%s]", node.type())
                        );
                }
            }
            return res;
        }

        @Override
        public Collection<String> sequence(final String key) {
            return this.original.yamlSequence(key)
                .values()
                .stream()
                .map(node -> node.asScalar().value())
                .collect(Collectors.toList());
        }

        @Override
        public StorageConfig config(final String key) {
            return new YamlStorageConfig(this.original.yamlMapping(key));
        }
    }
}
