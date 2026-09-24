package com.wecca.canoeanalysis.aop;

import javafx.application.Platform;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Debouncer {
    /**
     * Run delayed work on a daemon thread so this helper can never keep PADDL
     * alive after the last JavaFX window closes.
     */
    private static final ThreadFactory DAEMON_THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "paddl-debouncer");
        thread.setDaemon(true);
        return thread;
    };
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(DAEMON_THREAD_FACTORY);
    private static ScheduledFuture<?> scheduledTask;

    private Debouncer() {
    }

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
    public static synchronized <T> void debounceConsumer(Consumer<T> consumer, T arg, int delayMs) {
        // Cancel the scheduled task if it has not yet started running, otherwise let the task finish (i.e. don't interrupt it)
        if (scheduledTask != null && !scheduledTask.isDone())
            scheduledTask.cancel(false);

        // Capture this call's argument directly so a later call cannot replace
        // the value between the scheduler firing and JavaFX handling the task.
        scheduledTask = scheduler.schedule(() ->
                Platform.runLater(() -> consumer.accept(arg)),
                delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancel pending work and release the scheduler during application exit.
     * This is safe to call repeatedly from JavaFX's lifecycle callback.
     */
    public static synchronized void shutdown() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
        scheduler.shutdownNow();
    }
}
