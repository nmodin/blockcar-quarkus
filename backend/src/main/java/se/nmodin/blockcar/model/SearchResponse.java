package se.nmodin.blockcar.model;

import java.util.List;

public record SearchResponse(
    List<CarListing> listings,
    Integer count,
    String evaluation
) {
}
