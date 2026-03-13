package se.nmodin.blockcar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import se.nmodin.blockcar.model.CarListing;
import se.nmodin.blockcar.model.Location;
import se.nmodin.blockcar.model.SearchRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BlocketService {

    private static final Logger LOG = Logger.getLogger(BlocketService.class);
    private static final String BLOCKET_API_URL = "https://www.blocket.se/mobility/search/api/search/SEARCH_ID_CAR_USED";
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<CarListing> searchCars(SearchRequest request) {
        LOG.info("Searching for cars with filters: " + request);

        try {
            Client client = ClientBuilder.newClient();

            // Build the target with query parameters
            var target = client.target(BLOCKET_API_URL)
                .queryParam("sort", "PUBLISHED_DESC")
                .queryParam("page", request.page());

            // Add price filters
            if (request.minPrice() != null && request.minPrice() > 0) {
                target = target.queryParam("price_from", request.minPrice());
            }
            if (request.maxPrice() != null && request.maxPrice() < 1000000) {
                target = target.queryParam("price_to", request.maxPrice());
            }

            // Add year filters
            if (request.minYear() != null) {
                target = target.queryParam("year_from", request.minYear());
            }
            if (request.maxYear() != null) {
                target = target.queryParam("year_to", request.maxYear());
            }

            // Add mileage filters (note: API uses "milage" not "mileage")
            if (request.minMileage() != null) {
                target = target.queryParam("milage_from", request.minMileage());
            }
            if (request.maxMileage() != null) {
                target = target.queryParam("milage_to", request.maxMileage());
            }

            // Add location filters
            if (request.locations() != null && !request.locations().isEmpty()) {
                for (String locationName : request.locations()) {
                    Location location = Location.fromName(locationName);
                    if (location != null) {
                        target = target.queryParam("location", location.getCode());
                    }
                }
            }

            String url = target.getUri().toString();
            LOG.info("Calling Blocket API: " + url);

            // Make the API call with User-Agent header
            String responseJson = target
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent", USER_AGENT)
                .get(String.class);

            // Parse response
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            List<CarListing> listings = parseBlocketResponse(response);

            // Apply max age filter if specified
            if (request.maxAgeDays() != null && request.maxAgeDays() > 0) {
                Instant cutoffDate = Instant.now().minus(request.maxAgeDays(), ChronoUnit.DAYS);
                listings = listings.stream()
                    .filter(car -> {
                        if (car.publishedDate() == null) return true;
                        try {
                            // Parse timestamp from published_date
                            long timestamp = Long.parseLong(car.publishedDate());
                            Instant publishedInstant = Instant.ofEpochMilli(timestamp);
                            return publishedInstant.isAfter(cutoffDate);
                        } catch (Exception e) {
                            return true; // Include if we can't parse
                        }
                    })
                    .toList();
            }

            // Limit results
            int limit = request.limit() != null ? request.limit() : 20;
            if (listings.size() > limit) {
                listings = listings.subList(0, limit);
            }

            LOG.info("Found " + listings.size() + " cars");
            return listings;

        } catch (Exception e) {
            LOG.error("Error searching cars from Blocket: " + e.getMessage(), e);
            LOG.warn("Falling back to demo data");
            return getDemoListings();
        }
    }

    @SuppressWarnings("unchecked")
    private List<CarListing> parseBlocketResponse(Map<String, Object> response) {
        List<CarListing> listings = new ArrayList<>();

        // The response has a "docs" array containing the ad listings
        List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");
        if (docs == null) {
            LOG.warn("No 'docs' found in response");
            return listings;
        }

        for (Map<String, Object> ad : docs) {
            try {
                // Extract ad ID
                Object adIdObj = ad.get("ad_id");
                if (adIdObj == null) {
                    adIdObj = ad.get("id");
                }
                String adId = adIdObj != null ? adIdObj.toString() : "unknown";

                // Extract title/heading
                String title = getStringValue(ad, "heading");
                if (title == null) {
                    title = getStringValue(ad, "subject");
                }

                // Extract price
                Integer price = 0;
                Object priceObj = ad.get("price");
                if (priceObj instanceof Map) {
                    Map<String, Object> priceMap = (Map<String, Object>) priceObj;
                    price = getIntValue(priceMap, "amount");
                }

                // Extract image URL
                String imageUrl = null;
                Object imageObj = ad.get("image");
                if (imageObj instanceof Map) {
                    Map<String, Object> imageMap = (Map<String, Object>) imageObj;
                    imageUrl = getStringValue(imageMap, "url");
                }

                // Extract other fields
                Integer year = getIntValue(ad, "year");
                Integer mileage = getIntValue(ad, "mileage");
                String fuelType = getStringValue(ad, "fuel");
                String transmission = getStringValue(ad, "transmission");
                String location = getStringValue(ad, "location");

                // Determine seller type
                String orgName = getStringValue(ad, "organisation_name");
                String sellerType = orgName != null ? "Återförsäljare (" + orgName + ")" : "Privatperson";

                // Extract URL
                String url = getStringValue(ad, "canonical_url");
                if (url == null) {
                    url = "https://www.blocket.se/mobility/item/" + adId;
                }

                // Extract timestamp
                Long timestamp = getLongValue(ad, "timestamp");
                String publishedDate = timestamp != null ? formatTimestamp(timestamp) : null;

                // Extract brand and model from title
                String brand = "";
                String model = "";
                if (title != null && !title.isEmpty()) {
                    String[] parts = title.split(" ", 2);
                    if (parts.length > 0) {
                        brand = parts[0];
                    }
                    if (parts.length > 1) {
                        model = parts[1];
                    }
                }

                CarListing listing = new CarListing(
                    adId,
                    title,
                    url,
                    imageUrl,
                    price,
                    year,
                    mileage,
                    location,
                    brand,
                    model,
                    fuelType,
                    transmission,
                    sellerType,
                    null, // description not in search results
                    publishedDate
                );

                listings.add(listing);

            } catch (Exception e) {
                LOG.warn("Error parsing ad: " + e.getMessage());
            }
        }

        return listings;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private List<CarListing> getDemoListings() {
        // Demo data for fallback
        return List.of(
            new CarListing(
                "demo1",
                "Volvo V70 2.5T - Välvårdad familjebil",
                "https://www.blocket.se/annons/demo1",
                "https://via.placeholder.com/300x200",
                85000,
                2015,
                15000,
                "Stockholm",
                "Volvo",
                "V70",
                "Bensin",
                "Automat",
                "Privatperson",
                "Välskött Volvo V70 med fullständig servicehistorik. Nya bromsar och sommardäck.",
                "2024-03-10"
            ),
            new CarListing(
                "demo2",
                "Toyota Auris Hybrid - Miljövänlig och ekonomisk",
                "https://www.blocket.se/annons/demo2",
                "https://via.placeholder.com/300x200",
                125000,
                2018,
                8500,
                "Göteborg",
                "Toyota",
                "Auris",
                "Hybrid",
                "Automat",
                "Återförsäljare",
                "Sparsam Toyota Auris Hybrid med Toyota garanti. Bra skick och låg förbrukning.",
                "2024-03-09"
            ),
            new CarListing(
                "demo3",
                "BMW 320d Touring - Sportig och bekväm",
                "https://www.blocket.se/annons/demo3",
                "https://via.placeholder.com/300x200",
                195000,
                2019,
                6200,
                "Malmö",
                "BMW",
                "320d",
                "Diesel",
                "Automat",
                "Återförsäljare",
                "BMW 320d Touring med M-sportpaket. Utrustad med navigation, skinn och panoramatak.",
                "2024-03-08"
            )
        );
    }
}
