<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const menuAbierto = ref(false)

const esArtista = localStorage.getItem('rol') === 'ARTISTA'

const ICONOS = {
  perfil: 'M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z',
  apariencia: 'M4.098 19.902a3.75 3.75 0 0 0 5.304 0l6.401-6.402M6.75 21A3.75 3.75 0 0 1 3 17.25V4.125C3 3.504 3.504 3 4.125 3h5.25c.621 0 1.125.504 1.125 1.125v4.072M6.75 21a3.75 3.75 0 0 0 3.75-3.75V8.197M6.75 21h13.125c.621 0 1.125-.504 1.125-1.125v-5.25c0-.621-.504-1.125-1.125-1.125h-4.072M10.5 8.197l2.88-2.88c.438-.439 1.15-.439 1.59 0l3.712 3.713c.44.44.44 1.152 0 1.59l-2.879 2.88M6.75 17.25h.008v.008H6.75v-.008Z',
  privacidad: 'M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z',
  cuenta: 'M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z',
}

const secciones = [
  { nombre: 'Perfil', to: '/configuracion/perfil', icono: ICONOS.perfil },
  { nombre: 'Apariencia', to: '/configuracion/apariencia', icono: ICONOS.apariencia },
  { nombre: 'Privacidad y contenido', to: '/configuracion/privacidad', icono: ICONOS.privacidad },
  { nombre: 'Cuenta y seguridad', to: '/configuracion/cuenta', icono: ICONOS.cuenta },
]

const esActiva = (to) => route.path === to
const nombreActivo = computed(() => secciones.find((s) => esActiva(s.to))?.nombre || 'Configuración')
</script>

<template>
  <div class="w-full bg-crema text-cafe">
    <div class="max-w-screen-2xl mx-auto px-4 sm:px-8 py-8 lg:flex lg:gap-8">
      <!-- Menú -->
      <aside class="lg:w-64 shrink-0 mb-4 lg:mb-0">
        <div class="lg:sticky lg:top-16">
          <h1 class="hidden lg:block text-xl font-normal text-granate mb-4 px-1">Configuración</h1>

          <!-- Toggle en móvil -->
          <button
            type="button"
            class="lg:hidden flex w-full items-center justify-between gap-3 rounded-xl border border-rosa bg-blanco px-4 py-3 text-sm font-medium text-granate shadow-sm transition hover:bg-rosa/10"
            :aria-expanded="menuAbierto"
            @click="menuAbierto = !menuAbierto"
          >
            <span class="flex items-center gap-2">
              <span>{{ nombreActivo }}</span>
            </span>
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
            class="space-y-1"
            :class="menuAbierto ? 'mt-2 block' : 'hidden lg:block lg:mt-0'"
          >
            <router-link
              v-for="s in secciones"
              :key="s.to"
              :to="s.to"
              class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition"
              :class="esActiva(s.to)
                ? 'bg-rosa/40 text-granate font-medium'
                : 'text-cafe hover:bg-rosa/20'"
              @click="menuAbierto = false"
            >
              <svg
                class="h-5 w-5 shrink-0"
                :class="esActiva(s.to) ? 'text-coral' : 'text-cafe/50'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                aria-hidden="true"
              >
                <path stroke-linecap="round" stroke-linejoin="round" :d="s.icono" />
              </svg>
              <span>{{ s.nombre }}</span>
            </router-link>

            <router-link
              v-if="esArtista"
              to="/panel"
              class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm text-cafe/70 transition hover:bg-rosa/20 hover:text-granate"
              @click="menuAbierto = false"
            >
              <span aria-hidden="true">←</span>
              <span>Volver a mi panel</span>
            </router-link>
          </nav>
        </div>
      </aside>

      <!-- Contenido -->
      <div class="flex-1 min-w-0">
        <router-view />
      </div>
    </div>
  </div>
</template>
