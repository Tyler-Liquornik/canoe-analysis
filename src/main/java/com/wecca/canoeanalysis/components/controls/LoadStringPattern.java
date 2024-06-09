package com.wecca.canoeanalysis.components.controls;

import lombok.Getter;

@Getter
public enum LoadStringPattern {

    // Likely need to tweak these hard to get right
    POINT_LOAD("Load:\\s*[-+]?[0-9]*\\.?[0-9]+kN,\\s*[-+]?[0-9]*\\.?[0-9]+m", "Load: 0.00kN, 0.00m"),
    SUPPORT("Support:\\s*[-+]?[0-9]*\\.?[0-9]+kN,\\s*[-+]?[0-9]*\\.?[0-9]+m", "Support: 0.00kN, 0.00m"),
    DISTRIBUTED_LOAD("Load:\\s*[-+]?[0-9]*\\.?[0-9]+kN\\/m,\\s*\\[[-+]?[0-9]*\\.?[0-9]+m,\\s*[-+]?[0-9]*\\.?[0-9]+m\\]", "Load: 0.00kN, [0.00m, 0.00m]");

    private final String pattern;
    private final String template;

    LoadStringPattern(String pattern, String template) {
        this.pattern = pattern;
        this.template = template;
    }
}