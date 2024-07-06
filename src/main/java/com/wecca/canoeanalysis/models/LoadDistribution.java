package com.wecca.canoeanalysis.models;

public abstract class LoadDistribution extends Load {
    public LoadDistribution(LoadType type) {
        super(type);
    }
    public abstract Section getSection();
}
