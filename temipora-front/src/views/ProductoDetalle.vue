<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import ProductoVistaDetalle from '../components/ProductoVistaDetalle.vue'

const route = useRoute()
const idProducto = computed(() => Number(route.params.id))

const producto = ref(null)
const ledger = ref(null)
const cargando = ref(true)
const error = ref('')
const POR_PAGINA = 4
const variantesPagina = ref([])
const paginaVariantes = ref(1)
const totalPaginasVariantes = ref(1)
const totalVariantes = ref(0)
const paginaLedger = ref(1)

const imagenes = computed(() => {
  const deGaleria = (producto.value?.imagenes || []).map((imagen) => imagen.url)
  const deVariantes = (producto.value?.variantes || [])
    .map((variante) => variante.imagenUrl)
    .filter(Boolean)
  const portada = producto.value?.imagenPortada
  return [...new Set([portada, ...deGaleria, ...deVariantes].filter(Boolean))]
})

const precioDesde = computed(() => {
  if (!producto.value) return '—'
  return formatearPrecio(producto.value.precioDesde, producto.value.moneda || 'ARS')
})

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
      api.get(`/mis-productos/${idProducto.value}`),
      api.get(`/mis-productos/${idProducto.value}/variantes`, {
        params: { pagina: 1, tamano: POR_PAGINA },
      }),
      api.get(`/mis-productos/${idProducto.value}/ledger`, {
        params: { pagina: 1, tamano: POR_PAGINA },
      }),
    ])
    producto.value = productoRes.data
    aplicarVariantes(variantesRes.data)
    aplicarLedger(ledgerRes.data)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la vista previa del producto.'
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
    const res = await api.get(`/mis-productos/${idProducto.value}/variantes`, {
      params: { pagina: destino, tamano: POR_PAGINA },
    })
    aplicarVariantes(res.data)
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
    const res = await api.get(`/mis-productos/${idProducto.value}/ledger`, {
      params: { pagina: destino, tamano: POR_PAGINA },
    })
    aplicarLedger(res.data)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar el historial de autenticidad.'
  }
}

onMounted(cargar)
</script>

<template>
  <div>
    <router-link
      to="/panel/productos"
      class="text-sm text-granate transition hover:text-coral"
    >
      ← Volver a mis productos
    </router-link>

    <div class="mt-4 rounded-xl border border-coral/25 bg-rosa/20 px-4 py-3 text-sm text-granate">
      <span class="font-semibold">Vista previa del artista.</span>
      Esta pantalla todavía no es pública; te muestra cómo quedará la publicación
      y el estado de su registro de autenticidad.
    </div>

    <CargandoPagina v-if="cargando" tipo="detalle" mensaje="Cargando producto…" />
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else-if="producto" class="mt-6">
      <ProductoVistaDetalle
        modo="previa"
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
        :precio-desde="precioDesde"
        @cambiar-pagina-variantes="cambiarPaginaVariantes"
        @cambiar-pagina-ledger="cambiarPaginaLedger"
      >
        <template #acciones>
          <router-link
            :to="`/panel/productos/${producto.idProducto}/editar`"
            class="flex w-full items-center justify-center rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo"
          >
            {{ producto.estado === 'BORRADOR' ? 'Continuar borrador' : 'Editar producto' }}
          </router-link>
        </template>
      </ProductoVistaDetalle>
    </div>
  </div>
</template>
