<template>
  <div class="container">
    <h1>🚗 Blockcar</h1>
    <p class="subtitle">Hitta bästa bilköpet med AI-hjälp</p>

    <div class="filters">
      <!-- Price and Year Range -->
      <div class="filter-section">
        <h3 class="section-title">Pris och Årsmodell</h3>
        <div class="filter-row-two">
          <div class="filter-group">
            <label>Prisintervall (kr)</label>
            <div class="range-inputs">
              <input type="number" v-model.number="filters.minPrice" placeholder="Min pris" />
              <span class="range-separator">—</span>
              <input type="number" v-model.number="filters.maxPrice" placeholder="Max pris" />
            </div>
          </div>

          <div class="filter-group">
            <label>Årsmodell</label>
            <div class="range-inputs">
              <input type="number" v-model.number="filters.minYear" placeholder="Min år" />
              <span class="range-separator">—</span>
              <input type="number" v-model.number="filters.maxYear" placeholder="Max år" />
            </div>
          </div>
        </div>
      </div>

      <!-- Mileage and Age -->
      <div class="filter-section">
        <h3 class="section-title">Miltal och Annonsålder</h3>
        <div class="filter-row-two">
          <div class="filter-group">
            <label>Max miltal</label>
            <input type="number" v-model.number="filters.maxMileage" placeholder="T.ex. 15000" />
          </div>

          <div class="filter-group">
            <label>Max dagar sedan publicerad</label>
            <input type="number" v-model.number="filters.maxAgeDays" placeholder="T.ex. 7" />
          </div>
        </div>
      </div>

      <!-- Location and Results -->
      <div class="filter-section">
        <h3 class="section-title">Plats och Resultat</h3>
        <div class="filter-row-two">
          <div class="filter-group">
            <label>Län (håll Ctrl/Cmd för flera val)</label>
            <select v-model="filters.locations" multiple class="location-select">
              <option value="stockholm">Stockholm</option>
              <option value="uppsala">Uppsala</option>
              <option value="sodermanland">Södermanland</option>
              <option value="ostergotland">Östergötland</option>
              <option value="jonkoping">Jönköping</option>
              <option value="kronoberg">Kronoberg</option>
              <option value="kalmar">Kalmar</option>
              <option value="gotland">Gotland</option>
              <option value="blekinge">Blekinge</option>
              <option value="skane">Skåne</option>
              <option value="halland">Halland</option>
              <option value="vastra_gotaland">Västra Götaland</option>
              <option value="varmland">Värmland</option>
              <option value="orebro">Örebro</option>
              <option value="vastmanland">Västmanland</option>
              <option value="dalarna">Dalarna</option>
              <option value="gavleborg">Gävleborg</option>
              <option value="vasternorrland">Västernorrland</option>
              <option value="jamtland">Jämtland</option>
              <option value="vasterbotten">Västerbotten</option>
              <option value="norrbotten">Norrbotten</option>
            </select>
          </div>

          <div class="filter-group">
            <label>Max antal resultat</label>
            <input type="number" v-model.number="filters.limit" min="5" max="50" />
          </div>
        </div>
      </div>

      <!-- AI Evaluation -->
      <div class="filter-section">
        <div class="checkbox-group">
          <input type="checkbox" id="evaluate" v-model="filters.evaluate" />
          <label for="evaluate">Utvärdera med Claude AI</label>
        </div>
      </div>

      <button
        class="btn btn-primary"
        @click="searchCars"
        :disabled="loading"
      >
        {{ loading ? 'Söker...' : 'Sök bilar' }}
      </button>
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>

    <div v-if="loading" class="loading">
      {{ statusMessage }}
    </div>

    <div v-if="results && !loading" class="results">
      <div class="results-header">
        <h2>Hittade {{ results.count }} bilar</h2>
      </div>

      <div class="car-grid">
        <div v-for="car in results.listings" :key="car.id" class="car-card">
          <img :src="car.image_url" :alt="car.title" class="car-image" />
          <div class="car-content">
            <h3 class="car-title">{{ car.title }}</h3>
            <div class="car-price">{{ formatPrice(car.price) }} kr</div>
            <div class="car-details">
              <div class="car-detail"><strong>År:</strong> {{ car.year }}</div>
              <div class="car-detail"><strong>Mil:</strong> {{ formatMileage(car.mileage) }}</div>
              <div class="car-detail"><strong>Bränsle:</strong> {{ car.fuel_type }}</div>
              <div class="car-detail"><strong>Växellåda:</strong> {{ car.transmission }}</div>
              <div class="car-detail"><strong>Plats:</strong> {{ car.location }}</div>
              <div class="car-detail"><strong>Säljare:</strong> {{ car.seller_type }}</div>
            </div>
            <a :href="car.url" target="_blank" class="car-link">Visa annons →</a>
          </div>
        </div>
      </div>

      <div v-if="results.evaluation" class="evaluation">
        <h2>🤖 Claude AI Utvärdering</h2>
        <div class="evaluation-content" v-html="renderMarkdown(results.evaluation)"></div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'App',
  data() {
    return {
      filters: {
        minPrice: 0,
        maxPrice: 1000000,
        minYear: null,
        maxYear: null,
        maxMileage: null,
        maxAgeDays: null,
        locations: [],
        limit: 20,
        evaluate: false
      },
      results: null,
      loading: false,
      error: null,
      statusMessage: 'Söker efter bilar...'
    }
  },
  methods: {
    async searchCars() {
      this.loading = true
      this.error = null
      this.results = null
      this.statusMessage = 'Söker efter bilar...'

      try {
        const response = await fetch('/api/cars/search-stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(this.filters)
        })

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // Process complete events (separated by double newline)
          let eventEnd
          while ((eventEnd = buffer.indexOf('\n\n')) !== -1) {
            const eventText = buffer.substring(0, eventEnd)
            buffer = buffer.substring(eventEnd + 2)

            console.log('SSE Event received:', eventText)

            // Parse event and data lines
            const lines = eventText.split('\n')
            let eventType = null
            let eventData = null

            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventType = line.substring(6).trim()
              } else if (line.startsWith('data:')) {
                eventData = line.substring(5).trim()
              }
            }

            console.log('Parsed - Type:', eventType, 'Data:', eventData)

            if (eventType && eventData !== null) {
              if (eventType === 'status') {
                this.statusMessage = eventData
                console.log('Status updated to:', eventData)
              } else if (eventType === 'result') {
                this.results = JSON.parse(eventData)
                console.log('Results received:', this.results.count, 'cars')
              } else if (eventType === 'error') {
                this.error = 'Ett fel uppstod vid sökning: ' + eventData
                this.loading = false
                return
              } else if (eventType === 'done') {
                console.log('Search complete')
                this.loading = false
                return
              }
            }
          }
        }

        this.loading = false
      } catch (err) {
        console.error('Search error:', err)
        this.error = 'Ett fel uppstod vid sökning: ' + (err.message || err)
        this.loading = false
      }
    },
    formatPrice(price) {
      return new Intl.NumberFormat('sv-SE').format(price)
    },
    formatMileage(mileage) {
      return new Intl.NumberFormat('sv-SE').format(mileage) + ' mil'
    },
    renderMarkdown(text) {
      // Simple markdown rendering
      return text
        .replace(/^### (.*$)/gim, '<h3>$1</h3>')
        .replace(/^## (.*$)/gim, '<h2>$1</h2>')
        .replace(/^# (.*$)/gim, '<h1>$1</h1>')
        .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
        .replace(/\*(.*)\*/gim, '<em>$1</em>')
        .replace(/^\- (.*$)/gim, '<li>$1</li>')
        .replace(/\n/g, '<br />')
    }
  }
}
</script>
