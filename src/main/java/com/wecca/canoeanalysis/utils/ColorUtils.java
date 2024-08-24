package com.wecca.canoeanalysis.utils;

import javafx.scene.paint.Color;

public class ColorUtils {

    /**
     * Clamps a given value to ensure it lies within the range [0, 1].
     * If the value is less than 0, it returns 0. If the value is greater than 1, it returns 1.
     * Otherwise, it returns the value itself.
     *
     * @param value The input value to be clamped.
     * @return The clamped value, guaranteed to be within the range [0, 1].
     */
    public static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Converts a color from RGB (Red, Green, Blue) format to HSL (Hue, Saturation, Lightness) format.
     *
     * @param r The red component of the color, in the range [0, 1].
     * @param g The green component of the color, in the range [0, 1].
     * @param b The blue component of the color, in the range [0, 1].
     * @return A double array containing the HSL components:
     *         - H: (Hue) in the range [0, 1], where 0 represents 0 degrees (red)
     *           and 1 represents 360 degrees.
     *         - S:  (Saturation) in the range [0, 1], where 0 is gray (no color)
     *           and 1 is fully saturated.
     *         - L:  (Lightness) in the range [0, 1], where 0 is black and 1 is white.
     */
    public static double[] rgbToHsl(double r, double g, double b) {
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

    /**
     * Converts a color from HSL (Hue, Saturation, Lightness) format to RGB format.
     *
     * @param h The hue of the color, a value between 0 and 1, where 0 represents 0 degrees
     *          on the color wheel (red) and 1 represents a complete 360-degree rotation.
     * @param s The saturation of the color, a value between 0 (gray) and 1 (fully saturated).
     * @param l The lightness of the color, a value between 0 (black) and 1 (white).
     * @return A Color object in RGB format with the red, green, and blue components
     *         in the range [0, 1] and full opacity (alpha = 1.0).
     */
    public static Color hslToRgb(double h, double s, double l) {
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

    /**
     * Converts a hue value to an RGB component value.
     * Used in the conversion process from HSL (Hue, Saturation, Lightness) to RGB (Red, Green, Blue).
     *
     * @param p The first temporary value used in the RGB calculation (related to lightness).
     * @param q The second temporary value used in the RGB calculation (related to chroma and lightness).
     * @param t The hue value, typically in the range [0, 1], that determines which color component to calculate.
     *          If the value of t is outside this range, it is adjusted by adding or subtracting 1.
     * @return A double representing the RGB component value, in the range [0, 1].
     *         The value can represent either Red, Green, or Blue depending on the input parameters.
     */
    public static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }

    /**
     * Convert a color object with RGB values to a hex representation as a string.
     *
     * @param color the color object to convert
     * @return the color in hex format as a string
     */
    public static String colorToHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Converts an integer representation of a color (ARGB/RGB) to a JavaFX Color object.
     * @param colorInt the integer representing the color in RGB format
     * @return the corresponding Color object
     */
    public static Color colorFromInteger(int colorInt) {
        int red = (colorInt >> 16) & 0xFF;
        int green = (colorInt >> 8) & 0xFF;
        int blue = colorInt & 0xFF;

        // Normalize to [0, 1] as JavaFX Color uses double values for RGB
        return Color.rgb(red, green, blue);
    }

    /**
     * @param color the original color
     * @param opacity the new opacity (between 0.0 and 1.0)
     * @return a new Color object with the same RGB values and updated opacity
     */
    public static Color withOpacity(Color color, double opacity) {
        // Ensure the opacity is within the valid range
        opacity = clamp(opacity);
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                opacity
        );
    }
}
