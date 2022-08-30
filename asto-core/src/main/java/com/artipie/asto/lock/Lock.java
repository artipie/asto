/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.lock;

import java.util.concurrent.CompletionStage;

/**
 * Asynchronous lock that might be successfully obtained by one thread only at a time.
 *
 * @since 0.24
 */
public interface Lock {

    /**
     * Acquire the lock.
     *
     * @return Completion of lock acquire operation.
     */
    CompletionStage<Void> acquire();

    /**
     * Release the lock.
     *
     * @return Completion of lock release operation.
     */
    CompletionStage<Void> release();
}
