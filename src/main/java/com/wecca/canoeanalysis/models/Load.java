package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PointLoad.class, name = "Point Load"),
        @JsonSubTypes.Type(value = UniformLoadDistribution.class, name = "Simple Load Distribution"),
        @JsonSubTypes.Type(value = PiecewiseContinuousLoadDistribution.class, name = "Complex Load Distribution")
})
public abstract class Load {
    protected String type;
    @JsonIgnore
    public double getMaxSignedValue() {return getForce();}
    @JsonIgnore
    public abstract double getForce();
    @JsonIgnore
    public abstract double getX();
}
