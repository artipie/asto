/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.google.common.base.Strings;
import com.jcabi.log.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * Storages to get instance of storage.
 *
 * @since 1.13.0
 */
public final class Storages {
    /**
     * Environment parameter to define packages to find storage factories.
     * Package names should be separated by semicolon ';'.
     */
    public static final String SCAN_PACK = "STORAGE_FACTORY_SCAN_PACKAGES";

    /**
     * Default package to find storage factories.
     */
    private static final String DEFAULT_PACKAGE = "com.artipie.asto";

    /**
     * Environment parameters.
     */
    private final Map<String, String> env;

    /**
     * Storage factories.
     */
    private final Map<String, StorageFactory> factories;

    /**
     * Ctor.
     *
     * @param env Environment parameters.
     */
    private Storages(final Map<String, String> env) {
        this.env = env;
        this.factories = new HashMap<>();
    }

    /**
     * Factory method to get instance of {@code Storages}.
     *
     * @return Storages.
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static Storages newStorages() {
        return newStorages(System.getenv());
    }

    /**
     * Factory method to get instance of {@code Storages}.
     *
     * @param env Environment parameters.
     * @return Storages.
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static Storages newStorages(final Map<String, String> env) {
        final Storages res = new Storages(env);
        res.init();
        return res;
    }

    /**
     * Gets a new instance of storage by type.
     *
     * @param type Type of storage.
     * @param cfg Storage config.
     * @return Storage.
     */
    public Storage newStorage(final String type, final YamlMapping cfg) {
        final StorageFactory factory = this.factories.get(type);
        if (factory == null) {
            throw new StorageFactoryNotFoundException(type);
        }
        return factory.newStorage(cfg);
    }

    /**
     * Known storage types.
     *
     * @return Set of storage types.
     */
    public Set<String> types() {
        return this.factories.keySet();
    }

    /**
     * Fills {@code factories} map.
     */
    private void init() {
        this.initiateFrom(Storages.DEFAULT_PACKAGE);
        final String pgs = this.env.get(Storages.SCAN_PACK);
        if (!Strings.isNullOrEmpty(pgs)) {
            this.initiateFrom(pgs.split(";"));
        }
    }

    /**
     * Finds and initiates annotated classes in passed packages.
     * <p>
     * Cannot use {@code ConfigurationBuilder} due to
     * <a href="https://github.com/ronmamo/reflections/issues/378">bug</a>.
     *
     * @param pkgs Packages.
     */
    private void initiateFrom(final String... pkgs) {
        for (final String pkg : pkgs) {
            new Reflections(pkg)
                .get(Scanners.TypesAnnotated.with(ArtipieStorageFactory.class).asClass())
                .forEach(
                    clazz -> {
                        final String type = Arrays.stream(clazz.getAnnotations())
                            .filter(ArtipieStorageFactory.class::isInstance)
                            .map(a -> ((ArtipieStorageFactory) a).value())
                            .findFirst()
                            .orElseThrow(
                                // @checkstyle LineLengthCheck (3 lines)
                                () -> new ArtipieException("Annotation 'ArtipieStorageFactory' should have a not empty value")
                            );
                        final StorageFactory existed = this.factories.get(type);
                        if (existed != null) {
                            throw new ArtipieException(
                                String.format(
                                    "Storage factory with type '%s' already exists [class=%s].",
                                    type, existed.getClass().getSimpleName()
                                )
                            );
                        }
                        try {
                            this.factories.put(
                                type,
                                (StorageFactory) clazz.getDeclaredConstructor().newInstance()
                            );
                            Logger.info(
                                this,
                                "Initiated storage factory [type=%s, class=%s]",
                                type, clazz.getSimpleName()
                            );
                        } catch (final InstantiationException | IllegalAccessException
                            | InvocationTargetException | NoSuchMethodException err) {
                            throw new ArtipieException(err);
                        }
                    }
                );
        }
    }

}
