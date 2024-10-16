package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.controllers.modules.BeamController;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.data.DevConfig;
import com.wecca.canoeanalysis.models.data.Settings;
import com.wecca.canoeanalysis.models.load.Load;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Marshalling and unmarshalling of POJO models in YAML for state management
 * This includes writing YAML to and from files
 */
@Traceable
public class YamlMarshallingService {

    @Setter
    private static MainController mainController;
    @Setter
    private static BeamController beamController;
    private static final ObjectMapper yamlMapper;
    public static final String SETTINGS_FILE_PATH = ResourceManagerService.getResourceFilePathString("settings/settings.yaml", true);
    public static final String DEV_CONFIG_FILE_PATH = ResourceManagerService.getResourceFilePathString("settings/dev-config.yaml", true);
    public static final boolean TRACING;

    // Initializations, and loading state
    static {
        yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        yamlMapper.findAndRegisterModules();

        try {
            TRACING = YamlMarshallingService
                    .loadYamlData(DevConfig.class, new DevConfig(false),
                            YamlMarshallingService.DEV_CONFIG_FILE_PATH).isTracing();
        } catch (IOException e) {throw new RuntimeException(e);}
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
                for (Load load : canoe.getLoads())
                    adjustedCanoe.addLoad(load);
                beamController.setCanoe(adjustedCanoe);
                mainController.showSnackbar("Successfully uploaded " + fileToUpload.getName());
            } catch (IOException ex) {
                mainController.showSnackbar("Could not parse \"" + fileToUpload.getName() + "\".");
            }
        }
    }

    /**
     * Write the current state of settings to a file
     * @param settings the POJO containing all settings information
     */
    public static void saveSettings(Settings settings) throws IOException {
        yamlMapper.writeValue(new File(SETTINGS_FILE_PATH), settings);
    }

    /**
     *
     * @param dataClass the POJO representing the structure of the data to load
     * @param defaultData the data to upload if the file does not exist
     * @param path to the YAML file to demarshall
     * @return the demarshalled YAML file as a POJO
     */
    public static <T> T loadYamlData(Class<T> dataClass, T defaultData, String path) throws IOException {
        File file = new File(path);
        if (file.exists())
            return yamlMapper.readValue(file, dataClass);
        return defaultData;
    }
}
