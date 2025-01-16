package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Bézier tangent point, a circle with a white border
 */
@Getter @Setter
public class PointGraphic extends Group implements Graphic {

    private Circle outerCircle;
    private Circle innerCircle;
    private boolean isColored;
    private double outerRingPercentage = 0.25;

    /**
     * Constructs a point as with two circles, to give the point a "border" with differently colored circles
     *
     * @param radius   The total radius of the outer circle.
     * @param x             The x-coordinate for the center of the circles.
     * @param y             The y-coordinate for the center of the circles.
     */
    public PointGraphic(double x, double y, double radius) {
        // Calculate the radii of the inner and outer circles
        double innerCircleRadius = radius * (1 - outerRingPercentage);

        // Note: +1 shifts graphics misalignment
        outerCircle = new Circle(x, y + 1, radius);
        outerCircle.setFill(ColorPaletteService.getColor("white"));
        innerCircle = new Circle(x, y + 1, innerCircleRadius);
        innerCircle.setFill(ColorPaletteService.getColor("above-surface"));
        this.isColored = false;

        // Set depth and register for color management
        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
        draw();
    }

    // Not required due to simplicity of the graphic
    @Override
    public void draw() {
        getChildren().addAll(outerCircle, innerCircle);
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;
        Color primaryOrAboveSurface = setColored ? ColorPaletteService.getColor("primary")
                : ColorPaletteService.getColor("above-surface");
        innerCircle.setFill(primaryOrAboveSurface);
    }

    @Override
    public double getX() {
        return outerCircle.getCenterX();
    }
}
