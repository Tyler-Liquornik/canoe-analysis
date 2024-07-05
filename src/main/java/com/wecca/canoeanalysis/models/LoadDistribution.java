package com.wecca.canoeanalysis.models;

public abstract class LoadDistribution extends Load {
    public LoadDistribution(String type) {
        super(type);
    }
    public abstract Section getSection();
}
