<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '../components/BrandLogo.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import PanelNotificaciones from '../components/PanelNotificaciones.vue'

const route = useRoute()
const router = useRouter()

const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')
const menuAbierto = ref(false)
const menuAdminAbierto = ref(false)
const contenedorMenu = ref(null)

const secciones = [
  { nombre: 'Usuarios', to: '/admin/usuarios', activa: true },
  { nombre: 'Productos', to: '/admin/tipos/producto', activa: true },
  { nombre: 'Servicios', to: '/admin/tipos/servicio', activa: true },
  { nombre: 'Servicios archivados', to: '/admin/servicios/archivados', activa: true },
  {
    nombre: 'Etiquetas y categorías',
    base: '/admin/etiquetas',
    subsecciones: [
      { nombre: 'Etiquetas', to: '/admin/etiquetas' },
      { nombre: 'Categorías', to: '/admin/etiquetas/categorias' },
    ],
  },
  {
    nombre: 'Arte',
    base: '/admin/arte',
    subsecciones: [
      { nombre: 'Medios', to: '/admin/arte/medios' },
      { nombre: 'Tipos de arte', to: '/admin/arte/tipos' },
      { nombre: 'Materiales', to: '/admin/arte/materiales' },
    ],
  },
  {
    nombre: 'Ajustes generales',
    base: '/admin/ajustes',
    subsecciones: [
      { nombre: 'Países', to: '/admin/ajustes/paises' },
      { nombre: 'Monedas', to: '/admin/ajustes/monedas' },
      { nombre: 'Cotización', to: '/admin/ajustes/cotizacion' },
    ],
  },
  { nombre: 'Moderación', to: null, activa: false },
  { nombre: 'Sanciones', to: null, activa: false },
  { nombre: 'Notificaciones', to: '/admin/notificaciones', activa: true },
  { nombre: 'Auditoría', to: null, activa: false },
]

const esActiva = (to) => to && route.path === to

const submenusAbiertos = ref(
  Object.fromEntries(
    secciones
      .filter((s) => s.subsecciones)
      .map((s) => [s.nombre, route.path.startsWith(s.base)]),
  ),
)

const grupoActivo = (s) => s.subsecciones?.some((sub) => route.path === sub.to)

watch(
  () => route.path,
  (ruta) => {
    menuAdminAbierto.value = false
    for (const s of secciones) {
      if (s.subsecciones && ruta.startsWith(s.base)) {
        submenusAbiertos.value[s.nombre] = true
      }
    }
  },
)

function actualizarFotoPerfil(event) {
  urlFotoPerfil.value = event.detail?.url || ''
}

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

function manejarClickExterior(event) {
  if (contenedorMenu.value && !contenedorMenu.value.contains(event.target)) {
    menuAbierto.value = false
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    menuAbierto.value = false
    menuAdminAbierto.value = false
  }
}

onMounted(() => {
  window.addEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
  document.addEventListener('click', manejarClickExterior)
  document.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => {
  window.removeEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
  document.removeEventListener('click', manejarClickExterior)
  document.removeEventListener('keydown', manejarTecla)
})

function irAPerfil() {
  router.push({ name: 'perfil', params: { nombreUsuario: nombreUsuario.value } })
}

function cerrarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="flex min-h-screen w-full flex-col bg-blanco text-cafe">
    <header class="sticky top-0 z-50 border-b border-rosa/40 bg-blanco/95 backdrop-blur-md">
      <div class="flex h-14 w-full items-center justify-between px-4 sm:h-16 sm:px-8">
        <div class="flex items-center gap-3 sm:gap-4">
          <button
            type="button"
            class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-rosa/60 bg-blanco/70 text-granate transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 md:hidden"
            :aria-expanded="menuAdminAbierto"
            aria-controls="menu-admin"
            :aria-label="menuAdminAbierto ? 'Cerrar menú de administración' : 'Abrir menú de administración'"
            @click="menuAdminAbierto = !menuAdminAbierto"
          >
            <svg v-if="!menuAdminAbierto" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
            <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="m6 6 12 12M18 6 6 18" />
            </svg>
          </button>

          <router-link
            to="/"
            class="group inline-flex items-center rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-4 focus-visible:ring-offset-blanco"
            aria-label="Temipora, ir a la plataforma"
          >
            <BrandLogo />
          </router-link>
          <span class="hidden h-5 w-px bg-rosa/60 sm:block" aria-hidden="true"></span>
          <span class="hidden text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/50 sm:block">
            Administración
          </span>
        </div>

        <div class="flex items-center gap-2.5">
          <ThemeToggle />

          <PanelNotificaciones />

          <div ref="contenedorMenu" class="relative">
            <button
              class="flex min-h-10 items-center gap-2 rounded-full p-0.5 pr-1.5 transition hover:bg-rosa/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
            aria-haspopup="menu"
            :aria-expanded="menuAbierto"
            aria-label="Abrir menú de usuario"
            @click="alternarMenu"
          >
            <img
              v-if="urlFotoPerfil"
              :src="urlFotoPerfil"
              alt=""
              class="h-8.5 w-8.5 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
            />
            <span
              v-else
              class="flex h-8.5 w-8.5 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate shadow-sm ring-1 ring-white/70"
            >
              {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
            </span>
            <span class="hidden text-sm font-semibold text-cafe sm:inline">{{ nombreUsuario }}</span>
            <svg class="hidden h-4 w-4 text-cafe/50 transition-transform sm:block" :class="menuAbierto ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd" />
            </svg>
          </button>

          <div
            v-if="menuAbierto"
            class="absolute right-0 z-40 mt-2 w-56 overflow-hidden rounded-2xl border border-rosa/50 bg-blanco p-1.5 shadow-[0_18px_40px_rgba(74,29,46,0.16)]"
            role="menu"
          >
            <button
              class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
              role="menuitem"
              @click="menuAbierto = false; irAPerfil()"
            >
              Perfil
            </button>
            <button
              class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
              role="menuitem"
              @click="menuAbierto = false; router.push({ name: 'inicio' })"
            >
              Ver plataforma
            </button>
            <button
              class="mt-1 w-full rounded-xl border-t border-rosa/30 px-3.5 py-2.5 text-left text-sm text-rojizo transition hover:bg-rosa/20"
              role="menuitem"
              @click="menuAbierto = false; cerrarSesion()"
            >
              Cerrar sesión
            </button>
          </div>
          </div>
        </div>
      </div>
    </header>

    <div
      v-if="menuAdminAbierto"
      class="fixed inset-x-0 bottom-0 top-14 z-30 bg-granate/40 backdrop-blur-sm sm:top-16 md:hidden"
      aria-hidden="true"
      @click="menuAdminAbierto = false"
    ></div>

    <div class="flex flex-1">
      <!-- Sidebar -->
      <aside
        id="menu-admin"
        class="fixed bottom-0 left-0 top-14 z-40 flex w-72 max-w-[85vw] shrink-0 flex-col overflow-y-auto bg-granate text-inverso-contraste transition-transform duration-300 sm:top-16 md:sticky md:bottom-auto md:top-16 md:z-auto md:h-[calc(100vh-4rem)] md:min-h-[calc(100vh-4rem)] md:w-60 md:max-w-none md:translate-x-0"
        :class="menuAdminAbierto ? 'translate-x-0' : '-translate-x-full'"
      >
        <div class="border-b border-blanco/10 px-5 py-3">
          <p class="text-xs uppercase tracking-wider text-inverso-contraste/50">Panel de administración</p>
        </div>

        <nav class="flex-1 py-3">
          <div class="space-y-1">
            <template v-for="s in secciones" :key="s.nombre">
              <template v-if="s.subsecciones">
                <button
                  class="w-full flex items-center justify-between px-5 py-2 text-sm transition"
                  :class="grupoActivo(s) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste'"
                  :aria-expanded="!!submenusAbiertos[s.nombre]"
                  @click="submenusAbiertos[s.nombre] = !submenusAbiertos[s.nombre]"
                >
                  <span>{{ s.nombre }}</span>
                  <svg
                    class="w-4 h-4 transition-transform"
                    :class="submenusAbiertos[s.nombre] ? 'rotate-90' : ''"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                    aria-hidden="true"
                  >
                    <path
                      fill-rule="evenodd"
                      d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z"
                      clip-rule="evenodd"
                    />
                  </svg>
                </button>
                <div v-if="submenusAbiertos[s.nombre]" class="space-y-1 pb-1">
                  <router-link
                    v-for="sub in s.subsecciones"
                    :key="sub.to"
                    :to="sub.to"
                    class="flex items-center gap-3 pl-9 pr-5 py-2 text-sm transition"
                    :class="esActiva(sub.to) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste'"
                  >
                    <span>{{ sub.nombre }}</span>
                  </router-link>
                </div>
              </template>
              <router-link
                v-else-if="s.activa"
                :to="s.to"
                class="flex items-center gap-3 px-5 py-2 text-sm transition"
                :class="esActiva(s.to) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste'"
              >
                <span>{{ s.nombre }}</span>
              </router-link>
              <div
                v-else
                class="flex items-center gap-3 px-5 py-2 text-sm text-inverso-contraste/30 cursor-not-allowed select-none"
                :title="'Próximamente'"
              >
                <span>{{ s.nombre }}</span>
              </div>
            </template>
          </div>
        </nav>
      </aside>

      <!-- Contenido -->
      <main class="min-w-0 flex-1 bg-blanco">
        <router-view />
      </main>
    </div>
  </div>
</template>