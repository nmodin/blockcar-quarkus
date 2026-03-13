package se.nmodin.blockcar.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import se.nmodin.blockcar.model.CarListing;
import se.nmodin.blockcar.model.SearchRequest;
import se.nmodin.blockcar.model.SearchResponse;
import se.nmodin.blockcar.service.BlocketService;
import se.nmodin.blockcar.service.ClaudeService;

import java.util.List;

@Path("/api/cars")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarSearchResource {

    private static final Logger LOG = Logger.getLogger(CarSearchResource.class);

    private final BlocketService blocketService;
    private final ClaudeService claudeService;

    public CarSearchResource(BlocketService blocketService, ClaudeService claudeService) {
        this.blocketService = blocketService;
        this.claudeService = claudeService;
    }

    @POST
    @Path("/search")
    public SearchResponse searchCars(SearchRequest request) {
        LOG.info("Received search request: " + request);

        // Search for cars
        List<CarListing> listings = blocketService.searchCars(request);

        // Evaluate with Claude if requested
        String evaluation = null;
        if (request.evaluate() && !listings.isEmpty()) {
            LOG.info("Evaluating listings with Claude");
            evaluation = claudeService.evaluateListings(listings);
        }

        return new SearchResponse(listings, listings.size(), evaluation);
    }

    @GET
    @Path("/health")
    public String health() {
        return "{\"status\": \"ok\"}";
    }
}
