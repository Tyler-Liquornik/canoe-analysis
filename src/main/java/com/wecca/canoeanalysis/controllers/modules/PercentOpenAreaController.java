package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.utils.ColorUtils;
import com.wecca.canoeanalysis.utils.InputParsingUtils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
    private JFXColorPicker colorPicker;

    private File imageFile = null;
    private final Map<Integer, Integer> colorFrequencyMap = new HashMap<>();

    public void handleUploadOrClearButton() {
        String uploadOrClearText = uploadOrClearButton.getText();
        if (uploadOrClearText.equals("Delete Image"))
            deleteImage();
        else if (uploadOrClearText.equals("Browse Image"))
            uploadImage();
    }

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
        colorPicker.setValue(Color.web("#FFFFFF"));
        resultTextField.setText("???");
        colorPicker.setDisable(true);
        colorPicker.setOpacity(1);
        colorFrequencyMap.clear();
    }

    public void uploadImage() {
        Stage popupStage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        imageFile = fileChooser.showOpenDialog(popupStage);

        if (imageFile == null)
            return;

        try {
            Image image = new Image(new FileInputStream(imageFile));
            imageview.setImage(image);

            if (image != null) { // Checking if an image has been uploaded

                // Variable for saving final height and width
                double width = 0;
                double height = 0;

                // Ratios of how the image fits into the image view (this maintains the aspect ratio while editing size)
                double ratioX = imageview.getFitWidth() / image.getWidth();
                double ratioY = imageview.getFitHeight() / image.getHeight();

                double reductionCoefficient = 0; // Variable to store the reduction coefficient for scaling the image

                // Determining which ratio is smaller to maintain the aspect ratio of the image
                if(ratioX >= ratioY) {
                    reductionCoefficient = ratioY; // If height ratio is smaller or equal, use it to fit image within bounds
                } else {
                    reductionCoefficient = ratioX; // Otherwise, use width ratio
                }

                // Calculate the new width and height based on the reduction coefficient
                width = image.getWidth() * reductionCoefficient;
                height = image.getHeight() * reductionCoefficient;

                // Center the image within the imageView by adjusting the x and y positions
                imageview.setX((imageview.getFitWidth() - width) / 2);
                imageview.setY((imageview.getFitHeight() - height) / 2);
            }

            popupStage.close();
            uploadOrClearButton.setText("Delete Image");
            orLabel.setText("");
            dragAndDropLabel.setText("");
            cloud.setOpacity(0);
            cloudBackground.setOpacity(0);
            colorPicker.setDisable(false);

            ImagePlus imagePlus = IJ.openImage(imageFile.getAbsolutePath());
            ImageProcessor imageProcessor = imagePlus.getProcessor();
            if (imageProcessor.getBitDepth() != 24) {
                IJ.run(imagePlus, "RBG Color", "");
            }

            for (int y = 0; y < imageProcessor.getHeight(); y++) {
                for (int x = 0; x < imageProcessor.getWidth(); x++) {
                    int pixel = imageProcessor.getPixel(x, y);

                    colorFrequencyMap.put(pixel, colorFrequencyMap.getOrDefault(pixel, 0) + 1);
                }
            }

            Color prominentColor = getSecondMostProminentColor(colorFrequencyMap);
            colorPicker.setValue(prominentColor);

        } catch (FileNotFoundException ignored) {}
    }

    public void handleAnalyzeImageButton() {
        if (imageFile != null) {
            boolean isPassingPoaValid = InputParsingUtils.validateTextAsPercent(passingPOATextField.getText());
            if (isPassingPoaValid) {
                double passingPoa = Double.parseDouble(passingPOATextField.getText());
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

    private double getPoaFromImage(File file) {
        Color color = colorPicker.getValue();
        int r = (int) (color.getRed() * 255);//get the RGB color of the ColorPicker
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        int count = 0;//counter for counting the pixels closest to the RGB color of the ColorPicker

        ImagePlus img = IJ.openImage(file.getAbsolutePath());
        ImageProcessor ip = img.getProcessor();
        IJ.run(img, "RBG Color", "");
        //IJ.run(img, "Subtract Background...", "rolling=50");
        //IJ.run(img, "Gaussian Blur...", "sigma=2");
        // IJ.setAutoThreshold(img, "Default");
        //IJ.run(img, "Convert to Mask", "");
        //IJ.run(img, "Set Measurements...", "area redirect=None decimal=3");
        //IJ.run(img, "Analyze Particles...", "exclude clear");

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int pixel = ip.getPixel(x, y);
                java.awt.Color color1 = new java.awt.Color(pixel);//get the RGB colors of each pixel in the image
                int red = color1.getRed();
                int green = color1.getGreen();
                int blue = color1.getBlue();


                //System.out.println("Red: " + red + ", Green: " + green + ", Blue: " + blue);
                double distance = Math.sqrt(Math.pow( r - red, 2) + Math.pow(g - green, 2) + Math.pow(b - blue, 2));
                //calculate the vector distance between the RGB colorPicker and the RGB pixels of the image
                //System.out.println(distance);
                if (distance <= 80.0){//if the distance is within 80px range
                    count++;//keep track of the pixels close to the value of the colorPicker
                }

            }
        }

        //System.out.println("RGB: (" + r + ", " + g + ", " + b + ")");
        System.out.println(count);

        ResultsTable rt = Analyzer.getResultsTable();
        double openArea = 0;
        for (int i = 0; i < rt.getCounter(); i++) {
            openArea += rt.getValue("Area", i);
        }
        double totalArea = img.getWidth() * img.getHeight();
        return (count / totalArea) * 100;
    }

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

        return ColorUtils.colorFromInteger(secondMostColor);
    }

    /**
     * Operations called on initialization of the view
     * @param url unused, part of javafx framework
     * @param resourceBundle unused, part of javafx framework
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        mainController.resetToolBarButtons();
        passingPOATextField.setText("40.00");
        colorPicker.setValue(Color.web("#FFFFFF"));
        colorPicker.setDisable(true);
        colorPicker.setOpacity(1);
    }
}
