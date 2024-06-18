package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.utils.MathUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.function.Function;

/**
 * Represents a section of the canoe with start and end positions, width, and bulkhead properties.
 */
@Getter @Setter @NoArgsConstructor
public class CanoeSection {
    private double start;
    private double end;
    private double width; // Width of the section in the z-axis
    private double bulkheadVolume; // Volume of the styrofoam bulkhead; change to hasBulkead at fill inside walls auto (need hullThickness too will be at the section level)
    private Function<Double, Double> hullShapeFunction;

    /**
     * Constructs a CanoeSection with specified dimensions and bulkhead properties.
     *
     * @param start the start position of the section in meters
     * @param end the end position of the section in meters
     * @param width the width of the section in meters
     * @param bulkheadVolume the volume of the styrofoam bulkhead in cubic meters
     * @param hullShapeFunction the function that defines the shape of the hull in this section
     */
    public CanoeSection(double start, double end, double width, double bulkheadVolume, Function<Double, Double> hullShapeFunction) {
        this.start = start;
        this.end = end;
        this.width = width;
        this.bulkheadVolume = bulkheadVolume;
        this.hullShapeFunction = hullShapeFunction;
    }

    public double getLength() {
        return end - start;
    }

    /**
     * Calculates the depth of the section based on the hull shape function.
     *
     * @return the depth of the section in meters
     */
    public double getDepth() {
        double maxDepth = 0;
        double stepSize = (getLength()) / 1000;
        for (double x = start; x <= end; x += stepSize) {
            double depth = hullShapeFunction.apply(x);
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        return maxDepth;
    }

    /**
     * Calculates the overall density of the section considering the bulkhead and concrete densities.
     *
     * @param concreteDensity the density of the concrete in kg/m^3
     * @param bulkheadDensity the density of the styrofoam bulkhead in kg/m^3
     * @return the overall density of the section in kg/m^3
     */
    public double getOverallDensity(double concreteDensity, double bulkheadDensity) {
        double canoeVolume = getVolume(); // Approximate volume (integral of the hull curve)
        return (canoeVolume * concreteDensity + bulkheadVolume * bulkheadDensity) / getLength();
    }

    /**
     * Calculates the approximate volume of the section excluding bulkhead volume.
     *
     * @return the volume of the section in cubic meters
     */
    public double getVolume() {
        double area = MathUtils.integrate(hullShapeFunction, start, end); // Integrate hull shape over the section
        return area * width; // Volume is area times width
    }
}