package com.wecca.canoeanalysis.models;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public abstract class LoadDistribution extends Load {
    public LoadDistribution(LoadType type) {
        super(type);
    }
    public abstract Section getSection();
}
