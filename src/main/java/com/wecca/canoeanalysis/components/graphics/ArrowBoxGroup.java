package com.wecca.canoeanalysis.components.graphics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.scene.Group;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ArrowBoxGroup extends Group implements Graphic{
    private final List<ArrowBox> arrowBoxes;
    private boolean isColored;

    public ArrowBoxGroup(List<ArrowBox> arrowBoxes) {
        super();
        this.arrowBoxes = sectionArrowBoxes(arrowBoxes);
        getChildren().addAll(this.arrowBoxes);
    }

    private List<ArrowBox> sectionArrowBoxes(List<ArrowBox> arrowBoxes) {
        if (arrowBoxes == null || arrowBoxes.size() < 2)
            throw new IllegalArgumentException("At least two arrow boxes required");

        List<ArrowBox> sectionedArrowBoxes = new ArrayList<>();
        for (int i = 0; i < arrowBoxes.size(); i++) {
            ArrowBox arrowBox = getSectionedArrowBox(arrowBoxes, i);
            sectionedArrowBoxes.add(arrowBox);
        }

        return sectionedArrowBoxes;
    }

    @JsonIgnore
    private static ArrowBox getSectionedArrowBox(List<ArrowBox> arrowBoxes, int arrowBoxListIndex) {
        ArrowBoxSectionState sectionState;
        if (arrowBoxListIndex == 0)
            sectionState = ArrowBoxSectionState.LEFT_END_SECTION;
        else if (arrowBoxListIndex == arrowBoxes.size() - 1)
            sectionState = ArrowBoxSectionState.RIGHT_END_SECTION;
        else
            sectionState = ArrowBoxSectionState.MIDDLE_SECTION;
        return new ArrowBox(arrowBoxes.get(arrowBoxListIndex).getX(), arrowBoxes.get(arrowBoxListIndex).getStartY(),
                arrowBoxes.get(arrowBoxListIndex).getRX(), arrowBoxes.get(arrowBoxListIndex).getEndY(), sectionState);
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;
        for (ArrowBox arrowBox : arrowBoxes) {
            arrowBox.recolor(setColored);
        }
    }

    @Override
    public double getX() {
        return arrowBoxes.getFirst().getX();
    }
}
