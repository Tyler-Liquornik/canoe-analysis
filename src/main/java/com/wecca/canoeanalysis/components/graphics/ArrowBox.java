package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
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

    private final Arrow lArrow;
    private final Arrow rArrow;
    private final Rectangle box;
    private final Line borderLine;

    private final static double defaultThickness = 1;

    // Constructor
    public ArrowBox(double lX, double startY, double rX, double endY)
    {
        super();

        this.lX = lX;
        this.rX = rX;
        this.startY = startY;
        this.endY = endY;

        // Arrows at left and right bound the rectangle
        lArrow = new Arrow(lX, startY, lX, endY);
        rArrow = new Arrow(rX, startY, rX, endY);

        // Box depends on if this is an upwards or downwards facing ArrowBox
        // Extra pixels to fill weird whitespaces
        if (startY < endY)
            box = new Rectangle(lX, startY, rX - lX, endY - startY + 2);
        else
            box = new Rectangle(lX, endY - 2, rX - lX, startY - endY + 2);

        // box.setFill(lighten(ColorPalette.ICON.getColor()));
        box.setFill(ColorPaletteService.getColor("above-surface"));

        borderLine = new Line(lX, startY, rX, startY);
        borderLine.setStroke(ColorPaletteService.getColor("white"));
        borderLine.setStrokeWidth(defaultThickness);

        getChildren().addAll(box, lArrow, rArrow, borderLine);

        JFXDepthManager.setDepth(this, 4);
    }

    // Accessors
    @Override
    public double getX() {return getLX();}

    // Change the color of the arrow box
    @Override
    public void recolor(Color color) {
        lArrow.setFill(color);
        rArrow.setFill(color);
        borderLine.setStroke(color);

        if (color.equals(Color.WHITE)) // hard coded for now as lightening looks weird for white arrows
            box.setFill(ColorPaletteService.getColor("above-surface"));
        else
            box.setFill(ColorPaletteService.getColor("primary-light"));
    }
}
