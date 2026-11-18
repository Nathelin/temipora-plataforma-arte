<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  agregarProductoFavorito,
  cargarIdsProductosFavoritos,
  eliminarProductoFavorito,
} from '../servicios/productos'
import api from '../servicios/api'
import TarjetaProducto from '../components/TarjetaProducto.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelFiltrosProductos from '../components/filtros/PanelFiltrosProductos.vue'
import {
  aParams,
  aQueryUrl,
  cantidadActivos,
  cargarFacetas,
  chipsActivos,
  desdeQuery,
  estadoInicial,
  quitarChip,
} from '../servicios/filtrosProducto'

const route = useRoute()
const router = useRouter()

const productos = ref([])
const tipos = ref([])
const medios = ref([])
const etiquetas = ref([])
const categorias = ref([])
const monedaUsuario = ref('ARS')
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const favoritos = ref(new Set())
const actualizandoFavorito = ref(new Set())
const filtrosAbiertos = ref(false)
const facetas = ref(null)
const pagina = ref(0)
const tamanio = 24
const totalElementos = ref(0)
const hayMas = ref(false)

const filtros = ref(desdeQuery(route.query))

const chips = computed(() => chipsActivos(filtros.value, {
  tipos: tipos.value,
  medios: medios.value,
  etiquetas: etiquetas.value,
  moneda: monedaUsuario.value,
}))

const hayFiltros = computed(() => chips.value.length > 0)
const activos = computed(() => cantidadActivos(filtros.value))

let secuencia = 0
let temporizador = null

async function cargar({ reiniciar = true } = {}) {
  const actual = ++secuencia
  if (reiniciar) {
    pagina.value = 0
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  error.value = ''
  try {
    const params = aParams(filtros.value, { pagina: pagina.value, tamanio })
    const [lista, facetasRes] = await Promise.all([
      api.get('/productos', { params }),
      reiniciar ? cargarFacetas(filtros.value).catch(() => null) : Promise.resolve(null),
    ])
    if (actual !== secuencia) return
    // Si las facetas fallan se conservan los conteos previos: mejor un número
    // viejo que un panel vacío.
    if (facetasRes) facetas.value = facetasRes
    const data = lista.data || {}
    if (reiniciar) {
      productos.value = data.contenido || []
    } else {
      productos.value = [...productos.value, ...(data.contenido || [])]
    }
    totalElementos.value = data.totalElementos || 0
    hayMas.value = !!data.hayMas
  } catch (e) {
    if (actual !== secuencia) return
    error.value = e.response?.data?.message || 'No se pudieron cargar los productos.'
  } finally {
    if (actual === secuencia) {
      cargando.value = false
      cargandoMas.value = false
    }
  }
}

function cargarMas() {
  if (!hayMas.value || cargandoMas.value || cargando.value) return
  pagina.value += 1
  cargar({ reiniciar: false })
}

function actualizarFiltros(cambios) {
  filtros.value = { ...filtros.value, ...cambios }
}

function limpiarFiltros() {
  const orden = filtros.value.orden
  filtros.value = { ...estadoInicial(), orden }
}

function quitar(chip) {
  filtros.value = quitarChip(filtros.value, chip)
}

watch(filtros, () => {
  clearTimeout(temporizador)
  temporizador = setTimeout(() => {
    sincronizarUrl()
    cargar()
  }, 350)
}, { deep: true })

function sincronizarUrl() {
  const query = aQueryUrl(filtros.value)
  if (JSON.stringify(query) === JSON.stringify(route.query)) return
  router.replace({ query })
}

async function cargarFavoritos() {
  if (!localStorage.getItem('token')) return
  try {
    const ids = await cargarIdsProductosFavoritos()
    favoritos.value = new Set(ids)
  } catch {
    favoritos.value = new Set()
  }
}

async function alternarFavorito(producto) {
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const id = producto.idProducto
  if (actualizandoFavorito.value.has(id)) return
  actualizandoFavorito.value.add(id)
  try {
    if (favoritos.value.has(id)) {
      await eliminarProductoFavorito(id)
      favoritos.value.delete(id)
    } else {
      await agregarProductoFavorito(id)
      favoritos.value.add(id)
    }
  } catch {
    // Sin cambios si falla.
  } finally {
    actualizandoFavorito.value.delete(id)
  }
}

function alTeclear(event) {
  if (event.key === 'Escape' && filtrosAbiertos.value) filtrosAbiertos.value = false
}

onMounted(async () => {
  document.addEventListener('keydown', alTeclear)
  cargarFavoritos()
  const nombreUsuario = localStorage.getItem('nombreUsuario')
  const [tiposRes, mediosRes, etiquetasRes, categoriasRes, perfilRes] = await Promise.all([
    api.get('/tipos-producto').catch(() => null),
    api.get('/medios').catch(() => null),
    api.get('/etiquetas/selector').catch(() => null),
    api.get('/etiquetas/categorias').catch(() => null),
    nombreUsuario ? api.get(`/perfil/${nombreUsuario}`).catch(() => null) : Promise.resolve(null),
  ])
  tipos.value = tiposRes?.data || []
  medios.value = (mediosRes?.data || []).filter((m) => m.activo !== false)
  etiquetas.value = etiquetasRes?.data || []
  categorias.value = categoriasRes?.data || []
  const codigo = perfilRes?.data?.monedaPreferida
  if (codigo) monedaUsuario.value = codigo
  cargar()
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', alTeclear)
})
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8 sm:py-10">
    <PageHeader
      eyebrow="Productos"
      titulo="Arte listo para llevar"
      descripcion="Piezas, adoptables, recursos digitales y reventas de artistas de la plataforma."
    >
      <template #acciones>
        <router-link
          to="/servicios"
          class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
        >
          Buscar un servicio a medida →
        </router-link>
      </template>
    </PageHeader>

    <div class="mt-6 lg:grid lg:grid-cols-[18rem_minmax(0,1fr)] lg:items-start lg:gap-8">
      <aside class="hidden lg:sticky lg:top-20 lg:block lg:h-[calc(100vh-6rem)] lg:overflow-y-auto">
        <PanelFiltrosProductos
          :estado="filtros"
          :tipos="tipos"
          :medios="medios"
          :etiquetas="etiquetas"
          :categorias="categorias"
          :moneda="monedaUsuario"
          :facetas="facetas"
          :cargando="cargando"
          @actualizar="actualizarFiltros"
          @limpiar="limpiarFiltros"
        />
      </aside>

      <div class="min-w-0">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-3">
            <button
              type="button"
              class="inline-flex min-h-10 items-center gap-2 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20 lg:hidden"
              :aria-expanded="filtrosAbiertos"
              @click="filtrosAbiertos = true"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M4 6h16M7 12h10M10 18h4" />
              </svg>
              Filtros
              <span
                v-if="activos"
                class="rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold leading-none text-white"
              >
                {{ activos }}
              </span>
            </button>

            <p v-if="!cargando && !error" class="text-sm text-cafe/60">
              {{ totalElementos }}
              {{ totalElementos === 1 ? 'publicación' : 'publicaciones' }}
            </p>
          </div>

          <button
            v-if="hayFiltros"
            type="button"
            class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
            @click="limpiarFiltros"
          >
            Limpiar filtros
          </button>
        </div>

        <ul v-if="chips.length" class="mt-3 flex flex-wrap gap-2">
          <li v-for="chip in chips" :key="`${chip.clave}-${chip.valor}-${chip.texto}`">
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-full border border-coral/30 bg-coral/10 px-3 py-1 text-xs font-semibold text-granate transition hover:border-rojizo/40 hover:bg-coral/15"
              :aria-label="`Quitar filtro ${chip.texto}`"
              @click="quitar(chip)"
            >
              <span class="max-w-56 truncate">{{ chip.texto }}</span>
              <span aria-hidden="true" class="text-cafe/50">✕</span>
            </button>
          </li>
        </ul>

        <p v-if="cargando" class="mt-6 py-16 text-center text-cafe/60">Cargando productos...</p>
        <p v-else-if="error" role="alert" class="mt-6 py-16 text-center text-rojizo">{{ error }}</p>

        <template v-else>
          <div
            v-if="!productos.length"
            class="mt-6 rounded-3xl border border-dashed border-rosa/60 px-4 py-16 text-center"
          >
            <p class="text-sm font-medium text-granate">
              {{ hayFiltros ? 'Ninguna publicación coincide con los filtros.' : 'Todavía no hay publicaciones.' }}
            </p>
            <p class="mt-1 text-sm text-cafe/50">
              {{ hayFiltros ? 'Probá quitando algún filtro.' : 'Volvé pronto: los artistas están preparando piezas.' }}
            </p>
            <button
              v-if="hayFiltros"
              type="button"
              class="mt-4 text-sm font-semibold text-coral transition hover:text-rojizo"
              @click="limpiarFiltros"
            >
              Limpiar filtros
            </button>
          </div>

          <div v-else class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            <TarjetaProducto
              v-for="producto in productos"
              :key="producto.esReventa
                ? `r-${producto.idPublicacionReventa}`
                : `p-${producto.idProducto}`"
              :producto="producto"
              mostrar-favorito
              :es-favorito="favoritos.has(producto.idProducto)"
              :actualizando-favorito="actualizandoFavorito.has(producto.idProducto)"
              @alternar-favorito="alternarFavorito"
            />
          </div>

          <div v-if="hayMas" class="mt-8 flex justify-center">
            <button
              type="button"
              :disabled="cargandoMas"
              class="inline-flex min-h-11 items-center rounded-full bg-coral px-6 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
              @click="cargarMas"
            >
              {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <div
      v-if="filtrosAbiertos"
      class="fixed inset-0 z-50 flex lg:hidden"
      role="dialog"
      aria-modal="true"
      aria-label="Filtros del catálogo"
    >
      <div class="absolute inset-0 bg-granate/40" @click="filtrosAbiertos = false"></div>
      <div class="relative ml-auto flex h-full w-full max-w-sm flex-col bg-crema shadow-2xl">
        <div class="flex items-center justify-between border-b border-borde/50 px-4 py-3">
          <h2 class="font-display text-base font-bold text-granate">Filtros</h2>
          <button
            type="button"
            class="flex h-9 w-9 items-center justify-center rounded-full text-cafe/60 transition hover:bg-rosa/30 hover:text-granate"
            aria-label="Cerrar filtros"
            @click="filtrosAbiertos = false"
          >
            ✕
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-4">
          <PanelFiltrosProductos
            :estado="filtros"
            :tipos="tipos"
            :medios="medios"
            :etiquetas="etiquetas"
            :categorias="categorias"
            :moneda="monedaUsuario"
            :facetas="facetas"
            :cargando="cargando"
            @actualizar="actualizarFiltros"
            @limpiar="limpiarFiltros"
          />
        </div>
        <div class="border-t border-borde/50 p-4">
          <button
            type="button"
            class="w-full rounded-full bg-coral px-5 py-2.5 text-sm font-bold text-white transition hover:bg-rojizo"
            @click="filtrosAbiertos = false"
          >
            {{ cargando
              ? 'Cargando...'
              : `Ver ${totalElementos} ${totalElementos === 1 ? 'publicación' : 'publicaciones'}` }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
