package com.wecca.canoeanalysis.models.data;
import javafx.scene.text.TextFlow;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class Equation {
    private TextFlow parameter;
    private TextFlow description;
    private TextFlow value;
}

