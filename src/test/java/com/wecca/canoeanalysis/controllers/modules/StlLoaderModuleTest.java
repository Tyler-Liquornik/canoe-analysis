package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Navigation wiring checks for the intentionally blank STL Loader module. */
class StlLoaderModuleTest {

    @Test
    void moduleSelectorPointsToTheStlLoaderView() {
        ModuleSelectorController.Module module = ModuleSelectorController.Module.STL_LOADER;

        assertEquals("stl-loader-view", module.getViewName());
        assertNotNull(CanoeAnalysisApplication.class.getResource(
                "view/" + module.getViewName() + ".fxml"));
    }
}
