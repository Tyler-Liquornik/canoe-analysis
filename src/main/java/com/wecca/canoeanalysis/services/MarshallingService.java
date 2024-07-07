package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.wecca.canoeanalysis.controllers.BeamController;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//TODO: completely rework with new hull model
public class MarshallingService {

    @Setter
    private static MainController mainController;
    @Setter
    private static BeamController beamController;
    private static final ObjectMapper yamlMapper;

    static {
        yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        yamlMapper.findAndRegisterModules();
    }

    /**
     * Prompts the user the download the current cano model to a YAML representation
     * @param canoe the canoe model to download to YAML
     * @param stage the stage to have the FileChooser model popup onto
     * @return the YAML file downloaded, or null if no file was downloaded
     */
    public static File exportCanoeToYAML(Canoe canoe, Stage stage){
        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Download Canoe");

        // Set extension filter for YAML files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("YAML (*.yaml)", "*.yaml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Set default file name
        fileChooser.setInitialFileName("canoe");

        // Show save file dialog
        File fileToDownload = fileChooser.showSaveDialog(stage);

        if (fileToDownload != null)
            try {
                yamlMapper.writeValue(fileToDownload, canoe);
            } catch (IOException e) {
                e.printStackTrace();
            }

        return fileToDownload;
    }

    /**
     * Prompts the user the upload a YAML file representing a new canoe model to upload
     * @param stage the stage to have the FileChooser model popup onto
     */
    public static void importCanoeFromYAML(Stage stage) {
        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Canoe");

        // Set extension filter for YAML files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("YAML files (*.yaml, *.yml)", "*.yaml", "*.yml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show open file dialog
        File fileToUpload = fileChooser.showOpenDialog(stage);

        if (fileToUpload != null)
        {
            try
            {
                Canoe canoe = combinePointLoads(validateCanoe(yamlMapper.readValue(fileToUpload, Canoe.class)));
                beamController.setCanoe(canoe);
                mainController.showSnackbar("Successfully uploaded Canoe Model");
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
                mainController.showSnackbar("Could not parse \"" + fileToUpload.getName() + "\".");
            }
        }
    }

    /**
     * Validate basic assumptions of the canoe model that would not be caught be the YAML parser
     * @param canoe the canoe which may be an invalid model
     * @return the same canoe if valid, or null if invalid
     */
    private static Canoe validateCanoe(Canoe canoe) {

        if (canoe.getHull().getLength() < 0.01)
        {
            mainController.showSnackbar("Length must be at least 0.01m");
            canoe = null;
        }

        for (Load load : canoe.getLoads())
        {
            if (Math.abs(load.getMaxSignedValue()) < 0.01)
            {
                mainController.showSnackbar("All load magnitudes must be at least 0.01kN");
                canoe = null;
            }

            if (load instanceof UniformLoadDistribution dLoad && dLoad.getX() >= dLoad.getRx())
            {
                mainController.showSnackbar("Right interval bound must be greater than the left bound");
                canoe = null;
            }

            if ((load instanceof PointLoad pLoad && !(0 <= pLoad.getX() && pLoad.getX() <= canoe.getHull().getLength())) ||
                    (load instanceof UniformLoadDistribution dLoad && !((0 <= dLoad.getX() && dLoad.getX() <= canoe.getHull().getLength()) ||
                                (0 <= dLoad.getRx() && dLoad.getRx() <= canoe.getHull().getLength()))))
            {
                mainController.showSnackbar("All loads must be contained within the canoe's length");
                canoe = null;
            }
        }

        return canoe;
    }

    /**
     * Automatically combines point loads at the same x
     * Will avoid solving issues with a YAML model with point loads at the same x, still allowing the user to upload it
     * @param canoe the canoe which may have point loads at the same x value
     * @return the canoe with point loads combined if any loads were any the same x
     */
    private static Canoe combinePointLoads(Canoe canoe)
    {
        double length = canoe.getHull().getLength();
        Hull hull = canoe.getHull();
        List<Load> newLoads = new ArrayList<>();
        newLoads.addAll(canoe.getAllLoads(PointLoad.class));
        newLoads.addAll(canoe.getAllLoads(PointLoad.class));

        Canoe newCanoe = new Canoe(); // hardcoded to 2024 numbers for now
        newCanoe.setHull(hull);
        for (Load load : newLoads) {newCanoe.addLoad(load);}

        return newCanoe;
    }
}
