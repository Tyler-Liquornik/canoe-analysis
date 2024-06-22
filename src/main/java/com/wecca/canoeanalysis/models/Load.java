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
public abstract class Load {
    protected String type;
    protected double mag;
    protected double x;
}
