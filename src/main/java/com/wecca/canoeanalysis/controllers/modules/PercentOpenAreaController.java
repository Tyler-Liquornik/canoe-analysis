package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
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
    public JFXTextField percentOpenAreaTextField;
    public JFXTextField openAreaTextField;
    public JFXTextField totalAreaTextField;
    public FontAwesomeIcon uploadIcon;
    public JFXButton upButton;
    public Button clButton;
    public Label dragAndDropLabel;
    public Label orLabel;
    public Rectangle colorRectangle;

    @FXML
    private ImageView imageview;
    @FXML
    private Label OpenArea, TotalArea, PercentOpenArea;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());

        mainController.resetToolBarButtons();
    }


    public void uploadButton() {

        Stage popupStage = new Stage();

        // Create a FileChooser to select image files
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(popupStage);
        if (file != null) {
            try {
                // Load the selected image file
                Image image = new Image(new FileInputStream(file));

                // Display the image in the main ImageView
                imageview.setImage(image);
                // Close the popup window
                popupStage.close();
                uploadIcon.setOpacity(0);
                upButton.setOpacity(0);
                orLabel.setText("");
                dragAndDropLabel.setText("");




            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            ImagePlus img = IJ.openImage(file.getAbsolutePath());

            // Convert to grayscale if needed
            ImageProcessor ip = img.getProcessor();
            if (ip.getBitDepth() != 8) {
                IJ.run(img, "8-bit", "");
            }

            IJ.run(img, "Subtract Background...", "rolling=50");
            IJ.run(img, "Gaussian Blur...", "sigma=2");

            // Threshold the image to segment the mesh and open areas
            IJ.setAutoThreshold(img, "Default");
            IJ.run(img, "Convert to Mask", "");

            // Analyze particles to get the area of open spaces
            IJ.run(img, "Set Measurements...", "area redirect=None decimal=3");
            IJ.run(img, "Analyze Particles...", "clear");

            // Get the ResultsTable that contains the measurements
            ResultsTable rt = Analyzer.getResultsTable();

            // Calculate the total area of the open spaces
            double openArea = 0.0;
            for (int i = 0; i < rt.getCounter(); i++) {
                openArea += rt.getValue("Area", i);
            }

            // Get the total area of the image
            double totalArea = img.getWidth() * img.getHeight();

            // Calculate the percent open area
            double percentOpenArea = (openArea / totalArea) * 100;

            //Map which store an integer value representing a color, and the frequency in which it appears
            Map<Integer, Integer> colorFrequencyMap = new HashMap<>();

            //loop which will iterate over whole image and get the color of the pixel and store it in the color variable
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                int color = ip.getPixel(x, y);

                // Increase the frequency count for this color
                colorFrequencyMap.put(color, colorFrequencyMap.getOrDefault(color, 0) + 1);
                }
            }

            //method which will get the most common color in the image
            Color color = getMostCommonColor(colorFrequencyMap);

            //following method will set the color to the most common color
            setColor(color.getRed(),color.getGreen(),color.getBlue());


            // Set the results
            openAreaTextField.setText(String.valueOf(openArea));
            totalAreaTextField.setText(String.valueOf(totalArea));
            percentOpenAreaTextField.setText(String.format("%.2f%%", percentOpenArea));
            System.out.printf("Percent Open Area: %.2f", percentOpenArea);
        }
    }

    public Color getMostCommonColor(Map<Integer, Integer> colorFrequencyMap) {
        int mostProminentColor = 0;
        int maxFrequency = 0;
        //this loop will iterate through all the colors in the hashmap, and use their related frequencies to find the most common frequency
        for (Map.Entry<Integer, Integer> entry : colorFrequencyMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                mostProminentColor = entry.getKey();
                maxFrequency = entry.getValue();
            }
        }
        // this will convert the integer value into a Color object which can be displayed as an RGB value
        return new Color(mostProminentColor);
    }

    public void clearImage() {
        imageview.setImage(null);
        uploadIcon.setOpacity(1);
        upButton.setOpacity(1);
        dragAndDropLabel.setText("Drag and Drop to Upload Image");
        orLabel.setText("OR");
    }

    public void setColor(int red, int green, int blue) {
        // Create a Color object from the RGB values
        javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(red, green, blue);

        // Set the fill of the Rectangle to the created Color
        colorRectangle.setFill(fxColor);
    }
} // Test commit