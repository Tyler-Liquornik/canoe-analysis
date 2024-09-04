package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.utils.InputParsingUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import java.awt.Color;

import ij.ImagePlus;
import ij.IJ;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.measure.ResultsTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class PercentOpenAreaController implements Initializable {

    @Setter
    private static MainController mainController;
    @FXML
    private JFXTextField passingPOATextField;
    @FXML
    private JFXButton uploadOrClearButton;
    @FXML
    private Label resultTextField, dragAndDropLabel, orLabel, passLabel, failLabel;
    @FXML
    private ImageView imageview;
    @FXML
    private FontAwesomeIcon cloud;
    @FXML
    private Rectangle cloudBackground;
    @FXML
    private ColorPicker colorPicker;

    private File imageFile = null;
    private final Map<Integer, Integer> colorFrequencyMap = new HashMap<>();

    /**
     * Either upload or clear the image depending on state
     */
    public void handleUploadOrClearButton() {
        String uploadOrClearText = uploadOrClearButton.getText();
        if (uploadOrClearText.equals("Delete Image"))
            deleteImage();
        else if (uploadOrClearText.equals("Browse Image"))
            uploadImage();
    }

    /**
     * Delete an image on the screen and replace with the "browse image" state
     */
    public void deleteImage() {
        cloud.setOpacity(1);
        cloudBackground.setOpacity(1);
        dragAndDropLabel.setText("Drag & Drop to Upload Image");
        uploadOrClearButton.setText("Browse Image");
        orLabel.setText("OR");
        resultTextField.setText("");
        passLabel.setVisible(false);
        failLabel.setVisible(false);
        imageFile = null;
        imageview.setImage(null);
        colorPicker.setValue(javafx.scene.paint.Color.web("#FFFFFF"));
        resultTextField.setText("???");
        colorPicker.setDisable(true);
        colorPicker.setOpacity(1);
        colorFrequencyMap.clear();
    }

    /**
     * Upload an image to view on the screen
     */
    public void uploadImage() {
        // Upload image file
        Stage popupStage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser
                .ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        imageFile = fileChooser.showOpenDialog(popupStage);

        // Check if the user canceled the file chooser
        if (imageFile == null)
            return;

        try {
            // Update state
            Image image = new Image(new FileInputStream(imageFile));
            imageview.setImage(image);
            popupStage.close();
            uploadOrClearButton.setText("Delete Image");
            orLabel.setText("");
            dragAndDropLabel.setText("");
            cloud.setOpacity(0);
            cloudBackground.setOpacity(0);
            colorPicker.setDisable(false);

            // Convert to grayscale if needed
            ImagePlus imagePlus = IJ.openImage(imageFile.getAbsolutePath());
            ImageProcessor imageProcessor = imagePlus.getProcessor();
            if (imageProcessor.getBitDepth() != 8) {
                IJ.run(imagePlus, "8-bit", "");
            }

            // Loop to iterate over the image and track color frequency
            for (int y = 0; y < imageProcessor.getHeight(); y++) {
                for (int x = 0; x < imageProcessor.getWidth(); x++) {
                    int color = imageProcessor.getPixel(x, y);
                    // Increase the frequency count for this color
                    colorFrequencyMap.put(color, colorFrequencyMap.getOrDefault(color, 0) + 1);
                }
            }

            // Set the color picker to the most prominent color
            Color color = getSecondMostProminentColor(colorFrequencyMap);
            colorPicker.setValue(javafx.scene.paint.Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));

        } catch (FileNotFoundException ignored) {
            System.out.println("test");
        }
    }

    /**
     * Handler for the analyze image button to validate inputs and execute analyze
     */
    public void handleAnalyzeImageButton() {
        if (imageFile != null) {
            boolean isPassingPoaValid = InputParsingUtil.validateTextAsPercent(passingPOATextField.getText());
            if (isPassingPoaValid) {
                double passingPoa = Double.parseDouble(passingPOATextField.getText()) / 100;
                double poa = getPoaFromImage(imageFile);
                resultTextField.setText(String.format("%.2f", poa));
                boolean pass = poa >= passingPoa;
                passLabel.setVisible(pass);
                failLabel.setVisible(!pass);
            } else
                mainController.showSnackbar("Please enter a valid passing POA percentage");
        } else
            mainController.showSnackbar("Cannot analyze before uploading image");
    }

    /**
     * Analyze an image file for percent open area
     * @param file the image file to analyze
     * @return the percent open area
     */
    private double getPoaFromImage(File file) {
        // ImageJ setup
        ImagePlus img = IJ.openImage(file.getAbsolutePath());
        ImageProcessor ip = img.getProcessor();
        if (ip.getBitDepth() != 8)
            IJ.run(img, "8-bit", "");
        IJ.run(img, "Subtract Background...", "rolling=50");
        IJ.run(img, "Gaussian Blur...", "sigma=2");
        IJ.setAutoThreshold(img, "Default");
        IJ.run(img, "Convert to Mask", "");
        IJ.run(img, "Set Measurements...", "area redirect=None decimal=3");
        IJ.run(img, "Analyze Particles...", "exclude clear");

        // Calculate the percent open area
        ResultsTable rt = Analyzer.getResultsTable();
        double openArea = 0;
        for (int i = 0; i < rt.getCounter(); i++) {
            openArea += rt.getValue("Area", i);
        }
        double totalArea = img.getWidth() * img.getHeight();
        return (openArea / totalArea) * 100;
    }

    /**
     * Get the second most prominent color in a map
     * It is assumed POA > 50% so the second most prominent color is most likely to be the mesh color
     * @param colorFrequencyMap the map to check
     * @return the most prominent color
     */
    public Color getSecondMostProminentColor(Map<Integer, Integer> colorFrequencyMap) {
        int mostColor = -1, secondMostColor = -1;
        int maxFreq = 0, secondMaxFreq = 0;

        for (Map.Entry<Integer, Integer> entry : colorFrequencyMap.entrySet()) {
            int color = entry.getKey(), freq = entry.getValue();

            if (freq > maxFreq) {
                secondMostColor = mostColor;
                secondMaxFreq = maxFreq;
                mostColor = color;
                maxFreq = freq;
            } else if (freq > secondMaxFreq) {
                secondMostColor = color;
                secondMaxFreq = freq;
            }
        }

        if (secondMostColor == -1)
            throw new RuntimeException("Cannot determine second most prominent color.");

        return new Color(secondMostColor);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        mainController.resetToolBarButtons();
        passingPOATextField.setText("40.00");
        colorPicker.setValue(javafx.scene.paint.Color.web("#FFFFFF"));
        colorPicker.setDisable(true);
        colorPicker.setOpacity(1);
    }
}