/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.redis;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Storages;
import com.artipie.asto.test.StorageWhiteboxVerification;
import org.junit.After;
import org.junit.Before;
import org.testcontainers.containers.GenericContainer;

/**
 * Redis storage verification test.
 *
 * @checkstyle ProtectedMethodInFinalClassCheck (500 lines)
 * @since 0.1
 */
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.AvoidDuplicateLiterals"})
public final class RedisStorageWhiteboxVerificationTest extends StorageWhiteboxVerification {

    /**
     * Default redis port.
     */
    private static final int DEF_PORT = 6379;

    /**
     * Redis test container.
     */
    private GenericContainer<?> redis;

    /**
     * Redis storage.
     */
    private Storage storage;

    @Before
    public void setUp() {
        this.redis = new GenericContainer<>("redis:3-alpine")
            .withExposedPorts(RedisStorageWhiteboxVerificationTest.DEF_PORT);
        this.redis.start();
        this.storage = new Storages()
            .newStorage("redis", config(this.redis.getFirstMappedPort()));
    }

    @After
    public void tearDown() {
        this.redis.stop();
    }

    @Override
    protected Storage newStorage() {
        return this.storage;
    }

    private static YamlMapping config(final Integer port) {
        return Yaml.createYamlMappingBuilder()
            .add("type", "redis")
            .add(
                "config",
                Yaml.createYamlMappingBuilder()
                    .add(
                        "singleServerConfig",
                        Yaml.createYamlMappingBuilder()
                            .add(
                                "address",
                                String.format("redis://127.0.0.1:%d", port)
                            ).build()
                    ).build()
            ).build();
    }
}
