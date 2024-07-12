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

/**
 * Icon used for a uniformly distributed load
 */
@Getter @Setter
public class ArrowBox extends Group implements Graphic {

    private double lX;
    private double rX;
    private double startY;
    private double endY;
    private Arrow lArrow;
    private Arrow rArrow;
    private Rectangle box;
    private Line borderLine;
    private boolean isColored;

    public ArrowBox(double lX, double startY, double rX, double endY) {
        super();
        this.lX = lX;
        this.rX = rX;
        this.startY = startY;
        this.endY = endY;

        this.isColored = false;
        draw();
        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        // Arrows at left and right bound the rectangle
        lArrow = new Arrow(lX, startY, lX, endY);
        rArrow = new Arrow(rX, startY, rX, endY);

        // Box depends on if this is an upwards or downwards facing ArrowBox
        // Extra pixels to fill weird whitespaces
        if (startY < endY) {
            box = new Rectangle(lX, startY, rX - lX, endY - startY + 2);
        } else {
            box = new Rectangle(lX, endY - 2, rX - lX, startY - endY + 2);
        }
        box.setFill(ColorPaletteService.getColor("above-surface"));

        borderLine = new Line(lX, startY, rX, startY);
        borderLine.setStroke(ColorPaletteService.getColor("white"));

        getChildren().addAll(box, lArrow, rArrow, borderLine);
    }

    @Override
    public double getX() {
        return getLX();
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color outlineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        Color fillColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("above-surface");

        lArrow.setFill(outlineColor);
        rArrow.setFill(outlineColor);
        borderLine.setStroke(outlineColor);
        box.setFill(fillColor);
    }
}
