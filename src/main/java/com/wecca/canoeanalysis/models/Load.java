package com.wecca.canoeanalysis.models;

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
        @JsonSubTypes.Type(value = UniformDistributedLoad.class, name = "Distributed Load")
})
public abstract class Load{
    String type;
    double mag;
    double x;

    // Scaled on the canoe to the size of the canoe (beam) container in pixels on the GUI
    public double getXScaled(double containerWidth, double canoeLength)
    {
        return (this.x / canoeLength) * containerWidth;
    }
}
