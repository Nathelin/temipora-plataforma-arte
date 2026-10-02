<script setup>
import { ref, computed, watch } from 'vue'
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
      await Promise.all([cargarPortfolio(true), cargarEtiquetasPortfolio()])
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

const filtros = ref({ titulo: '', etiqueta: '', medio: '', sensible: '', orden: 'RECIENTE' })
const etiquetasPortfolio = ref([])
const paginaPortfolio = ref(0)
const totalPortfolio = ref(0)
const cargandoPortfolio = ref(false)
const errorListaPortfolio = ref('')

const hayFiltrosActivos = computed(() => {
  const f = filtros.value
  return !!(f.titulo.trim() || f.etiqueta || f.medio || f.sensible)
})

const filtrosAbiertos = ref(false)

const cantidadFiltrosActivos = computed(() => {
  const f = filtros.value
  let total = 0
  if (f.titulo.trim()) total++
  if (f.etiqueta) total++
  if (f.medio) total++
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
    if (f.medio) params.medio = f.medio
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

function limpiarFiltros() {
  const f = filtros.value
  if (!hayFiltrosActivos.value && (f.orden || 'RECIENTE') === 'RECIENTE') return
  filtros.value = { titulo: '', etiqueta: '', medio: '', sensible: '', orden: 'RECIENTE' }
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
  filtros.value = { titulo: '', etiqueta: '', medio: '', sensible: '', orden: 'RECIENTE' }
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

function abrirDetalleImagen(imagen) {
  imagenSeleccionada.value = imagen
  mostrarImagenSensible.value = false
  errorDetalle.value = ''
}

function cerrarDetalleImagen() {
  imagenSeleccionada.value = null
  mostrarImagenSensible.value = false
}

function revelarImagenSensible() {
  mostrarImagenSensible.value = true
}

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
                    <label class="block text-xs text-cafe/60 mb-1">Medio</label>
                    <select v-model="filtros.medio" class="block w-full rounded-lg border-0 py-2 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none bg-blanco">
                      <option value="">Todos</option>
                      <option value="TRADICIONAL">Tradicional</option>
                      <option value="DIGITAL">Digital</option>
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
                        class="absolute inset-0 flex items-center justify-center bg-granate/10 text-3xl text-white drop-shadow"
                        :title="`Contenido sensible: ${obra.contenidoSensible}`"
                        :aria-label="`Contenido sensible: ${obra.contenidoSensible}`"
                      >
                        ⚠
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
    <div v-if="imagenSeleccionada" class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4" @click.self="cerrarDetalleImagen">
      <div class="w-full max-w-2xl bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <div class="relative overflow-hidden rounded-lg bg-rosa/20">
          <img
            :src="imagenSeleccionada.url"
            :alt="imagenSeleccionada.titulo"
            class="w-full max-h-[55vh] object-contain transition duration-200"
            :class="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible ? 'blur-[18px] scale-105' : ''"
          />
          <span
            v-if="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible"
            class="absolute inset-0 flex items-center justify-center text-5xl text-white drop-shadow"
            :title="`Contenido sensible: ${imagenSeleccionada.contenidoSensible}`"
            :aria-label="`Contenido sensible: ${imagenSeleccionada.contenidoSensible}`"
          >
            ⚠
          </span>
        </div>
        <div v-if="imagenSeleccionada.requiereAdvertencia && !mostrarImagenSensible" class="mt-4 rounded-lg bg-rosa/30 px-4 py-3 text-sm text-cafe">
          <label>Esta imagen tiene contenido sensible categorizado como: {{ imagenSeleccionada.contenidoSensible.toLowerCase() }}.    </label>
          <button
            type="button"
            class="mt-3 rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition"
            @click="revelarImagenSensible"
          >
            Ver imagen
          </button>
        </div>
        <h2 class="text-xl text-granate mt-5">{{ imagenSeleccionada.titulo }}</h2>
        <p class="text-sm text-cafe mt-2">{{ imagenSeleccionada.descripcion }}</p>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-4 text-sm text-cafe/70">
          <p><span class="text-rojizo">Medio:</span> {{ imagenSeleccionada.medio }}</p>
          <p><span class="text-rojizo">Materiales:</span> {{ imagenSeleccionada.materiales || 'No especificados' }}</p>
          <p><span class="text-rojizo">Subida:</span> {{ new Date(imagenSeleccionada.fechaSubida).toLocaleString() }}</p>
          <p><span class="text-rojizo">Contenido sensible:</span> {{ imagenSeleccionada.contenidoSensible || 'No' }}</p>
        </div>
        <div v-if="imagenSeleccionada.etiquetas && imagenSeleccionada.etiquetas.length" class="mt-3">
          <span class="text-sm text-rojizo">Etiquetas:</span>
          <div class="flex flex-wrap gap-2 mt-1.5">
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
        <p v-if="errorDetalle" class="text-rojizo text-sm text-right mt-4">{{ errorDetalle }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button
            v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
            @click="irASubirImagen(imagenSeleccionada)"
          >
            Editar
          </button>
          <button
            v-if="esDuenio"
            class="px-4 py-2 rounded-md text-sm text-rojizo border border-rojizo/40 hover:bg-rojizo/10 transition"
            @click="eliminarImagenPortfolio"
          >
            Eliminar
          </button>
          <button class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition" @click="cerrarDetalleImagen">Cerrar</button>
        </div>
      </div>
    </div>

    </template>
  </div>
</template>