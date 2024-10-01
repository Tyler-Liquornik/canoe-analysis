package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.FunctionSection;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @EqualsAndHashCode(callSuper = true)
public abstract class LoadDistribution extends Load {
    @JsonProperty("section")
    protected FunctionSection section;
    public LoadDistribution(LoadType type, FunctionSection section) {
        super(type);
        this.section = section;
    }
}
