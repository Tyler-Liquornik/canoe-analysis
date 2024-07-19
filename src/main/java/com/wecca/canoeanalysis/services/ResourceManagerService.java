package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Objects;

/**
 * ColorManagerService, and potentially other future services require writing to resource files
 * When PADDL is deployed into a JAR file, this is not possible with resources from within the JAR
 * This service will detect if the program is being run from within a JAR
 * If that is the case, it will make use of an external the resource from within the .app or .exe
 * Copying of files into the external resource folder is handled by deploy-mac.sh or deploy-pc.sh
 * The file writing strategy is determined at runtime to be optimized for both a JAR and in an IDE
 */
public class ResourceManagerService {
    private static final Path resourcesDir;

    static {
        try {
            resourcesDir = isRunningFromJar() ? getBundledAppResourcesPath() : getMavenBuiltResourcesPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines the path of the resources folder when running from an JAR file.
     * @return the fully qualified path for PADDL.app/Contents/Resources
     */
    private static Path getBundledAppResourcesPath() throws URISyntaxException {
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
}
