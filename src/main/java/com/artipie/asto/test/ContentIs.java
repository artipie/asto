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
package com.artipie.asto.test;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import java.nio.charset.Charset;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for {@link Content}.
 * @since 0.24
 */
public final class ContentIs extends TypeSafeMatcher<Content> {

    /**
     * Byte array matcher.
     */
    private final Matcher<byte[]> matcher;

    /**
     * Content is a string with encoding.
     * @param expected String
     * @param enc Encoding charset
     */
    public ContentIs(final String expected, final Charset enc) {
        this(expected.getBytes(enc));
    }

    /**
     * Content is a byte array.
     * @param expected Byte array
     */
    public ContentIs(final byte[] expected) {
        this(Matchers.equalTo(expected));
    }

    /**
     * Content matches for byte array matcher.
     * @param matcher Byte array matcher
     */
    public ContentIs(final Matcher<byte[]> matcher) {
        this.matcher = matcher;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("has bytes ").appendValue(this.matcher);
    }

    @Override
    public boolean matchesSafely(final Content item) {
        final byte[] actual = new Concatenation(item).single()
            .map(buf -> new Remaining(buf).bytes()).blockingGet();
        return this.matcher.matches(actual);
    }
}
