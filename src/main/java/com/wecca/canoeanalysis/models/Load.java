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
        @JsonSubTypes.Type(value = PointLoad.class, names = {"Point Load"}),
        @JsonSubTypes.Type(value = UniformLoadDistribution.class, name = "Distributed Load")
})
public abstract class Load {
    protected LoadType type;
    @JsonIgnore
    public double getMaxSignedValue() {return getForce();}
    @JsonIgnore
    public abstract double getForce();
    @JsonIgnore
    public abstract double getX();
}
