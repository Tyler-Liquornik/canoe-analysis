package com.wecca.canoeanalysis.components.graphics;
import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

// Pinned support icons
@Getter @Setter
public class SupportTriangle extends Group implements Graphic
{
    // Fields
    private static final double defaultSideLength = 20.0;
    private double tipX;
    private double tipY;

    private Polygon triangle;
    private Line baseLine;

    private boolean isColored;

    // Constructor
    public SupportTriangle(double tipX, double tipY, double sideLength)
    {
        super();

        this.tipX = tipX;
        this.tipY = tipY;

        makeTriangle(tipX, tipY, sideLength);
    }

    // Simpler constructor with default side length
    public SupportTriangle(double tipX, double tipY)
    {
        this(tipX, tipY, defaultSideLength);
    }

    // Accessors
    @Override
    public double getX() {return getTipX();}

    // Create the triangle
    private void makeTriangle(double tipX, double tipY, double sideLength)
    {
        // Bottom left point and bottom right points on the triangle (they are on the same y coordinate)
        double x1 = -0.5 * sideLength + tipX;
        double x2 = 0.5 * sideLength + tipX;
        double y = (Math.sqrt(3) / 2) * sideLength + tipY;

        // Create the triangle polygon
        Polygon triangle = new Polygon();
        triangle.setStroke(ColorPaletteService.getColor("white"));
        triangle.getPoints().addAll(
                tipX, tipY,
                x1, y,
                x2, y
        );

        triangle.setFill(ColorPaletteService.getColor("above-surface")); // hard coded for now as lightening looks weird for white arrows

        // Create the lines sticking out
        Line baseLine = new Line(x1 - sideLength / 2, y, x2 + sideLength / 2, y);
        baseLine.setStroke(ColorPaletteService.getColor("white"));

        // Set fields
        this.triangle = triangle;
        this.baseLine = baseLine;

        // Add the shapes to the group
        this.getChildren().addAll(triangle, baseLine);

        JFXDepthManager.setDepth(this, 5);

        this.isColored = false;
        ColorManagerService.registerInColorPalette(this);
    }

    // Change the color of the support triangle
    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color outlineColor = setColored ? ColorPaletteService.getColor("primary") :
                ColorPaletteService.getColor("white");
        Color fillColor = setColored ? ColorPaletteService.getColor("primary-light") :
                ColorPaletteService.getColor("above-surface");

        baseLine.setStroke(outlineColor);
        triangle.setStroke(outlineColor);
        triangle.setFill(fillColor);
    }
}
