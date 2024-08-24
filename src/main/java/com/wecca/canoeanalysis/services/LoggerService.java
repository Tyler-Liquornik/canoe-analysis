package com.wecca.canoeanalysis.services;

import ch.qos.logback.core.PropertyDefinerBase;
import com.wecca.canoeanalysis.models.function.Section;
import javafx.geometry.Point2D;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

// TODO: ensure this all works on macOS when I get access to a Macbook

/**
 * Production logs to debug.txt need to go through SLF4J + Logback to work properly
 * This service ensures all System logs are redirected through SLF4J + Logback
 */
@Slf4j
public class LoggerService {

    /**
     * Redirects System.err to SLF4J's log.error() and System.out to SLF4J's log.info().
     * Note: We lose printing on the same line because SLF4J logs messages one at a time.
     * Thus print and println basically become the same thing
     */
    public static void redirectSystemStreamsToLogger() {
        // Redirect System.err to log.error()
        PrintStream errStream = new PrintStream(System.err) {
            @Override
            public void println(String s) {
                log.error(s);
            }

            @Override
            public void print(String s) {
                log.error(s);
            }

            @Override
            public void println(Object obj) {
                if (obj instanceof Throwable) {
                    log.error("Exception occurred: ", (Throwable) obj);
                } else {
                    log.error(obj.toString());
                }
            }

            @Override
            public PrintStream printf(@NonNull String format, Object... args) {
                log.error(String.format(format, args));
                return this;
            }
        };
        System.setErr(errStream);

        // Redirect System.out to log.info()
        PrintStream outStream = new PrintStream(System.out) {
            @Override
            public void println(String s) {
                log.info(s);
            }

            @Override
            public void print(String s) {
                log.info(s);
            }

            @Override
            public void println(Object obj) {
                log.info(obj.toString());
            }

            @Override
            public PrintStream printf(@NonNull String format, Object... args) {
                log.info(String.format(format, args));
                return this;
            }
        };
        System.setOut(outStream);
    }

    /**
     * Creates a parent folder for debug.txt if it doesn't already exist
     */
    public static void createDebugFileParentFolder() {
        if (ResourceManagerService.isRunningFromJar()) {
            // Resolve the log directory inside the external resources folder
            Path logDir = ResourceManagerService.getResourcePathResolved("logs");
            // Ensure the log directory exists, or create it
            if (!Files.exists(logDir)) {
                try {
                    Files.createDirectories(logDir);
                } catch (IOException e) {
                    throw new RuntimeException("Could not create log directory", e);
                }
            }
        }
    }

    /**
     * Define the logPath property dynamically based on the environment.
     * Used by logback.xml in <define />
     */
    public static class LogPathPropertyDefiner extends PropertyDefinerBase {
        @Override
        public String getPropertyValue() {
            if (ResourceManagerService.isRunningFromJar()) {
                Path logDir = ResourceManagerService.getResourcePathResolved("logs");
                return logDir.resolve("debug.txt").toString();
            }
            else
                return null;
        }
    }

    /**
     * Log points to get a block of text that looks like:
     * 01:44:25.557 [INFO] x: 0.0, y: 0.6160168685657584
     * 01:44:25.557 [INFO] x: 0.005, y: 0.6102826148344151
     * 01:44:25.557 [INFO] x: 0.01, y: 0.6046022417000867
     * 01:44:25.557 [INFO] x: 0.015, y: 0.5989757491627734
     * ... and so on for hundreds (or thousands...) of lines
     *
     * Then paste the whole block on text into log_data in
     * util/matplotlib/scatter-plot.py and run that script to make a plot to visualize the function
     *
     * Should not be in production code, for debugging use
     *
     * @param function the function to log points for
     * @param section the section on which to log points for the function
     */
    public static void logPoints(Function<Double, Double> function, Section section, int numSamples) {
        // Sample the function over [start, end] and print x and y values
        double x = section.getX();
        double rx = section.getRx();
        System.out.println("Sampling the function over the domain [" + x + ", " + rx + "]:");
        double step = (rx - x) / numSamples;
        for (int i = 0; i <= numSamples; i++) {
            double xValue = x + i * step;
            double yValue = function.apply(xValue);
            System.out.println("x: " + xValue + ", y: " + yValue);
        }
    }

    /**
     * See other overload of logPoints, same idea
     * @param points to log
     */
    public static void logPoints(List<Point2D> points) {
        System.out.println("Logging 2D points:");
        for (Point2D point : points) {
            double xValue = point.getX();
            double yValue = point.getY();
            System.out.println("x: " + xValue + ", y: " + yValue);
        }
    }
}
