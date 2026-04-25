import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/jobs': 'http://control-plane:8080',
      '/executions': 'http://control-plane:8080',
      '/metrics': 'http://control-plane:8080',
      '/actuator': 'http://control-plane:8080',
    }
  }
})