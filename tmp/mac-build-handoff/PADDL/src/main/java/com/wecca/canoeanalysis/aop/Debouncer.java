package com.wecca.canoeanalysis.aop;

import javafx.application.Platform;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Debouncer {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledTask;
    private static Object lastConsumerArg;

    /**
     * Debounces a Consumer task.
     * If a previous task is pending, it cancels it.
     * Only the most recent argument is processed.
     * @param consumer The Consumer to execute.
     * @param arg The argument for the Consumer.
     * @param delayMs Delay in milliseconds before executing the task.
     * @param <T> The type of the argument.
     */
    @SuppressWarnings("unchecked")
    public static <T> void debounceConsumer(Consumer<T> consumer, T arg, int delayMs) {
        lastConsumerArg = arg;

        // Cancel the scheduled task if it has not yet started running, otherwise let the task finish (i.e. don't interrupt it)
        if (scheduledTask != null && !scheduledTask.isDone())
            scheduledTask.cancel(false);

        // Schedule the new task
        scheduledTask = scheduler.schedule(() ->
                Platform.runLater(() -> consumer.accept((T) lastConsumerArg)),
                delayMs, TimeUnit.MILLISECONDS);
    }
}
