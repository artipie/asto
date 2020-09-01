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
import com.artipie.asto.Key;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Cache control.
 * @since 0.24
 */
public interface CacheControl {

    /**
     * Validate cached item: checks if cached value can be used or needs to be updated by fresh
     * value.
     * @param item Cached item
     * @param content Content supplier
     * @return True if cached item can be used, false if needs to be updated
     */
    CompletionStage<Boolean> validate(Key item, AsyncContent content);

    /**
     * Standard cache controls.
     * @since 0.24
     */
    enum Standard implements CacheControl {
        /**
         * Don't use cache, always invalidate.
         */
        NO_CACHE((item, content) -> CompletableFuture.completedFuture(false)),
        /**
         * Always use cache, don't invalidate.
         */
        ALWAYS((item, content) -> CompletableFuture.completedFuture(true));

        /**
         * Origin cache control.
         */
        private final CacheControl origin;

        /**
         * Ctor.
         * @param origin Cache control
         */
        Standard(final CacheControl origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Boolean> validate(final Key item, final AsyncContent supplier) {
            return this.origin.validate(item, supplier);
        }
    }

    /**
     * All cache controls should validate the cache.
     * @since 0.25
     */
    final class All implements CacheControl {

        /**
         * Cache control items.
         */
        private final Collection<CacheControl> items;

        /**
         * All of items should validate the cache.
         * @param items Cache controls
         */
        public All(final CacheControl... items) {
            this(Arrays.asList(items));
        }

        /**
         * All of items should validate the cache.
         * @param items Cache controls
         */
        public All(final Collection<CacheControl> items) {
            this.items = items;
        }

        @Override
        public CompletionStage<Boolean> validate(final Key key, final AsyncContent content) {
            return Observable.fromIterable(this.items)
                .flatMapSingle(item -> SingleInterop.fromFuture(item.validate(key, content)))
                .all(item -> item)
                .to(SingleInterop.get());
        }
    }
}
