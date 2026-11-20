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

// Los archivos de /assets llevan hash en el nombre: su contenido no cambia
// nunca para una URL dada. El preview de Vite los sirve con no-cache por
// defecto, lo que obliga a bajar todo de nuevo en cada recarga (muy caro por
// el túnel). Les damos caché inmutable solo en el preview de demo.
function cacheInmutableAssets() {
  return {
    name: 'temipora-cache-inmutable-assets',
    configurePreviewServer(servidor) {
      servidor.middlewares.use((req, res, next) => {
        const ruta = (req.url || '').split('?')[0]
        if (!ruta.startsWith('/assets/')) {
          next()
          return
        }

        res.setHeader('Cache-Control', 'public, max-age=31536000, immutable')

        // El middleware estático del preview puede sobrescribir la cabecera;
        // interceptamos esa escritura para conservar la caché inmutable.
        const escribirCabecera = res.setHeader.bind(res)
        res.setHeader = (nombre, valor) => {
          if (String(nombre).toLowerCase() === 'cache-control') {
            return escribirCabecera(
              'Cache-Control',
              'public, max-age=31536000, immutable',
            )
          }
          return escribirCabecera(nombre, valor)
        }
        next()
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    cacheInmutableAssets(),
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
