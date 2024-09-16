package com.wecca.canoeanalysis.services;

import ch.qos.logback.core.PropertyDefinerBase;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
