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
import com.artipie.asto.Key;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Generic reactive cache which returns cached content by key of exist or loads from remote and
 * cache if doesn't exit.
 *
 * @since 0.24
 */
public interface Cache {

    /**
     * No cache, just load remote resource.
     */
    Cache NOP = (key, remote, ctl) -> remote.get();

    /**
     * Try to load content from cache or fallback to remote publisher if cached key doesn't exist.
     * When loading remote item, the cache may save its content to the cache storage.
     * @param key Cached item key
     * @param remote Remote source
     * @param control Cache control
     * @return Content for key
     */
    CompletionStage<Optional<? extends Content>> load(
        Key key, Remote remote, CacheControl control
    );
}
