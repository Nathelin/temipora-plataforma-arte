<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { useSubirImagen } from '../composables/useSubirImagen'

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

async function cargarPerfil() {
  cargandoPerfil.value = true
  errorCarga.value = ''
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
      await Promise.all([cargarPortfolio(true), cargarEtiquetasPortfolio(), cargarTiposArte()])
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
})

watch(() => route.params.nombreUsuario, cargarPerfil, { immediate: true })

const esArtista = computed(() => perfil.value.rol === 'ARTISTA')
const tabActiva = ref('portfolio')

const nombreParaMostrar = computed(() => perfil.value.nombreDisplay || perfil.value.nombreUsuario)

const esDuenio = computed(() => {
  const actual = localStorage.getItem('nombreUsuario') || ''
  return actual.toLowerCase() === perfil.value.nombreUsuario.toLowerCase()
})

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
    <div class="relative h-48 sm:h-64 bg-coral/20 w-full z-0">
      <img
        v-if="perfil.urlBanner"
        :src="perfil.urlBanner"
        alt="Banner"
        class="w-full h-full object-cover"
      />

    </div>

    <div class="max-w-screen-2xl mx-auto px-4 sm:px-8">
      <div class="flex flex-col sm:flex-row sm:items-end gap-4 sm:gap-6 pb-6 border-b border-rosa pt-2 sm:pt-4">
        <div class="relative w-40 sm:w-48 h-10 sm:h-12 shrink-0 z-10">
          <div class="absolute bottom-0 left-0 w-40 h-40 sm:w-48 sm:h-48 rounded-full overflow-hidden border-4 border-blanco bg-coral/20 shadow-md">
            <img
              v-if="perfil.urlFotoPerfil"
              :src="perfil.urlFotoPerfil"
              alt="Perfil"
              class="w-full h-full object-cover"
            />
            <span
              v-else
              class="w-full h-full flex items-center justify-center text-5xl text-granate"
            >
              {{ fotoInicial(perfil.nombreUsuario) }}
            </span>
          </div>
        </div>

        <div class="flex-1 mt-2 sm:mt-0">
          <h1 class="text-2xl sm:text-3xl font-medium text-granate">{{ nombreParaMostrar }}</h1>
          <p class="text-sm text-cafe/60 mb-3">@{{ perfil.nombreUsuario }}</p>

          <div class="max-w-2xl flex items-start gap-3">
            <p class="text-sm text-cafe leading-relaxed line-clamp-3">
              {{ perfil.biografia || 'Sin biografía.' }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-3 shrink-0 mt-4 sm:mt-0 pb-1 sm:pb-3">
          <button v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-granate border border-rosa bg-blanco hover:bg-rosa/20 transition"
            @click="irAConfiguracion('privacidad')"
          >
            Preferencias
          </button>
          <button
            v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
            @click="irAConfiguracion('perfil')"
          >
            Editar perfil
          </button>
        </div>
      </div>

      <!-- Pestañas -->
      <div class="pb-6">
        <template v-if="esArtista">
          <div class="flex items-center gap-6 border-b border-rosa">
            <button
              class="py-3 text-sm transition border-b-2"
              :class="tabActiva === 'portfolio' ? 'border-coral text-granate font-medium' : 'border-transparent text-cafe/60 hover:text-cafe'"
              @click="tabActiva = 'portfolio'"
            >
              Portfolio
            </button>
            <button
              class="py-3 text-sm transition border-b-2"
              :class="tabActiva === 'servicios' ? 'border-coral text-granate font-medium' : 'border-transparent text-cafe/60 hover:text-cafe'"
              @click="tabActiva = 'servicios'"
            >
              Servicios
            </button>
            <button
              class="py-3 text-sm transition border-b-2"
              :class="tabActiva === 'productos' ? 'border-coral text-granate font-medium' : 'border-transparent text-cafe/60 hover:text-cafe'"
              @click="tabActiva = 'productos'"
            >
              Productos
            </button>
          </div>

            <div v-if="tabActiva === 'portfolio'" class="mt-6">
              <!-- Barra compacta de filtros (siempre inicia colapsada) -->
              <div class="mb-5 bg-blanco border border-rosa rounded-xl p-3">
                <div class="flex flex-wrap items-center gap-3">
                  <div class="flex-1 min-w-44 relative">
                    <input
                      v-model="filtros.titulo"
                      type="text"
                      placeholder="Buscar por título..."
                      class="pl-4 pr-9 block w-full rounded-lg border-0 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
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
                    class="px-3 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
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
                    class="px-3 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
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
                    <select v-model="filtros.etiqueta" class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco">
                      <option value="">Todas</option>
                      <option v-for="e in etiquetasPortfolio" :key="e.idEtiqueta" :value="e.idEtiqueta">
                        {{ e.nombre }}
                      </option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Tipo de arte</label>
                    <select v-model="filtros.tipoArte" class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco">
                      <option value="">Todos</option>
                      <option v-for="t in tiposArte" :key="t.idTipoArte" :value="t.idTipoArte">
                        {{ t.nombre }}
                      </option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Contenido sensible</label>
                    <select v-model="filtros.sensible" class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco">
                      <option value="">Todos</option>
                      <option value="true">Con contenido sensible</option>
                      <option value="false">Sin contenido sensible</option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-xs text-cafe/60 mb-1">Orden</label>
                    <select v-model="filtros.orden" class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco">
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

              <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                <button
                  v-for="obra in perfil.portfolio"
                  :key="obra.idImagen"
                  class="rounded-xl overflow-hidden bg-rosa/20 aspect-square flex flex-col text-left hover:ring-2 hover:ring-coral transition"
                  @click="abrirDetalleImagen(obra)"
                >
                  <div class="min-h-0 flex-1 bg-coral/20">
                    <div class="relative w-full h-full">
                      <img
                        v-if="obra.url"
                        :src="obra.url"
                        :alt="obra.titulo"
                        class="w-full h-full object-cover transition duration-200"
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
                  </div>
                  <div class="shrink-0 bg-rosa/20 p-3 text-sm text-cafe truncate">{{ obra.titulo }}</div>
                </button>
              </div>

              <div v-if="perfil.portfolio.length && perfil.portfolio.length < totalPortfolio" class="mt-6 text-center">
                <button
                  type="button"
                  :disabled="cargandoPortfolio"
                  class="px-4 py-2 rounded-md text-sm text-granate border border-rosa hover:bg-rosa/20 disabled:opacity-50 transition"
                  @click="cargarMasPortfolio"
                >
                  {{ cargandoPortfolio ? 'Cargando...' : 'Cargar más' }}
                </button>
              </div>
            </div>

          <div v-if="tabActiva === 'servicios'" class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div v-for="s in perfil.servicios" :key="s.id" class="bg-blanco border border-rosa rounded-xl p-5">
              <h3 class="text-granate">{{ s.titulo }}</h3>
              <p class="text-sm text-cafe/70 mt-1">{{ s.descripcion }}</p>
              <p class="text-sm text-rojizo mt-3">{{ s.precio }}</p>
            </div>
          </div>

          <div v-if="tabActiva === 'productos'" class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div v-for="p in perfil.productos" :key="p.id" class="bg-blanco border border-rosa rounded-xl p-5">
              <h3 class="text-granate">{{ p.titulo }}</h3>
              <p class="text-sm text-cafe/70 mt-1">{{ p.descripcion }}</p>
              <p class="text-sm text-rojizo mt-3">{{ p.precio }}</p>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex items-center gap-6 border-b border-rosa">
            <button class="py-3 text-sm transition border-b-2 border-coral text-granate font-medium">
              Pedidos
            </button>
          </div>

          <div class="mt-6 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
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
              class="absolute left-3 top-3 flex items-center gap-2 rounded-full bg-granate/70 px-3 py-1 text-xs text-crema"
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
              class="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-granate/60 px-2.5 py-0.5 text-xs text-crema"
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
      <div v-if="zoomActivo" class="fixed inset-0 z-[70] flex items-center justify-center bg-granate/95 p-4" @click="cerrarZoom">
        <button
          type="button"
          class="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-blanco/10 text-2xl leading-none text-crema hover:bg-blanco/20 transition"
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