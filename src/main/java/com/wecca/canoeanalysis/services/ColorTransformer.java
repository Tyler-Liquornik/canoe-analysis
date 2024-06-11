package com.wecca.canoeanalysis.services;

import javafx.scene.paint.Color;

@FunctionalInterface
public interface ColorTransformer {
    Color transform(Color color);

    static Color transform(Color color, ColorTransformer transformer) {
        return transformer.transform(color);
    }
}