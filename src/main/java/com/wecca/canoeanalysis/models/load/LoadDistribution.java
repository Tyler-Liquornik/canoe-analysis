package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.Section;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @EqualsAndHashCode(callSuper = true)
public abstract class LoadDistribution extends Load {
    @JsonProperty("section")
    protected Section section;
    public LoadDistribution(LoadType type, Section section) {
        super(type);
        this.section = section;
    }
}
