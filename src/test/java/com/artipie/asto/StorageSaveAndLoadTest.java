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

import com.artipie.asto.blocking.BlockingStorage;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link Storage#save(Key, Content)} and {@link Storage#value(Key)}.
 *
 * @since 0.14
 */
@ExtendWith(StorageExtension.class)
public final class StorageSaveAndLoadTest {

    @TestTemplate
    @Timeout(1)
    void shouldSave(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] data = "0".getBytes();
        final Key key = new Key.From("shouldSave");
        blocking.save(key, data);
        MatcherAssert.assertThat(
            blocking.value(key),
            Matchers.equalTo(data)
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldSaveFromMultipleBuffers(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldSaveFromMultipleBuffers");
        storage.save(
            key,
            new Content.OneTime(
                new Content.From(
                    Flowable.fromArray(
                        ByteBuffer.wrap("12".getBytes()),
                        ByteBuffer.wrap("34".getBytes()),
                        ByteBuffer.wrap("5".getBytes())
                    )
                )
            )
        ).get();
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            Matchers.equalTo("12345".getBytes())
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldSaveEmpty(final Storage storage) throws Exception {
        final Key key = new Key.From("shouldSaveEmpty");
        storage.save(key, new Content.OneTime(new Content.From(Flowable.empty()))).get();
        MatcherAssert.assertThat(
            "Saved content should be empty",
            new BlockingStorage(storage).value(key),
            Matchers.equalTo(new byte[0])
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldSaveWhenValueAlreadyExists(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final byte[] original = "1".getBytes();
        final byte[] updated = "2".getBytes();
        final Key key = new Key.From("shouldSaveWhenValueAlreadyExists");
        blocking.save(key, original);
        blocking.save(key, updated);
        MatcherAssert.assertThat(
            blocking.value(key),
            Matchers.equalTo(updated)
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldFailToSaveErrorContent(final Storage storage) throws Exception {
        Assertions.assertThrows(
            Exception.class,
            () -> storage.save(
                new Key.From("shouldFailToSaveErrorContent"),
                new Content.OneTime(new Content.From(Flowable.error(new IllegalStateException())))
            ).join()
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldFailOnSecondConsumeAttempt(final Storage storage) {
        final Key.From key = new Key.From("key");
        storage.save(
            key,
            new Content.OneTime(new Content.From("val".getBytes()))
        ).join();
        final Content value = storage.value(key).join();
        Flowable.fromPublisher(value).toList().blockingGet();
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> Flowable.fromPublisher(value).toList().blockingGet()
        );
    }

    @TestTemplate
    @Timeout(1)
    void shouldFailToLoadAbsentValue(final Storage storage) throws Exception {
        final BlockingStorage blocking = new BlockingStorage(storage);
        final Key key = new Key.From("shouldFailToLoadAbsentValue");
        Assertions.assertThrows(RuntimeException.class, () -> blocking.value(key));
    }

    @TestTemplate
    @Timeout(1)
    void shouldNotSavePartial(final Storage storage) {
        final Key key = new Key.From("shouldNotSavePartial");
        storage.save(
            key,
            new Content.From(
                Flowable.concat(
                    Flowable.just(ByteBuffer.wrap(new byte[] {1})),
                    Flowable.error(new IllegalStateException())
                )
            )
        ).exceptionally(ignored -> null).join();
        MatcherAssert.assertThat(
            storage.exists(key).join(),
            Matchers.equalTo(false)
        );
    }

    @TestTemplate
    void shouldReturnContentWithSpecifiedSize(final Storage storage) throws Exception {
        final byte[] content = "1234".getBytes();
        final Key key = new Key.From("shouldReturnContentWithSpecifiedSize");
        storage.save(
            key,
            new Content.OneTime(new Content.From(content))
        ).get();
        MatcherAssert.assertThat(
            storage.value(key).get().size().get(),
            new IsEqual<>((long) content.length)
        );
    }
}
