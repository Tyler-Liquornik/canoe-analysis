package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.Canoe;
import com.wecca.canoeanalysis.models.Load;
import com.wecca.canoeanalysis.models.PointLoad;
import com.wecca.canoeanalysis.models.UniformDistributedLoad;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSerializationService {

    @Setter
    private static MainController mainController;
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    public static File exportCanoeToYAML(Canoe canoe, Stage stage) throws IOException {
        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Download Canoe");

        // Set extension filter for YAML files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("YAML files (*.yaml)", "*.yaml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Set default file name
        fileChooser.setInitialFileName("canoe");

        // Show save file dialog
        File fileToDownload = fileChooser.showSaveDialog(stage);

        if (fileToDownload != null)
            yamlMapper.writeValue(fileToDownload, canoe);

        return fileToDownload;
    }

    public static Canoe importCanoeFromYAML(Stage stage)
    {
        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Canoe");

        // Set extension filter for YAML files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("YAML files (*.yaml)", "*.yaml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show open file dialog
        File fileToUpload = fileChooser.showOpenDialog(stage);

        if (fileToUpload != null)
        {
            try
            {
                Canoe canoe = yamlMapper.readValue(fileToUpload, Canoe.class);
                Canoe combinedPointLoadsCanoe = combinePointLoads(canoe);

                if (canoe.equals(combinedPointLoadsCanoe))
                {
                    // Notify the user some loads have combined or cancelled
                }

                return validateValidCanoe(yamlMapper.readValue(fileToUpload, Canoe.class));
            }
            catch (IOException ex)
            {
                mainController.showSnackbar("Could not parse \"" + fileToUpload.getName() + "\"." );
                return null;
            }
        }
        else
            return null;
    }

    private static Canoe validateValidCanoe(Canoe canoe) {

        if (canoe.getLength() < 0.01)
        {
            mainController.showSnackbar("Length must be at least 0.01m");
            canoe = null;
        }

        for (Load load : canoe.getLoads())
        {
            if (load.getMag() < 0.01)
            {
                mainController.showSnackbar("All load magnitudes must be at least 0.01kN");
                canoe = null;
            }

            if (load instanceof UniformDistributedLoad dLoad && dLoad.getX() >= dLoad.getRx())
            {
                mainController.showSnackbar("Right interval bound must be greater than the left bound");
            }

            if ((load instanceof PointLoad pLoad && !(0 <= pLoad.getX() && pLoad.getX() <= canoe.getLength())) ||
                    (load instanceof UniformDistributedLoad dLoad && !((0 <= dLoad.getX() && dLoad.getX() <= canoe.getLength()) ||
                                (0 <= dLoad.getRx() && dLoad.getRx() <= canoe.getLength()))))
            {
                mainController.showSnackbar("All loads must be contained within the canoe's length");
                canoe = null;
            }
        }

        return canoe;
    }

    private static Canoe combinePointLoads(Canoe canoe) {
        List<PointLoad> pLoads = canoe.getPLoads();
        List<UniformDistributedLoad> dLoads = canoe.getDLoads(); // Store distributed loads before clearing

        // Use a map to combine PointLoad objects by their x position
        Map<Double, PointLoad> combinedPLoads = new HashMap<>();

        for (PointLoad pLoad : pLoads) {
            double x = pLoad.getX();
            if (combinedPLoads.containsKey(x)) {
                // Combine magnitudes if x position already exists
                PointLoad existingPLoad = combinedPLoads.get(x);
                existingPLoad.setMag(existingPLoad.getMag() + pLoad.getMag());
            } else {
                // Add new PointLoad to the map
                combinedPLoads.put(x, new PointLoad(pLoad.getMag(), x, pLoad.isSupport()));
            }
        }

        // Filter out PointLoads with zero magnitude
        List<PointLoad> combinedPLoadList = combinedPLoads.values().stream()
                .filter(p -> Double.compare(p.getMag(), 0.0) != 0)
                .toList();

        // Clear current loads in the canoe
        canoe.clearLoads();

        // Add back combined PointLoads
        for (PointLoad combinedPLoad : combinedPLoadList) {
            canoe.addLoad(combinedPLoad);
        }

        // Add back existing DistributedLoads
        for (UniformDistributedLoad dLoad : dLoads) {
            canoe.addLoad(dLoad);
        }

        return canoe;
    }
}
