/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.events;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Start quarts service.
 * @since 1.17
 */
public final class QuartsService {

    /**
     * Quartz scheduler.
     */
    private final Scheduler scheduler;

    /**
     * Ctor.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public QuartsService() {
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            QuartsService.this.scheduler.shutdown();
                        } catch (final SchedulerException error) {
                            Logger.error(this, error.getMessage());
                        }
                    }
                }
            );
        } catch (final SchedulerException error) {
            throw new ArtipieException(error);
        }
    }

    /**
     * Adds event processor to the quarts job. The job is repeating forever every
     * given seconds.
     * @param queue Queue to get data from
     * @param consumer How to consume the data collection
     * @param seconds Seconds interval for scheduling
     * @param <T> Data item object type
     * @throws SchedulerException On error
     */
    public <T> void addPeriodicEventsProcessor(
        final EventQueue<T> queue, final Consumer<Collection<T>> consumer, final int seconds
    ) throws SchedulerException {
        final JobDataMap data = new JobDataMap();
        data.put("elements", queue);
        data.put("action", consumer);
        final String id = String.join(
            "-", EventsProcessor.class.getSimpleName(), UUID.randomUUID().toString()
        );
        this.scheduler.scheduleJob(
            JobBuilder.newJob(EventsProcessor.class)
                .withIdentity(String.join("-", "job", id))
                .setJobData(data).build(),
            TriggerBuilder.newTrigger()
                .withIdentity(String.join("-", "trigger", id))
                .startNow()
                .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(seconds))
                .build()
        );
    }

    /**
     * Start quartz.
     */
    public void start() {
        try {
            this.scheduler.start();
        } catch (final SchedulerException error) {
            throw new ArtipieException(error);
        }
    }

}
