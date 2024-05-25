package com.wecca.canoeanalysis.graphics;

import com.jfoenix.controls.JFXComboBox;
import javafx.animation.TranslateTransition;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Duration;

public class SlidingJFXComboBox<T> extends JFXComboBox<T> {

    public SlidingJFXComboBox() {
        this.setCellFactory(this::createAnimatedCell);
    }

    private ListCell<T> createAnimatedCell(ListView<T> listView) {
        return new AnimatedListCell<>();
    }

    private static class AnimatedListCell<E> extends ListCell<E> {

        private final TranslateTransition transition;

        public AnimatedListCell() {
            transition = new TranslateTransition(Duration.millis(200), this);
            transition.setFromY(-10);
            transition.setToY(0);
        }

        @Override
        protected void updateItem(E item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.toString());
                transition.playFromStart();
            }
        }
    }
}