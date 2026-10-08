package com.wecca.canoeanalysis.aop;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for the background thread that previously kept PADDL alive. */
class DebouncerTest {

    @Test
    void schedulerCannotKeepTheApplicationProcessAlive() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            // A long delay starts the scheduler thread without reaching
            // Platform.runLater, which requires a running JavaFX toolkit.
            Debouncer.debounceConsumer(ignored -> { }, "test", 60_000);

            Thread schedulerThread = waitForSchedulerThread();
            assertNotNull(schedulerThread);
            assertTrue(schedulerThread.isDaemon(),
                    "The debounce scheduler must not keep PADDL alive after its windows close");

            Debouncer.shutdown();
            schedulerThread.join(1_000);
            assertFalse(schedulerThread.isAlive(),
                    "Application shutdown must terminate the debounce scheduler");
        });
    }

    private static Thread waitForSchedulerThread() throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            Thread matchingThread = Thread.getAllStackTraces().keySet().stream()
                    .filter(thread -> thread.getName().equals("paddl-debouncer"))
                    .findFirst()
                    .orElse(null);
            if (matchingThread != null) {
                return matchingThread;
            }
            Thread.sleep(25);
        }
        return null;
    }
}
