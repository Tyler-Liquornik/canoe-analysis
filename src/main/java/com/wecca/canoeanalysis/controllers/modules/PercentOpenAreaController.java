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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import ij.ImagePlus;
import ij.IJ;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.measure.ResultsTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ResourceBundle;

public class PercentOpenAreaController implements Initializable {

    @Setter
    private static MainController mainController;
    public JFXTextField percentOpenAreaTextField;
    public JFXTextField openAreaTextField;
    public JFXTextField totalAreaTextField;
    public JFXTextField PassingPOA;
    public FontAwesomeIcon uploadIcon;
    public JFXButton upButton;
    public Button clButton;
    public Label resultTextField;
    public Label dragAndDropLabel;
    public Label orLabel;
    public Label Pass;
    public Label Fail;

    @FXML
    private ImageView imageview;
    @FXML
    private Label OpenArea, TotalArea, PercentOpenArea;
    private File file = null;

    private double openArea = 0.0;
    private double totalArea;
    private double percentOpenArea;


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
        file = fileChooser.showOpenDialog(popupStage);
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
        }
    }

    public void AnalyzeImageButton(){

        if (file != null){
            double POA = AnalyzeImage(file);
            if (PassingPOA != null){
                try {
                    double value = Double.parseDouble(PassingPOA.getText());
                    if (0 < value && value < 100) {
                        if (POA >= value) {
                            Pass.setVisible(true);
                            Fail.setVisible(false);
                            openAreaTextField.setText(String.valueOf(openArea));
                            totalAreaTextField.setText(String.valueOf(totalArea));
                            resultTextField.setText(String.format("%.2f", percentOpenArea));
                            openArea = 0;
                            totalArea = 0;
                        }
                        else if (POA < value) {
                            Pass.setVisible(false);
                            Fail.setVisible(true);
                            openAreaTextField.setText(String.valueOf(openArea));
                            totalAreaTextField.setText(String.valueOf(totalArea));
                            resultTextField.setText(String.format("%.2f", percentOpenArea));
                            openArea = 0;
                            totalArea = 0;
                        }
                    }
                    else {
                        mainController.showSnackbar("Enter a number between 0 and 100");
                    }
                } catch (NumberFormatException ex){
                    mainController.showSnackbar("Enter a number");
                }
            }
            else {
                mainController.showSnackbar("Enter an estimated result percentage");
            }
        }
        else {
            mainController.showSnackbar("Please upload an image file");
        }
    }

    public void clearImage() {
        imageview.setImage(null);
        uploadIcon.setOpacity(1);
        upButton.setOpacity(1);
        dragAndDropLabel.setText("Drag and Drop to Upload Image");
        orLabel.setText("OR");
        openAreaTextField.setText("");
        totalAreaTextField.setText("");
        resultTextField.setText("");
        PassingPOA.setText("");
        Pass.setVisible(true);
        Fail.setVisible(true);
    }

    private double AnalyzeImage(File file){

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
        IJ.run(img, "Analyze Particles...", "exclude clear");

        // Get the ResultsTable that contains the measurements
        ResultsTable rt = Analyzer.getResultsTable();

        // Calculate the total area of the open spaces
        for (int i = 0; i < rt.getCounter(); i++) {
            openArea += rt.getValue("Area", i);
        }

        // Get the total area of the image
        totalArea = img.getWidth() * img.getHeight();

        // Calculate the percent open area
        percentOpenArea = (openArea / totalArea) * 100;

        // Set the results
        System.out.printf("Percent Open Area: %.2f", percentOpenArea);

        return percentOpenArea;
    }
}