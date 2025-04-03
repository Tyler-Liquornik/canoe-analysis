package com.wecca.canoeanalysis.models.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DevConfig {
    @JsonProperty("tracing")
    private boolean tracing;
    @JsonProperty("debouncing")
    private boolean debouncing;

    @JsonCreator
    public DevConfig(@JsonProperty("tracing") boolean tracing, @JsonProperty("debouncing") boolean debouncing) {
        this.tracing = tracing;
        this.debouncing = debouncing;
    }
}
