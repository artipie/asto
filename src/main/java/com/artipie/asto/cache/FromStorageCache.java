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

import com.artipie.asto.AsyncContent;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Cache implementation that tries to obtain items from storage cache,
 * validates it and returns if valid. If item is not present in storage or is not valid,
 * it is loaded from remote.
 * @since 0.24
 */
public final class FromStorageCache implements Cache {

    /**
     * Back-end storage.
     */
    private final Storage storage;

    /**
     * New storage cache.
     * @param storage Back-end storage for cache
     */
    public FromStorageCache(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Optional<? extends Content>> load(final Key key,
        final AsyncContent remote, final CacheControl control) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.storage);
        return rxsto.exists(key)
            .filter(exists -> exists)
            .flatMapSingleElement(
                exists -> SingleInterop.fromFuture(
                    control.validate(key, () -> this.storage.value(key))
                )
            )
            .filter(valid -> valid)
            .<Optional<? extends Content>>flatMapSingleElement(
                ignore -> rxsto.value(key).map(Optional::of)
            ).doOnError(err -> Logger.warn(this, "Failed to read cached item: %[exception]s", err))
            .onErrorComplete()
            .switchIfEmpty(
                SingleInterop.fromFuture(
                    remote.get().handle(
                        (content, throwable) -> {
                            final Optional<? extends Content> res;
                            if (throwable == null) {
                                res = Optional.of(content);
                            } else {
                                res = Optional.empty();
                            }
                            return res;
                        }
                    )
                ).<Optional<? extends Content>>flatMap(
                    content -> content.map(
                        val -> rxsto.save(key, new Content.From(val.size(), val))
                            .andThen(rxsto.value(key).map(Optional::of))
                    ).orElse(Single.just(Optional.empty()))
                )
            )
            .to(SingleInterop.get());
    }
}
