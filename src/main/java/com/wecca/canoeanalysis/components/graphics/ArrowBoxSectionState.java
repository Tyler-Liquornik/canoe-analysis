package com.wecca.canoeanalysis.components.graphics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Used for load distributions
 */
@Getter @AllArgsConstructor
public enum ArrowBoxSectionState
{
    NON_SECTIONED(true, true),
    MIDDLE_SECTION(false, false),
    LEFT_END_SECTION(true, false),
    RIGHT_END_SECTION(false, true);

    private final boolean showLArrow;
    private final boolean showRArrow;
}
