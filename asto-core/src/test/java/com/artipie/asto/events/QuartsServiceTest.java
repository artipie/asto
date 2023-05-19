/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.events;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

/**
 * Test for {@link QuartsService}.
 * @since 1.17
 * @checkstyle IllegalTokenCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class QuartsServiceTest {

    /**
     * Quartz service to test.
     */
    private QuartsService service;

    @BeforeEach
    void init() {
        this.service = new QuartsService();
    }

    @Test
    void runsQuartsJobs() throws SchedulerException, InterruptedException {
        final EventQueue<Character> queue = new EventQueue<>();
        final TestConsumer consumer = new TestConsumer();
        this.service.addPeriodicEventsProcessor(queue, consumer, 1);
        this.service.start();
        for (char sym = 'a'; sym <= 'z'; sym++) {
            queue.put(sym);
            if ((int) sym % 5 == 0) {
                Thread.sleep(1500);
            }
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> consumer.cnt.get() == 26);
    }

    @Test
    void stopsJobIfDataAreNotPresent() throws SchedulerException {
        this.service.addPeriodicEventsProcessor(null, new TestConsumer(), 1);
        this.service.start();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> new StdSchedulerFactory().getScheduler().getCurrentlyExecutingJobs().isEmpty()
        );
    }

    /**
     * Test consumer.
     * @since 1.17
     */
    static final class TestConsumer implements Consumer<Collection<Character>> {

        /**
         * Count for accept method call.
         */
        private final AtomicInteger cnt = new AtomicInteger();

        @Override
        public void accept(final Collection<Character> strings) {
            strings.forEach(ignored -> this.cnt.incrementAndGet());
        }
    }

}
