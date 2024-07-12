package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import lombok.Getter;
import lombok.Setter;

/**
 * Icon used for a point load
 */
@Getter @Setter
public class Arrow extends Path implements Graphic {

    private static final double arrowHeadSize = 10;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private boolean isColored;
    private boolean isHighlighted;

    public Arrow(double startX, double endX, double startY, double endY) {
        super();

        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.isHighlighted = false;

        strokeProperty().bind(fillProperty());
        setFill(ColorPaletteService.getColor("white"));

        draw();
        JFXDepthManager.setDepth(this, 4);
    }
    public void draw() {
        // Line
        getElements().add(new MoveTo(startX, startY));
        getElements().add(new LineTo(endX, endY));

        // Arrow Head
        double angle = Math.atan2((endY - startY), (endX - startX)) - Math.PI / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // Adjust arrow head size for a less bloated appearance
        double scaledArrowHeadSize = arrowHeadSize * 0.6;

        // Left point
        double x1 = (-0.5 * cos + Math.sqrt(3) / 2 * sin) * scaledArrowHeadSize + endX;
        double y1 = (-0.5 * sin - Math.sqrt(3) / 2 * cos) * scaledArrowHeadSize + endY;
        // Right Point
        double x2 = (0.5 * cos + Math.sqrt(3) / 2 * sin) * scaledArrowHeadSize + endX;
        double y2 = (0.5 * sin - Math.sqrt(3) / 2 * cos) * scaledArrowHeadSize + endY;

        // Close arrow head triangle
        getElements().add(new LineTo(x1, y1));
        getElements().add(new LineTo(x2, y2));
        getElements().add(new LineTo(endX, endY));

        this.isColored = false;
        ColorManagerService.registerInColorPalette(this);
    }

    // Accessors
    @Override
    public double getX() {return Math.min(startX, endX);} // Leftmost point is considered

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        if (setColored)
            setFill(ColorPaletteService.getColor("primary"));
        else
            setFill(ColorPaletteService.getColor("white"));
    }
}
