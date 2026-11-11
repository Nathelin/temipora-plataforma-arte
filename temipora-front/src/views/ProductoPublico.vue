<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatearPrecio } from '../servicios/moneda'
import {
  agregarProductoFavorito,
  cargarIdsProductosFavoritos,
  cargarLedgerProductoPublico,
  cargarProductoPublico,
  cargarVariantesProductoPublico,
  eliminarProductoFavorito,
  etiquetaDisponibilidad,
} from '../servicios/productos'
import ProductoVistaDetalle from '../components/ProductoVistaDetalle.vue'
import { useCarrito } from '../composables/useCarrito'
import { useNotificaciones } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()

const idProducto = computed(() => Number(route.params.id))

const producto = ref(null)
const cargando = ref(true)
const error = ref('')

const POR_PAGINA = 4
const variantesPagina = ref([])
const paginaVariantes = ref(1)
const totalPaginasVariantes = ref(1)
const totalVariantes = ref(0)
const ledger = ref(null)
const paginaLedger = ref(1)

const esFavorito = ref(false)
const actualizandoFavorito = ref(false)

const { cantidad: cantidadCarrito, agregar: agregarAlCarritoStore } = useCarrito()
const { exito, error: toastError } = useNotificaciones()

async function agregarAlCarrito(variante) {
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await agregarAlCarritoStore(variante.idVariante, 1)
    exito(`“${variante.nombre}” se agregó al carrito.`)
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo agregar al carrito.')
  }
}

const imagenes = computed(() => {
  const deGaleria = (producto.value?.imagenes || []).map((imagen) => imagen.url)
  const deVariantes = (producto.value?.variantes || [])
    .map((variante) => variante.imagenUrl)
    .filter(Boolean)
  const portada = producto.value?.imagenPortada
  return [...new Set([portada, ...deGaleria, ...deVariantes].filter(Boolean))]
})

const precioMostrado = computed(() =>
  formatearPrecio(producto.value?.precioDesde, producto.value?.moneda || 'ARS'),
)
const disponibilidad = computed(() => etiquetaDisponibilidad(producto.value))

const totalPaginasLedger = computed(
  () => Math.max(1, Number(ledger.value?.totalPaginas) || 1),
)
const totalLedger = computed(() => Number(ledger.value?.totalElementos) || 0)

function aplicarVariantes(data) {
  variantesPagina.value = data?.contenido || []
  paginaVariantes.value = data?.pagina || 1
  totalPaginasVariantes.value = Math.max(1, Number(data?.totalPaginas) || 1)
  totalVariantes.value = Number(data?.totalElementos) || 0
}

function aplicarLedger(data) {
  ledger.value = data || null
  paginaLedger.value = data?.pagina || 1
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [productoRes, variantesRes, ledgerRes] = await Promise.all([
      cargarProductoPublico(idProducto.value),
      cargarVariantesProductoPublico(idProducto.value, {
        pagina: 1,
        tamano: POR_PAGINA,
      }),
      cargarLedgerProductoPublico(idProducto.value, {
        pagina: 1,
        tamano: POR_PAGINA,
      }),
    ])
    producto.value = productoRes
    aplicarVariantes(variantesRes)
    aplicarLedger(ledgerRes)
    await cargarFavorito()
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Este producto no está disponible.'
      : (e.response?.data?.message
        || 'No se pudo cargar el producto.')
  } finally {
    cargando.value = false
  }
}

async function cambiarPaginaVariantes(paso) {
  const destino = Math.min(
    Math.max(paginaVariantes.value + paso, 1),
    totalPaginasVariantes.value,
  )
  if (destino === paginaVariantes.value) return
  try {
    const data = await cargarVariantesProductoPublico(idProducto.value, {
      pagina: destino,
      tamano: POR_PAGINA,
    })
    aplicarVariantes(data)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudieron cargar las variantes.'
  }
}

async function cambiarPaginaLedger(paso) {
  const destino = Math.min(
    Math.max(paginaLedger.value + paso, 1),
    totalPaginasLedger.value,
  )
  if (destino === paginaLedger.value) return
  try {
    const data = await cargarLedgerProductoPublico(idProducto.value, {
      pagina: destino,
      tamano: POR_PAGINA,
    })
    aplicarLedger(data)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar el historial de autenticidad.'
  }
}

async function cargarFavorito() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const ids = await cargarIdsProductosFavoritos()
    esFavorito.value = ids.includes(idProducto.value)
  } catch {
    esFavorito.value = false
  }
}

async function alternarFavorito() {
  if (actualizandoFavorito.value) return
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  actualizandoFavorito.value = true
  try {
    if (esFavorito.value) {
      await eliminarProductoFavorito(idProducto.value)
      esFavorito.value = false
    } else {
      await agregarProductoFavorito(idProducto.value)
      esFavorito.value = true
    }
  } catch {
    // Se mantiene el estado anterior si la llamada falla.
  } finally {
    actualizandoFavorito.value = false
  }
}

watch(idProducto, cargar)
onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8">
    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando...</p>

    <div v-else-if="error" class="mx-auto max-w-xl rounded-2xl border border-rosa/60 bg-blanco px-6 py-12 text-center">
      <p class="font-display text-lg font-bold text-granate">{{ error }}</p>
      <router-link
        to="/productos"
        class="mt-4 inline-block text-sm font-medium text-coral transition hover:text-rojizo"
      >
        ← Volver al catálogo
      </router-link>
    </div>

    <template v-else-if="producto">
      <router-link
        to="/productos"
        class="text-sm text-granate transition hover:text-coral"
      >
        ← Volver a productos
      </router-link>

      <div class="mt-6">
        <ProductoVistaDetalle
          modo="publico"
          :producto="producto"
          :imagenes="imagenes"
          :variantes-pagina="variantesPagina"
          :pagina-variantes="paginaVariantes"
          :total-paginas-variantes="totalPaginasVariantes"
          :total-variantes="totalVariantes"
          :ledger="ledger"
          :pagina-ledger="paginaLedger"
          :total-paginas-ledger="totalPaginasLedger"
          :total-ledger="totalLedger"
          :disponibilidad="disponibilidad"
          :precio-desde="precioMostrado"
          @cambiar-pagina-variantes="cambiarPaginaVariantes"
          @cambiar-pagina-ledger="cambiarPaginaLedger"
          @agregar-carrito="agregarAlCarrito"
        >
          <template #acciones>
            <router-link
              v-if="cantidadCarrito"
              to="/carrito"
              class="flex w-full items-center justify-center gap-2 rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo"
            >
              Ir al carrito
              <span class="rounded-full bg-white/20 px-2 py-0.5 text-[11px] font-bold">
                {{ cantidadCarrito }}
              </span>
            </router-link>
            <p v-else class="text-center text-[11px] leading-relaxed text-cafe/45">
              Elegí una variante y tocá “Agregar” para sumarla al carrito.
            </p>

            <button
              type="button"
              class="flex w-full items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-semibold transition disabled:cursor-wait disabled:opacity-60"
              :class="esFavorito
                ? 'border-coral bg-coral/10 text-granate hover:bg-coral/20'
                : 'border-rosa text-cafe hover:bg-rosa/20'"
              :disabled="actualizandoFavorito"
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
              {{ esFavorito ? 'En tus favoritos' : 'Agregar a favoritos' }}
            </button>
          </template>
        </ProductoVistaDetalle>
      </div>
    </template>
  </div>
</template>
