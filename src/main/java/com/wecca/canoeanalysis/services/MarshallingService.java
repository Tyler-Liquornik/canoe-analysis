package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.controllers.modules.BeamController;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.PunchingShearController;
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
import java.util.List;
import java.util.function.Consumer;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

/**
 * Marshalling and unmarshalling of POJO models
 * Only dealing with YAML and file I/O so far
 */
@Traceable
public class MarshallingService {

    @Setter
    private static MainController mainController;
    @Setter
    private static BeamController beamController;
    @Setter
    private static PunchingShearController punchingShearController;
    private static final ObjectMapper yamlMapper;
    private static final ObjectMapper smileMapper;
    public static final String SETTINGS_FILE_PATH = ResourceManagerService.getResourceFilePathString("settings/settings.yaml", true);
    public static final String DEV_CONFIG_FILE_PATH = ResourceManagerService.getResourceFilePathString("settings/dev-config.yaml", true);
    public static final boolean TRACING;

    // Initializations, and loading state
    static {
        yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        yamlMapper.findAndRegisterModules();
        smileMapper = new ObjectMapper(new SmileFactory());

        try {
            TRACING = MarshallingService
                    .loadYamlData(DevConfig.class, new DevConfig(false),
                            MarshallingService.DEV_CONFIG_FILE_PATH).isTracing();
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

                for (int i = 0; i < canoe.getLoads().size(); i++) {
                    if (canoe.getLoads().get(i).getType().name().equals("BUOYANCY")) {
                        canoe.getLoads().remove(i);
                        i--;  // Adjust index since the list has shrunk
                    }
                }

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
     * Prompts the user to upload a YAML file representing a new canoe model and processes it.
     * The uploaded canoe is used for general beam control operations.
     * @param stage the stage to display the FileChooser dialog
     */
    public static void importCanoeFromYAML(Stage stage) {
        uploadAndProcessCanoe(stage, adjustedCanoe -> {
            // Custom processing for the general canoe import
            adjustedCanoe.setHull(adjustedCanoe.getHull());
            for (Load load : adjustedCanoe.getLoads())
                adjustedCanoe.addLoad(load);
            beamController.setCanoe(adjustedCanoe);
        },false);
    }

    /**
     * Prompts the user to upload a YAML file representing a canoe model specifically for
     * punching shear operations and processes it.
     * @param stage the stage to display the FileChooser dialog
     */
    public static void punchingShearImportCanoeFromYAML(Stage stage) {
        uploadAndProcessCanoe(stage, adjustedCanoe -> punchingShearController.setValues(adjustedCanoe),true);
    }

    /**
     * Handles the common logic for uploading and processing one to many canoe YAML file(s).
     * Allows custom processing logic to be applied to the parsed and adjusted Canoe object.
     * @param stage the stage to display the FileChooser dialog
     * @param canoeProcessor a Consumer function to process the adjusted Canoe object
     */
    private static void uploadAndProcessCanoe(Stage stage, Consumer<Canoe> canoeProcessor, boolean allowMultipleFiles) {
        // Create a file chooser
        FileChooser fileChooser = new FileChooser();

        // Set extension filter for YAML files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("YAML files (*.yaml, *.yml)", "*.yaml", "*.yml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show open file dialog
        if (!allowMultipleFiles) {
            fileChooser.setTitle("Upload Canoe");
            File fileToUpload = fileChooser.showOpenDialog(stage);
            if (fileToUpload != null) {
                try {
                    Canoe canoe = yamlMapper.readValue(fileToUpload, Canoe.class);

                    // Rebuild the canoe to trigger validations and data restructuring
                    Canoe adjustedCanoe = new Canoe();
                    if(canoe.getSessionMaxShear() > 0) adjustedCanoe.setSessionMaxShear(canoe.getSessionMaxShear());
                    adjustedCanoe.setSolveType(canoe.getSolveType());
                    adjustedCanoe.setHull(canoe.getHull());
                    for (Load load : canoe.getLoads()) {
                        adjustedCanoe.addLoad(load);
                    }

                    // Apply the custom processing logic
                    canoeProcessor.accept(adjustedCanoe);

                    mainController.showSnackbar("Successfully uploaded " + fileToUpload.getName());
                } catch (IOException ex) {
                    mainController.showSnackbar("Could not parse \"" + fileToUpload.getName() + "\".");
                }
            }
        }
        else {
            fileChooser.setTitle("Upload one or more Canoes");
            List<File> filesToUpload = fileChooser.showOpenMultipleDialog(stage);
            if (filesToUpload != null) {
                try {
                    double maxSessionShear = Double.MIN_VALUE;
                    String fileName = null;
                    for(File fileToUpload : filesToUpload) {

                        Canoe canoe = yamlMapper.readValue(fileToUpload, Canoe.class);

                        // Rebuild the canoe to trigger validations and data restructuring
                        Canoe adjustedCanoe = new Canoe();
                        if (canoe.getSessionMaxShear() > 0) adjustedCanoe.setSessionMaxShear(canoe.getSessionMaxShear());
                        adjustedCanoe.setSolveType(canoe.getSolveType());
                        adjustedCanoe.setHull(canoe.getHull());
                        for (Load load : canoe.getLoads()) {
                            adjustedCanoe.addLoad(load);
                        }

                        // Apply the custom processing logic
                        if (adjustedCanoe.getSessionMaxShear() > maxSessionShear) {
                            maxSessionShear = adjustedCanoe.getSessionMaxShear();
                            fileName = fileToUpload.getName();

                            canoeProcessor.accept(adjustedCanoe);
                        }
                    }
                    mainController.showSnackbar(String.format("Success. File with name \"%s\" has max absolute shear of %.2f N.", fileName, maxSessionShear * 1000));
                } catch (IOException ex) {
                    mainController.showSnackbar("Could not parse files.");
                }
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
        try {
            File file = new File(path);
            if (file.exists())
                return yamlMapper.readValue(file, dataClass);
        }
        catch (Exception ignored) {}
        return defaultData;
    }

    /**
     * Note: type is checked, IDE is wrongly giving a warning which was suppressed
     * Deep copies an object in-memory by marshalling and unmarshalling the object such that all fields are reinitialized recursively.
     * @param <T> The type of the object being copied.
     * @param marshallableObject The object to deep copy which is integrated with Jackson for YAML marshalling.
     * @return A deep copy of the object, or null if copying fails.
     */
    @SuppressWarnings("unchecked")
    public static <T> T smileDeepCopy(T marshallableObject) {
        if (marshallableObject == null) return null;
        try {
            byte[] data = smileMapper.writeValueAsBytes(marshallableObject);
            return (T) smileMapper.readValue(data, marshallableObject.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Note: type is checked, IDE is wrongly giving a warning which was suppressed
     * Deep copies an object in-memory by marshalling and unmarshalling the object such that all fields are reinitialized recursively.
     * @param <T>  The type of the object being copied.
     * @param marshallableObject The object to deep copy which is integrated with Jackson for YAML marshalling.
     * @return A deep copy of the object, or null if copying fails.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T marshallableObject) {
        if (marshallableObject == null) return null;
        try {
            String yamlString = yamlMapper.writeValueAsString(marshallableObject);
            Object read = yamlMapper.readValue(yamlString, marshallableObject.getClass());
            Class<?> classT = marshallableObject.getClass();
            if (classT.isInstance(read)) return (T) read;
            else throw new RuntimeException("Null read object during deep copy");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
