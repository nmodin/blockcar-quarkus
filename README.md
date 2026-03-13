# Blockcar Quarkus

En modern reimplementering av Blockcar med Quarkus backend och Vue.js frontend. Applikationen hjälper dig hitta de bästa bilköpen på Blocket.se med hjälp av Claude AI.

## 🏗️ Arkitektur

Detta är ett multi-module Maven-projekt med följande struktur:

```
blockcar-quarkus/
├── pom.xml                 # Parent POM
├── backend/                # Quarkus backend modul
│   ├── src/main/java/      # Java källkod
│   │   └── se/nmodin/blockcar/
│   │       ├── model/      # Data models (CarListing, SearchRequest, etc.)
│   │       ├── resource/   # REST endpoints
│   │       └── service/    # Business logic (BlocketService, ClaudeService)
│   └── src/main/resources/
│       ├── application.yml # Quarkus konfiguration
│       └── META-INF/resources/ # Statiska filer (Vue build output)
└── frontend/               # Vue.js frontend modul
    ├── src/
    │   ├── App.vue        # Huvudkomponent
    │   ├── main.js        # Entry point
    │   └── style.css      # Styling
    ├── index.html
    ├── package.json
    └── vite.config.js
```

## ✨ Funktioner

- 🔍 Sök bilar på Blocket med avancerade filter
- 💰 Filtrera på prisintervall, årsmodell, miltal
- 📍 Sök i specifika regioner (alla svenska län)
- 🤖 AI-utvärdering med Claude för att hitta bästa köpen
- 🎨 Modern och responsiv Vue.js UI
- ⚡ Snabb Quarkus backend med REST API
- 🔌 **Direktintegration med Blocket.se API** - Använder samma API-endpoints som den officiella webbplatsen

## 🚀 Kom igång

### Förutsättningar

- Java 17 eller senare
- Maven 3.8+
- Node.js 20+ (för lokal frontend-utveckling)
- Anthropic API-nyckel (för Claude AI-utvärdering)

### Installation

1. **Konfigurera miljövariabler**

   Skapa en `.env`-fil eller sätt miljövariabeln:
   ```bash
   export ANTHROPIC_API_KEY="din-api-nyckel-här"
   ```

2. **Bygg hela projektet**

   Från root-katalogen:
   ```bash
   mvn clean install
   ```

   Detta kommer:
   - Bygga Quarkus backend
   - Installera Node.js och npm (via frontend-maven-plugin)
   - Bygga Vue frontend med Vite
   - Kopiera Vue's `dist/` till `backend/target/classes/META-INF/resources/`

3. **Starta applikationen**

   ```bash
   cd backend
   mvn quarkus:dev
   ```

   Applikationen är nu tillgänglig på:
   - **Frontend:** http://localhost:8080
   - **API:** http://localhost:8080/api/cars/search

## 🛠️ Utveckling

### Frontend-utveckling med hot reload

För snabbare frontend-utveckling kan du köra Vue dev-servern separat:

```bash
cd frontend
npm install  # Första gången
npm run dev
```

Detta startar Vite dev-servern på http://localhost:3000 med hot reload och proxying till Quarkus backend på port 8080.

### Backend-utveckling

Quarkus dev-mode har inbyggd hot reload:

```bash
cd backend
mvn quarkus:dev
```

Ändringar i Java-kod kompileras automatiskt när du sparar.

### API Endpoints

#### POST `/api/cars/search`

Sök efter bilar med filter.

**Request body:**
```json
{
  "min_price": 50000,
  "max_price": 200000,
  "min_year": 2015,
  "max_year": 2023,
  "min_mileage": 0,
  "max_mileage": 15000,
  "max_age_days": 30,
  "locations": ["stockholm", "goteborg"],
  "limit": 20,
  "page": 1,
  "evaluate": true
}
```

**Tillgängliga län (locations):**
- stockholm, uppsala, sodermanland, ostergotland, jonkoping, kronoberg
- kalmar, gotland, blekinge, skane, halland, vastra_gotaland
- varmland, orebro, vastmanland, dalarna, gavleborg
- vasternorrland, jamtland, vasterbotten, norrbotten

**Response:**
```json
{
  "listings": [
    {
      "id": "123",
      "title": "Volvo V70 2.5T",
      "price": 85000,
      "year": 2015,
      "mileage": 15000,
      "location": "Stockholm",
      "brand": "Volvo",
      "model": "V70",
      "fuel_type": "Bensin",
      "transmission": "Automat",
      "seller_type": "Privatperson",
      "url": "https://www.blocket.se/...",
      "image_url": "https://...",
      "description": "...",
      "published_date": "2024-03-10 14:30"
    }
  ],
  "count": 3,
  "evaluation": "# Claude AI Utvärdering\n\n..."
}
```

## 🔌 API-integrationer

### Blocket API

Applikationen använder Blocket.se's sök-API:

- **Endpoint:** `https://www.blocket.se/mobility/search/api/search/SEARCH_ID_CAR_USED`
- **Metod:** GET med query parameters
- **User-Agent:** Krävs för att simulera en webbläsarförfrågan

Implementationen baseras på samma API-anrop som används av Blocket.se's egen webbplats. API:t är publikt tillgängligt men utan officiell dokumentation. Vid problem med API-anrop faller systemet tillbaka på demo-data.

**API-parametrar som stöds:**

| Parameter | Typ | Beskrivning |
|-----------|-----|-------------|
| `price_from` / `price_to` | Integer | Prisintervall i SEK |
| `year_from` / `year_to` | Integer | Årsmodell |
| `milage_from` / `milage_to` | Integer | Miltal (OBS: stavning i API) |
| `location` | String | Län-kod (t.ex. "0.300001" för Stockholm) |
| `page` | Integer | Sidnummer (default: 1) |
| `sort` | String | Sortering (default: "PUBLISHED_DESC") |

**Viktigt:** Eftersom detta är ett inofficiellt API utan dokumentation kan det ändras när som helst av Blocket. Applikationen har inbyggd felhantering och fallback till demo-data.

### Anthropic Claude API

Applikationen använder Anthropic's Messages API för AI-utvärdering:

- **Endpoint:** `https://api.anthropic.com/v1/messages`
- **Metod:** POST med JSON body
- **Headers:**
  - `x-api-key`: Din Anthropic API-nyckel
  - `anthropic-version`: `2023-06-01`
- **Model:** `claude-sonnet-4-20250514`

Implementationen använder direkta HTTP-anrop via JAX-RS Client istället för Anthropic SDK för bättre kontroll och enklare underhåll.

## 📦 Produktion

### Bygg native executable (valfritt)

För maximal prestanda kan du bygga en native executable med GraalVM:

```bash
mvn clean package -Dnative
```

Detta kräver GraalVM installerat.

### Bygg vanlig JAR

```bash
mvn clean package
```

Kör sedan:
```bash
java -jar backend/target/quarkus-app/quarkus-run.jar
```

## 🐙 Skapa GitHub Repository

För att publicera detta som ett nytt GitHub-repo:

1. **Skapa repository på GitHub**
   - Gå till https://github.com/new
   - Namnge det `blockcar-quarkus`
   - Skapa **utan** README, .gitignore eller licens (vi har redan dessa)

2. **Initiera Git och pusha**

   ```bash
   cd /Users/nmodin/repos/blockcar-quarkus

   # Initiera git
   git init

   # Skapa .gitignore
   cat > .gitignore << 'EOF'
   # Maven
   target/
   pom.xml.tag
   pom.xml.releaseBackup
   pom.xml.versionsBackup
   pom.xml.next
   release.properties

   # Node
   node_modules/
   dist/
   frontend/node/
   frontend/node_modules/

   # IDE
   .idea/
   .vscode/
   *.iml
   .DS_Store

   # Environment
   .env
   .env.local
   EOF

   # Lägg till alla filer
   git add .

   # Första commit
   git commit -m "Initial commit: Quarkus + Vue multi-module project"

   # Koppla till remote (ersätt med din URL)
   git remote add origin https://github.com/nmodin/blockcar-quarkus.git

   # Pusha till GitHub
   git branch -M main
   git push -u origin main
   ```

3. **Lägg till .env.example**

   Det kan vara bra att skapa en `.env.example`:
   ```bash
   echo "ANTHROPIC_API_KEY=your-api-key-here" > .env.example
   git add .env.example
   git commit -m "Add .env.example"
   git push
   ```

## 🔧 Konfiguration

### Backend Configuration (application.yml)

```yaml
quarkus:
  http:
    port: 8080
    cors:
      ~: true

anthropic:
  api-key: ${ANTHROPIC_API_KEY:}
  model: claude-sonnet-4-20250514
```

### Frontend Configuration (vite.config.js)

```javascript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
```

## 🧪 Testing

Kör backend-tester:
```bash
cd backend
mvn test
```

## 📝 Licens

MIT License - se LICENSE-filen för detaljer.

## 🤝 Bidra

1. Forka projektet
2. Skapa en feature branch (`git checkout -b feature/amazing-feature`)
3. Commit dina ändringar (`git commit -m 'Add amazing feature'`)
4. Pusha till branchen (`git push origin feature/amazing-feature`)
5. Öppna en Pull Request

## 📧 Kontakt

Niklas Modin - [@nmodin](https://github.com/nmodin)

Projekt Link: https://github.com/nmodin/blockcar-quarkus
