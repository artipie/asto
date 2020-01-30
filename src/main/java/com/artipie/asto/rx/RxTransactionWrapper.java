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

import com.artipie.asto.Key;
import com.artipie.asto.Transaction;
import hu.akarnokd.rxjava3.jdk8interop.CompletableInterop;
import hu.akarnokd.rxjava3.jdk8interop.SingleInterop;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Collection;
import java.util.List;
import org.reactivestreams.FlowAdapters;

/**
 * A reactive wrapper of {@link RxTransaction}.
 *
 * @since 0.10
 */
public final class RxTransactionWrapper implements RxTransaction {

    /**
     * The wrapped storage.
     */
    private final Transaction wrapped;

    /**
     * Ctor.
     * @param wrapped The storage to wrapp
     */
    public RxTransactionWrapper(final Transaction wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Completable commit() {
        return CompletableInterop.fromFuture(this.wrapped.commit());
    }

    @Override
    public Completable rollback() {
        return CompletableInterop.fromFuture(this.wrapped.rollback());
    }

    @Override
    public Single<Boolean> exists(final Key key) {
        return SingleInterop.fromFuture(this.wrapped.exists(key));
    }

    @Override
    public Single<Collection<Key>> list(final String prefix) {
        return SingleInterop.fromFuture(this.wrapped.list(prefix));
    }

    @Override
    public Completable save(final Key key, final Flowable<Byte> content) {
        return CompletableInterop.fromFuture(
            this.wrapped.save(
                key,
                FlowAdapters.toFlowPublisher(content)
            )
        );
    }

    @Override
    public Single<Flowable<Byte>> value(final Key key) {
        return SingleInterop.fromFuture(this.wrapped.value(key))
            .map(flow -> Flowable.fromPublisher(FlowAdapters.toPublisher(flow)));
    }

    @Override
    public Single<RxTransaction> transaction(final List<Key> keys) {
        return SingleInterop.fromFuture(this.wrapped.transaction(keys))
            .map(RxTransactionWrapper::new);
    }
}
