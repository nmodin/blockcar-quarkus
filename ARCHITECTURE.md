# Blockcar Quarkus - Architecture

## Project Structure

```
blockcar-quarkus/
├── backend/                    # Quarkus backend module
│   └── src/main/java/se/nmodin/blockcar/
│       ├── model/             # Data models
│       │   ├── CarListing.java
│       │   ├── Location.java
│       │   ├── SearchRequest.java
│       │   └── SearchResponse.java
│       ├── resource/          # REST endpoints
│       │   └── CarSearchResource.java
│       └── service/           # Business logic
│           ├── BlocketService.java
│           └── ClaudeService.java
└── frontend/                   # Vue 3 frontend module
    └── src/
        ├── App.vue            # Main component
        ├── main.js
        └── style.css
```

## Dependency Injection

This project uses **constructor injection** throughout, which is the recommended approach in Quarkus/CDI for:
- Better testability
- Immutability (final fields)
- Explicit dependencies
- Null-safety

### Examples

**REST Resource:**
```java
@Path("/api/cars")
public class CarSearchResource {
    private final BlocketService blocketService;
    private final ClaudeService claudeService;

    public CarSearchResource(BlocketService blocketService, ClaudeService claudeService) {
        this.blocketService = blocketService;
        this.claudeService = claudeService;
    }
}
```

**Service with Configuration:**
```java
@ApplicationScoped
public class ClaudeService {
    private final String apiKey;

    public ClaudeService(@ConfigProperty(name = "anthropic.api-key") String apiKey) {
        this.apiKey = apiKey;
    }
}
```

## API Integrations

### Blocket API

**Implementation:** Direct HTTP calls using JAX-RS Client

```java
Client client = ClientBuilder.newClient();
var target = client.target(BLOCKET_API_URL)
    .queryParam("price_from", minPrice)
    .queryParam("location", locationCode);

String response = target
    .request(MediaType.APPLICATION_JSON)
    .header("User-Agent", USER_AGENT)
    .get(String.class);
```

**Response Parsing:**
- Uses Jackson ObjectMapper to parse JSON
- Extracts data from nested structures (`price.amount`, `image.url`)
- Maps Swedish county names to API location codes
- Falls back to demo data on errors

### Claude API

**Implementation:** Direct HTTP calls using JAX-RS Client (no SDK)

```java
ObjectNode requestBody = objectMapper.createObjectNode();
requestBody.put("model", MODEL_NAME);
requestBody.put("max_tokens", 4096);

ArrayNode messages = requestBody.putArray("messages");
ObjectNode message = messages.addObject();
message.put("role", "user");
message.put("content", prompt);

String responseJson = client.target(ANTHROPIC_API_URL)
    .request(MediaType.APPLICATION_JSON)
    .header("x-api-key", apiKey)
    .header("anthropic-version", ANTHROPIC_VERSION)
    .post(Entity.json(requestBody), String.class);
```

**Benefits of Direct API Calls:**
- No SDK version conflicts
- Full control over requests/responses
- Easier to debug
- Smaller dependency footprint
- Better error handling

## Build Process

1. **Frontend Build** (Maven frontend-plugin):
   - Installs Node.js 20.11.0 and npm 10.2.4
   - Runs `npm install`
   - Builds Vue app with Vite → `frontend/dist/`

2. **Backend Build** (Maven resources plugin):
   - Copies `frontend/dist/` to `backend/target/classes/META-INF/resources/`
   - Quarkus serves static files from this location
   - Compiles Java code with Quarkus

3. **Result:**
   - Single JAR contains both frontend and backend
   - Frontend accessible at `http://localhost:8080/`
   - API accessible at `http://localhost:8080/api/`

## Configuration

### application.yml

```yaml
quarkus:
  http:
    port: 8080
    cors:
      ~: true

anthropic:
  api-key: ${ANTHROPIC_API_KEY:}
```

### Environment Variables

- `ANTHROPIC_API_KEY` - Required for Claude AI evaluation

## Key Design Decisions

1. **No Anthropic SDK** - Direct HTTP calls avoid SDK compatibility issues
2. **Constructor Injection** - Preferred over field injection for testability
3. **ObjectMapper per service** - Instantiated directly, not injected
4. **Fallback to Demo Data** - Graceful degradation if Blocket API fails
5. **Swedish Location Enum** - Type-safe mapping of län names to API codes
6. **Record Classes** - Used for immutable DTOs (CarListing, SearchRequest, etc.)

## Error Handling

- **Blocket API errors** → Returns demo data
- **Claude API errors** → Returns error message string
- **Missing API key** → Returns warning message
- **Parse errors** → Logs warning, skips problematic entries

## Testing Strategy

- Unit tests can mock services via constructor injection
- Integration tests can use Quarkus test framework
- Frontend tests can use Vitest (configured in Vite)
