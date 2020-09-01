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
import com.artipie.asto.ext.ContentDigest;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * By digest verification.
 * @since 0.25
 */
public final class DigestVerification implements CacheControl {

    /**
     * Message digest.
     */
    private final Supplier<MessageDigest> digest;

    /**
     * Expected digest.
     */
    private final byte[] expected;

    /**
     * New digest verification.
     * @param digest Message digest has func
     * @param expected Expected digest bytes
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public DigestVerification(final Supplier<MessageDigest> digest, final byte[] expected) {
        this.digest = digest;
        this.expected = expected;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key item, final AsyncContent content) {
        return content.get().thenCompose(pub -> new ContentDigest(pub, this.digest).bytes())
            .thenApply(actual -> Arrays.equals(this.expected, actual));
    }
}
