package se.nmodin.blockcar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import se.nmodin.blockcar.model.CarListing;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ClaudeService {

    private static final Logger LOG = Logger.getLogger(ClaudeService.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL_NAME = "claude-sonnet-4-20250514";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ClaudeService(@ConfigProperty(name = "anthropic.api-key") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    public String evaluateListings(List<CarListing> listings) {
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("Anthropic API key not configured");
            return "⚠️ Claude evaluation unavailable - API key not configured. Set ANTHROPIC_API_KEY environment variable.";
        }

        try {
            Client client = ClientBuilder.newClient();
            String prompt = createPrompt(listings);

            LOG.info("Sending evaluation request to Claude with " + listings.size() + " listings");

            // Build request body using Jackson
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("max_tokens", 4096);

            // Add messages array
            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", prompt);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            LOG.debug("Request JSON: " + requestJson);

            // Make API call
            String responseJson = client.target(ANTHROPIC_API_URL)
                .request(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .post(Entity.json(requestJson), String.class);

            LOG.debug("Response JSON: " + responseJson);

            // Parse response
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

            // Extract text from content array
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

            if (content != null && !content.isEmpty()) {
                StringBuilder result = new StringBuilder();
                for (Map<String, Object> block : content) {
                    if ("text".equals(block.get("type"))) {
                        result.append(block.get("text"));
                    }
                }
                return result.toString();
            }

            return "❌ No response from Claude";

        } catch (Exception e) {
            LOG.error("Error calling Claude API: " + e.getMessage(), e);
            return "❌ Error evaluating listings: " + e.getMessage();
        }
    }

    private String createPrompt(List<CarListing> listings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Du är en expert på att bedöma begagnade bilar på den svenska marknaden. ");
        prompt.append("Analysera följande bilannonser från Blocket och ge en utvärdering.\n\n");
        prompt.append("# Bilar att utvärdera:\n\n");

        int index = 1;
        for (CarListing car : listings) {
            prompt.append(String.format("## Bil %d: %s\n", index++, car.title()));
            prompt.append(String.format("- **Pris:** %,d kr\n", car.price()));
            prompt.append(String.format("- **Årsmodell:** %d\n", car.year()));
            prompt.append(String.format("- **Miltal:** %,d mil\n", car.mileage()));
            prompt.append(String.format("- **Märke/Modell:** %s %s\n", car.brand(), car.model()));
            prompt.append(String.format("- **Bränsle:** %s\n", car.fuelType()));
            prompt.append(String.format("- **Växellåda:** %s\n", car.transmission()));
            prompt.append(String.format("- **Säljare:** %s\n", car.sellerType()));
            prompt.append(String.format("- **Plats:** %s\n", car.location()));
            prompt.append(String.format("- **URL:** %s\n", car.url()));
            if (car.description() != null && !car.description().isBlank()) {
                prompt.append(String.format("- **Beskrivning:** %s\n", car.description()));
            }
            prompt.append("\n");
        }

        prompt.append("# Uppdrag:\n\n");
        prompt.append("1. Rangordna de tre bästa bilarna baserat på pris/värde-förhållande\n");
        prompt.append("2. För varje bil, ge ett betyg på pris/värde mellan 1-5 stjärnor\n");
        prompt.append("3. Identifiera eventuella röda flaggor (ex. högt miltal för årsmodell, ovanligt lågt pris)\n");
        prompt.append("4. Ge specifika frågor att ställa till säljaren för varje bil\n");
        prompt.append("5. Ge en sammanfattande rekommendation\n\n");
        prompt.append("Svara på svenska och använd markdown-formatering för tydlighet.");

        return prompt.toString();
    }
}
