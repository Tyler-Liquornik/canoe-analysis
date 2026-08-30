package com.wecca.canoeanalysis.models.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Settings {
    @JsonProperty("primaryColor")
    private String primaryColor;

    @JsonCreator
    public Settings(@JsonProperty("primaryColor") String primaryColor) {
        this.primaryColor = primaryColor;
    }
}
