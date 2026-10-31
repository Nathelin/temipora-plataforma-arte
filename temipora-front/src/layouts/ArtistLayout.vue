<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const menuAbierto = ref(false)
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const ICONOS = {
  resumen: 'M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z',
  servicios: 'M2.25 7.125C2.25 6.504 2.754 6 3.375 6h6c.621 0 1.125.504 1.125 1.125v3.75c0 .621-.504 1.125-1.125 1.125h-6a1.125 1.125 0 0 1-1.125-1.125v-3.75ZM14.25 8.625c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-8.25ZM3.75 16.125c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v2.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-2.25Z',
  solicitudes: 'M2.25 13.5h3.86a2.25 2.25 0 0 1 2.012 1.244l.256.512a2.25 2.25 0 0 0 2.013 1.244h3.218a2.25 2.25 0 0 0 2.013-1.244l.256-.512a2.25 2.25 0 0 1 2.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 0 0-2.15-1.588H6.911a2.25 2.25 0 0 0-2.15 1.588L2.35 13.177a2.25 2.25 0 0 0-.1.661Z',
  portfolio: 'm2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z',
  formulario: 'M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z',
  terminos: 'M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z',
  etiquetas: 'M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z',
  cobros: 'M2.25 8.25h19.5M2.25 9h19.5m-16.5 5.25h6m-6 2.25h3m-3.75 3h15a2.25 2.25 0 0 0 2.25-2.25V6.75A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25v10.5A2.25 2.25 0 0 0 4.5 19.5Z',
  cuenta: 'M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z',
  perfil: 'M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z',
  mensajes: 'M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z',
}

const secciones = [
  {
    titulo: 'Gestión',
    items: [
      { nombre: 'Resumen', to: '/panel', icono: ICONOS.resumen },
      { nombre: 'Servicios', to: '/panel/servicios', base: '/panel/servicios', icono: ICONOS.servicios },
      { nombre: 'Solicitudes', to: '/panel/solicitudes', base: '/panel/solicitudes', icono: ICONOS.solicitudes },
      { nombre: 'Pedidos', to: '/panel/pedidos', base: '/panel/pedidos', icono: ICONOS.solicitudes },
      { nombre: 'Mensajes', to: '/panel/mensajes', base: '/panel/mensajes', icono: ICONOS.mensajes },
      { nombre: 'Cobros', to: '/configuracion/cobros', icono: ICONOS.cobros },
      { nombre: 'Portfolio', to: '/panel/portfolio', base: '/panel/portfolio', icono: ICONOS.portfolio },
    ],
  },
  {
    titulo: 'Configuración de servicios',
    items: [
      { nombre: 'Formulario de pedidos', to: '/panel/formulario-pedidos', icono: ICONOS.formulario },
      { nombre: 'Términos y condiciones', to: '/panel/terminos', icono: ICONOS.terminos },
      { nombre: 'Etiquetas', to: '/panel/etiquetas', icono: ICONOS.etiquetas },
    ],
  },
]

const todos = secciones.flatMap((seccion) => seccion.items)
const esActiva = (item) =>
  item.base ? route.path.startsWith(item.base) : route.path === item.to
const nombreActivo = computed(() => todos.find(esActiva)?.nombre || 'Mi panel')
</script>

<template>
  <div class="w-full bg-crema text-cafe">
    <div class="mx-auto max-w-screen-2xl px-4 py-8 sm:px-8 lg:flex lg:gap-8">
      <aside class="mb-4 shrink-0 lg:mb-0 lg:w-64">
        <div class="lg:sticky lg:top-16">
          <h1 class="mb-4 hidden px-1 font-display text-xl font-bold text-granate lg:block">Panel de artista</h1>

          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 rounded-xl border border-rosa bg-blanco px-4 py-3 text-sm font-medium text-granate shadow-sm transition hover:bg-rosa/10 lg:hidden"
            :aria-expanded="menuAbierto"
            @click="menuAbierto = !menuAbierto"
          >
            <span>{{ nombreActivo }}</span>
            <svg
              class="h-4 w-4 shrink-0 transition-transform"
              :class="menuAbierto ? 'rotate-180' : ''"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
            </svg>
          </button>

          <nav
            class="mt-2 space-y-5 lg:mt-0 lg:block"
            :class="menuAbierto ? 'block' : 'hidden'"
            aria-label="Panel de artista"
          >
            <div v-for="seccion in secciones" :key="seccion.titulo">
              <p class="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/40">
                {{ seccion.titulo }}
              </p>
              <div class="space-y-1">
                <router-link
                  v-for="item in seccion.items"
                  :key="item.to"
                  :to="item.to"
                  class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition"
                  :class="esActiva(item)
                    ? 'bg-rosa/40 font-medium text-granate'
                    : 'text-cafe hover:bg-rosa/20'"
                  @click="menuAbierto = false"
                >
                  <svg
                    class="h-5 w-5 shrink-0"
                    :class="esActiva(item) ? 'text-coral' : 'text-cafe/50'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.7"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" :d="item.icono" />
                  </svg>
                  <span>{{ item.nombre }}</span>
                </router-link>
              </div>
            </div>

            <div class="space-y-1 border-t border-rosa/50 pt-4">
              <router-link
                to="/configuracion/perfil"
                class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-cafe transition hover:bg-rosa/20"
                @click="menuAbierto = false"
              >
                <svg class="h-5 w-5 shrink-0 text-cafe/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="ICONOS.cuenta" />
                </svg>
                <span>Configuración de cuenta</span>
              </router-link>
              <router-link
                v-if="nombreUsuario"
                :to="{ name: 'perfil', params: { nombreUsuario } }"
                class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-cafe transition hover:bg-rosa/20"
                @click="menuAbierto = false"
              >
                <svg class="h-5 w-5 shrink-0 text-cafe/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="ICONOS.perfil" />
                </svg>
                <span>Ver mi perfil público</span>
              </router-link>
            </div>
          </nav>
        </div>
      </aside>

      <div class="min-w-0 flex-1">
        <router-view />
      </div>
    </div>
  </div>
</template>
