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
  '.temipora-demo.online',
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

// El HTML de la SPA no lleva hash y es el que referencia los chunks con su
// hash. Si el navegador o un CDN lo cachea, tras un nuevo build el HTML viejo
// pide chunks que ya no existen (404). Por eso el HTML siempre se revalida.
function cacheHtmlFresco() {
  return {
    name: 'temipora-cache-html-fresco',
    configurePreviewServer(servidor) {
      servidor.middlewares.use((req, res, next) => {
        const ruta = (req.url || '').split('?')[0]
        const aceptaHtml = String(req.headers.accept || '').includes('text/html')
        if (ruta.startsWith('/assets/')
            || (!aceptaHtml && ruta !== '/' && !ruta.endsWith('.html'))) {
          next()
          return
        }

        const escribirCabecera = res.setHeader.bind(res)
        res.setHeader = (nombre, valor) => {
          if (String(nombre).toLowerCase() === 'cache-control') {
            return escribirCabecera(
              'Cache-Control',
              'no-cache, no-store, must-revalidate',
            )
          }
          return escribirCabecera(nombre, valor)
        }
        next()
      })
    },
  }
}

// El HTML se sirve igual para cualquier ruta de la SPA, así que no puede saber
// qué vista se va a abrir. El chunk de Explorar y las fuentes latinas se
// precargan desde el <head> para que empiecen a bajar junto con el entry, en
// vez de esperar a que el router los descubra (un viaje extra al túnel).
function precargarVistaYFuentes() {
  return {
    name: 'temipora-preload-vista-y-fuentes',
    apply: 'build',
    transformIndexHtml: {
      order: 'post',
      handler(html, ctx) {
        const bundle = Object.values(ctx.bundle || {})
        const etiquetas = []

        const explorar = bundle.find(
          (item) => item.type === 'chunk' && item.name === 'Explorar',
        )
        if (explorar) {
          etiquetas.push({
            tag: 'link',
            attrs: {
              rel: 'modulepreload',
              crossorigin: '',
              href: `/${explorar.fileName}`,
            },
            injectTo: 'head',
          })
        }

        const fuentes = bundle.filter(
          (item) => item.type === 'asset'
            && /(?:^|\/)(bricolage-grotesque-latin|dm-sans-latin)-wght-normal-[^/]*\.woff2$/.test(item.fileName),
        )
        for (const fuente of fuentes) {
          etiquetas.push({
            tag: 'link',
            attrs: {
              rel: 'preload',
              as: 'font',
              type: 'font/woff2',
              crossorigin: '',
              href: `/${fuente.fileName}`,
            },
            injectTo: 'head',
          })
        }

        return etiquetas.length ? { html, tags: etiquetas } : html
      },
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    cacheInmutableAssets(),
    cacheHtmlFresco(),
    precargarVistaYFuentes(),
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
