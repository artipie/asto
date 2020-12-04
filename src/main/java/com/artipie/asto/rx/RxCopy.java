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
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.Collection;
import java.util.Optional;

/**
 * A reactive version of {@link com.artipie.asto.Copy}.
 *
 * @since 0.19
 * @checkstyle NonStaticMethodCheck (500 lines)
 * @checkstyle MemberNameCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public class RxCopy {

    /**
     * The default parallelism level.
     */
    private static final Integer DEFLT_PARALLELISM = Runtime.getRuntime().availableProcessors();

    /**
     * The storage to copy from.
     */
    private final RxStorage from;

    /**
     * The keys to transfer.
     */
    private final Optional<Collection<Key>> keys;

    /**
     * Amount of parallel copy operations.
     */
    private final Integer parallelism;

    /**
     * Ctor.
     * @param from The storage to copy from.
     */
    public RxCopy(final RxStorage from) {
        this(from, Optional.empty(), RxCopy.DEFLT_PARALLELISM);
    }

    /**
     * Ctor.
     * @param from The storage to copy from.
     * @param keys The keys to copy.
     */
    public RxCopy(final RxStorage from, final Collection<Key> keys) {
        this(from, Optional.of(keys), RxCopy.DEFLT_PARALLELISM);
    }

    /**
     * Ctor.
     * @param from The storage to copy from.
     * @param keys The keys to copy.
     * @param parallelism The parallelism level.
     */
    public RxCopy(final RxStorage from, final Collection<Key> keys, final Integer parallelism) {
        this(from, Optional.of(keys), parallelism);
    }

    /**
     * Ctor.
     * @param from The storage to copy from.
     * @param keys The keys to copy, all keys are copied if collection is not specified.
     * @param parallelism The parallelism level.
     */
    private RxCopy(
        final RxStorage from,
        final Optional<Collection<Key>> keys,
        final Integer parallelism
    ) {
        this.from = from;
        this.keys = keys;
        this.parallelism = parallelism;
    }

    /**
     * Copy key to storage.
     * @param to The storage to copy to.
     * @return The completion signal.
     */
    public Completable copy(final RxStorage to) {
        return Completable.concat(
            this.keys.map(Flowable::fromIterable)
                .orElseGet(() -> this.from.list(Key.ROOT).flattenAsFlowable(ks -> ks))
                .map(
                    key -> Completable.defer(
                        () -> to.save(
                            key,
                            new Content.From(this.from.value(key).flatMapPublisher(cnt -> cnt))
                        )
                    )
                ).buffer(this.parallelism).map(Completable::merge)
        );
    }
}
