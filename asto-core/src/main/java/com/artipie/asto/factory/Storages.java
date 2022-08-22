/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jcabi.log.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
     * Storage factories.
     */
    private final Map<String, StorageFactory> factories;

    /**
     * Ctor.
     */
    public Storages() {
        this(System.getenv());
    }

    /**
     * Ctor.
     *
     * @param env Environment parameters.
     */
    public Storages(final Map<String, String> env) {
        this.factories = init(env);
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
            throw new StorageNotFoundException(type);
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
     * Finds and initiates annotated classes in default and env packages.
     *
     * @param env Environment parameters.
     * @return Map of StorageFactories.
     */
    private static Map<String, StorageFactory> init(final Map<String, String> env) {
        final List<String> pkgs = Lists.newArrayList(Storages.DEFAULT_PACKAGE);
        final String pgs = env.get(Storages.SCAN_PACK);
        if (!Strings.isNullOrEmpty(pgs)) {
            pkgs.addAll(Arrays.asList(pgs.split(";")));
        }
        final Map<String, StorageFactory> res = new HashMap<>();
        pkgs.forEach(
            pkg -> new Reflections(pkg)
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
                        final StorageFactory existed = res.get(type);
                        if (existed != null) {
                            throw new ArtipieException(
                                String.format(
                                    "Storage factory with type '%s' already exists [class=%s].",
                                    type, existed.getClass().getSimpleName()
                                )
                            );
                        }
                        try {
                            res.put(
                                type,
                                (StorageFactory) clazz.getDeclaredConstructor().newInstance()
                            );
                            Logger.info(
                                Storages.class,
                                "Initiated storage factory [type=%s, class=%s]",
                                type, clazz.getSimpleName()
                            );
                        } catch (final InstantiationException | IllegalAccessException
                            | InvocationTargetException | NoSuchMethodException err) {
                            throw new ArtipieException(err);
                        }
                    }
                )
        );
        return res;
    }
}
