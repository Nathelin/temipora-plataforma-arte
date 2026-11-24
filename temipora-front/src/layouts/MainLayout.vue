<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '../components/BrandLogo.vue'
import ImagenOptimizada from '../components/ImagenOptimizada.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import MenuTemaMovil from '../components/MenuTemaMovil.vue'
import PanelNotificaciones from '../components/PanelNotificaciones.vue'
import AceptacionTerminosModal from '../components/AceptacionTerminosModal.vue'
import { useMensajes } from '../composables/useMensajes'
import { useMensajeriaSocket } from '../composables/useMensajeriaSocket'
import { useNotificacionesSistema } from '../composables/useNotificacionesSistema'
import { useCarrito } from '../composables/useCarrito'
import { cuandoEsteLibre } from '../composables/useTareasDiferidas'
import { obtenerContacto } from '../servicios/contacto'
import { obtenerMiEstadoCuenta } from '../servicios/miEstadoCuenta'

const route = useRoute()
const router = useRouter()

const token = ref(localStorage.getItem('token') || '')
const rol = ref(localStorage.getItem('rol') || '')
const nombreUsuario = ref(localStorage.getItem('nombreUsuario') || '')
const urlFotoPerfil = ref(localStorage.getItem('urlFotoPerfil') || '')

const contactoEmail = ref('temiporasupport@gmail.com')
const estadoCuenta = ref(null)

let cancelarTareasIniciales = null

async function cargarEstadoCuenta() {
  if (!token.value || rol.value === 'ADMIN') {
    estadoCuenta.value = null
    return
  }
  try {
    const res = await obtenerMiEstadoCuenta()
    estadoCuenta.value = res.data
  } catch {
    estadoCuenta.value = null
  }
}

const avisoPagoPendiente = computed(() => Boolean(
  (estadoCuenta.value?.deudas?.length || 0) > 0
  && estadoCuenta.value?.alcance !== 'PAGOS',
))
const cuentaSuspendida = computed(() => Boolean(
  estadoCuenta.value?.sancionado
  && estadoCuenta.value?.alcance === 'COMPRAS',
))
const primeraDeuda = computed(() => estadoCuenta.value?.deudas?.[0] || null)

onMounted(() => {
  cuandoEsteLibre(async () => {
    contactoEmail.value = await obtenerContacto()
    await cargarEstadoCuenta()
  })
})

const menuAbierto = ref(false)
const menuMovilAbierto = ref(false)
const temaMovilAbierto = ref(false)
const contenedorMenu = ref(null)
const contenedorNav = ref(null)

const estaLogueado = computed(() => !!token.value)
const esAdmin = computed(() => rol.value === 'ADMIN')
const esArtista = computed(() => rol.value === 'ARTISTA')
const puedeVerActividad = computed(() => estaLogueado.value && !esAdmin.value)
const enActividad = computed(() => route.path.startsWith('/compras')
  || route.path.startsWith('/pedidos')
  || route.path.startsWith('/solicitudes'))
const enCarrito = computed(() => route.path.startsWith('/carrito'))
const enPanel = computed(() => route.path.startsWith('/panel'))
const enMensajes = computed(() => route.path.startsWith('/mensajes')
  || route.path.startsWith('/panel/mensajes'))
const enFavoritos = computed(() => route.path.startsWith('/favoritos'))
const enExplorar = computed(() => route.path.startsWith('/explorar'))
const enServicios = computed(() => route.path.startsWith('/servicios'))
const enProductos = computed(() => route.path.startsWith('/productos'))
const enAdmin = computed(() => route.path.startsWith('/admin'))
// Con sesión el logo lleva a Explorar; sin sesión, a la landing pública.
// No se redirige `/` para no dejar la landing inalcanzable.
const destinoLogo = computed(() => (estaLogueado.value ? '/explorar' : '/'))

const {
  noLeidos: mensajesNoLeidos,
  solicitudesPendientes,
  iniciarPolling: iniciarPollingMensajes,
  detenerPolling: detenerPollingMensajes,
  cargarContador: cargarContadorMensajes,
  limpiar: limpiarMensajes,
} = useMensajes()

const {
  limpiar: limpiarNotificaciones,
  detenerPolling: detenerPollingNotificaciones,
} = useNotificacionesSistema()

const {
  cantidad: cantidadCarrito,
  cargar: cargarCarrito,
  limpiar: limpiarCarrito,
} = useCarrito()

const {
  conectar: conectarSocket,
  desconectar: desconectarSocket,
  suscribir: suscribirSocket,
} = useMensajeriaSocket()
let quitarSuscripcionSocket = null
const totalMensajes = computed(
  () => mensajesNoLeidos.value + solicitudesPendientes.value,
)
const badgeMensajes = computed(() =>
  totalMensajes.value > 9 ? '9+' : String(totalMensajes.value),
)

function alternarMenu() {
  menuAbierto.value = !menuAbierto.value
}

function alternarMenuMovil() {
  menuMovilAbierto.value = !menuMovilAbierto.value
  if (!menuMovilAbierto.value) {
    temaMovilAbierto.value = false
  }
}

function clickDentroDe(event, contenedor) {
  if (!contenedor) {
    return false
  }
  return event.composedPath().includes(contenedor)
}

function manejarClickExterior(event) {
  if (contenedorMenu.value && !clickDentroDe(event, contenedorMenu.value)) {
    menuAbierto.value = false
  }
  if (contenedorNav.value && !clickDentroDe(event, contenedorNav.value)) {
    menuMovilAbierto.value = false
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    menuAbierto.value = false
    menuMovilAbierto.value = false
    temaMovilAbierto.value = false
  }
}

function actualizarFotoPerfil(event) {
  urlFotoPerfil.value = event.detail?.url || ''
}

onMounted(() => {
  window.addEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
  document.addEventListener('click', manejarClickExterior)
  document.addEventListener('keydown', manejarTecla)

  // Contadores, carrito y WebSocket no son necesarios para pintar la página:
  // esperan a que el navegador quede libre para no competir con la vista.
  cancelarTareasIniciales = cuandoEsteLibre(() => {
    if (!esAdmin.value) {
      iniciarPollingMensajes()
    }

    if (estaLogueado.value && !esAdmin.value) {
      cargarCarrito()
      conectarSocket()
      quitarSuscripcionSocket = suscribirSocket((evento) => {
        if (evento?.tipo === 'MENSAJE'
          && evento.mensaje
          && !evento.mensaje.esPropio) {
          cargarContadorMensajes({ silencioso: true })
        }
        if (evento?.tipo === 'CONVERSACION') {
          cargarContadorMensajes({ silencioso: true })
        }
      })
    }
  })
})

onBeforeUnmount(() => {
  cancelarTareasIniciales?.()
  window.removeEventListener('perfil-foto-actualizada', actualizarFotoPerfil)
  document.removeEventListener('click', manejarClickExterior)
  document.removeEventListener('keydown', manejarTecla)
  detenerPollingMensajes()
  quitarSuscripcionSocket?.()
  desconectarSocket()
})

function cerrarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  localStorage.removeItem('monedaPreferida')
  limpiarNotificaciones()
  detenerPollingNotificaciones()
  limpiarMensajes()
  limpiarCarrito()
  desconectarSocket()
  router.push({ name: 'login' })
}

function irAPerfil() {
  menuMovilAbierto.value = false
  router.push({ name: 'perfil', params: { nombreUsuario: nombreUsuario.value } })
}
</script>

<template>
  <div class="min-h-screen w-full flex flex-col bg-crema text-cafe">
    <header class="sticky top-0 z-30 border-b border-rosa/40 bg-crema/85 backdrop-blur-md">
      <div class="mx-auto flex h-14 max-w-screen-2xl items-center justify-between gap-4 px-4 sm:h-16 sm:px-8">
        <!-- Marca y navegación -->
        <div class="flex items-center gap-7">
          <router-link
            :to="destinoLogo"
            class="group inline-flex items-center rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-4 focus-visible:ring-offset-crema"
            :aria-label="estaLogueado ? 'Temipora, ir a explorar' : 'Temipora, ir al inicio'"
          >
            <BrandLogo solo-nombre />
          </router-link>

          <nav class="hidden items-center gap-6 lg:flex" aria-label="Navegación principal">
            <router-link
              v-if="estaLogueado"
              to="/explorar"
              class="relative text-sm font-medium text-cafe transition hover:text-coral after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-coral-vivo after:transition-all after:duration-200 hover:after:w-full focus-visible:text-coral focus-visible:outline-none"
              :class="enExplorar ? 'text-coral after:w-full' : ''"
              :aria-current="enExplorar ? 'page' : undefined"
            >
              Explorar
            </router-link>
            <router-link
              to="/servicios"
              class="relative text-sm font-medium text-cafe transition hover:text-coral after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-coral-vivo after:transition-all after:duration-200 hover:after:w-full focus-visible:text-coral focus-visible:outline-none"
              :class="enServicios ? 'text-coral after:w-full' : ''"
              :aria-current="enServicios ? 'page' : undefined"
            >
              Servicios
            </router-link>
            <router-link
              to="/productos"
              class="relative text-sm font-medium text-cafe transition hover:text-coral after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-coral-vivo after:transition-all after:duration-200 hover:after:w-full focus-visible:text-coral focus-visible:outline-none"
              :class="enProductos ? 'text-coral after:w-full' : ''"
              :aria-current="enProductos ? 'page' : undefined"
            >
              Productos
            </router-link>
            <router-link
              v-if="puedeVerActividad"
              to="/compras"
              class="relative inline-flex items-center gap-2 rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-sm font-semibold text-coral transition hover:bg-coral/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
              :class="enActividad ? 'ring-2 ring-coral/40' : ''"
              :aria-current="enActividad ? 'page' : undefined"
            >
              Mi actividad
            </router-link>
            <router-link
              v-if="estaLogueado && esAdmin"
              :to="{ name: 'admin-resumen' }"
              class="group inline-flex min-h-10 items-center gap-2 rounded-full bg-coral px-4 text-sm font-bold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:-translate-y-0.5 hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
              :class="enAdmin ? 'ring-2 ring-granate/30 ring-offset-2 ring-offset-crema' : ''"
              :aria-current="enAdmin ? 'page' : undefined"
              aria-label="Ir al panel de administración"
            >
              <svg class="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
              </svg>
              <span>Panel</span>
              <span class="transition-transform duration-200 group-hover:translate-x-0.5" aria-hidden="true">→</span>
            </router-link>
          </nav>
        </div>

        <!-- Acciones -->
        <div class="flex items-center gap-2.5 sm:gap-3">
          <router-link
            v-if="estaLogueado && esArtista"
            to="/panel"
            class="group hidden min-h-10 items-center gap-2 rounded-full bg-coral px-4 text-sm font-bold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:-translate-y-0.5 hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema lg:inline-flex"
            :class="enPanel ? 'ring-2 ring-granate/30 ring-offset-2 ring-offset-crema' : ''"
            aria-label="Ir a mi panel de artista"
          >
            <svg class="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
            </svg>
            <span>Panel de artista</span>
            <span class="transition-transform duration-200 group-hover:translate-x-0.5" aria-hidden="true">→</span>
          </router-link>

          <ThemeToggle v-if="!estaLogueado" class="hidden sm:inline-flex" />

          <router-link
            v-if="estaLogueado && !esAdmin"
            to="/carrito"
            class="relative hidden h-11 w-11 items-center justify-center rounded-full border border-rosa/60 bg-blanco/70 text-granate transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 sm:inline-flex"
            :class="enCarrito ? 'ring-2 ring-coral/40' : ''"
            :aria-current="enCarrito ? 'page' : undefined"
            :aria-label="cantidadCarrito
              ? `Carrito, ${cantidadCarrito} producto(s)`
              : 'Carrito'"
            title="Carrito"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.5l2.1 10.5a2.25 2.25 0 0 0 2.2 1.8h8.4a2.25 2.25 0 0 0 2.2-1.8L20.25 6H6.75" />
              <circle cx="9" cy="19.5" r="1.25" />
              <circle cx="17.25" cy="19.5" r="1.25" />
            </svg>
            <span
              v-if="cantidadCarrito"
              class="absolute -right-0.5 -top-0.5 inline-flex min-w-4 items-center justify-center rounded-full bg-coral px-1 py-0.5 text-[10px] font-bold leading-none text-white ring-2 ring-crema"
              aria-hidden="true"
            >
              {{ cantidadCarrito }}
            </span>
          </router-link>

          <router-link
            v-if="estaLogueado && !esAdmin"
            to="/mensajes"
            class="relative inline-flex h-11 w-11 items-center justify-center rounded-full border border-rosa/60 bg-blanco/70 text-granate transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
            :class="enMensajes ? 'ring-2 ring-coral/40' : ''"
            :aria-current="enMensajes ? 'page' : undefined"
            :aria-label="totalMensajes
              ? `Mensajes, ${totalMensajes} pendientes`
              : 'Mensajes'"
            title="Mensajes"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" />
            </svg>
            <span
              v-if="totalMensajes"
              class="absolute -right-0.5 -top-0.5 inline-flex min-w-4 items-center justify-center rounded-full bg-coral px-1 py-0.5 text-[10px] font-bold leading-none text-white ring-2 ring-crema"
              aria-hidden="true"
            >
              {{ badgeMensajes }}
            </span>
          </router-link>

          <PanelNotificaciones v-if="estaLogueado" />

          <template v-if="!estaLogueado">
            <router-link
              to="/login"
              class="hidden min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco/60 px-5 text-sm font-semibold text-granate transition hover:border-coral/40 hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 sm:inline-flex"
            >
              Iniciar sesión
            </router-link>
            <router-link
              to="/registrarse"
              class="hidden min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema sm:inline-flex"
            >
              Únete
            </router-link>
          </template>

          <template v-else>
            <div ref="contenedorMenu" class="relative">
              <button
                class="group flex min-h-11 items-center gap-2 rounded-full p-0.5 pr-1.5 transition hover:bg-rosa/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
                aria-haspopup="menu"
                :aria-expanded="menuAbierto"
                aria-label="Abrir menú de usuario"
                @click="alternarMenu"
              >
                <ImagenOptimizada
                  v-if="urlFotoPerfil"
                  :src="urlFotoPerfil"
                  alt=""
                  sizes="40px"
                  class="h-8.5 w-8.5 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
                />
                <span
                  v-else
                  class="flex h-8.5 w-8.5 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate shadow-sm ring-1 ring-white/70"
                >
                  {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
                </span>
                <span class="hidden text-sm font-semibold text-cafe lg:inline">{{ nombreUsuario }}</span>
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
                  :class="enFavoritos ? 'bg-rosa/20 font-semibold text-granate' : ''"
                  role="menuitem"
                  :aria-current="enFavoritos ? 'page' : undefined"
                  @click="menuAbierto = false; router.push({ name: 'favoritos' })"
                >
                  Favoritos
                </button>
                <button
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="menuAbierto = false; router.push({ name: 'configuracion-perfil' })"
                >
                  Configuración
                </button>
                <button
                  class="w-full rounded-xl px-3.5 py-2.5 text-left text-sm text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  role="menuitem"
                  @click="menuAbierto = false; router.push({ name: 'configuracion-apariencia' })"
                >
                  Apariencia
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
          </template>

          <!-- Menú móvil -->
          <div ref="contenedorNav" class="relative lg:hidden">
            <button
              type="button"
              class="relative inline-flex h-11 w-11 items-center justify-center rounded-full border border-rosa/60 bg-blanco/70 text-granate transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
              :aria-expanded="menuMovilAbierto"
              :aria-label="cantidadCarrito
                ? `Abrir menú de navegación, ${cantidadCarrito} producto(s) en el carrito`
                : 'Abrir menú de navegación'"
              @click="alternarMenuMovil"
            >
              <svg v-if="!menuMovilAbierto" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="M4 7h16M4 12h16M4 17h16" />
              </svg>
              <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <path d="m6 6 12 12M18 6 6 18" />
              </svg>
              <span
                v-if="cantidadCarrito"
                class="absolute -right-0.5 -top-0.5 inline-flex min-w-4 items-center justify-center rounded-full bg-coral px-1 py-0.5 text-[10px] font-bold leading-none text-white ring-2 ring-crema sm:hidden"
                aria-hidden="true"
              >
                {{ cantidadCarrito }}
              </span>
            </button>

            <div
              v-if="menuMovilAbierto"
              class="absolute right-0 z-50 mt-3 w-68 origin-top-right rounded-3xl border border-rosa/50 bg-blanco p-2 shadow-[0_18px_40px_rgba(74,29,46,0.18)]"
            >
              <router-link
                v-if="estaLogueado && esArtista"
                to="/panel"
                class="mb-1 flex w-full items-center justify-between gap-2 rounded-2xl bg-coral px-4 py-3 text-sm font-bold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
                aria-label="Ir a mi panel de artista"
                @click="menuMovilAbierto = false"
              >
                <span class="flex items-center gap-2.5">
                  <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
                  </svg>
                  <span>Panel de artista</span>
                </span>
                <span aria-hidden="true">→</span>
              </router-link>
              <div class="pt-1">
                <p class="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/40">
                  Catálogo
                </p>
                <router-link
                  v-if="estaLogueado"
                  to="/explorar"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  :class="enExplorar ? 'bg-rosa/40 text-granate' : ''"
                  :aria-current="enExplorar ? 'page' : undefined"
                  @click="menuMovilAbierto = false"
                >
                  Explorar
                </router-link>
                <router-link
                  to="/servicios"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  :class="enServicios ? 'bg-rosa/40 text-granate' : ''"
                  :aria-current="enServicios ? 'page' : undefined"
                  @click="menuMovilAbierto = false"
                >
                  Servicios
                </router-link>
                <router-link
                  to="/productos"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  :class="enProductos ? 'bg-rosa/40 text-granate' : ''"
                  :aria-current="enProductos ? 'page' : undefined"
                  @click="menuMovilAbierto = false"
                >
                  Productos
                </router-link>
              </div>

              <div v-if="puedeVerActividad" class="mt-1 border-t border-rosa/40 pt-2">
                <router-link
                  to="/compras"
                  class="flex w-full items-center justify-center gap-2 rounded-2xl border border-coral/25 bg-coral/10 px-4 py-3 text-sm font-semibold text-coral transition hover:bg-coral/15"
                  :class="enActividad ? 'ring-2 ring-coral/40' : ''"
                  :aria-current="enActividad ? 'page' : undefined"
                  @click="menuMovilAbierto = false"
                >
                  Mi actividad
                </router-link>
                <router-link
                  to="/carrito"
                  class="mt-1 flex w-full items-center justify-center gap-2 rounded-2xl border border-rosa px-4 py-3 text-sm font-semibold text-granate transition hover:bg-rosa/20"
                  :class="enCarrito ? 'ring-2 ring-coral/40' : ''"
                  :aria-current="enCarrito ? 'page' : undefined"
                  @click="menuMovilAbierto = false"
                >
                  Carrito
                  <span v-if="cantidadCarrito" class="rounded-full bg-coral px-2 py-0.5 text-[10px] font-bold text-white">
                    {{ cantidadCarrito }}
                  </span>
                </router-link>
              </div>

              <div v-if="estaLogueado && esAdmin" class="mt-1 border-t border-rosa/40 pt-2">
                <router-link
                  :to="{ name: 'admin-resumen' }"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  :class="enAdmin ? 'bg-rosa/40 text-granate' : ''"
                  :aria-current="enAdmin ? 'page' : undefined"
                  @click="menuMovilAbierto = false"
                >
                  Panel de administración
                </router-link>
              </div>

              <div v-if="!estaLogueado" class="mt-1 border-t border-rosa/40 pt-2">
                <p class="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/40">
                  Acceso
                </p>
                <MenuTemaMovil
                  id-panel="selector-tema-movil-invitado"
                  :abierto="temaMovilAbierto"
                  @alternar="temaMovilAbierto = !temaMovilAbierto"
                  @seleccionar="temaMovilAbierto = false"
                />
                <router-link
                  to="/login"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  @click="menuMovilAbierto = false"
                >
                  Iniciar sesión
                </router-link>
                <router-link
                  to="/registrarse"
                  class="mt-1 flex w-full items-center justify-center rounded-2xl bg-coral px-4 py-3 text-sm font-semibold text-white transition hover:bg-rojizo"
                  @click="menuMovilAbierto = false"
                >
                  Crear cuenta
                </router-link>
              </div>

              <div v-else class="mt-1 border-t border-rosa/40 pt-2">
                <p class="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/40">
                  Cuenta
                </p>
                <button
                  type="button"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-left text-sm font-semibold text-cafe transition hover:bg-rosa/20 hover:text-granate"
                  :class="enFavoritos ? 'bg-rosa/40 text-granate' : ''"
                  :aria-current="enFavoritos ? 'page' : undefined"
                  @click="menuMovilAbierto = false; router.push({ name: 'favoritos' })"
                >
                  Favoritos
                </button>
                <MenuTemaMovil
                  id-panel="selector-tema-movil"
                  :abierto="temaMovilAbierto"
                  @alternar="temaMovilAbierto = !temaMovilAbierto"
                  @seleccionar="temaMovilAbierto = false"
                />
                <button
                  type="button"
                  class="flex w-full items-center rounded-2xl px-4 py-3 text-left text-sm font-semibold text-rojizo transition hover:bg-rosa/20"
                  @click="menuMovilAbierto = false; cerrarSesion()"
                >
                  Cerrar sesión
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <div
      v-if="avisoPagoPendiente"
      class="border-b border-durazno/60 bg-durazno/30"
    >
      <div class="mx-auto flex max-w-screen-2xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-8">
        <p class="text-sm leading-relaxed text-cafe">
          <template v-if="cuentaSuspendida">
            Tenés un pago pendiente de una entrega final. Tus compras están
            suspendidas y solo podés mensajear al artista al que le debés.
          </template>
          <template v-else>
            Tenés un pago vencido de una entrega final. Regularizalo para evitar
            que se suspendan tus compras.
          </template>
        </p>
        <router-link
          v-if="primeraDeuda"
          :to="{ name: 'pedido-detalle', params: { idPedido: primeraDeuda.idPedido } }"
          class="inline-flex min-h-9 items-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Regularizar pago
        </router-link>
      </div>
    </div>

    <main class="flex-1 w-full">
      <!-- La key cubre cualquier parámetro (servicio, perfil, pedido, token):
           sin esto Vue reutiliza la vista al cambiar solo el parámetro y las
           pantallas que cargan en onMounted no se recargan. -->
      <router-view :key="JSON.stringify(route.params)" />
    </main>


    
    <footer class="bg-granate text-inverso-contraste mt-auto">
      <div class="mx-auto max-w-screen-2xl px-6 py-8 md:flex md:items-center md:justify-between lg:px-8">
        <div class="flex flex-wrap justify-center gap-x-6 gap-y-2 md:order-2">
          <router-link to="/about" class="text-sm leading-6 hover:text-inverso-suave transition">
            Sobre Nosotros
          </router-link>
          <router-link to="/terminos" class="text-sm leading-6 hover:text-inverso-suave transition">
            Términos de Uso
          </router-link>
          <router-link to="/contacto" class="text-sm leading-6 hover:text-inverso-suave transition">
            Contacto
          </router-link>
        </div>

        <div class="mt-8 md:order-1 md:mt-0 text-center md:text-left">
          <p class="text-xs leading-5 text-inverso-suave/80">
            &copy; 2026 Temipora. Todos los derechos reservados.
          </p>
          <p class="text-xs leading-5 text-inverso-suave/80 mt-1">
            Contacto:
            <a :href="`mailto:${contactoEmail}`" class="hover:underline text-inverso-contraste">
              {{ contactoEmail }}
            </a>
          </p>
        </div>
      </div>
    </footer>

    <AceptacionTerminosModal />
  </div>
</template>