package com.wecca.canoeanalysis.services;

import javafx.scene.paint.Color;

/**
 * Add transformations when introducing more color variants derived from a primary color
 * This addressed problems with JavaFX CSS limitations with color functions, and the lack of documentation for
 * algorithms that do exist and how they work. This will allow full customization with custom methods.
 */
public class ColorTransformationService {

    static Color lighten(Color color)
    {
        double lightenFactor = 0.4; // Adjust lightness to 0.6/1.0 (1.0 is white)

        double[] hsl = rgbToHsl(color.getRed(), color.getGreen(), color.getBlue());
        hsl[2] = clamp(hsl[2] + (1 - hsl[2]) * lightenFactor);
        return hslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    static Color darkenAndDesaturate(Color color)
    {
        double lightenFactor = 0.33;
        double saturationFactor = 0.12;

        double[] hsl = rgbToHsl(color.getRed(), color.getGreen(), color.getBlue());
        hsl[1] = saturationFactor; // Adjust saturation to 12%
        hsl[2] = lightenFactor; // Adjust lightness to 33%
        return hslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double[] rgbToHsl(double r, double g, double b) {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double h, s, l = (max + min) / 2.0;

        if (max == min) {
            h = s = 0; // achromatic
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }
        return new double[]{h, s, l};
    }

    private static Color hslToRgb(double h, double s, double l) {
        double r, g, b;
        if (s == 0) {
            r = g = b = l; // achromatic
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3.0);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3.0);
        }
        return new Color(r, g, b, 1.0);
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }
}
