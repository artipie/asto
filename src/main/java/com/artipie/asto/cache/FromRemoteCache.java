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
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * This cache implementation loads all the items from remote and caches it to storage. Content
 * is loaded from cache only if remote failed to return requested item.
 * @since 0.30
 */
public final class FromRemoteCache implements Cache {

    /**
     * Back-end storage.
     */
    private final Storage storage;

    /**
     * New remote cache.
     * @param storage Back-end storage for cache
     */
    public FromRemoteCache(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Optional<? extends Content>> load(
        final Key key, final Remote remote, final CacheControl control
    ) {
        return remote.get().handle(
            (content, throwable) -> {
                final CompletionStage<Optional<? extends Content>> res;
                if (throwable == null && content.isPresent()) {
                    res = this.storage.save(
                        key,  new Content.From(content.get().size(), content.get())
                    ).thenCompose(nothing -> this.storage.value(key))
                        .thenApply(Optional::of);
                } else {
                    res = new FromStorageCache(this.storage)
                        .load(key, new Remote.Failed(throwable), control);
                }
                return res;
            }
        ).thenCompose(Function.identity());
    }
}
