<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { useSubirImagen } from '../composables/useSubirImagen'
import { useNotificaciones } from '../composables/useNotificaciones'
import TarjetaServicio from '../components/TarjetaServicio.vue'

const route = useRoute()
const router = useRouter()


const perfil = ref({
  idUsuario: null,
  nombreUsuario: '',
  nombreDisplay: '',
  rol: '',
  biografia: '',
  urlFotoPerfil: '',
  urlBanner: '',
  // placeholders hasta que existan sus endpoints
  portfolio: [],
  servicios: [],
  productos: [],
  pedidos: [],
})

const cargandoPerfil = ref(true)
const errorCarga = ref('')

const { exito, error: notificarError } = useNotificaciones()
const esFavorito = ref(false)
const cargandoEstadoFavorito = ref(false)
const actualizandoFavorito = ref(false)

async function cargarPerfil() {
  cargandoPerfil.value = true
  errorCarga.value = ''
  esFavorito.value = false
  cargandoEstadoFavorito.value = false
  actualizandoFavorito.value = false
  try {
    const nombreUsuario = route.params.nombreUsuario
    const res = await api.get(`/perfil/${nombreUsuario}`)
    const data = res.data

    perfil.value = {
      ...data,
      portfolio: [],
      servicios: [],
      productos: [],
      pedidos: [],
    }

    if (data.rol === 'ARTISTA') {
      await Promise.all([cargarPortfolio(true), cargarEtiquetasPortfolio(), cargarTiposArte(), cargarServicios()])
      void cargarEstadoFavorito()
    } else {
      totalPortfolio.value = 0
      etiquetasPortfolio.value = []
    }
  } catch (e) {
    errorCarga.value = 'No se pudo cargar el perfil.'
  } finally {
    cargandoPerfil.value = false
  }
}

// ---------------------------------------------------------------
// Portfolio con filtros y paginación (20 por página)
// ---------------------------------------------------------------

const TAMANO_PAGINA = 20

const filtros = ref({ titulo: '', etiqueta: '', tipoArte: '', sensible: '', orden: 'RECIENTE' })
const etiquetasPortfolio = ref([])
const tiposArte = ref([])
const paginaPortfolio = ref(0)
const totalPortfolio = ref(0)
const cargandoPortfolio = ref(false)
const errorListaPortfolio = ref('')

const hayFiltrosActivos = computed(() => {
  const f = filtros.value
  return !!(f.titulo.trim() || f.etiqueta || f.tipoArte || f.sensible)
})

const filtrosAbiertos = ref(false)

const cantidadFiltrosActivos = computed(() => {
  const f = filtros.value
  let total = 0
  if (f.titulo.trim()) total++
  if (f.etiqueta) total++
  if (f.tipoArte) total++
  if (f.sensible) total++
  if (f.orden !== 'RECIENTE') total++
  return total
})

let secuenciaPortfolio = 0

async function cargarPortfolio(resetear = true, pagina = 0) {
  if (perfil.value.rol !== 'ARTISTA' || !perfil.value.nombreUsuario) return
  const miSecuencia = ++secuenciaPortfolio
  cargandoPortfolio.value = true
  if (resetear) errorListaPortfolio.value = ''
  try {
    const f = filtros.value
    const params = { page: pagina, size: TAMANO_PAGINA, orden: f.orden || 'RECIENTE' }
    const titulo = f.titulo.trim()
    if (titulo) params.titulo = titulo
    if (f.etiqueta) params.idEtiqueta = f.etiqueta
    if (f.tipoArte) params.idTipoArte = f.tipoArte
    if (f.sensible) params.sensible = f.sensible

    const res = await api.get(`/portfolio/${perfil.value.nombreUsuario}/paginado`, { params })
    if (miSecuencia !== secuenciaPortfolio) return
    const contenido = res.data.content || []
    perfil.value.portfolio = resetear ? contenido : [...perfil.value.portfolio, ...contenido]
    paginaPortfolio.value = pagina
    totalPortfolio.value = res.data.totalElements || 0
  } catch (e) {
    if (miSecuencia !== secuenciaPortfolio) return
    if (resetear) {
      perfil.value.portfolio = []
      totalPortfolio.value = 0
      errorListaPortfolio.value = 'No se pudo cargar el portfolio.'
    }
  } finally {
    if (miSecuencia === secuenciaPortfolio) cargandoPortfolio.value = false
  }
}

async function cargarEtiquetasPortfolio() {
  const nombre = perfil.value.nombreUsuario
  if (perfil.value.rol !== 'ARTISTA' || !nombre) return
  try {
    const res = await api.get(`/portfolio/${nombre}/etiquetas`)
    if (perfil.value.nombreUsuario === nombre) etiquetasPortfolio.value = res.data
  } catch (e) {
    etiquetasPortfolio.value = []
  }
}

async function cargarTiposArte() {
  try {
    const res = await api.get('/tipos-arte')
    tiposArte.value = res.data
  } catch (e) {
    tiposArte.value = []
  }
}

// ---------------------------------------------------------------
// Servicios del artista
// ---------------------------------------------------------------

const cargandoServicios = ref(false)
const errorServicios = ref('')

async function cargarServicios() {
  const nombre = perfil.value.nombreUsuario
  if (perfil.value.rol !== 'ARTISTA' || !nombre) return
  cargandoServicios.value = true
  errorServicios.value = ''
  try {
    const res = await api.get(`/servicios/artista/${nombre}`)
    if (perfil.value.nombreUsuario === nombre) {
      perfil.value.servicios = res.data
    }
  } catch (e) {
    if (perfil.value.nombreUsuario === nombre) {
      perfil.value.servicios = []
      errorServicios.value = 'No se pudieron cargar los servicios.'
    }
  } finally {
    if (perfil.value.nombreUsuario === nombre) cargandoServicios.value = false
  }
}

function limpiarFiltros() {
  const f = filtros.value
  if (!hayFiltrosActivos.value && (f.orden || 'RECIENTE') === 'RECIENTE') return
  filtros.value = { titulo: '', etiqueta: '', tipoArte: '', sensible: '', orden: 'RECIENTE' }
}

async function cargarMasPortfolio() {
  if (cargandoPortfolio.value) return
  await cargarPortfolio(false, paginaPortfolio.value + 1)
}

let temporizadorFiltros = null
watch(filtros, () => {
  if (temporizadorFiltros) clearTimeout(temporizadorFiltros)
  temporizadorFiltros = setTimeout(() => cargarPortfolio(true), 350)
}, { deep: true })

watch(() => route.params.nombreUsuario, () => {
  filtros.value = { titulo: '', etiqueta: '', tipoArte: '', sensible: '', orden: 'RECIENTE' }
  etiquetasPortfolio.value = []
  paginaPortfolio.value = 0
  totalPortfolio.value = 0
  errorListaPortfolio.value = ''
  perfil.value.servicios = []
  errorServicios.value = ''
  esFavorito.value = false
  cargandoEstadoFavorito.value = false
  actualizandoFavorito.value = false
})

watch(() => route.params.nombreUsuario, cargarPerfil, { immediate: true })

const esArtista = computed(() => perfil.value.rol === 'ARTISTA')
const tabActiva = ref('portfolio')

const nombreParaMostrar = computed(() => perfil.value.nombreDisplay || perfil.value.nombreUsuario)

const esDuenio = computed(() => {
  const actual = localStorage.getItem('nombreUsuario') || ''
  return actual.toLowerCase() === perfil.value.nombreUsuario.toLowerCase()
})

const haySesion = computed(() => !!localStorage.getItem('token'))
const puedeMarcarFavorito = computed(() => esArtista.value && !esDuenio.value)

async function cargarEstadoFavorito() {
  const nombre = perfil.value.nombreUsuario
  esFavorito.value = false
  if (!nombre || !puedeMarcarFavorito.value || !haySesion.value) return

  cargandoEstadoFavorito.value = true
  try {
    const res = await api.get(`/favoritos/${encodeURIComponent(nombre)}`)
    if (perfil.value.nombreUsuario === nombre) {
      esFavorito.value = !!res.data?.esFavorito
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
    }
  } finally {
    if (perfil.value.nombreUsuario === nombre) {
      cargandoEstadoFavorito.value = false
    }
  }
}

async function alternarFavorito() {
  if (!haySesion.value) {
    router.push({ name: 'login' })
    return
  }
  if (
    !puedeMarcarFavorito.value ||
    actualizandoFavorito.value ||
    cargandoEstadoFavorito.value
  ) {
    return
  }

  const nombre = perfil.value.nombreUsuario
  actualizandoFavorito.value = true
  try {
    if (esFavorito.value) {
      await api.delete(`/favoritos/${encodeURIComponent(nombre)}`)
      if (perfil.value.nombreUsuario === nombre) {
        esFavorito.value = false
        exito(`Quitaste a @${nombre} de tus favoritos.`)
      }
    } else {
      await api.post(`/favoritos/${encodeURIComponent(nombre)}`)
      if (perfil.value.nombreUsuario === nombre) {
        esFavorito.value = true
        exito(`Agregaste a @${nombre} a tus favoritos.`)
      }
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    notificarError(e.response?.data?.message || 'No se pudieron actualizar tus favoritos.')
  } finally {
    actualizandoFavorito.value = false
  }
}

function fotoInicial(nombre) {
  return (nombre || '?').charAt(0).toUpperCase()
}

const { abrir } = useSubirImagen()

const imagenSeleccionada = ref(null)
const mostrarImagenSensible = ref(false)
const zoomActivo = ref(false)
const botonRevelar = ref(null)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

function textoSensible(valor) {
  return ETIQUETAS_SENSIBLE[valor] || valor || 'Contenido sensible'
}

function seleccionarImagen(imagen) {
  imagenSeleccionada.value = imagen
  mostrarImagenSensible.value = false
  zoomActivo.value = false
  errorDetalle.value = ''
  if (imagen?.requiereAdvertencia) {
    nextTick(() => botonRevelar.value?.focus())
  }
}

function abrirDetalleImagen(imagen) {
  seleccionarImagen(imagen)
}

function cerrarDetalleImagen() {
  imagenSeleccionada.value = null
  mostrarImagenSensible.value = false
  zoomActivo.value = false
}

function revelarImagenSensible() {
  mostrarImagenSensible.value = true
}

function ocultarImagenSensible() {
  mostrarImagenSensible.value = false
}

const indiceSeleccionado = computed(() =>
  perfil.value.portfolio.findIndex(o => o.idImagen === imagenSeleccionada.value?.idImagen),
)

const hayAnterior = computed(() => indiceSeleccionado.value > 0)
const haySiguiente = computed(() =>
  indiceSeleccionado.value >= 0 && indiceSeleccionado.value < perfil.value.portfolio.length - 1,
)

function irAnterior() {
  if (!hayAnterior.value) return
  seleccionarImagen(perfil.value.portfolio[indiceSeleccionado.value - 1])
}

function irSiguiente() {
  if (!haySiguiente.value) return
  seleccionarImagen(perfil.value.portfolio[indiceSeleccionado.value + 1])
}

function abrirZoom() {
  const img = imagenSeleccionada.value
  if (!img) return
  if (img.requiereAdvertencia && !mostrarImagenSensible.value) return
  zoomActivo.value = true
}

function cerrarZoom() {
  zoomActivo.value = false
}

function manejarTeclaDetalle(event) {
  if (!imagenSeleccionada.value) return
  if (event.key === 'Escape') {
    if (zoomActivo.value) {
      zoomActivo.value = false
    } else {
      cerrarDetalleImagen()
    }
    return
  }
  if (event.key === 'ArrowLeft') irAnterior()
  if (event.key === 'ArrowRight') irSiguiente()
}

onMounted(() => window.addEventListener('keydown', manejarTeclaDetalle))
onBeforeUnmount(() => window.removeEventListener('keydown', manejarTeclaDetalle))

const errorDetalle = ref('')

async function eliminarImagenPortfolio() {
  const img = imagenSeleccionada.value
  if (!img) return
  const confirmar = window.confirm(
    `¿Eliminar "${img.titulo}" de tu portfolio? Esta acción no se puede deshacer.`,
  )
  if (!confirmar) return

  errorDetalle.value = ''
  try {
    await api.post(`/portfolio/usuarios/${perfil.value.idUsuario}/imagenes/${img.idImagen}/baja`)
    perfil.value.portfolio = perfil.value.portfolio.filter(o => o.idImagen !== img.idImagen)
    totalPortfolio.value = Math.max(0, totalPortfolio.value - 1)
    if (hayFiltrosActivos.value) {
      await cargarPortfolio(true)
    }
    cerrarDetalleImagen()
  } catch (e) {
    errorDetalle.value = e.response?.data?.message || 'No se pudo eliminar la imagen.'
  }
}

function irAConfiguracion(seccion = 'perfil') {
  router.push({ name: `configuracion-${seccion}` })
}

function irASubirImagen(imagen = null) {
  abrir(imagen ? 'editar' : 'crear', imagen)
  router.push({ name: 'portfolio-subir' })
}
</script>

<template>
  <div class="w-full">
    <div v-if="cargandoPerfil" class="max-w-screen-2xl mx-auto px-4 sm:px-8 py-16 text-center text-cafe/60">
      Cargando perfil...
    </div>
    <div v-else-if="errorCarga" class="max-w-screen-2xl mx-auto px-4 sm:px-8 py-16 text-center text-rojizo">
      {{ errorCarga }}
    </div>
    <template v-else>
    <!-- Banner -->
    <div class="relative h-44 w-full overflow-hidden bg-linear-to-br from-rosa via-durazno to-mantequilla sm:h-64">
      <div class="absolute -left-10 top-6 h-44 w-44 rounded-full bg-white/20 blur-2xl" aria-hidden="true"></div>
      <div class="absolute right-10 top-8 h-28 w-28 rounded-full bg-coral/20 blur-xl" aria-hidden="true"></div>
      <img
        v-if="perfil.urlBanner"
        :src="perfil.urlBanner"
        alt="Banner"
        class="h-full w-full object-cover"
      />
      <div v-if="perfil.urlBanner" class="absolute inset-0 bg-linear-to-t from-granate/35 via-transparent to-transparent" aria-hidden="true"></div>
    </div>

    <div class="mx-auto max-w-screen-2xl px-4 sm:px-8">
      <div class="flex flex-col gap-4 border-b border-borde/50 pb-8 pt-2 sm:flex-row sm:items-end sm:gap-6 sm:pt-4">
        <div class="relative z-10 h-8 w-36 shrink-0 sm:h-10 sm:w-44">
          <div class="absolute bottom-0 left-0 h-36 w-36 overflow-hidden rounded-full border-4 border-white bg-coral/20 shadow-xl ring-4 ring-rosa/30 sm:h-44 sm:w-44">
            <img
              v-if="perfil.urlFotoPerfil"
              :src="perfil.urlFotoPerfil"
              alt="Perfil"
              class="h-full w-full object-cover"
            />
            <span
              v-else
              class="flex h-full w-full items-center justify-center font-display text-5xl font-extrabold text-granate"
            >
              {{ fotoInicial(perfil.nombreUsuario) }}
            </span>
          </div>
        </div>

        <div class="flex-1 sm:mt-0">
          <h1 class="font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">{{ nombreParaMostrar }}</h1>
          <p class="text-sm text-cafe/60">@{{ perfil.nombreUsuario }}</p>

          <div class="mt-2 flex max-w-2xl items-start gap-3">
            <p class="line-clamp-3 text-sm leading-relaxed text-cafe/80">
              {{ perfil.biografia || 'Sin biografía.' }}
            </p>
          </div>
        </div>

        <div class="mt-4 flex shrink-0 flex-wrap items-center gap-3 pb-1 sm:mt-0 sm:pb-3">
          <button
            v-if="puedeMarcarFavorito"
            type="button"
            class="inline-flex min-h-10 items-center gap-2 rounded-full border px-5 text-sm font-semibold transition disabled:cursor-wait disabled:opacity-60"
            :class="esFavorito
              ? 'border-coral bg-coral text-white hover:bg-rojizo'
              : 'border-coral/50 bg-superficie text-granate hover:bg-rosa/25'"
            :disabled="actualizandoFavorito || cargandoEstadoFavorito"
            :aria-pressed="esFavorito"
            @click="alternarFavorito"
          >
            <svg
              class="h-4 w-4"
              viewBox="0 0 24 24"
              :fill="esFavorito ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.8"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
            </svg>
            <span>{{ actualizandoFavorito ? 'Guardando...' : esFavorito ? 'En favoritos' : 'Favorito' }}</span>
          </button>
          <button v-if="esDuenio"
            class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-superficie px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25"
            @click="irAConfiguracion('privacidad')"
          >
            Preferencias
          </button>
          <button
            v-if="esDuenio"
            class="inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
            @click="irAConfiguracion('perfil')"
          >
            Editar perfil
          </button>
        </div>
      </div>

      <!-- Pestañas -->
      <div class="pb-6">
        <template v-if="esArtista">
          <div class="flex flex-wrap items-center gap-2 border-b border-borde/50 pb-1.5 pt-1.5">
            <button
              class="rounded-full px-5 py-2.5 text-sm font-semibold transition"
              :class="tabActiva === 'portfolio' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:bg-rosa/25 hover:text-granate'"
              @click="tabActiva = 'portfolio'"
            >
              Portfolio
            </button>
            <button
              class="rounded-full px-5 py-2.5 text-sm font-semibold transition"
              :class="tabActiva === 'servicios' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:bg-rosa/25 hover:text-granate'"
              @click="tabActiva = 'servicios'"
            >
              Servicios
            </button>
            <button
              class="rounded-full px-5 py-2.5 text-sm font-semibold transition"
              :class="tabActiva === 'productos' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:bg-rosa/25 hover:text-granate'"
              @click="tabActiva = 'productos'"
            >
              Productos
            </button>
          </div>

            <div v-if="tabActiva === 'portfolio'" class="mt-6">
              <!-- Barra compacta de filtros (siempre inicia colapsada) -->
              <div class="mb-6 rounded-3xl border border-borde/60 bg-superficie p-4 shadow-[0_10px_28px_rgba(74,29,46,0.06)]">
                <div class="flex flex-wrap items-center gap-3">
                  <div class="flex-1 min-w-44 relative">
                    <input
                      v-model="filtros.titulo"
                      type="text"
                      placeholder="Buscar por título..."
                      class="block w-full rounded-full border-0 bg-crema py-2.5 pl-4 pr-9 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
                    />
                    <button
                      v-if="filtros.titulo"
                      type="button"
                      class="absolute right-2.5 top-1/2 -translate-y-1/2 text-cafe/50 hover:text-granate"
                      aria-label="Limpiar búsqueda"
                      @click="filtros.titulo = ''"
                    >
                      ✕
                    </button>
                  </div>

                  <button
                    type="button"
                    class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/25"
                    :aria-expanded="filtrosAbiertos"
                    @click="filtrosAbiertos = !filtrosAbiertos"
                  >
                    Filtros
                    <span
                      v-if="cantidadFiltrosActivos"
                      class="ml-1.5 inline-flex items-center justify-center min-w-5 h-5 px-1 rounded-full bg-coral text-white text-xs"
                    >
                      {{ cantidadFiltrosActivos }}
                    </span>
                    <span class="ml-1" aria-hidden="true">{{ filtrosAbiertos ? '▴' : '▾' }}</span>
                  </button>

                  <button
                    type="button"
                    class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/25"
                    @click="limpiarFiltros"
                  >
                    Limpiar
                  </button>

                  <p v-if="totalPortfolio > 0" class="ml-auto text-sm text-cafe/60">
                    Mostrando {{ perfil.portfolio.length }} de {{ totalPortfolio }} imágenes
                  </p>
                </div>

                <div
                  v-if="filtrosAbiertos"
                  class="mt-3 pt-3 border-t border-rosa/60 grid grid-cols-2 lg:grid-cols-4 gap-3"
                >
                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Etiqueta</label>
                    <select v-model="filtros.etiqueta" class="block w-full rounded-2xl border-0 bg-crema py-2.5 pl-4 pr-8 text-sm text-texto ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                      <option value="">Todas</option>
                      <option v-for="e in etiquetasPortfolio" :key="e.idEtiqueta" :value="e.idEtiqueta">
                        {{ e.nombre }}
                      </option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Tipo de arte</label>
                    <select v-model="filtros.tipoArte" class="block w-full rounded-2xl border-0 bg-crema py-2.5 pl-4 pr-8 text-sm text-texto ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                      <option value="">Todos</option>
                      <option v-for="t in tiposArte" :key="t.idTipoArte" :value="t.idTipoArte">
                        {{ t.nombre }}
                      </option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Contenido sensible</label>
                    <select v-model="filtros.sensible" class="block w-full rounded-2xl border-0 bg-crema py-2.5 pl-4 pr-8 text-sm text-texto ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                      <option value="">Todos</option>
                      <option value="true">Con contenido sensible</option>
                      <option value="false">Sin contenido sensible</option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Orden</label>
                    <select v-model="filtros.orden" class="block w-full rounded-2xl border-0 bg-crema py-2.5 pl-4 pr-8 text-sm text-texto ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none">
                      <option value="RECIENTE">Más recientes</option>
                      <option value="ANTIGUO">Más antiguos</option>
                    </select>
                  </div>
                </div>
              </div>

              <p v-if="cargandoPortfolio && !perfil.portfolio.length" class="text-sm text-cafe/60 py-8 text-center">
                Cargando portfolio...
              </p>
              <p v-else-if="errorListaPortfolio" class="text-sm text-rojizo py-8 text-center">
                {{ errorListaPortfolio }}
              </p>
              <p v-else-if="!perfil.portfolio.length && hayFiltrosActivos" class="text-sm text-cafe/60 py-8">
                No hay imágenes que coincidan con los filtros.
                <button type="button" class="ml-2 underline text-granate hover:text-coral transition" @click="limpiarFiltros">
                  Limpiar filtros
                </button>
              </p>
              <p v-else-if="!perfil.portfolio.length" class="text-sm text-cafe/60 py-8">
                Este portfolio todavía no tiene imágenes.
                <button
                  v-if="esDuenio"
                  type="button"
                  class="ml-2 underline text-granate hover:text-coral transition"
                  @click="irASubirImagen()"
                >
                  Sube tu primera imagen
                </button>
              </p>

              <div v-else class="grid grid-cols-2 gap-4 sm:grid-cols-3 sm:gap-5 md:grid-cols-4">
                <button
                  v-for="obra in perfil.portfolio"
                  :key="obra.idImagen"
                  class="group aspect-square overflow-hidden rounded-[22px] border border-borde/60 bg-superficie p-2 text-left shadow-[0_10px_26px_rgba(74,29,46,0.07)] transition duration-300 hover:-translate-y-1 hover:border-coral/40 hover:shadow-[0_18px_38px_var(--sombra-acento)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
                  @click="abrirDetalleImagen(obra)"
                >
                  <div class="relative h-[calc(100%-2.25rem)] min-h-0 overflow-hidden rounded-2xl bg-coral/20">
                    <img
                      v-if="obra.url"
                      :src="obra.url"
                      :alt="obra.titulo"
                      class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
                      :class="obra.requiereAdvertencia ? 'blur-[18px] scale-105' : ''"
                    />
                    <span
                      v-if="obra.requiereAdvertencia"
                      class="absolute inset-0 flex items-center justify-center bg-granate/20 text-white drop-shadow"
                      :title="`Contenido sensible: ${textoSensible(obra.contenidoSensible)}`"
                      :aria-label="`Contenido sensible: ${textoSensible(obra.contenidoSensible)}`"
                    >
                      <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
                      </svg>
                    </span>
                  </div>
                  <div class="truncate px-2 pb-1 pt-2.5 text-sm font-semibold text-granate">{{ obra.titulo }}</div>
                </button>
              </div>

              <div v-if="perfil.portfolio.length && perfil.portfolio.length < totalPortfolio" class="mt-6 text-center">
                <button
                  type="button"
                  :disabled="cargandoPortfolio"
                  class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-superficie px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25 disabled:opacity-50"
                  @click="cargarMasPortfolio"
                >
                  {{ cargandoPortfolio ? 'Cargando...' : 'Cargar más' }}
                </button>
              </div>
            </div>

            <div v-if="tabActiva === 'servicios'" class="mt-6">
              <p v-if="cargandoServicios && !perfil.servicios.length" class="text-sm text-cafe/60 py-8 text-center">
                Cargando servicios...
              </p>
              <p v-else-if="errorServicios" class="text-sm text-rojizo py-8 text-center">
                {{ errorServicios }}
              </p>
              <p v-else-if="!perfil.servicios.length" class="text-sm text-cafe/60 py-8">
                Este artista todavía no ofrece servicios.
                <button
                  v-if="esDuenio"
                  type="button"
                  class="ml-2 underline text-granate hover:text-coral transition"
                  @click="router.push('/mis-servicios/crear')"
                >
                  Creá tu primer servicio
                </button>
              </p>
              <div v-else class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                <TarjetaServicio v-for="s in perfil.servicios" :key="s.idServicio" :servicio="s" />
              </div>
            </div>

          <div v-if="tabActiva === 'productos'" class="mt-6">
            <p v-if="!perfil.productos.length" class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center text-sm text-cafe/60">
              Este artista todavía no tiene productos publicados.
            </p>
            <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div v-for="p in perfil.productos" :key="p.id" class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
                <h3 class="text-granate">{{ p.titulo }}</h3>
                <p class="text-sm text-cafe/70 mt-1">{{ p.descripcion }}</p>
                <p class="text-sm text-rojizo mt-3">{{ p.precio }}</p>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex flex-wrap items-center gap-2 border-b border-borde/50 pb-1.5 pt-1.5">
            <button class="rounded-full bg-coral px-5 py-2.5 text-sm font-semibold text-white shadow-sm">
              Pedidos
            </button>
          </div>

          <p v-if="!perfil.pedidos.length" class="mt-6 rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center text-sm text-cafe/60">
            Todavía no hay pedidos para mostrar.
          </p>
          <div v-else class="mt-6 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            <div v-for="pedido in perfil.pedidos" :key="pedido.id" class="rounded-xl overflow-hidden bg-rosa/20 aspect-square flex flex-col">
              <div class="flex-1 bg-coral/20">
                <img v-if="pedido.url" :src="pedido.url" alt="" class="w-full h-full object-cover" />
              </div>
              <div class="p-3">
                <div class="text-sm text-cafe">{{ pedido.titulo }}</div>
                <div class="text-xs text-cafe/50">{{ pedido.fecha }}</div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Modal detalle de imagen -->
    <div v-if="imagenSeleccionada" class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4 py-6" @click.self="cerrarDetalleImagen">
      <div class="relative w-full max-w-5xl bg-blanco rounded-2xl shadow-xl max-h-[92vh] overflow-y-auto lg:h-[85vh] lg:overflow-hidden">
        <button
          type="button"
          class="absolute right-3 top-3 z-20 flex h-9 w-9 items-center justify-center rounded-full bg-blanco/90 text-xl leading-none text-cafe shadow ring-1 ring-rosa hover:bg-rosa/20 transition"
          aria-label="Cerrar"
          @click="cerrarDetalleImagen"
        >
          ×
        </button>

        <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_22rem] lg:h-full">
          <!-- Columna imagen -->
          <div class="relative flex items-center justify-center bg-cafe/5 p-4 min-h-[42vh] lg:min-h-0 lg:h-full overflow-hidden">
            <img
              :src="imagenSeleccionada.url"
              :alt="imagenSeleccionada.titulo"
              class="max-h-[45vh] max-w-full object-contain transition duration-200 lg:max-h-full"
              :class="[
                imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible ? 'blur-[18px] scale-105' : 'cursor-zoom-in',
              ]"
              @click="abrirZoom"
            />
            <!-- Gate de contenido sensible -->
            <div
              v-if="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible"
              class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-granate/30 px-6 text-center"
              role="note"
              aria-live="polite"
            >
              <span class="flex h-12 w-12 items-center justify-center rounded-full bg-blanco/90 text-granate" aria-hidden="true">
                <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
                </svg>
              </span>
              <p class="text-sm font-medium text-white drop-shadow">Contenido sensible</p>
              <span class="rounded-full bg-blanco/90 px-3 py-0.5 text-xs text-cafe">
                {{ textoSensible(imagenSeleccionada.contenidoSensible) }}
              </span>
              <button
                ref="botonRevelar"
                type="button"
                :aria-pressed="mostrarImagenSensible"
                class="mt-1 rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
                @click.stop="revelarImagenSensible"
              >
                Ver de todos modos
              </button>
            </div>

            <!-- Aviso con la imagen sensible revelada -->
            <div
              v-if="imagenSeleccionada.requiereAdvertencia && mostrarImagenSensible"
              class="absolute left-3 top-3 flex items-center gap-2 rounded-full bg-granate/70 px-3 py-1 text-xs text-inverso-contraste"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.04 12.32a1 1 0 0 1 0-.64C3.43 7.51 7.36 4.5 12 4.5s8.57 3.01 9.96 7.18a1 1 0 0 1 0 .64C20.57 16.49 16.64 19.5 12 19.5s-8.57-3.01-9.96-7.18Z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
              </svg>
              <span>Mostrando contenido sensible</span>
              <button
                type="button"
                :aria-pressed="mostrarImagenSensible"
                class="ml-1 underline hover:text-white"
                @click.stop="ocultarImagenSensible"
              >
                Ocultar
              </button>
            </div>

            <!-- Navegación entre imágenes -->
            <button
              v-if="hayAnterior"
              type="button"
              class="absolute left-3 top-1/2 -translate-y-1/2 flex h-10 w-10 items-center justify-center rounded-full bg-blanco/90 text-2xl leading-none text-granate shadow ring-1 ring-rosa hover:bg-rosa/20 transition"
              aria-label="Imagen anterior"
              @click.stop="irAnterior"
            >
              ‹
            </button>
            <button
              v-if="haySiguiente"
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 flex h-10 w-10 items-center justify-center rounded-full bg-blanco/90 text-2xl leading-none text-granate shadow ring-1 ring-rosa hover:bg-rosa/20 transition"
              aria-label="Imagen siguiente"
              @click.stop="irSiguiente"
            >
              ›
            </button>
            <span
              v-if="perfil.portfolio.length > 1"
              class="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-granate/60 px-2.5 py-0.5 text-xs text-inverso-contraste"
            >
              {{ indiceSeleccionado + 1 }} / {{ perfil.portfolio.length }}
            </span>
          </div>

          <!-- Rail de detalles -->
          <div class="flex flex-col border-t border-rosa/60 p-6 lg:h-full lg:overflow-y-auto lg:border-t-0 lg:border-l">
            <div class="flex-1">
              <h2 class="text-2xl text-granate pr-8">{{ imagenSeleccionada.titulo }}</h2>
              <p v-if="!esDuenio" class="mt-2 text-sm text-cafe/60">Por {{ nombreParaMostrar }}</p>

              <p v-if="imagenSeleccionada.descripcion" class="mt-4 text-sm text-cafe leading-relaxed whitespace-pre-wrap">{{ imagenSeleccionada.descripcion }}</p>

              <dl class="mt-8 space-y-5 border-t border-rosa/60 pt-6 text-sm">
                <div>
                  <dt class="text-xs text-cafe/50">Tipo de arte</dt>
                  <dd class="text-cafe">{{ imagenSeleccionada.tipoArte?.nombre || '—' }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-cafe/50">Fecha de subida</dt>
                  <dd class="text-cafe">{{ new Date(imagenSeleccionada.fechaSubida).toLocaleString() }}</dd>
                </div>
                <div v-if="imagenSeleccionada.contenidoSensible">
                  <dt class="text-xs text-cafe/50">Contenido sensible</dt>
                  <dd class="text-cafe">{{ textoSensible(imagenSeleccionada.contenidoSensible) }}</dd>
                </div>
              </dl>

              <div v-if="imagenSeleccionada.materiales && imagenSeleccionada.materiales.length" class="mt-8">
                <p class="text-xs text-cafe/50 mb-2">Materiales y herramientas</p>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="m in imagenSeleccionada.materiales"
                    :key="m.idMaterial"
                    class="px-2.5 py-0.5 rounded-full text-xs bg-durazno/40 text-cafe"
                    :title="m.nombreTipoArte ? `${m.nombre} · ${m.nombreTipoArte}` : m.nombre"
                  >
                    {{ m.nombre }}
                  </span>
                </div>
              </div>

              <div v-if="imagenSeleccionada.etiquetas && imagenSeleccionada.etiquetas.length" class="mt-8">
                <p class="text-xs text-cafe/50 mb-2">Etiquetas</p>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="e in imagenSeleccionada.etiquetas"
                    :key="e.idEtiqueta"
                    class="px-2.5 py-0.5 rounded-full text-xs"
                    :class="e.estado === 'PENDIENTE' ? 'bg-coral/15 text-coral' : 'bg-durazno/40 text-cafe'"
                    :title="e.estado === 'PENDIENTE' ? 'Pendiente de evaluación' : 'Etiqueta disponible'"
                  >
                    {{ e.nombre }}<span v-if="e.estado === 'PENDIENTE'" class="opacity-70"> · pendiente</span>
                  </span>
                </div>
              </div>

              <p v-if="errorDetalle" class="mt-6 text-sm text-rojizo">{{ errorDetalle }}</p>
            </div>

            <div v-if="esDuenio" class="mt-8 flex flex-wrap justify-end gap-3 border-t border-rosa/60 pt-6">
              <button
                class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
                @click="irASubirImagen(imagenSeleccionada)"
              >
                Editar
              </button>
              <button
                class="px-4 py-2 rounded-md text-sm text-rojizo border border-rojizo/40 hover:bg-rojizo/10 transition"
                @click="eliminarImagenPortfolio"
              >
                Eliminar
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Zoom a pantalla completa -->
      <div v-if="zoomActivo" class="fixed inset-0 z-70 flex items-center justify-center bg-granate/95 p-4" @click="cerrarZoom">
        <button
          type="button"
          class="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-2xl leading-none text-inverso-contraste hover:bg-white/20 transition"
          aria-label="Cerrar zoom"
          @click.stop="cerrarZoom"
        >
          ×
        </button>
        <img
          :src="imagenSeleccionada.url"
          :alt="imagenSeleccionada.titulo"
          class="max-h-[95vh] max-w-[95vw] object-contain"
          @click.stop
        />
      </div>
    </div>

    </template>
  </div>
</template>