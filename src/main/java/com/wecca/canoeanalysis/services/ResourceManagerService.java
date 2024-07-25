package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Objects;

/**
 * ResourceManagerService is responsible for managing resource files.
 * It detects whether the application is running from a JAR or in an IDE
 * and sets the resource paths accordingly. It also differentiates between
 * macOS and Windows resource locations.
 */
public class ResourceManagerService {
    private static final Path resourcesDir;

    static {
        try {
            if (isRunningFromJar()) {
                if (isMac())
                    resourcesDir = getBundledAppResourcesPathMac();
                else if (isWindows())
                    resourcesDir = getBundledAppResourcesPathWindows();
                else
                    throw new UnsupportedOperationException("Unsupported operating system");
            }
            else
                resourcesDir = getMavenBuiltResourcesPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines the path of the resources folder when running from a JAR file on macOS.
     * @return the fully qualified path for PADDL.app/Contents/Resources
     */
    private static Path getBundledAppResourcesPathMac() throws URISyntaxException {
        return Paths.get(new File(CanoeAnalysisApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI())
                .getParentFile()
                .getParentFile()
                .getParent(), "Contents", "Resources");
    }

    /**
     * Determines the path of the resources folder when running from a JAR file on Windows.
     * @return the fully qualified path for the resources directory
     */
    private static Path getBundledAppResourcesPathWindows() throws URISyntaxException {
        return Paths.get(new File(CanoeAnalysisApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI())
                .getParentFile()
                .getParent(), "resources");
    }

    /**
     * Determines the fully qualified path where resource folders are located in the Maven's 'target' folder
     * @return the fully qualified path for target/classes/com/wecca/canoeanalysis
     */
    private static Path getMavenBuiltResourcesPath() throws URISyntaxException {
        URI driverUri = Objects.requireNonNull(CanoeAnalysisApplication.class
                .getResource(CanoeAnalysisApplication.class.getSimpleName() + ".class")).toURI();
        return Paths.get(driverUri).getParent();
    }

    /**
     * Returns the fully qualified path of the specified resource file.
     * @param resourcePath the path to the resource file from within the resources directory (part AFTER resources/...)
     * @param asAbsolutePath treats the path as absolute by adding a '/' and removing the prefix "file:"
     * @return the fully qualified path as a string
     */
    public static String getResourceFilePathString(String resourcePath, boolean asAbsolutePath) {
        Path stylesheetPath = resourcesDir.resolve(resourcePath);
        String pathString = stylesheetPath.toUri().toString();
        return convertFileUrl(pathString, asAbsolutePath);
    }

    /**
     * @param resourcePath the path to the resource within the resources directory
     * @return the resolved Path object for the specified resource.
     */
    public static Path getResourcePathResolved(String resourcePath) {
        return Paths.get(resourcesDir.resolve(resourcePath).toUri());
    }

    /**
     * @param url the file URL to convert
     * @param asAbsolutePath treats the path as absolute by adding a '/' and removing the prefix "file:"
     * @return the converted file URL to the correct format.
     */
    private static String convertFileUrl(String url, boolean asAbsolutePath) {
        String prefix = asAbsolutePath ? "/" : "";
        String replacement = asAbsolutePath ? "" : "file:/";
        return url.startsWith("file:///") ? prefix + url.replaceFirst("file:///", replacement) : prefix + url;
    }

    /**
     * Checks the protocol of a URL within the project (entry point class by convention)
     * This can be either 'file:' or 'jar:' depending on the runtime environment
     * @return true if the application is running from a JAR file, false otherwise
     */
    private static boolean isRunningFromJar() {
        String classJar = Objects.requireNonNull(CanoeAnalysisApplication.class.getResource
                (CanoeAnalysisApplication.class.getSimpleName() + ".class")).toString();
        return classJar.startsWith("jar:");
    }

    /**
     * Checks if the operating system is macOS.
     * @return true if the operating system is macOS, false otherwise
     */
    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    /**
     * Checks if the operating system is Windows.
     * @return true if the operating system is Windows, false otherwise
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
