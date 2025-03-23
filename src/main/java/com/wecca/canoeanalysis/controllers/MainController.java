package com.wecca.canoeanalysis.controllers;

import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.effects.JFXDepthManager;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import com.jfoenix.controls.JFXSnackbarLayout;
import com.wecca.canoeanalysis.components.controls.IconButton;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.modules.ModuleController;
import com.wecca.canoeanalysis.controllers.popups.UploadAlertController;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import com.wecca.canoeanalysis.services.MarshallingService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

@Getter @Setter
public class MainController implements Initializable {

    @FXML
    private Button hamburgerButton;
    @FXML
    private AnchorPane menuDrawer;
    @FXML
    private JFXHamburger hamburger;
    @FXML
    private JFXSnackbar snackbar;
    @FXML
    private AnchorPane root, moduleInjectionRoot, toolBarPane;

    // Stage state management
    private Scene primaryScene;
    private Stage primaryStage;
    private double stageXOffset;
    private double stageYOffset;

    // Drawer state management
    private boolean isDrawerOpen;
    private AnimationTimer drawerTimer;
    private double drawerTargetX;

    // Each module will have a set of custom toolbar buttons on top of the minimize and close window buttons
    private List<Button> moduleToolBarButtons = new ArrayList<>();

    // Back reference to the selected controller
    @Getter @Setter
    private ModuleController currentModuleController;

    /**
     * Mouse clicked event to set the current location of the window
     */
    public void draggableWindowGetLocation(MouseEvent event) {
        WindowManagerService.setStageOffsets(event);
    }

    /**
     * Mouse dragged event handler to move the window
     */
    public void draggableWindowMove(MouseEvent event) {
        WindowManagerService.moveStage(event, primaryStage);
    }

    /**
     * Close the window when the "X" button is pressed
     */
    public void closeWindow() {
        WindowManagerService.closeWindow(primaryStage);
    }

    /**
     * Minimize the window the "-" button is pressed
     */
    public void minimizeWindow() {
        WindowManagerService.minimizeWindow(primaryStage);
    }

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

    public void startDrawerTimer() {
        hamburgerButton.setDisable(true);
        hamburger.setDisable(false);

        if (drawerTimer != null)
            drawerTimer.stop();

        drawerTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double currentX = menuDrawer.getTranslateX();
                if (currentX != drawerTargetX) {
                    double newX = currentX + (drawerTargetX - currentX) * 0.075;
                    menuDrawer.setTranslateX(newX);
                    if (Math.abs(newX - drawerTargetX) < 1) {
                        menuDrawer.setTranslateX(drawerTargetX);
                        stop();
                        isDrawerOpen = drawerTargetX == 0;
                        hamburgerButton.setDisable(false);
                    }
                }
                else {
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
        JFXSnackbarLayout snackbarLayout = new JFXSnackbarLayout(message, "DISMISS", event -> closeSnackBar(snackbar));
        snackbarLayout.setPrefHeight(50);
        JFXSnackbar.SnackbarEvent snackbarEvent = new JFXSnackbar.SnackbarEvent(snackbarLayout, Duration.seconds(5), null);
        snackbar.fireEvent(snackbarEvent);
    }

    public void initializeSnackbar()
    {
        snackbar = new JFXSnackbar(root);
        JFXDepthManager.setDepth(snackbar, 5);
        snackbar.setPrefWidth(200);
        snackbar.setViewOrder(Integer.MIN_VALUE);
        snackbar.getStylesheets().add(ResourceManagerService.getResourceFilePathString("css/style.css", false));
        ColorManagerService.registerForRecoloringFromStylesheet(snackbar, "css/style.css");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wecca/canoeanalysis/view/module-selector-view.fxml"));
            AnchorPane drawerContent = loader.load();
            menuDrawer.getChildren().setAll(drawerContent);
            menuDrawer.setTranslateX(-menuDrawer.getPrefWidth());
            JFXDepthManager.setDepth(menuDrawer, 5);
        } catch (IOException ignored) {}
    }

    /**
     * Set the toolbar buttons based on the glyphs (icon) and respective and onClick functions
     * @param iconGlyphToFunctionMap a map of <Button Icon : Button function>
     */
    public void setIconToolBarButtons(LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap) {
        List<Button> buttons = new ArrayList<>();
        for (Map.Entry<IconGlyphType, Consumer<MouseEvent>> entry : iconGlyphToFunctionMap.entrySet()) {
            Button button = IconButton.getToolbarButton(entry.getKey(), entry.getValue());
            buttons.add(button);
        }
        addOrSetToolBarButtons(buttons);
    }

    /**
     * When the toolbar only has the hamburger in it, add all the icons
     * The next time this is called, the size of the buttons list must match the current amount of module tool bar buttons
     * Module toolbar buttons are those except the hamburger, close, and minimize buttons
     * @param buttons the buttons to add or set
     */
    public void addOrSetToolBarButtons(List<Button> buttons) {
        double buttonWidth = buttons.getFirst().prefWidth(-1);

        // Account for space taken up by minimize and close window buttons and padding
        double buttonX = toolBarPane.getWidth() - buttonWidth * 2 -
                AnchorPane.getRightAnchor(toolBarPane.getChildren().getLast());
        double buttonY = 0.0;

        moduleToolBarButtons = buttons;
        for (Button button : moduleToolBarButtons) {
            buttonX = buttonX - buttonWidth;
            button.setLayoutX(buttonX);
            button.setLayoutY(buttonY);
        }

        // No module toolbar buttons have been added yet, only 3 buttons are the menu, close, and minimize
        if (toolBarPane.getChildren().size() == 3) {
            // Extra index skips the hamburger which is always first
            toolBarPane.getChildren().addAll(1, moduleToolBarButtons);
        }
        else {
            for (int i = 1; i <= moduleToolBarButtons.size(); i++) {
                toolBarPane.getChildren().set(i, moduleToolBarButtons.get(i - 1));
            }
        }
    }

    /**
     * Disable a module toolbar button (doesn't include the hamburger, minus, or X buttons)
     * @param b choose whether to enable or disable
     * @param index to choose which button to disable
     */
    public void disableModuleToolBarButton(boolean b, int index) {
        Button button = moduleToolBarButtons.get(index);
        double opacity = b ? 0.4 : 1.0;
        button.setStyle("-fx-opacity: " + opacity);
        button.setDisable(b);
    }

    /**
     * Disable all module toolbar buttons (doesn't include the hamburger, minus, or X buttons)
     * @param b choose whether to enable or disable
     */
    public void disableAllModuleToolbarButtons(boolean b) {
        for (int i = 0; i < moduleToolBarButtons.size(); i++) {
            disableModuleToolBarButton(b, i);
        }
    }

    /**
     * Remove all toolbar buttons
     */
    public void resetToolBarButtons() {
        if (!moduleToolBarButtons.isEmpty()) {
            // Extra index skips the hamburger which is always first
            toolBarPane.getChildren().remove(1, 1 + moduleToolBarButtons.size());
            moduleToolBarButtons.clear();
        }
    }

    /**
     * Flash the icon inside one of the module toolbar buttons with a DropShadow effect as a hint for the user to press there.
     * @param index of the button to flash in the module toolbar
     * @param duration the total duration in ms the DropShadow effect should take
     */
    public void flashModuleToolBarButton(int index, double duration) {
        if (index < 0 || index >= moduleToolBarButtons.size())
            throw new IllegalArgumentException("Invalid index for toolbar button");

        // Setup drop shadow on button
        Button button = moduleToolBarButtons.get(index);
        FontAwesomeIcon icon = (FontAwesomeIcon) button.getGraphic();
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(ColorPaletteService.getColor("above-surface"));
        dropShadow.setRadius(0);
        dropShadow.setSpread(0);
        icon.setEffect(dropShadow);

        // Create a Timeline to animate the DropShadow effect
        double fadeInDuration = Math.min(duration * 0.25, 500);
        double fadeOutDuration = Math.min(duration * 0.25, 500);
        Timeline flashTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(dropShadow.radiusProperty(), 0),
                        new KeyValue(dropShadow.spreadProperty(), 0)),
                new KeyFrame(Duration.millis(fadeInDuration),
                        new KeyValue(dropShadow.radiusProperty(), 10),
                        new KeyValue(dropShadow.spreadProperty(), 0.5)),
                new KeyFrame(Duration.millis(fadeInDuration + duration),
                        new KeyValue(dropShadow.radiusProperty(), 10),
                        new KeyValue(dropShadow.spreadProperty(), 0.5)),
                new KeyFrame(Duration.millis(fadeInDuration + duration + fadeOutDuration),
                        new KeyValue(dropShadow.radiusProperty(), 0),
                        new KeyValue(dropShadow.spreadProperty(), 0))
        );
        flashTimeline.play();
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize components
        initializeHamburger();
        initializeDrawer();
        initializeSnackbar();

        // State init
        stageXOffset = 0;
        stageYOffset = 0;
        isDrawerOpen = false;

        // Pass references to services that require it
        MarshallingService.setMainController(this);
        UploadAlertController.setMainController(this);
    }
}