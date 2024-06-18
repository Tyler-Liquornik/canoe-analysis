package com.wecca.canoeanalysis.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum PhysicalConstants {
    FEET_TO_METRES(0.3048), // conversion factor ft to m
    POUNDS_TO_KG(0.45359237), // conversion factor lb to kg
    GRAVITY(9.80665), // gravity on earth [m/s^2]
    DENSITY_OF_WATER(997); // [kg/m^3]

    private final double value;
}
