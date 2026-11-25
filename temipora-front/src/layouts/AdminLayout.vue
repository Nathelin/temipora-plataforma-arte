<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '../components/BrandLogo.vue'
import { useAdminMenu } from '../composables/useAdminMenu'
import PanelNotificaciones from '../components/PanelNotificaciones.vue'
import SpinnerCarga from '../components/SpinnerCarga.vue'
import AceptacionTerminosModal from '../components/AceptacionTerminosModal.vue'
import { useNotificacionesSistema } from '../composables/useNotificacionesSistema'
import api from '../servicios/api'
import {
  Archive,
  Ban,
  ChartNoAxesCombined,
  Clock,
  Coins,
  FileText,
  Globe,
  LayoutDashboard,
  Megaphone,
  MessageSquare,
  Package,
  Palette,
  PanelLeftClose,
  PanelLeftOpen,
  Percent,
  Receipt,
  ShieldAlert,
  ShieldCheck,
  Star,
  Tags,
  UserX,
  Users,
  Wrench,
} from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const saliendo = ref(false)

const {
  limpiar: limpiarNotificaciones,
  detenerPolling: detenerPollingNotificaciones,
} = useNotificacionesSistema()

const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')
const menuAbierto = ref(false)
const menuAdminAbierto = ref(false)
const contenedorMenu = ref(null)

const grupos = [
  {
    id: 'comunidad',
    nombre: 'Comunidad',
    items: [
      { nombre: 'Usuarios', to: '/admin/usuarios', icono: Users },
      {
        nombre: 'Reseñas',
        to: '/admin/resenas/criterios',
        rutas: ['/admin/resenas/criterios', '/admin/resenas/configuracion'],
        icono: Star,
      },
      {
        nombre: 'Moderación',
        to: '/admin/resenas/reportadas',
        rutas: ['/admin/resenas/reportadas', '/admin/resenas/historial'],
        icono: ShieldAlert,
        badge: 'reportes',
      },
      {
        nombre: 'Sanciones',
        to: '/admin/sanciones',
        rutas: ['/admin/sanciones', '/admin/apelaciones'],
        icono: Ban,
      },
      { nombre: 'Bajas de cuenta', to: '/admin/ajustes/bajas-cuenta', icono: UserX },
    ],
  },
  {
    id: 'catalogo',
    nombre: 'Catálogo',
    items: [
      { nombre: 'Tipos de producto', to: '/admin/tipos/producto', icono: Package },
      { nombre: 'Tipos de servicio', to: '/admin/tipos/servicio', icono: Wrench },
      { nombre: 'Servicios archivados', to: '/admin/servicios/archivados', icono: Archive },
      {
        nombre: 'Arte',
        to: '/admin/arte/medios',
        rutas: ['/admin/arte/medios', '/admin/arte/tipos', '/admin/arte/materiales'],
        icono: Palette,
      },
      {
        nombre: 'Etiquetas y categorías',
        to: '/admin/etiquetas',
        rutas: ['/admin/etiquetas', '/admin/etiquetas/categorias'],
        icono: Tags,
        badge: 'etiquetas',
      },
    ],
  },
  {
    id: 'comercio',
    nombre: 'Comercio',
    items: [
      {
        nombre: 'Presupuestos e hitos',
        to: '/admin/ajustes/presupuestos',
        rutas: ['/admin/ajustes/presupuestos', '/admin/ajustes/pedidos'],
        icono: Receipt,
      },
      { nombre: 'Ofertas por inactividad', to: '/admin/campanas/promociones', icono: Megaphone },
      { nombre: 'Sustituciones de artistas', to: '/admin/campanas/sustituciones', icono: Users },
      { nombre: 'Comisiones', to: '/admin/ajustes/comisiones', icono: Percent },
      {
        nombre: 'Monedas y cotización',
        to: '/admin/ajustes/monedas',
        rutas: ['/admin/ajustes/monedas', '/admin/ajustes/cotizacion'],
        icono: Coins,
      },
    ],
  },
  {
    id: 'sistema',
    nombre: 'Sistema',
    items: [
      { nombre: 'Países', to: '/admin/ajustes/paises', icono: Globe },
      { nombre: 'Mensajería', to: '/admin/ajustes/mensajeria', icono: MessageSquare },
      { nombre: 'Anclaje en Bitcoin', to: '/admin/anclaje', icono: ShieldCheck },
      { nombre: 'Términos de la plataforma', to: '/admin/ajustes/terminos-plataforma', icono: FileText },
    ],
  },
]

const utilidades = [
  { nombre: 'Auditoría', to: '/admin/auditoria', icono: Clock },
]

const principales = [
  { nombre: 'Resumen', to: '/admin', icono: LayoutDashboard },
  {
    nombre: 'Informes',
    to: '/admin/informes/ventas',
    base: '/admin/informes',
    icono: ChartNoAxesCombined,
  },
]

const esActiva = (to) => Boolean(to) && route.path === to
const esActivaPrincipal = (item) => item.base
  ? route.path.startsWith(item.base)
  : route.path === item.to
const estaActivo = (item) => (item.rutas ? item.rutas.includes(route.path) : esActiva(item.to))
const ariaActual = (item) => {
  if (!estaActivo(item)) return null
  return route.path === item.to ? 'page' : 'true'
}

const { colapsada } = useAdminMenu()

function alternarColapsada() {
  colapsada.value = !colapsada.value
}

const conteos = ref({ reportes: 0, etiquetas: 0 })

async function cargarConteos() {
  const [reportes, etiquetas] = await Promise.allSettled([
    api.get('/admin/resenias/reportadas', { params: { pagina: 0, tamanio: 1 } }),
    api.get('/admin/etiquetas/conteo-pendientes'),
  ])
  if (reportes.status === 'fulfilled') {
    conteos.value.reportes = Number(reportes.value.data?.totalElementos ?? 0)
  }
  if (etiquetas.status === 'fulfilled') {
    conteos.value.etiquetas = Number(etiquetas.value.data?.pendientes ?? 0)
  }
}

function badgeDe(item) {
  const valor = item.badge ? Number(conteos.value[item.badge] ?? 0) : 0
  if (!valor) return null
  return valor > 99 ? '99+' : String(valor)
}

const filtro = ref('')
const termino = computed(() => filtro.value.trim().toLowerCase())
const filtrando = computed(() => termino.value.length > 0)

function coincide(texto) {
  return texto.toLowerCase().includes(termino.value)
}

const gruposVisibles = computed(() => {
  if (!filtrando.value) return grupos

  return grupos
    .map((grupo) => ({
      ...grupo,
      items: grupo.items.filter((item) => coincide(item.nombre)),
    }))
    .filter((grupo) => grupo.items.length)
})

watch(
  () => route.path,
  () => {
    menuAdminAbierto.value = false
    cargarConteos()
  },
  { immediate: true },
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

async function cerrarSesion() {
  if (saliendo.value) return
  saliendo.value = true
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  limpiarNotificaciones()
  detenerPollingNotificaciones()
  try {
    await router.push({ name: 'login' })
  } finally {
    saliendo.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen w-full flex-col bg-blanco text-cafe">
    <div
      v-if="saliendo"
      class="fixed inset-0 z-[130] flex items-center justify-center bg-granate/30 px-4 backdrop-blur-sm"
      role="status"
      aria-live="polite"
    >
      <div class="flex items-center gap-3 rounded-2xl bg-blanco px-5 py-4 shadow-xl">
        <SpinnerCarga tamano="md" etiqueta="Cerrando sesión" />
        <span class="text-sm text-cafe">Cerrando sesión…</span>
      </div>
    </div>
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
              class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
              role="menuitem"
              @click="menuAbierto = false; router.push({ name: 'configuracion-apariencia' })"
            >
              Apariencia
            </button>
            <button
              class="mt-1 flex w-full items-center gap-2 rounded-xl border-t border-rosa/30 px-3.5 py-2.5 text-left text-sm text-rojizo transition hover:bg-rosa/20 disabled:cursor-wait disabled:opacity-60"
              role="menuitem"
              :disabled="saliendo"
              :aria-busy="saliendo"
              @click="menuAbierto = false; cerrarSesion()"
            >
              <SpinnerCarga v-if="saliendo" etiqueta="Cerrando sesión" />
              {{ saliendo ? 'Cerrando sesión…' : 'Cerrar sesión' }}
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
        class="fixed bottom-0 left-0 top-14 z-40 flex w-72 max-w-[85vw] shrink-0 flex-col overflow-hidden bg-granate text-inverso-contraste transition-transform duration-300 motion-reduce:transition-none sm:top-16 md:sticky md:bottom-auto md:top-16 md:z-auto md:h-[calc(100vh-4rem)] md:min-h-[calc(100vh-4rem)] md:max-w-none md:translate-x-0"
        :class="[
          menuAdminAbierto ? 'translate-x-0' : '-translate-x-full',
          colapsada ? 'md:w-[4.5rem]' : 'md:w-[16.25rem]',
        ]"
      >
        <div
          class="flex h-12 shrink-0 items-center justify-between gap-2 border-b border-blanco/10 pl-5 pr-2"
          :class="colapsada ? 'md:justify-center md:pl-2' : ''"
        >
          <p
            class="text-xs uppercase tracking-wider text-inverso-contraste/50"
            :class="colapsada ? 'md:sr-only' : ''"
          >
            Panel de administración
          </p>
          <button
            type="button"
            class="hidden h-8 w-8 shrink-0 items-center justify-center rounded-lg text-inverso-contraste/60 transition-colors hover:bg-white/10 hover:text-inverso-contraste focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none md:inline-flex"
            :aria-expanded="!colapsada"
            :aria-label="colapsada ? 'Expandir menú de administración' : 'Colapsar menú de administración'"
            :title="colapsada ? 'Expandir menú' : 'Colapsar menú'"
            @click="alternarColapsada"
          >
            <PanelLeftOpen v-if="colapsada" class="h-4 w-4" aria-hidden="true" />
            <PanelLeftClose v-else class="h-4 w-4" aria-hidden="true" />
          </button>
        </div>

        <div class="shrink-0 px-4 pt-3 md:hidden">
          <label for="buscar-secciones" class="sr-only">Buscar sección</label>
          <input
            id="buscar-secciones"
            v-model="filtro"
            type="search"
            placeholder="Buscar sección"
            class="block w-full rounded-lg border-0 bg-blanco/10 px-3 py-2 text-sm text-inverso-contraste placeholder:text-inverso-contraste/40 focus:bg-blanco/15 focus:outline-none focus:ring-2 focus:ring-coral/60"
          />
        </div>

        <nav
          class="min-h-0 flex-1 overflow-y-auto py-3"
          aria-label="Secciones de administración"
        >
          <router-link
            v-for="item in principales"
            :key="item.nombre"
            :to="item.to"
            class="relative flex h-11 items-center gap-3 px-5 py-1 text-sm transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
            :class="[
              esActivaPrincipal(item) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste',
              colapsada ? 'md:justify-center md:px-0' : '',
            ]"
            :aria-current="esActivaPrincipal(item) ? 'page' : null"
            :aria-label="item.nombre"
            :title="colapsada ? item.nombre : null"
          >
            <span
              v-if="esActivaPrincipal(item)"
              class="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-coral-vivo"
              aria-hidden="true"
            ></span>
            <component
              :is="item.icono"
              class="h-5 w-5 shrink-0"
              :stroke-width="esActivaPrincipal(item) ? 2.2 : 1.75"
              aria-hidden="true"
            />
            <span :class="colapsada ? 'md:hidden' : ''">{{ item.nombre }}</span>
          </router-link>

          <div
            v-for="grupo in gruposVisibles"
            :key="grupo.id"
            role="group"
            :aria-labelledby="`grupo-${grupo.id}`"
            class="mt-4"
          >
            <div class="flex h-7 items-center px-5">
              <p
                :id="`grupo-${grupo.id}`"
                class="text-[11px] font-semibold uppercase tracking-[0.14em] text-inverso-contraste/40"
                :class="colapsada ? 'md:sr-only' : ''"
              >
                {{ grupo.nombre }}
              </p>
              <div
                v-if="colapsada"
                class="hidden h-px flex-1 bg-blanco/10 md:block"
                aria-hidden="true"
              ></div>
            </div>

            <div class="space-y-0.5">
              <template v-for="item in grupo.items" :key="item.nombre">
                <router-link
                  v-if="item.disponible !== false"
                  :to="item.to"
                  class="relative flex h-11 items-center gap-3 px-5 py-1 text-sm transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
                  :class="[
                    estaActivo(item) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste',
                    colapsada ? 'md:justify-center md:px-0' : '',
                  ]"
                  :aria-current="ariaActual(item)"
                  :aria-label="item.nombre"
                  :title="colapsada ? item.nombre : null"
                >
                  <span
                    v-if="estaActivo(item)"
                    class="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-coral-vivo"
                    aria-hidden="true"
                  ></span>
                  <component
                    :is="item.icono"
                    class="h-5 w-5 shrink-0"
                    :stroke-width="estaActivo(item) ? 2.2 : 1.75"
                    aria-hidden="true"
                  />
                  <span
                    class="min-w-0 line-clamp-2 whitespace-normal break-words leading-tight"
                    :class="colapsada ? 'md:hidden' : ''"
                  >{{ item.nombre }}</span>
                  <span
                    v-if="badgeDe(item)"
                    class="inline-flex min-w-5 items-center justify-center rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-semibold leading-none text-white tabular-nums"
                    :class="colapsada ? 'md:hidden' : ''"
                    :aria-label="`${badgeDe(item)} pendientes`"
                  >
                    {{ badgeDe(item) }}
                  </span>
                </router-link>

                <div
                  v-else
                  class="flex h-11 items-center gap-3 px-5 py-1 text-sm text-inverso-contraste/35 cursor-not-allowed select-none"
                  :class="colapsada ? 'md:justify-center md:px-0' : ''"
                  aria-disabled="true"
                  :title="colapsada ? item.nombre : null"
                >
                  <component :is="item.icono" class="h-5 w-5 shrink-0" :stroke-width="1.75" aria-hidden="true" />
                  <span class="min-w-0 flex-1 line-clamp-2 whitespace-normal break-words leading-tight" :class="colapsada ? 'md:hidden' : ''">{{ item.nombre }}</span>
                  <span class="text-[10px] uppercase tracking-wide text-inverso-contraste/30" :class="colapsada ? 'md:hidden' : ''">Próximamente</span>
                </div>
              </template>
            </div>
          </div>

          <p v-if="!gruposVisibles.length" class="px-5 py-6 text-sm text-inverso-contraste/50">
            No hay secciones que coincidan con “{{ filtro.trim() }}”.
          </p>
        </nav>

        <div v-if="!filtrando" class="shrink-0 border-t border-blanco/10 px-2 py-3">
          <router-link
            v-for="item in utilidades"
            :key="item.nombre"
            :to="item.to"
            class="relative flex h-11 items-center gap-3 px-3 py-1 text-sm transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
            :class="[
              esActiva(item.to) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste',
              colapsada ? 'md:justify-center md:px-0' : '',
            ]"
            :aria-current="esActiva(item.to) ? 'page' : null"
            :aria-label="item.nombre"
            :title="colapsada ? item.nombre : null"
          >
            <span
              v-if="esActiva(item.to)"
              class="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-coral-vivo"
              aria-hidden="true"
            ></span>
            <component
              :is="item.icono"
              class="h-5 w-5 shrink-0"
              :stroke-width="esActiva(item.to) ? 2.2 : 1.75"
              aria-hidden="true"
            />
            <span class="min-w-0 line-clamp-2 whitespace-normal break-words leading-tight" :class="colapsada ? 'md:hidden' : ''">{{ item.nombre }}</span>
          </router-link>
        </div>
      </aside>

      <!-- Contenido -->
      <main class="min-w-0 flex-1 bg-blanco">
        <router-view :key="JSON.stringify(route.params)" />
      </main>
    </div>
    <AceptacionTerminosModal />
  </div>
</template>
