package se.nmodin.blockcar.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.OutboundSseEvent;
import org.jboss.logging.Logger;
import se.nmodin.blockcar.model.CarListing;
import se.nmodin.blockcar.model.SearchRequest;
import se.nmodin.blockcar.model.SearchResponse;
import se.nmodin.blockcar.service.BlocketService;
import se.nmodin.blockcar.service.ClaudeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Path("/api/cars")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarSearchResource {

    private static final Logger LOG = Logger.getLogger(CarSearchResource.class);

    private final BlocketService blocketService;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public CarSearchResource(BlocketService blocketService, ClaudeService claudeService, ObjectMapper objectMapper) {
        this.blocketService = blocketService;
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
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

    @POST
    @Path("/search-stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Consumes(MediaType.APPLICATION_JSON)
    public void searchCarsStream(SearchRequest request, @Context SseEventSink eventSink, @Context Sse sse) {
        new Thread(() -> {
            try {
                LOG.info("Received SSE search request: " + request);

                // Send initial status
                sendEvent(eventSink, sse, "status", "Söker efter bilar...");

                // Search for cars
                List<CarListing> listings = blocketService.searchCars(request);

                // Send status update after fetching cars
                if (request.evaluate() && !listings.isEmpty()) {
                    sendEvent(eventSink, sse, "status", "Analyserar...");
                }

                // Evaluate with Claude if requested
                String evaluation = null;
                if (request.evaluate() && !listings.isEmpty()) {
                    LOG.info("Evaluating listings with Claude");
                    evaluation = claudeService.evaluateListings(listings);
                }

                // Send final result
                SearchResponse response = new SearchResponse(listings, listings.size(), evaluation);
                sendEvent(eventSink, sse, "result", objectMapper.writeValueAsString(response));

                // Send completion event
                sendEvent(eventSink, sse, "done", "");

            } catch (Exception e) {
                LOG.error("Error in SSE search", e);
                sendEvent(eventSink, sse, "error", e.getMessage());
            } finally {
                eventSink.close();
            }
        }).start();
    }

    private void sendEvent(SseEventSink eventSink, Sse sse, String name, String data) {
        OutboundSseEvent event = sse.newEventBuilder()
                .name(name)
                .data(data)
                .build();

        CompletionStage<?> stage = eventSink.send(event);

        // Wait for the event to be sent before continuing
        try {
            stage.toCompletableFuture().get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("Failed to send SSE event: " + name, e);
        }
    }
}
