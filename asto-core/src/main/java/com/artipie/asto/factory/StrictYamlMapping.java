/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.amihaiemil.eoyaml.BaseYamlMapping;
import com.amihaiemil.eoyaml.Comment;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlNodeNotFoundException;
import java.util.Set;

/**
 * Decorator for a {@link YamlMapping} which throws
 * YamlNodeNotFoundException, instead of returning null,
 * if a given key doesn't exist in the mapping, or if it points
 * to a different type of node than the demanded one.<br><br>
 * It is based on the fail-fast and null-is-bad idea <br>
 * see here: http://www.yegor256.com/2014/05/13/why-null-is-bad.html
 * <p>
 * Copied from {@code com.amihaiemil.web:eo-yaml} due to the original class is deprecated,
 * but a new implementation is not supplied.
 *
 * @since 1.13.0
 */
public final class StrictYamlMapping extends BaseYamlMapping {

    /**
     * Original YamlMapping.
     */
    private final YamlMapping decorated;

    /**
     * Ctor.
     *
     * @param decorated Original YamlMapping
     */
    public StrictYamlMapping(final YamlMapping decorated) {
        this.decorated = decorated;
    }

    @Override
    public Set<YamlNode> keys() {
        return this.decorated.keys();
    }

    @Override
    public YamlNode value(final YamlNode key) {
        final YamlNode found = this.decorated.value(key);
        if (found == null) {
            throw new YamlNodeNotFoundException(
                String.format("No YAML found for key %s", key)
            );
        }
        return found;
    }

    @Override
    public Comment comment() {
        return this.decorated.comment();
    }
}
