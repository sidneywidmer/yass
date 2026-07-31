import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

import path from "path"

export default defineConfig({
  plugins: [react()],
  server: {
    // Listen on every interface so the dev server is reachable under YASS_HOST.
    host: true,
    allowedHosts: process.env.YASS_HOST ? [process.env.YASS_HOST] : [],
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})
