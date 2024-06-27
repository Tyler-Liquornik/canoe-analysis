package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

// Uniform distributed loads are to be represented as "arrow boxes"
@Getter @Setter
public class ArrowBox extends Group implements Graphic
{
    // Fields
    private double lX;
    private double rX;
    private double startY;
    private double endY;
    private ArrowBoxSectionState arrowBoxSectionState;

    private final Arrow lArrow;
    private final Arrow rArrow;
    private final Rectangle box;
    private final Line borderLine;

    private boolean isColored;

    private final static double defaultThickness = 1;

    // Constructor
    public ArrowBox(double lX, double startY, double rX, double endY, ArrowBoxSectionState arrowBoxSectionState)
    {
        super();

        this.lX = lX;
        this.rX = rX;
        this.startY = startY;
        this.endY = endY;

        // Arrows at left and right bound the rectangle
        lArrow = arrowBoxSectionState.isShowLArrow() ? new Arrow(lX, startY, lX, endY) : null;
        rArrow = arrowBoxSectionState.isShowRArrow() ? new Arrow(rX, startY, rX, endY) : null;

        // Box depends on if this is an upwards or downwards facing ArrowBox
        // Extra pixels to fill weird whitespaces
        if (startY < endY)
            box = new Rectangle(lX, startY, rX - lX, endY - startY + 2);
        else
            box = new Rectangle(lX, endY - 2, rX - lX, startY - endY + 2);

        box.setFill(ColorPaletteService.getColor("above-surface"));

        borderLine = new Line(lX, startY, rX, startY);
        borderLine.setStroke(ColorPaletteService.getColor("white"));
        borderLine.setStrokeWidth(defaultThickness);

        getChildren().addAll(box, borderLine);
        if (lArrow != null) {getChildren().add(lArrow);}
        if (rArrow != null) {getChildren().add(rArrow);}

        if (arrowBoxSectionState == ArrowBoxSectionState.NON_SECTIONED)
            JFXDepthManager.setDepth(this, 4);

        this.isColored = false;
        ColorManagerService.registerInColorPalette(this);
    }

    // Accessors
    @Override
    public double getX() {return getLX();}

    // Change the color of the arrow box
    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color outlineColor = setColored ? ColorPaletteService.getColor("primary") :
                ColorPaletteService.getColor("white");
        Color fillColor = setColored ? ColorPaletteService.getColor("primary-light") :
                ColorPaletteService.getColor("above-surface");

        if (lArrow != null)
            lArrow.setFill(outlineColor);
        if (rArrow != null)
            rArrow.setFill(outlineColor);
        borderLine.setStroke(outlineColor);
        box.setFill(fillColor);
    }
}
