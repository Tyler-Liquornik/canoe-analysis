package com.wecca.canoeanalysis.services.color;

import com.wecca.canoeanalysis.utils.ColorUtils;
import javafx.scene.paint.Color;

/**
 * <p>
 * ColorTransformationService dynamically updates with Java based CSS functions enabled by the reflection API.
 * Styles are accessible from ColorPalette for in-Java styling and style.css for in-CSS styling.
 * </p>
 * The goal of this is to overcome the issue of JavaFX CSS lacking functionality with color functions,
 * which combined with the lack of documentation makes dealing with CSS colors a challenging task
 * <p>
 * Usage:
 * </p>
 * <p>
 * 1. Define a marker for the base color variable in the universal CSS selector * {}
 *    - Examples:
 *     * {
 *         -fx-base: #000000 /* This is a marker, any valid hex color can go here
 *         -fx-primary: #123ABC /* This is a marker, any valid hex color can go here
 *         -fx-secondary: #FFFFFF /* This is a marker, any valid hex color can go here
 *       }
 * </p>
 * <p>
 * 2. Define any number color of transformation methods in ColorTransformationService for a given base:
 *    - Methods public static and named in the format "base_transformation".
 *    - These are your custom defined methods to derive new colors in the palette from the base
 *    - Examples:
 *      public static Color base_transformation(Color color) { ... }
 *      public static Color primary_light(Color color) { ... }
 *      public static Color secondary_complement(Color color) { ... }
 *
 *    - Note that each of base, primary, or secondary (any base) could have many transformations each
 *    - No transformations need to be specified if you only want to dynamically change one color (the base) with no associated palette
 * </p>
 * <p>
 * 3. Use ColorManagerService to derive colors:
 *    - deriveColors will scan ColorTransformationService for all transformation methods
 *      containing the specified base, and generate derived colors
 *    - Examples:
 *      ColorManagerService.addColorPalette("base", "#BB86FC");
 *      ColorManagerService.addColorPalette("primary", "#4169E1");
 *      ColorManagerService.addColorPalette("secondary", "#5CE47B");
 * </p>
 * <p>
 * 4. Access generated color
 *    - You can access colors generated at build time via CSS variables (check the built CSS file, it should have the generated colors)
 *    - Result:
 *    * {
 *            -fx-base: #BB86FC
 *            -fx-base-transformed: TRANSFORMED COLOR
 *            -fx-primary: #4169E1
 *            -fx-primary-light: LIGHTENED COLOR
 *            -fx-secondary: #5CE47B
 *            -fx-secondary-complement: COMPLEMENTED COLOR
 *      }
 *    - You can also access colors in your Java code through ColorPalette
 *    - Result:
 *      Color baseTransform = ColorPalette.getColor("base-transform");
 *      Color primaryLight = ColorPalette.getColor("primary-light");
 *      Color secondaryComplement = ColorPalette.getColor("secondary-complement")
 * </p>
 */
public class ColorTransformationService {

    public static Color primary_light(Color color)
    {
        double lightenFactor = 0.4;
        double saturationFactor = 0.8;

        double[] hsl = ColorUtils.rgbToHsl(color.getRed(), color.getGreen(), color.getBlue());
        hsl[1] = saturationFactor;
        hsl[2] = ColorUtils.clamp(hsl[2] + (1 - hsl[2]) * lightenFactor);
        return ColorUtils.hslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    public static Color primary_desaturated(Color color)
    {
        double lightenFactor = 0.33;
        double saturationFactor = 0.12;

        double[] hsl = ColorUtils.rgbToHsl(color.getRed(), color.getGreen(), color.getBlue());
        hsl[1] = saturationFactor; // Adjust saturation to 12%
        hsl[2] = lightenFactor; // Adjust lightness to 33%
        return ColorUtils.hslToRgb(hsl[0], hsl[1], hsl[2]);
    }
}
