<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  agregarProductoFavorito,
  cargarCatalogoProductos,
  cargarIdsProductosFavoritos,
  eliminarProductoFavorito,
} from '../servicios/productos'
import api from '../servicios/api'
import TarjetaProducto from '../components/TarjetaProducto.vue'

const route = useRoute()
const router = useRouter()

const productos = ref([])
const tipos = ref([])
const medios = ref([])
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')

const favoritos = ref(new Set())
const actualizandoFavorito = ref(new Set())

const orden = ref('RELEVANCIA')
const filtrosAbiertos = ref(false)
const pagina = ref(0)
const tamanio = 24
const totalElementos = ref(0)
const hayMas = ref(false)

const filtros = ref({
  titulo: '',
  nombreArtista: '',
  idTipoProducto: '',
  idMedio: '',
  sensible: '',
  precioMin: '',
  precioMax: '',
})

let secuencia = 0
let temporizador = null

const cantidadFiltrosActivos = computed(() => {
  let total = 0
  if (filtros.value.nombreArtista.trim()) total++
  if (filtros.value.idTipoProducto) total++
  if (filtros.value.idMedio) total++
  if (filtros.value.sensible) total++
  if (filtros.value.precioMin) total++
  if (filtros.value.precioMax) total++
  return total
})

const hayFiltros = computed(
  () => cantidadFiltrosActivos.value > 0 || filtros.value.titulo.trim() !== '',
)

const descripcionTipoSeleccionado = computed(() => {
  const tipo = tipos.value.find(
    (t) => String(t.idTipoProducto) === String(filtros.value.idTipoProducto),
  )
  return tipo?.descripcion || ''
})

function params() {
  const salida = {
    orden: orden.value,
    pagina: pagina.value,
    tamanio,
  }
  if (filtros.value.titulo.trim()) salida.titulo = filtros.value.titulo.trim()
  if (filtros.value.nombreArtista.trim()) {
    salida.nombreArtista = filtros.value.nombreArtista.trim()
  }
  if (filtros.value.idTipoProducto) {
    salida.idTipoProducto = filtros.value.idTipoProducto
  }
  if (filtros.value.idMedio) salida.idMedio = filtros.value.idMedio
  if (filtros.value.sensible) salida.sensible = filtros.value.sensible === 'con'
  if (filtros.value.precioMin) salida.precioMin = filtros.value.precioMin
  if (filtros.value.precioMax) salida.precioMax = filtros.value.precioMax
  return salida
}

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
    const data = await cargarCatalogoProductos(params())
    if (actual !== secuencia) return
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
  pagina.value += 1
  cargar({ reiniciar: false })
}

function limpiarFiltros() {
  filtros.value = {
    titulo: '',
    nombreArtista: '',
    idTipoProducto: '',
    idMedio: '',
    sensible: '',
    precioMin: '',
    precioMax: '',
  }
}

watch(
  filtros,
  () => {
    clearTimeout(temporizador)
    temporizador = setTimeout(() => cargar(), 350)
  },
  { deep: true },
)

watch(orden, () => cargar())

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

onMounted(async () => {
  // Los chips de Explorar llegan como query para abrir el catálogo filtrado.
  const tipoInicial = route.query.idTipoProducto
  if (tipoInicial) filtros.value.idTipoProducto = String(tipoInicial)
  const medioInicial = route.query.idMedio
  if (medioInicial) filtros.value.idMedio = String(medioInicial)

  cargarFavoritos()
  try {
    const [tiposRes, mediosRes] = await Promise.all([
      api.get('/tipos-producto'),
      api.get('/medios'),
    ])
    tipos.value = tiposRes.data
    medios.value = mediosRes.data
  } catch {
    tipos.value = []
    medios.value = []
  }
  cargar()
})
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="text-xs font-bold uppercase tracking-[0.18em] text-coral">
          Productos
        </p>
        <h1 class="mt-2 font-display text-3xl font-extrabold text-granate sm:text-4xl">
          Arte listo para llevar
        </h1>
        <p class="mt-1 text-sm text-cafe/60">
          Piezas, adoptables y recursos digitales de artistas de la plataforma.
        </p>
      </div>
      <router-link
        to="/servicios"
        class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
      >
        Buscar un servicio a medida →
      </router-link>
    </div>

    <div class="mt-6 rounded-[22px] border border-borde/60 bg-superficie p-4 shadow-sm">
      <div class="flex flex-wrap items-center gap-3">
        <div class="relative min-w-0 flex-1">
          <input
            v-model="filtros.titulo"
            type="search"
            placeholder="Buscar por título del producto..."
            class="w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
        </div>

        <select
          v-model="orden"
          class="rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          aria-label="Ordenar resultados"
        >
          <option value="RELEVANCIA">Para vos</option>
          <option value="RECIENTES">Más recientes</option>
          <option value="TITULO">Título</option>
          <option value="PRECIO_ASC">Menor precio</option>
          <option value="PRECIO_DESC">Mayor precio</option>
        </select>

        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-lg border border-rosa px-3 py-2 text-sm font-medium text-cafe transition hover:bg-rosa/20"
          @click="filtrosAbiertos = !filtrosAbiertos"
        >
          Filtros
          <span
            v-if="cantidadFiltrosActivos"
            class="rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold leading-none text-white"
          >
            {{ cantidadFiltrosActivos }}
          </span>
        </button>

        <button
          v-if="hayFiltros"
          type="button"
          class="text-sm text-cafe/60 transition hover:text-granate"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>

      <div v-if="filtrosAbiertos" class="mt-4 grid gap-4 border-t border-rosa/40 pt-4 sm:grid-cols-2 lg:grid-cols-3">
        <label class="block text-xs font-medium text-cafe/70">
          Artista
          <input
            v-model="filtros.nombreArtista"
            type="text"
            placeholder="@usuario"
            class="mt-1 w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
        </label>

        <div>
          <label class="block text-xs font-medium text-cafe/70">
            Tipo de producto
            <select
              v-model="filtros.idTipoProducto"
              class="mt-1 w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            >
              <option value="">Todos</option>
              <option
                v-for="tipo in tipos"
                :key="tipo.idTipoProducto"
                :value="tipo.idTipoProducto"
              >
                {{ tipo.nombre }}
              </option>
            </select>
          </label>
          <p v-if="descripcionTipoSeleccionado" class="mt-1 text-[11px] leading-relaxed text-cafe/50">
            {{ descripcionTipoSeleccionado }}
          </p>
        </div>

        <label class="block text-xs font-medium text-cafe/70">
          Medio
          <select
            v-model="filtros.idMedio"
            class="mt-1 w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          >
            <option value="">Todos</option>
            <option v-for="medio in medios" :key="medio.idMedio" :value="medio.idMedio">
              {{ medio.nombre }}
            </option>
          </select>
        </label>

        <label class="block text-xs font-medium text-cafe/70">
          Contenido sensible
          <select
            v-model="filtros.sensible"
            class="mt-1 w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          >
            <option value="">Todos</option>
            <option value="con">Con contenido sensible</option>
            <option value="sin">Sin contenido sensible</option>
          </select>
        </label>

        <label class="block text-xs font-medium text-cafe/70">
          Precio mínimo
          <input
            v-model="filtros.precioMin"
            type="number"
            min="0"
            class="mt-1 w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
        </label>

        <label class="block text-xs font-medium text-cafe/70">
          Precio máximo
          <input
            v-model="filtros.precioMax"
            type="number"
            min="0"
            class="mt-1 w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          />
        </label>
      </div>
    </div>

    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando productos...</p>
    <p v-else-if="error" role="alert" class="py-16 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <div class="mt-6 flex items-center justify-between gap-3">
        <p class="text-xs text-cafe/55">
          {{ totalElementos }}
          {{ totalElementos === 1 ? 'producto' : 'productos' }}
        </p>
      </div>

      <div
        v-if="!productos.length"
        class="mt-6 rounded-2xl border border-dashed border-rosa/60 px-4 py-16 text-center"
      >
        <p class="text-sm font-medium text-granate">
          {{ hayFiltros ? 'Ningún producto coincide con los filtros.' : 'Todavía no hay productos publicados.' }}
        </p>
        <p class="mt-1 text-sm text-cafe/50">
          {{ hayFiltros ? 'Probá quitando algún filtro.' : 'Volvé pronto: los artistas están preparando piezas.' }}
        </p>
        <button
          v-if="hayFiltros"
          type="button"
          class="mt-4 text-sm font-medium text-coral transition hover:text-rojizo"
          @click="limpiarFiltros"
        >
          Limpiar filtros
        </button>
      </div>

      <div v-else class="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
        <TarjetaProducto
          v-for="producto in productos"
          :key="producto.idProducto"
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
          class="rounded-lg border border-rosa px-5 py-2.5 text-sm font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-50"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
      </div>
    </template>
  </div>
</template>
