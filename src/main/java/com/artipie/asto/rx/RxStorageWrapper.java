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
package com.artipie.asto.rx;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Collection;
import java.util.function.Function;

/**
 * Reactive wrapper over {@code Storage}.
 *
 * @since 0.9
 */
public final class RxStorageWrapper implements RxStorage {

    /**
     * Wrapped storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage The storage
     */
    public RxStorageWrapper(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Single<Boolean> exists(final Key key) {
        return Single.defer(() -> SingleInterop.fromFuture(this.storage.exists(key)));
    }

    @Override
    public Single<Collection<Key>> list(final Key prefix) {
        return Single.defer(() -> SingleInterop.fromFuture(this.storage.list(prefix)));
    }

    @Override
    public Completable save(final Key key, final Content content) {
        return Completable.defer(
            () -> CompletableInterop.fromFuture(this.storage.save(key, content))
        );
    }

    @Override
    public Completable move(final Key source, final Key destination) {
        return Completable.defer(
            () -> CompletableInterop.fromFuture(this.storage.move(source, destination))
        );
    }

    @Override
    public Single<Long> size(final Key key) {
        return Single.defer(() -> SingleInterop.fromFuture(this.storage.size(key)));
    }

    @Override
    public Single<Content> value(final Key key) {
        return Single.defer(() -> SingleInterop.fromFuture(this.storage.value(key)));
    }

    @Override
    public Completable delete(final Key key) {
        return Completable.defer(() -> CompletableInterop.fromFuture(this.storage.delete(key)));
    }

    @Override
    public <T> Single<T> exclusively(
        final Key key,
        final Function<RxStorage, Single<T>> operation
    ) {
        return Single.defer(
            () -> SingleInterop.fromFuture(
                this.storage.exclusively(
                    key,
                    st -> operation.apply(new RxStorageWrapper(st)).to(SingleInterop.get())
                )
            )
        );
    }
}
