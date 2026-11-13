import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

import tailwindcss from '@tailwindcss/vite'

const proxy = {
  '/api': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
  },
  '/uploads': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
  },
  '/ws': {
    target: 'ws://127.0.0.1:8080',
    ws: true,
    changeOrigin: true,
  },
}

const allowedHosts = [
  'exploring-slobbery-shrivel.ngrok-free.dev',
  '.ngrok-free.dev',
  '.trycloudflare.com',
  '.devtunnels.ms',
]

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    allowedHosts,
    proxy,
  },
  // Modo demo: `npm run demo` compila y sirve el build estático.
  // Usa el mismo puerto que el dev server para conservar la URL del túnel y
  // las URLs de MercadoPago configuradas en application-local.properties.
  // Sin HMR ni miles de módulos sueltos, el túnel mueve mucho menos tráfico.
  preview: {
    host: true,
    port: 5173,
    strictPort: true,
    allowedHosts,
    proxy,
  },
})
