package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.wecca.canoeanalysis.controllers.BeamController;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.data.Settings;
import com.wecca.canoeanalysis.models.load.Load;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class YamlMarshallingService {

    @Setter
    private static MainController mainController;
    @Setter
    private static BeamController beamController;
    private static final ObjectMapper yamlMapper;
    private static final String SETTINGS_FILE_PATH = "settings.yaml";

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

        if (fileToDownload != null) {
            try {
                // Serialize the object to a YAML string
                String yamlString = yamlMapper.writeValueAsString(canoe);

                // Add a comment to the top of the YAML string
                String comment = "# Please do not manually modify the contents of this file before uploading, it may result in unexpected results\n";
                yamlString = comment + yamlString;

                // Write the YAML string to a file
                try (FileWriter writer = new FileWriter(fileToDownload)) {
                    writer.write(yamlString);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        if (fileToUpload != null) {
            try {
                Canoe canoe = yamlMapper.readValue(fileToUpload, Canoe.class);

                // Rebuild the canoe trigger validations and data restructuring
                Canoe adjustedCanoe = new Canoe();
                adjustedCanoe.setHull(canoe.getHull());
                boolean disableHullBuilder = !adjustedCanoe.getHull().equals(
                        SharkBaitHullLibrary.generateDefaultHull(canoe.getHull().getLength()));
                mainController.disableModuleToolBarButton(disableHullBuilder, 2);
                for (Load load : canoe.getLoads())
                    adjustedCanoe.addLoad(load);
                beamController.setCanoe(adjustedCanoe);
                mainController.showSnackbar("Successfully uploaded Canoe Model");
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
                mainController.showSnackbar("Could not parse \"" + fileToUpload.getName() + "\".");
            }
        }
    }

    public static void saveSettings(Settings settings) throws IOException {
        yamlMapper.writeValue(new File(SETTINGS_FILE_PATH), settings);
    }

    public static Settings loadSettings() throws IOException {
        File file = new File(SETTINGS_FILE_PATH);
        if (file.exists()) {
            return yamlMapper.readValue(file, Settings.class);
        }
        return new Settings("#F96C37"); // The default orange color
    }
}
