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
package com.artipie.asto;

import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A publish which can be consumed only once.
 * @param <T> The type of publisher elements.
 * @since 0.23
 * @todo #204:30min Use one time publisher in Storage.
 *  We need to wrap all publishers created in Storage implementations with this one
 *  and to wrap all incoming publishers in tests.
 */
public final class OneTimePublisher<T> implements Publisher<T> {

    /**
     * The original publisher.
     */
    private final Publisher<T> original;

    /**
     * The amount of subscribers.
     */
    private final AtomicInteger subscribers;

    /**
     * Wrap a publish in a way it can be used only once.
     * @param original The original publisher.
     */
    public OneTimePublisher(final Publisher<T> original) {
        this.original = original;
        this.subscribers = new AtomicInteger(0);
    }

    @Override
    public void subscribe(final Subscriber<? super T> sub) {
        final int subs = this.subscribers.incrementAndGet();
        if (subs == 1) {
            this.original.subscribe(sub);
        } else {
            final String msg =
                "The subscriber could not be consumed more than once. Failed on #%d attempt";
            sub.onError(new IllegalStateException(String.format(msg, subs)));
        }
    }
}
