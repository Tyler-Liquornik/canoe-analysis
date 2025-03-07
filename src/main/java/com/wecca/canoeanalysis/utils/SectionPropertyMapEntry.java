package com.wecca.canoeanalysis.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.Section;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a Section with an associated property.
 * Represents a flattened a Map.Entry<Section, T> into a list of 3 fields.
 * This was the only was to get jackson to be able to serialize and deserialize a map cleanly
 * by using List<SectionWithProperty<T>>
 */
@Data @EqualsAndHashCode(callSuper = true)
public class SectionPropertyMapEntry extends Section {

    @JsonProperty("value")
    private String value;

    @JsonCreator
    public SectionPropertyMapEntry(@JsonProperty("x") double x, @JsonProperty("rx") double rx, @JsonProperty("value") String value) {
        super(x, rx);
        this.value = value;
    }
}
