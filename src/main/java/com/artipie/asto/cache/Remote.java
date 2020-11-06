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
import com.jcabi.log.Logger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Async {@link java.util.function.Supplier} of {@link java.util.concurrent.CompletionStage}
 * with {@link Optional} of {@link Content}. It's a {@link FunctionalInterface}.
 *
 * @since 0.32
 */
@FunctionalInterface
public interface Remote extends Supplier<CompletionStage<Optional<? extends Content>>> {

    @Override
    CompletionStage<Optional<? extends Content>> get();

    /**
     * Implementation of {@link Remote} that handle all possible errors and returns
     * empty {@link Optional} if any exception happened.
     * @since 0.32
     */
    class WithErrorHandling implements Remote {

        /**
         * Origin.
         */
        private final Remote origin;

        /**
         * Ctor.
         * @param origin Origin
         */
        public WithErrorHandling(final Remote origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Optional<? extends Content>> get() {
            return this.origin.get().handle(
                (content, throwable) -> {
                    final Optional<? extends Content> res;
                    if (throwable == null) {
                        res = content;
                    } else {
                        Logger.error(this.origin.getClass(), throwable.getMessage());
                        res = Optional.empty();
                    }
                    return res;
                }
            );
        }
    }

    /**
     * Failed remote.
     * @since 0.32
     */
    final class Failed implements Remote {

        /**
         * Failure cause.
         */
        private final Throwable reason;

        /**
         * Ctor.
         * @param reason Failure cause
         */
        public Failed(final Throwable reason) {
            this.reason = reason;
        }

        @Override
        public CompletionStage<Optional<? extends Content>> get() {
            final CompletableFuture<Optional<? extends Content>> res = new CompletableFuture<>();
            res.completeExceptionally(this.reason);
            return res;
        }
    }
}
