/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.StorageFactory;
import com.artipie.asto.factory.StrictYamlMapping;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import java.time.Duration;

/**
 * Etcd storage factory.
 * @since 1.13.0
 */
@ArtipieStorageFactory("etcd")
public final class EtcdStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final YamlMapping cfg) {
        final YamlMapping yaml = new StrictYamlMapping(cfg).yamlMapping("connection");
        final ClientBuilder builder = Client.builder()
            .endpoints(
                yaml.yamlSequence("endpoints").values()
                    .stream()
                    .map(node -> node.asScalar().value())
                    .toArray(String[]::new)
            );
        final String sto = yaml.string("timeout");
        if (sto != null) {
            builder.connectTimeout(Duration.ofMillis(Integer.parseInt(sto)));
        }
        return new EtcdStorage(builder.build());
    }
}
