package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Adds an extra property with type information
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = VertexFormParabola.class, name = "VertexFormParabola")
})
public interface TypedUnivariateFunction extends UnivariateFunction {
    @JsonProperty("type")
    UnivariateFunctionType getType();
}
