package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PointLoad.class, name = "PointLoad"),
        @JsonSubTypes.Type(value = UniformLoadDistribution.class, name = "UniformLoadDistribution"),
})
public abstract class Load {
    @JsonProperty("type")
    protected LoadType type;
    @JsonIgnore
    public abstract double getMaxSignedValue();
    @JsonIgnore
    public abstract double getMoment(double rotationX);
    @JsonIgnore
    public abstract double getForce();
    @JsonIgnore
    public abstract double getX();
}
