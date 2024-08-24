package com.wecca.canoeanalysis.models.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DevConfig {
    @JsonProperty("tracing")
    private boolean tracing;

    @JsonCreator
    public DevConfig(@JsonProperty("tracing") boolean tracing) {
        this.tracing = tracing;
    }
}
