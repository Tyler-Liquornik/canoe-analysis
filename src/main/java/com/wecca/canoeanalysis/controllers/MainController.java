package com.wecca.canoeanalysis.controllers;

import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.effects.JFXDepthManager;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.controls.CustomJFXSnackBarLayout;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML @Getter
    private Button hamburgerButton;
    @FXML @Getter
    private AnchorPane menuDrawer;
    @FXML @Getter
    private JFXHamburger hamburger;
    @FXML @Getter
    private JFXSnackbar snackbar;
    @FXML @Getter @Setter
    private AnchorPane root, moduleInjectionRoot, toolBarPane;

    @Getter @Setter
    private Scene primaryScene;
    @Getter @Setter
    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    // Drawer state management
    private boolean isDrawerOpen = false;
    private AnimationTimer drawerTimer;
    private double drawerTargetX; // Target X position for the drawer

    // Each module will have a set of custom toolbar buttons on top of the minimize and close window buttons
    private List<Button> moduleToolBarButtons = new ArrayList<>();

    /**
     * Mouse pressed event handler to record the current mouse position
     * @param event triggers the method
     */
    public void draggableWindowGetLocation(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    /** Mouse dragged event handler to move the window
     * @param event triggers the method
     */
    public void draggableWindowMove(MouseEvent event)
    {
        if (primaryStage != null)
        {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        }
    }

    /**
     * Close the window when the "X" button is pressed
     */
    public void closeWindow() {primaryStage.close();}

    /**
     * Minimize the window the "-" button is pressed
     */
    public void minimizeWindow() {primaryStage.setIconified(true);}

    public void toggleDrawer() {
        if (isDrawerOpen) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    public void openDrawer() {
        drawerTargetX = 0;
        menuDrawer.setVisible(true);
        startDrawerTimer();
    }

    public void closeDrawer() {
        drawerTargetX = -menuDrawer.getPrefWidth();
        startDrawerTimer();
    }

    public void startDrawerTimer()
    {
        hamburgerButton.setDisable(true);
        hamburger.setDisable(false);

        if (drawerTimer != null)
            drawerTimer.stop();

        drawerTimer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                double currentX = menuDrawer.getTranslateX();
                if (currentX != drawerTargetX)
                {
                    double newX = currentX + (drawerTargetX - currentX) * 0.075;
                    menuDrawer.setTranslateX(newX);
                    if (Math.abs(newX - drawerTargetX) < 1)
                    {
                        menuDrawer.setTranslateX(drawerTargetX);
                        stop();
                        isDrawerOpen = drawerTargetX == 0;
                        hamburgerButton.setDisable(false);
                    }
                }
                else
                {
                    stop();
                    hamburgerButton.setDisable(false);
                }
            }
        };
        drawerTimer.start();
    }

    public void showSnackbar(String message) {
        closeSnackBar(snackbar);
        initializeSnackbar(); // Reinitialization is required to fix visual bugs
        CustomJFXSnackBarLayout snackbarLayout = new CustomJFXSnackBarLayout(message, "DISMISS", event -> closeSnackBar(snackbar));
        snackbarLayout.setPrefHeight(50);
        Button dismissButton = snackbarLayout.getAction();
        dismissButton.getStyleClass().add("dismiss-button");
        JFXSnackbar.SnackbarEvent snackbarEvent = new JFXSnackbar.SnackbarEvent(snackbarLayout, Duration.seconds(3));
        snackbar.fireEvent(snackbarEvent);
    }

    public void initializeSnackbar()
    {
        snackbar = new JFXSnackbar(root);
        JFXDepthManager.setDepth(snackbar, 5);
        snackbar.setPrefWidth(200);
        snackbar.setViewOrder(Integer.MIN_VALUE);
        snackbar.getStylesheets().add(CanoeAnalysisApplication.class.getResource("css/style.css").toExternalForm());
    }

    // .close() has an unfixed bug in the JFoenix library itself
    // The bug at https://github.com/sshahine/JFoenix/issues/1101 has not been adequately fixed
    // Custom fix source: https://github.com/sshahine/JFoenix/issues/983 from GitHub user sawaYch
    public void closeSnackBar(JFXSnackbar sb) {
        Timeline closeAnimation = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        e -> sb.toFront(),
                        new KeyValue(sb.opacityProperty(), 1, Interpolator.EASE_IN),
                        new KeyValue(sb.translateYProperty(), 0, Interpolator.EASE_OUT)
                ),
                new KeyFrame(
                        Duration.millis(290),
                        new KeyValue(sb.visibleProperty(), true, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(300),
                        e -> sb.toBack(),
                        new KeyValue(sb.visibleProperty(), false, Interpolator.EASE_BOTH),
                        new KeyValue(sb.translateYProperty(),
                                sb.getLayoutBounds().getHeight(),
                                Interpolator.EASE_IN),
                        new KeyValue(sb.opacityProperty(), 0, Interpolator.EASE_OUT)
                )
        );
        closeAnimation.setCycleCount(1);
        closeAnimation.play();
    }

    public void initializeHamburger() {
        HamburgerBackArrowBasicTransition transition = new HamburgerBackArrowBasicTransition(hamburger);
        transition.setRate(-1);
        hamburgerButton.addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
            transition.setRate(transition.getRate() * -1);
            transition.play();
            toggleDrawer();
        });
    }

    public void initializeDrawer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wecca/canoeanalysis/view/side-drawer-view.fxml"));
            AnchorPane drawerContent = loader.load();
            menuDrawer.getChildren().setAll(drawerContent);
            menuDrawer.setTranslateX(-menuDrawer.getPrefWidth());
            JFXDepthManager.setDepth(menuDrawer, 5);
        } catch (IOException ignored) {}
    }

    public void addToolBarButtons(List<Button> buttons) {
        double buttonHeight = 34.0; // 1px difference is on purpose so the hover fill doesn't stick out of the toolbar
        double buttonWidth = 35.0;

        // Account for space taken up by minimize and close window buttons and padding
        double buttonX = toolBarPane.getWidth() - buttonWidth * 2 - AnchorPane.getRightAnchor(toolBarPane.getChildren().getLast());
        double buttonY = 0.0;

        moduleToolBarButtons = buttons;

        for (Button button : moduleToolBarButtons) {
            buttonX = buttonX - buttonWidth;

            button.setPrefHeight(buttonHeight);
            button.setPrefWidth(buttonWidth);
            button.setLayoutX(buttonX);
            button.setLayoutY(buttonY);
        }

        // Extra index skips the hamburger which is always first
        toolBarPane.getChildren().addAll(1, moduleToolBarButtons);
    }

    public void resetToolBarButtons() {
        if (!moduleToolBarButtons.isEmpty())
        {
            // Extra index skips the hamburger which is always first
            toolBarPane.getChildren().remove(1, 1 + moduleToolBarButtons.size());
            moduleToolBarButtons.clear();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize components
        initializeHamburger();
        initializeDrawer();
        initializeSnackbar();
    }
}
