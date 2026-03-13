package se.nmodin.blockcar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SearchRequest(
    @JsonProperty("min_price") Integer minPrice,
    @JsonProperty("max_price") Integer maxPrice,
    @JsonProperty("min_year") Integer minYear,
    @JsonProperty("max_year") Integer maxYear,
    @JsonProperty("min_mileage") Integer minMileage,
    @JsonProperty("max_mileage") Integer maxMileage,
    @JsonProperty("max_age_days") Integer maxAgeDays,
    List<String> locations,
    Integer limit,
    Integer page,
    Boolean evaluate
) {
    public SearchRequest {
        // Set defaults
        if (minPrice == null) minPrice = 0;
        if (maxPrice == null) maxPrice = 1000000;
        if (limit == null) limit = 20;
        if (page == null) page = 1;
        if (evaluate == null) evaluate = false;
    }
}
