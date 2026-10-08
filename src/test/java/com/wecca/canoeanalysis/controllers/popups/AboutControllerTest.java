package com.wecca.canoeanalysis.controllers.popups;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the public profile links displayed by the About window. */
class AboutControllerTest {

    @Test
    void profileLinksMatchTheRequestedPublicProfiles() {
        assertEquals(
                "https://www.linkedin.com/in/tyler-liquornik/",
                AboutController.TYLER_LINKEDIN_URL);
        assertEquals(
                "https://www.linkedin.com/in/perry-lycett-8b9272366/",
                AboutController.PERRY_LINKEDIN_URL);
    }

    @Test
    void aboutViewCreditsPerryForTheFailureEnvelope() throws IOException {
        URL view = CanoeAnalysisApplication.class.getResource("view/about-view.fxml");
        assertNotNull(view);

        try (InputStream stream = view.openStream()) {
            String fxml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(fxml.contains("He added PADDL's Failure Envelope module during the 2025 season."));
        }
    }
}
