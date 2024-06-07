package com.wecca.canoeanalysis.graphics.ui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSnackbarLayout;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.Setter;

// Implementing getter/setter functionality that isn't in the original class for some reason...
// This allows access to subcomponents of the snack bar for custom css implementations
// This is duplicated code from JFoenix but the best way I could find to do this at the moment
// JFoenix is an old unmaintained project so this is the best fixer at the moment
@Getter
@Setter
public class CustomJFXSnackBarLayout extends JFXSnackbarLayout {

    private Label toast;
    private JFXButton action;
    private StackPane actionContainer;
    private static final String DEFAULT_STYLE_CLASS = "jfx-snackbar-layout";

    public CustomJFXSnackBarLayout(String message, String actionText, EventHandler<ActionEvent> actionHandler) {
        super(message, actionText, actionHandler);
        this.initialize();
        this.toast = new Label();
        this.toast.setMinWidth(Double.NEGATIVE_INFINITY);
        this.toast.getStyleClass().add("jfx-snackbar-toast");
        this.toast.setWrapText(true);
        this.toast.setText(message);
        StackPane toastContainer = new StackPane(new Node[]{this.toast});
        toastContainer.setPadding(new Insets(20.0));
        this.actionContainer = new StackPane();
        this.actionContainer.setPadding(new Insets(0.0, 10.0, 0.0, 0.0));
        this.toast.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            if (this.getPrefWidth() == -1.0) {
                return this.getPrefWidth();
            } else {
                double actionWidth = this.actionContainer.isVisible() ? this.actionContainer.getWidth() : 0.0;
                return this.prefWidthProperty().get() - actionWidth;
            }
        }, new Observable[]{this.prefWidthProperty(), this.actionContainer.widthProperty(), this.actionContainer.visibleProperty()}));
        this.setLeft(toastContainer);
        this.setRight(this.actionContainer);
        if (actionText != null) {
            this.action = new JFXButton();
            this.action.setText(actionText);
            this.action.setOnAction(actionHandler);
            this.action.setMinWidth(Double.NEGATIVE_INFINITY);
            this.action.setButtonType(JFXButton.ButtonType.FLAT);
            this.action.getStyleClass().add("jfx-snackbar-action");
            this.actionContainer.getChildren().add(this.action);
            if (actionText != null && !actionText.isEmpty()) {
                this.action.setVisible(true);
                this.actionContainer.setVisible(true);
                this.actionContainer.setManaged(true);
                this.action.setText("");
                this.action.setText(actionText);
                this.action.setOnAction(actionHandler);
            } else {
                this.actionContainer.setVisible(false);
                this.actionContainer.setManaged(false);
                this.action.setVisible(false);
            }
        }

    }

    public String getToast() {
        return this.toast.getText();
    }

    public void setToast(String toast) {
        this.toast.setText(toast);
    }

    private void initialize() {
        this.getStyleClass().add("jfx-snackbar-layout");
    }
}

