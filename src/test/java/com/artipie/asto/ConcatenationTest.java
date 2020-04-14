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

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link Concatenation}.
 *
 * @since 0.17
 * @checkstyle ArrayTrailingCommaCheck (500 lines)
 */
final class ConcatenationTest {

    @ParameterizedTest
    @MethodSource("flows")
    void shouldReadBytes(final Publisher<ByteBuffer> publisher, final byte[] bytes) {
        final Content content = new Content.From(publisher);
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(content).single().blockingGet(),
                true
            ).bytes(),
            new IsEqual<>(bytes)
        );
    }

    @ParameterizedTest
    @MethodSource("flows")
    void shouldReadBytesTwice(final Publisher<ByteBuffer> publisher, final byte[] bytes) {
        final Content content = new Content.From(publisher);
        final byte[] first = new Remaining(
            new Concatenation(content).single().blockingGet(),
            true
        ).bytes();
        final byte[] second = new Remaining(
            new Concatenation(content).single().blockingGet(),
            true
        ).bytes();
        MatcherAssert.assertThat(
            second,
            new IsEqual<>(first)
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Object[]> flows() {
        final String data = "data";
        return Stream.of(
            new Object[] {Flowable.empty(), new byte[0]},
            new Object[] {Flowable.just(ByteBuffer.wrap(data.getBytes())), data.getBytes()},
            new Object[] {
                Flowable.just(
                    ByteBuffer.wrap("he".getBytes()),
                    ByteBuffer.wrap("ll".getBytes()),
                    ByteBuffer.wrap("o".getBytes())
                ),
                "hello".getBytes()
            }
        );
    }
}
