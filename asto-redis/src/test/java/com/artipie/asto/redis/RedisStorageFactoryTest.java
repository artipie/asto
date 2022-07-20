/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.redis;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.Storages;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;

/**
 * Tests for redis storage factory.
 *
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class RedisStorageFactoryTest {
    /**
     * Redis test container.
     */
    private GenericContainer<?> redis;

    @BeforeEach
    void setUp() {
        this.redis = new GenericContainer<>("redis:3-alpine")
            .withExposedPorts(6379);
        this.redis.start();
    }

    @AfterEach
    void tearDown() {
        this.redis.stop();
    }

    @Test
    void shouldCreateRedisStorage() {
        MatcherAssert.assertThat(
            new Storages()
                .newStorage("redis", redisConfig(this.redis.getFirstMappedPort())),
            new IsInstanceOf(RedisStorage.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenConfigIsNotDefined() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new Storages()
                .newStorage(
                    "redis",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "redis")
                        .build()
                )
        );
    }

    @Test
    void shouldUseDefaultRedisObjectNameWhenConfigNameIsNull() {
        final Key key = new Key.From("test_key");
        final byte[] data = "test_data".getBytes();
        new BlockingStorage(
            new Storages()
                .newStorage("redis", redisConfig(this.redis.getFirstMappedPort()))
        ).save(key, data);
        MatcherAssert.assertThat(
            new BlockingStorage(
                new Storages()
                    .newStorage(
                        "redis",
                        redisConfig(
                            this.redis.getFirstMappedPort(),
                            RedisStorageFactory.DEF_OBJ_NAME
                        )
                    )
            ).value(key),
            new IsEqual<>(data)
        );
    }

    @Test
    void shouldUseRedisObjectName() {
        final Key key = new Key.From("test_key");
        final byte[] data = "test_data".getBytes();
        new BlockingStorage(
            new Storages()
                .newStorage(
                    "redis", redisConfig(this.redis.getFirstMappedPort(), "redis_obj_1")
                )
        ).save(key, data);
        MatcherAssert.assertThat(
            "Should create RedisStorage based on an object with name 'redis_obj_1'",
            new BlockingStorage(
                new Storages()
                    .newStorage(
                        "redis",
                        redisConfig(
                            this.redis.getFirstMappedPort(),
                            "redis_obj_1"
                        )
                    )
            ).value(key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Should not exist in RedisStorage based on an object with name 'redis_obj_2'",
            new BlockingStorage(
                new Storages()
                    .newStorage(
                        "redis",
                        redisConfig(
                            this.redis.getFirstMappedPort(),
                            "redis_obj_2"
                        )
                    )
            ).exists(key),
            new IsEqual<>(false)
        );
    }

    private static YamlMapping redisConfig(final Integer port) {
        return redisConfig(port, null);
    }

    private static YamlMapping redisConfig(
        final Integer port,
        final String name) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder()
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
            );
        if (name != null) {
            builder = builder.add("name", name);
        }
        return builder.build();
    }

}
