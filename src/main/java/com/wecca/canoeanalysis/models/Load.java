package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode
public abstract class Load {
    @JsonProperty("type")
    protected LoadType type;
    @JsonIgnore
    public abstract double getMaxSignedValue();
    @JsonIgnore
    public abstract double getForce();
    @JsonIgnore
    public abstract double getX();
}
