package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.utils.SectionPropertyMapEntry;
import lombok.Data;
import lombok.NonNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates physical properties for a hull. This includes the material densities
 * as well as maps describing the wall thickness and bulkhead presence for each section.
 */
@Data
public class HullProperties {

    // These are not actual maps because I could not get the jackson serialization working for the life of me with actual maps
    // Once the new hull model is fully integrated, give it a go if you can figure it out, these should be maps lol
    @JsonProperty("thicknessMap")
    private List<SectionPropertyMapEntry> thicknessMap; // [[x, rx], thickness (mm)]
    @JsonProperty("bulkheadMap")
    private List<SectionPropertyMapEntry> bulkheadMap; // [[x, rx], T/F]

    /**
     * Constructs the HullProperties with the specified parameters.
     * @param thicknessMap a map from Section to wall thickness (in meters)
     * @param bulkheadMap a map from Section to a Boolean indicating if that section is bulkheaded
     */
    @JsonCreator
    public HullProperties(@NonNull @JsonProperty("thicknessMap") List<SectionPropertyMapEntry> thicknessMap,
                          @NonNull @JsonProperty("bulkheadMap") List<SectionPropertyMapEntry> bulkheadMap) {
        this.thicknessMap = thicknessMap;
        this.bulkheadMap = bulkheadMap;
    }

    /**
     * Copy constructor for deep cloning.
     * @param src the source HullProperties to copy
     */
    public HullProperties(@NonNull HullProperties src) {
        this.thicknessMap = src.thicknessMap.stream()
                .map(e -> new SectionPropertyMapEntry(e.getX(), e.getRx(), e.getValue()))
                .collect(Collectors.toList());
        this.bulkheadMap = src.bulkheadMap.stream()
                .map(e -> new SectionPropertyMapEntry(e.getX(), e.getRx(), e.getValue()))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public double getAverageThickness() {
        double totalLength = thicknessMap.stream()
                .mapToDouble(s -> s.getRx() - s.getX())
                .sum();
        if (totalLength == 0) return 0;
        double weightedThickness = thicknessMap.stream()
                .mapToDouble(s -> Double.parseDouble(s.getValue()) * (s.getRx() - s.getX()))
                .sum();
        return weightedThickness / totalLength;
    }
}
