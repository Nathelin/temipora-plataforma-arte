<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '../components/BrandLogo.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import PanelNotificaciones from '../components/PanelNotificaciones.vue'
import api from '../servicios/api'
import {
  Ban,
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
  Star,
  Tags,
  Users,
  Wrench,
} from '@lucide/vue'

const route = useRoute()
const router = useRouter()

const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')
const menuAbierto = ref(false)
const menuAdminAbierto = ref(false)
const contenedorMenu = ref(null)

const ABIERTOS_KEY = 'adminMenuAbiertos'

const grupos = [
  {
    id: 'comunidad',
    nombre: 'Comunidad',
    items: [
      { nombre: 'Usuarios', to: '/admin/usuarios', icono: Users },
      {
        nombre: 'Reseñas',
        icono: Star,
        items: [
          { nombre: 'Criterios de evaluación', to: '/admin/resenas/criterios' },
          { nombre: 'Configuración de reseñas', to: '/admin/resenas/configuracion' },
        ],
      },
      {
        nombre: 'Moderación',
        icono: ShieldAlert,
        badge: 'reportes',
        items: [
          { nombre: 'Reseñas reportadas', to: '/admin/resenas/reportadas' },
          { nombre: 'Historial de reportes', to: '/admin/resenas/historial' },
        ],
      },
      { nombre: 'Sanciones', icono: Ban, disponible: false },
    ],
  },
  {
    id: 'catalogo',
    nombre: 'Catálogo',
    items: [
      { nombre: 'Tipos de producto', to: '/admin/tipos/producto', icono: Package },
      {
        nombre: 'Servicios',
        icono: Wrench,
        items: [
          { nombre: 'Tipos', to: '/admin/tipos/servicio' },
          { nombre: 'Archivados', to: '/admin/servicios/archivados' },
        ],
      },
      {
        nombre: 'Arte',
        icono: Palette,
        items: [
          { nombre: 'Medios', to: '/admin/arte/medios' },
          { nombre: 'Tipos de arte', to: '/admin/arte/tipos' },
          { nombre: 'Materiales', to: '/admin/arte/materiales' },
        ],
      },
      {
        nombre: 'Etiquetas y categorías',
        icono: Tags,
        badge: 'etiquetas',
        items: [
          { nombre: 'Etiquetas', to: '/admin/etiquetas' },
          { nombre: 'Categorías', to: '/admin/etiquetas/categorias' },
        ],
      },
    ],
  },
  {
    id: 'comercio',
    nombre: 'Comercio',
    items: [
      {
        nombre: 'Presupuestos e hitos',
        icono: Receipt,
        items: [
          { nombre: 'Vigencia de presupuestos', to: '/admin/ajustes/presupuestos' },
          { nombre: 'Reglas de pedidos e hitos', to: '/admin/ajustes/pedidos' },
        ],
      },
      { nombre: 'Promociones y reactivación', to: '/admin/campanas/promociones', icono: Megaphone },
      { nombre: 'Comisiones', to: '/admin/ajustes/comisiones', icono: Percent },
      {
        nombre: 'Monedas y cotización',
        icono: Coins,
        items: [
          { nombre: 'Monedas', to: '/admin/ajustes/monedas' },
          { nombre: 'Cotización', to: '/admin/ajustes/cotizacion' },
        ],
      },
    ],
  },
  {
    id: 'sistema',
    nombre: 'Sistema',
    items: [
      { nombre: 'Países', to: '/admin/ajustes/paises', icono: Globe },
      { nombre: 'Mensajería', to: '/admin/ajustes/mensajeria', icono: MessageSquare },
      { nombre: 'Términos de la plataforma', to: '/admin/ajustes/terminos-plataforma', icono: FileText },
    ],
  },
]

const utilidades = [
  { nombre: 'Auditoría', icono: Clock, disponible: false },
]

const resumen = { nombre: 'Resumen', to: '/admin', icono: LayoutDashboard }

const esActiva = (to) => Boolean(to) && route.path === to
const grupoActivo = (item) => Boolean(item.items?.some((sub) => esActiva(sub.to)))

function leerAbiertos() {
  const guardado = leerJson(ABIERTOS_KEY)
  return Object.fromEntries(
    grupos.flatMap((grupo) =>
      grupo.items
        .filter((item) => item.items)
        .map((item) => [item.nombre, Boolean(guardado[item.nombre])]),
    ),
  )
}

function leerJson(clave) {
  try {
    return JSON.parse(localStorage.getItem(clave) || '{}') || {}
  } catch {
    return {}
  }
}

const abiertos = ref(leerAbiertos())

const COLAPSADA_KEY = 'adminMenuColapsada'
const colapsada = ref(localStorage.getItem(COLAPSADA_KEY) === '1')

function alternarColapsada() {
  colapsada.value = !colapsada.value
  try {
    localStorage.setItem(COLAPSADA_KEY, colapsada.value ? '1' : '0')
  } catch {
    /* sin persistencia disponible */
  }
}

function esEscritorio() {
  return window.matchMedia('(min-width: 768px)').matches
}

function alternar(nombre) {
  if (colapsada.value && esEscritorio()) {
    colapsada.value = false
    guardarColapsada()
    abiertos.value[nombre] = true
    guardarAbiertos()
    return
  }
  abiertos.value[nombre] = !abiertos.value[nombre]
  guardarAbiertos()
}

function guardarAbiertos() {
  try {
    localStorage.setItem(ABIERTOS_KEY, JSON.stringify(abiertos.value))
  } catch {
    /* sin persistencia disponible */
  }
}

function guardarColapsada() {
  try {
    localStorage.setItem(COLAPSADA_KEY, colapsada.value ? '1' : '0')
  } catch {
    /* sin persistencia disponible */
  }
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
    .map((grupo) => {
      const items = grupo.items
        .map((item) => {
          if (!item.items) {
            return coincide(item.nombre) ? item : null
          }
          if (coincide(item.nombre)) return item
          const hijos = item.items.filter((sub) => coincide(sub.nombre))
          return hijos.length ? { ...item, items: hijos } : null
        })
        .filter(Boolean)
      return { ...grupo, items }
    })
    .filter((grupo) => grupo.items.length)
})

watch(
  () => route.path,
  (ruta) => {
    menuAdminAbierto.value = false
    for (const grupo of grupos) {
      for (const item of grupo.items) {
        if (item.items?.some((sub) => sub.to === ruta)) {
          abiertos.value[item.nombre] = true
        }
      }
    }
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
            :to="resumen.to"
            class="relative flex h-11 items-center gap-3 px-5 py-1 text-sm transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
            :class="[
              esActiva(resumen.to) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste',
              colapsada ? 'md:justify-center md:px-0' : '',
            ]"
            :aria-current="esActiva(resumen.to) ? 'page' : null"
            :aria-label="resumen.nombre"
            :title="colapsada ? resumen.nombre : null"
          >
            <span
              v-if="esActiva(resumen.to)"
              class="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-coral-vivo"
              aria-hidden="true"
            ></span>
            <component
              :is="resumen.icono"
              class="h-5 w-5 shrink-0"
              :stroke-width="esActiva(resumen.to) ? 2.2 : 1.75"
              aria-hidden="true"
            />
            <span :class="colapsada ? 'md:hidden' : ''">{{ resumen.nombre }}</span>
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
                <template v-if="item.items">
                  <button
                    type="button"
                    class="relative flex h-11 w-full items-center gap-3 px-5 py-1 text-sm transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
                    :class="[
                      grupoActivo(item) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/65 hover:bg-white/10 hover:text-inverso-contraste',
                      colapsada ? 'md:justify-center md:px-0' : '',
                    ]"
                    :aria-expanded="filtrando || !!abiertos[item.nombre]"
                    :aria-label="item.nombre"
                    :title="colapsada ? item.nombre : null"
                    @click="alternar(item.nombre)"
                  >
                    <span
                      v-if="grupoActivo(item)"
                      class="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-coral-vivo"
                      aria-hidden="true"
                    ></span>
                    <component
                      :is="item.icono"
                      class="h-5 w-5 shrink-0"
                      :stroke-width="grupoActivo(item) ? 2.2 : 1.75"
                      aria-hidden="true"
                    />
                    <span
                      class="min-w-0 line-clamp-2 whitespace-normal break-words text-left leading-tight"
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
                    <svg
                      class="ml-auto h-4 w-4 shrink-0 transition-transform duration-200 motion-reduce:transition-none"
                      :class="[(filtrando || abiertos[item.nombre]) ? 'rotate-90' : '', colapsada ? 'md:hidden' : '']"
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
                  <div
                    v-if="filtrando || abiertos[item.nombre]"
                    class="space-y-0.5 pb-1"
                    :class="colapsada ? 'md:hidden' : ''"
                  >
                    <router-link
                      v-for="sub in item.items"
                      :key="sub.to"
                      :to="sub.to"
                      class="relative flex min-h-9 items-center py-1 pl-[3.25rem] pr-5 text-[13px] transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
                      :class="esActiva(sub.to) ? 'bg-coral/25 text-white font-medium' : 'text-inverso-contraste/60 hover:bg-white/10 hover:text-inverso-contraste'"
                      :aria-current="esActiva(sub.to) ? 'page' : null"
                    >
                      <span
                        v-if="esActiva(sub.to)"
                        class="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-coral-vivo"
                        aria-hidden="true"
                      ></span>
                      <span class="min-w-0 line-clamp-2 whitespace-normal break-words leading-tight">{{ sub.nombre }}</span>
                    </router-link>
                  </div>
                </template>

                <router-link
                  v-else-if="item.disponible !== false"
                  :to="item.to"
                  class="relative flex h-11 items-center gap-3 px-5 py-1 text-sm transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
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
          <div
            v-for="item in utilidades"
            :key="item.nombre"
            class="flex h-11 items-center gap-3 px-3 py-1 text-sm text-inverso-contraste/35 cursor-not-allowed select-none"
            :class="colapsada ? 'md:justify-center md:px-0' : ''"
            aria-disabled="true"
            :title="colapsada ? item.nombre : null"
          >
            <component :is="item.icono" class="h-5 w-5 shrink-0" :stroke-width="1.75" aria-hidden="true" />
            <span class="min-w-0 flex-1 line-clamp-2 whitespace-normal break-words leading-tight" :class="colapsada ? 'md:hidden' : ''">{{ item.nombre }}</span>
            <span class="text-[10px] uppercase tracking-wide text-inverso-contraste/30" :class="colapsada ? 'md:hidden' : ''">Próximamente</span>
          </div>
        </div>
      </aside>

      <!-- Contenido -->
      <main class="min-w-0 flex-1 bg-blanco">
        <router-view />
      </main>
    </div>
  </div>
</template>
