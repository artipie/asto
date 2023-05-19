/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/asto/LICENSE.txt
 */
package com.artipie.asto.events;

import com.artipie.ArtipieException;
import com.jcabi.log.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Job to process events from queue.
 * Class type is used as quarts job type and is instantiated inside {@link org.quartz}, so
 * this class must have empty ctor. Events queue and action to consume the event are
 * obtained from job context.
 * @param <T> Elements type to process
 * @since 1.17
 */
public final class EventsProcessor<T> implements Job {

    /**
     * Pack size.
     */
    private static final int PACK_SIZE = 10;

    /**
     * Elements.
     */
    private EventQueue<T> elements;

    /**
     * Action to perform on element.
     */
    private Consumer<Collection<T>> action;

    @Override
    public void execute(final JobExecutionContext context) {
        this.setAction(context);
        this.setElements(context);
        if (this.action == null || this.elements == null) {
            final JobKey key = context.getJobDetail().getKey();
            try {
                Logger.error(
                    this,
                    String.format(
                        "Events queue or action is null, processing failed. Stopping job %s...", key
                    )
                );
                new StdSchedulerFactory().getScheduler().deleteJob(key);
                Logger.error(this, String.format("Job %s stopped.", key));
            } catch (final SchedulerException error) {
                Logger.error(this, String.format("Error while stopping job %s", key));
                throw new ArtipieException(error);
            }
        } else {
            while (!this.elements.queue().isEmpty()) {
                final Collection<T> list = new ArrayList<>(EventsProcessor.PACK_SIZE);
                for (int ind = 0; ind < EventsProcessor.PACK_SIZE; ind = ind + 1) {
                    final T item = this.elements.queue().poll();
                    if (item == null) {
                        break;
                    } else {
                        list.add(item);
                    }
                }
                this.action.accept(list);
            }
        }
    }

    /**
     * Set elements queue from job context.
     * @param context Job context
     */
    @SuppressWarnings("unchecked")
    public void setElements(final JobExecutionContext context) {
        if (this.elements == null) {
            final Object obj = context.getJobDetail().getJobDataMap().get("elements");
            if (obj instanceof EventQueue) {
                this.elements = (EventQueue<T>) obj;
            }
        }
    }

    /**
     * Set elements consumer from job context.
     * @param context Job context
     */
    @SuppressWarnings("unchecked")
    public void setAction(final JobExecutionContext context) {
        if (this.action == null) {
            final Object obj = context.getJobDetail().getJobDataMap().get("action");
            if (obj instanceof Consumer) {
                this.action = (Consumer<Collection<T>>) obj;
            }
        }
    }
}
