package se.nmodin.blockcar.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CarListing(
    String id,
    String title,
    String url,
    @JsonProperty("image_url") String imageUrl,
    Integer price,
    Integer year,
    Integer mileage,
    String location,
    String brand,
    String model,
    @JsonProperty("fuel_type") String fuelType,
    String transmission,
    @JsonProperty("seller_type") String sellerType,
    String description,
    @JsonProperty("published_date") String publishedDate
) {
}
