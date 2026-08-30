package com.wecca.canoeanalysis;

/** JavaFX Workaround required to package the project into a JAR
 * Issues were arising when the "true" entry point extended javafx.application.Application
 */
public class Main {
    public static void main(final String[] args) {
        CanoeAnalysisApplication.main(args);
    }
}
