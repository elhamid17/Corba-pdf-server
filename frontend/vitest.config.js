import { defineConfig, configDefaults } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./src/test/setup.js'],
    // Les specs Playwright vivent dans e2e/ et sont exécutées par
    // `npm run test:e2e`, pas par Vitest (sinon collision de runners).
    exclude: [...configDefaults.exclude, 'e2e/**'],
  },
})
