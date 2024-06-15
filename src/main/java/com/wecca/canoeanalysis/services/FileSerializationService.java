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
import java.util.List;

public class FileSerializationService {

    @Setter
    private static MainController mainController;
    private static final ObjectMapper yamlMapper;

    static {
        yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        yamlMapper.findAndRegisterModules();
    }
    public static File exportCanoeToYAML(Canoe canoe, Stage stage) throws IOException {
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
            yamlMapper.writeValue(fileToDownload, canoe);

        return fileToDownload;
    }

    public static Canoe importCanoeFromYAML(Stage stage)
    {
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
                Canoe canoe = yamlMapper.readValue(fileToUpload, Canoe.class);
                Canoe combinedPointLoadsCanoe = combinePointLoads(canoe);

                if (!canoe.equals(combinedPointLoadsCanoe))
                    mainController.showSnackbar("Some points loads have been combined");

                return validateCanoe(yamlMapper.readValue(fileToUpload, Canoe.class));
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

    private static Canoe validateCanoe(Canoe canoe) {

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
                canoe = null;
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

    private static Canoe combinePointLoads(Canoe canoe)
    {
        double length = canoe.getLength();
        List<Load> newLoads = new ArrayList<>();
        newLoads.addAll(canoe.getPLoads());
        newLoads.addAll(canoe.getDLoads());

        Canoe newCanoe = new Canoe();
        newCanoe.setLength(length);
        for (Load load : newLoads) {newCanoe.addLoad(load);}

        return newCanoe;
    }
}
