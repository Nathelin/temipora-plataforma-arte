<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFecha } from '../servicios/pedidos'
import {
  FILTROS_ESTADO_VENTA,
  descripcionMedioPago,
  estadoVentaDe,
  estadoVentaProductoDe,
  necesitaSincronizar,
} from '../servicios/ventas'
import { usePanelArtista } from '../composables/usePanelArtista'
import ModalComoSeCalcula from '../components/ModalComoSeCalcula.vue'

const router = useRouter()

const TAMANIO = 10

const { dinero, cargar: cargarPanel } = usePanelArtista()

const vista = ref('servicios')

const estado = ref('')
const mes = ref('')
const idServicio = ref('')
const pagina = ref(0)
const ventas = ref([])
const totalElementos = ref(0)
const totalPaginas = ref(0)
const hayMas = ref(false)
const cargando = ref(true)
const error = ref('')
const errorAccion = ref('')
const sincronizando = ref([])
const servicios = ref([])
const modalCalculoAbierto = ref(false)

const idProducto = ref('')
const productos = ref([])
const ventasProductos = ref([])
const totalProductos = ref(0)
const totalPaginasProductos = ref(0)
const hayMasProductos = ref(false)
const paginaProductos = ref(0)
const cargandoProductos = ref(false)
const errorProductos = ref('')
const filaExpandida = ref(null)

const tarjetas = computed(() => [
  {
    etiqueta: 'Recibido total',
    valor: formatearPrecio(dinero.value.cobradoTotal, dinero.value.monedaMostrada),
    detalle: 'Neto de todos tus pagos acreditados.',
  },
  {
    etiqueta: 'Recibido este mes',
    valor: formatearPrecio(dinero.value.cobradoMes, dinero.value.monedaMostrada),
    detalle: 'Neto acreditado en el mes corriente.',
  },
  {
    etiqueta: 'Por cobrar',
    valor: formatearPrecio(dinero.value.porCobrar, dinero.value.monedaMostrada),
    detalle: 'Cuotas pendientes de pago en pedidos activos.',
  },
])

const hayFiltros = computed(
  () => Boolean(estado.value || mes.value || idServicio.value),
)
const hayFiltrosProductos = computed(
  () => Boolean(mes.value || idProducto.value),
)
const paginaMostrada = computed(() => (totalPaginas.value ? pagina.value + 1 : 0))
const puedeAnterior = computed(() => pagina.value > 0)
const puedeSiguiente = computed(() => hayMas.value)
const paginaMostradaProductos = computed(
  () => (totalPaginasProductos.value ? paginaProductos.value + 1 : 0),
)
const puedeAnteriorProductos = computed(() => paginaProductos.value > 0)
const puedeSiguienteProductos = computed(() => hayMasProductos.value)
const totalMostrado = computed(
  () => (vista.value === 'servicios' ? totalElementos.value : totalProductos.value),
)

function monedaNetoDe(venta) {
  if (venta.netoRecibidoMp != null) return venta.monedaCobro
  return venta.monedaMostrada || venta.monedaCobro
}

function netoDe(venta) {
  return venta.netoRecibidoMp ?? venta.netoMostrado ?? venta.montoNetoCobro
}

function netoVenta(venta) {
  return venta.netoMostrado ?? venta.montoNetoCobro
}

function formatearNeto(venta) {
  return formatearPrecio(
    netoVenta(venta),
    venta.monedaMostrada || venta.monedaCobro,
  )
}

function tieneDescuento(venta) {
  return Number(venta.descuentoMontoCobro) > 0
}

function liberacionDe(venta) {
  return venta.fechaLiberacion || venta.fechaAcreditacion
}

function estaSincronizando(idPago) {
  return sincronizando.value.includes(idPago)
}

async function sincronizarVenta(venta) {
  if (!venta?.puedeSincronizar || estaSincronizando(venta.idPago)) return
  sincronizando.value = [...sincronizando.value, venta.idPago]
  errorAccion.value = ''
  try {
    const res = await api.post(
      `/panel-artista/ventas/${venta.idPago}/sincronizar`,
    )
    const actualizado = res.data
    if (actualizado) {
      const indice = ventas.value.findIndex(
        (item) => item.idPago === venta.idPago,
      )
      if (indice >= 0) {
        ventas.value[indice] = actualizado
      }
    }
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudieron actualizar los datos de MercadoPago.'
  } finally {
    sincronizando.value = sincronizando.value.filter(
      (id) => id !== venta.idPago,
    )
  }
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const params = { pagina: pagina.value, tamanio: TAMANIO }
    if (estado.value) params.estado = estado.value
    if (mes.value) params.mes = mes.value
    if (idServicio.value) params.idServicio = idServicio.value
    const res = await api.get('/panel-artista/ventas', { params })
    ventas.value = res.data?.contenido || []
    totalElementos.value = Number(res.data?.totalElementos || 0)
    totalPaginas.value = Number(res.data?.totalPaginas || 0)
    hayMas.value = Boolean(res.data?.hayMas)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.data?.message || 'No se pudieron cargar tus ventas.'
  } finally {
    cargando.value = false
  }
}

async function cargarProductos() {
  cargandoProductos.value = true
  errorProductos.value = ''
  try {
    const params = { pagina: paginaProductos.value, tamanio: TAMANIO }
    if (mes.value) params.mes = mes.value
    if (idProducto.value) params.idProducto = idProducto.value
    const res = await api.get('/panel-artista/ventas/productos', { params })
    ventasProductos.value = res.data?.contenido || []
    totalProductos.value = Number(res.data?.totalElementos || 0)
    totalPaginasProductos.value = Number(res.data?.totalPaginas || 0)
    hayMasProductos.value = Boolean(res.data?.hayMas)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorProductos.value = e.response?.data?.message
      || 'No se pudieron cargar tus ventas de productos.'
  } finally {
    cargandoProductos.value = false
  }
}

async function cargarServicios() {
  try {
    const res = await api.get('/mis-servicios')
    servicios.value = res.data || []
  } catch {
    servicios.value = []
  }
}

async function cargarProductosFiltro() {
  try {
    const res = await api.get('/mis-productos')
    productos.value = res.data || []
  } catch {
    productos.value = []
  }
}

function cambiarVista(nueva) {
  if (vista.value === nueva) return
  vista.value = nueva
  filaExpandida.value = null
  if (nueva === 'productos') {
    paginaProductos.value = 0
    cargarProductos()
  } else {
    pagina.value = 0
    cargar()
  }
}

function aplicarFiltros() {
  pagina.value = 0
  cargar()
}

function limpiarFiltros() {
  estado.value = ''
  mes.value = ''
  idServicio.value = ''
  aplicarFiltros()
}

function aplicarFiltrosProductos() {
  paginaProductos.value = 0
  cargarProductos()
}

function limpiarFiltrosProductos() {
  mes.value = ''
  idProducto.value = ''
  aplicarFiltrosProductos()
}

function irAnterior() {
  if (!puedeAnterior.value) return
  pagina.value -= 1
  cargar()
}

function irSiguiente() {
  if (!puedeSiguiente.value) return
  pagina.value += 1
  cargar()
}

function irAnteriorProductos() {
  if (!puedeAnteriorProductos.value) return
  paginaProductos.value -= 1
  cargarProductos()
}

function irSiguienteProductos() {
  if (!puedeSiguienteProductos.value) return
  paginaProductos.value += 1
  cargarProductos()
}

function alternarFila(idCompraGrupo) {
  filaExpandida.value = filaExpandida.value === idCompraGrupo
    ? null
    : idCompraGrupo
}

function primerItem(venta) {
  return venta?.items?.[0] || null
}

function tituloPrincipal(venta) {
  return primerItem(venta)?.tituloProducto || 'Producto'
}

function variantePrincipal(venta) {
  return primerItem(venta)?.nombreVariante || ''
}

function imagenPrincipal(venta) {
  return primerItem(venta)?.imagenUrl || null
}

function cantidadProductos(venta) {
  return venta?.items?.length || 0
}

function netoProducto(venta) {
  return venta.netoRecibidoMp ?? venta.netoMostrado ?? venta.montoNetoCobro
}

function monedaNetoProducto(venta) {
  if (venta.netoRecibidoMp != null) return venta.monedaCobro
  return venta.monedaMostrada || venta.monedaCobro
}

function formatearNetoProducto(venta) {
  return formatearPrecio(netoProducto(venta), monedaNetoProducto(venta))
}

function formatearNetoMostradoProducto(venta) {
  return formatearPrecio(
    venta.netoMostrado ?? venta.montoNetoCobro,
    venta.monedaMostrada || venta.monedaCobro,
  )
}

function proveedorDe(venta) {
  if (!venta?.proveedor) return '—'
  return venta.proveedor === 'mercadopago' ? 'MercadoPago' : venta.proveedor
}

onMounted(() => {
  cargar()
  cargarServicios()
  cargarProductosFiltro()
  cargarPanel()
})
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8 sm:px-8 sm:py-10">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tus ingresos</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Ventas
        </h1>
        <p class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
          Todo lo que cobraste por tus pedidos de servicio y tus productos: lo que pagó el cliente,
          la comisión Temipora, el arancel de MercadoPago y tu neto.
        </p>
        <button
          type="button"
          class="mt-2 text-xs font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
          @click="modalCalculoAbierto = true"
        >
          ¿Cómo se calcula?
        </button>
      </div>
      <span
        v-if="vista === 'servicios' ? (!cargando && !error) : (!cargandoProductos && !errorProductos)"
        class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral sm:self-auto"
      >
        {{ totalMostrado }} {{ totalMostrado === 1 ? 'venta' : 'ventas' }}
      </span>
    </header>

    <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="tarjeta in tarjetas"
        :key="tarjeta.etiqueta"
        class="rounded-2xl border border-rosa/60 bg-blanco p-5"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
          {{ tarjeta.etiqueta }}
        </p>
        <p class="mt-3 font-display text-2xl leading-tight font-extrabold text-granate">
          {{ tarjeta.valor }}
        </p>
        <p class="mt-1 text-xs text-cafe/55">{{ tarjeta.detalle }}</p>
      </div>
    </div>

    <div
      class="mt-6 flex gap-1 rounded-xl border border-rosa/60 bg-blanco p-1"
      role="group"
      aria-label="Tipo de venta"
    >
      <button
        type="button"
        class="flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition"
        :class="vista === 'servicios'
          ? 'bg-coral text-white shadow-sm'
          : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
        :aria-pressed="vista === 'servicios'"
        @click="cambiarVista('servicios')"
      >
        Servicios
      </button>
      <button
        type="button"
        class="flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition"
        :class="vista === 'productos'
          ? 'bg-coral text-white shadow-sm'
          : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
        :aria-pressed="vista === 'productos'"
        @click="cambiarVista('productos')"
      >
        Productos
      </button>
    </div>

    <template v-if="vista === 'servicios'">
      <div class="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
          Estado
          <select
            v-model="estado"
            class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="aplicarFiltros"
          >
            <option v-for="opcion in FILTROS_ESTADO_VENTA" :key="opcion.valor" :value="opcion.valor">
              {{ opcion.etiqueta }}
            </option>
          </select>
        </label>

        <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
          Mes
          <input
            v-model="mes"
            type="month"
            class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="aplicarFiltros"
          />
        </label>

        <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60 sm:flex-1">
          Servicio
          <select
            v-model="idServicio"
            class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="aplicarFiltros"
          >
            <option value="">Todos mis servicios</option>
            <option
              v-for="servicio in servicios"
              :key="servicio.idServicio"
              :value="String(servicio.idServicio)"
            >
              {{ servicio.nombre }}
            </option>
          </select>
        </label>

        <button
          v-if="hayFiltros"
          type="button"
          class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>

      <p
        v-if="errorAccion"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAccion }}
      </p>

      <div v-if="cargando" class="mt-6 space-y-3" role="status" aria-live="polite">
        <span class="sr-only">Cargando ventas...</span>
        <div
          v-for="n in 3"
          :key="n"
          class="h-16 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"
        ></div>
      </div>

      <div
        v-else-if="error"
        class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center"
      >
        <p class="text-sm text-rojizo">{{ error }}</p>
        <button
          type="button"
          class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargar"
        >
          Reintentar
        </button>
      </div>

      <div
        v-else-if="!ventas.length"
        class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
      >
        <p class="text-lg font-semibold text-granate">
          {{ hayFiltros ? 'No hay ventas con esos filtros' : 'Todavía no tenés ventas' }}
        </p>
        <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/60">
          {{ hayFiltros
            ? 'Probá con otro estado, otro mes u otro servicio.'
            : 'Cuando un cliente pague un presupuesto, la venta va a aparecer acá.' }}
        </p>
        <button
          v-if="hayFiltros"
          type="button"
          class="mt-5 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="limpiarFiltros"
        >
          Limpiar filtros
        </button>
      </div>

      <div v-else class="mt-6">
        <div class="hidden overflow-x-auto rounded-2xl border border-rosa/60 bg-blanco md:block">
          <table class="min-w-full text-sm">
            <thead class="bg-rosa/20 text-left text-xs uppercase tracking-wide text-cafe/60">
              <tr>
                <th class="px-4 py-3 font-semibold">Fecha</th>
                <th class="px-4 py-3 font-semibold">Cliente</th>
                <th class="px-4 py-3 font-semibold">Servicio / hito</th>
                <th class="px-4 py-3 text-right font-semibold">El cliente pagó</th>
                <th class="px-4 py-3 text-right font-semibold">Comisión Temipora</th>
                <th class="px-4 py-3 text-right font-semibold">Tu neto</th>
                <th class="px-4 py-3 text-right font-semibold">Arancel MercadoPago</th>
                <th class="px-4 py-3 text-right font-semibold">Recibís</th>
                <th class="px-4 py-3 font-semibold">Medio de pago</th>
                <th class="px-4 py-3 font-semibold">Estado</th>
                <th class="px-4 py-3 font-semibold">Liberación</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="venta in ventas"
                :key="venta.idPago"
                class="border-t border-rosa/40 transition hover:bg-rosa/10"
              >
                <td class="px-4 py-3 whitespace-nowrap text-cafe/70">
                  {{ formatearFecha(venta.fecha) }}
                </td>
                <td class="px-4 py-3">
                  <router-link
                    :to="{ name: 'panel-pedido-detalle', params: { idPedido: venta.idPedido } }"
                    class="font-semibold text-coral transition hover:text-rojizo"
                  >
                    @{{ venta.nombreUsuarioCliente || 'usuario' }}
                  </router-link>
                </td>
                <td class="px-4 py-3">
                  <p class="font-medium text-granate">{{ venta.nombreServicio || 'Servicio' }}</p>
                  <p class="text-xs text-cafe/55">{{ venta.nombreHito }}</p>
                </td>
                <td class="px-4 py-3 text-right whitespace-nowrap text-cafe/80">
                  {{ formatearPrecio(venta.montoCobro, venta.monedaCobro) }}
                  <span
                    v-if="tieneDescuento(venta)"
                    class="mt-0.5 block text-[11px] text-cafe/50"
                  >
                    Desc. {{ formatearPrecio(venta.descuentoMontoCobro, venta.monedaCobro) }}
                    · lo financia Temipora
                  </span>
                </td>
                <td class="px-4 py-3 text-right whitespace-nowrap text-cafe/60">
                  {{ formatearPrecio(venta.comisionMontoCobro, venta.monedaCobro) }}
                </td>
                <td class="px-4 py-3 text-right whitespace-nowrap font-semibold text-granate">
                  {{ formatearNeto(venta) }}
                </td>
                <td class="px-4 py-3 text-right whitespace-nowrap">
                  <span class="block text-cafe/70">
                    {{ formatearPrecio(venta.arancelMp, venta.monedaCobro) }}
                  </span>
                  <button
                    v-if="necesitaSincronizar(venta)"
                    type="button"
                    :disabled="estaSincronizando(venta.idPago)"
                    class="mt-0.5 text-[11px] font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline disabled:opacity-50"
                    @click="sincronizarVenta(venta)"
                  >
                    {{ estaSincronizando(venta.idPago) ? 'Actualizando...' : 'Actualizar' }}
                  </button>
                </td>
                <td class="px-4 py-3 text-right whitespace-nowrap font-semibold text-granate">
                  {{ formatearPrecio(netoDe(venta), monedaNetoDe(venta)) }}
                </td>
                <td class="px-4 py-3 whitespace-nowrap text-cafe/70">
                  {{ descripcionMedioPago(venta) }}
                </td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                    :class="estadoVentaDe(venta.estado).clase"
                  >
                    {{ estadoVentaDe(venta.estado).etiqueta }}
                  </span>
                </td>
                <td class="px-4 py-3 whitespace-nowrap text-cafe/60">
                  {{ formatearFecha(liberacionDe(venta)) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <ul class="space-y-3 md:hidden">
          <li
            v-for="venta in ventas"
            :key="venta.idPago"
            class="rounded-2xl border border-rosa/60 bg-blanco p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate font-semibold text-granate">
                  {{ venta.nombreServicio || 'Servicio' }}
                </p>
                <p class="truncate text-xs text-cafe/55">
                  {{ venta.nombreHito }} · {{ formatearFecha(venta.fecha) }}
                </p>
              </div>
              <span
                class="shrink-0 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                :class="estadoVentaDe(venta.estado).clase"
              >
                {{ estadoVentaDe(venta.estado).etiqueta }}
              </span>
            </div>

            <dl class="mt-3 grid grid-cols-2 gap-2 text-sm">
              <div>
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Cliente</dt>
                <dd class="text-cafe/75">@{{ venta.nombreUsuarioCliente || 'usuario' }}</dd>
              </div>
              <div class="text-right">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Recibís</dt>
                <dd class="font-semibold text-granate">
                  {{ formatearPrecio(netoDe(venta), monedaNetoDe(venta)) }}
                </dd>
              </div>
              <div>
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">El cliente pagó</dt>
                <dd class="text-cafe/70">
                  {{ formatearPrecio(venta.montoCobro, venta.monedaCobro) }}
                </dd>
              </div>
              <div class="text-right">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Tu neto</dt>
                <dd class="text-cafe/80">
                  {{ formatearNeto(venta) }}
                </dd>
              </div>
              <div>
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Comisión Temipora</dt>
                <dd class="text-cafe/60">
                  {{ formatearPrecio(venta.comisionMontoCobro, venta.monedaCobro) }}
                </dd>
              </div>
              <div class="text-right">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Arancel MercadoPago</dt>
                <dd class="text-cafe/60">
                  {{ formatearPrecio(venta.arancelMp, venta.monedaCobro) }}
                </dd>
              </div>
              <div class="col-span-2 flex items-center justify-between">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Liberación</dt>
                <dd class="text-cafe/60">
                  {{ formatearFecha(liberacionDe(venta)) }}
                </dd>
              </div>
            </dl>

            <p v-if="tieneDescuento(venta)" class="mt-2 text-xs text-cafe/55">
              Descuento del cliente:
              {{ formatearPrecio(venta.descuentoMontoCobro, venta.monedaCobro) }}
              · lo financia Temipora
            </p>

            <p class="mt-2 text-xs text-cafe/55">
              Medio de pago: {{ descripcionMedioPago(venta) }}
            </p>

            <div class="mt-3 flex flex-wrap items-center gap-3">
              <router-link
                :to="{ name: 'panel-pedido-detalle', params: { idPedido: venta.idPedido } }"
                class="inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
              >
                Ver pedido
              </router-link>
              <button
                v-if="necesitaSincronizar(venta)"
                type="button"
                :disabled="estaSincronizando(venta.idPago)"
                class="text-sm font-semibold text-cafe/60 underline-offset-2 transition hover:text-granate hover:underline disabled:opacity-50"
                @click="sincronizarVenta(venta)"
              >
                {{ estaSincronizando(venta.idPago) ? 'Actualizando...' : 'Actualizar datos de MP' }}
              </button>
            </div>
          </li>
        </ul>

        <p class="mt-3 text-xs leading-relaxed text-cafe/50">
          “Recibís” usa el arancel real de MercadoPago cuando está disponible; si no,
          es una estimación.
        </p>

        <div
          v-if="totalPaginas > 1"
          class="mt-5 flex flex-wrap items-center justify-between gap-3"
        >
          <p class="text-xs text-cafe/55">
            Página {{ paginaMostrada }} de {{ totalPaginas }}
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              :disabled="!puedeAnterior"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
              @click="irAnterior"
            >
              Anterior
            </button>
            <button
              type="button"
              :disabled="!puedeSiguiente"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
              @click="irSiguiente"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
          Mes
          <input
            v-model="mes"
            type="month"
            class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="aplicarFiltrosProductos"
          />
        </label>

        <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60 sm:flex-1">
          Producto
          <select
            v-model="idProducto"
            class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="aplicarFiltrosProductos"
          >
            <option value="">Todos mis productos</option>
            <option
              v-for="producto in productos"
              :key="producto.idProducto"
              :value="String(producto.idProducto)"
            >
              {{ producto.titulo }}
            </option>
          </select>
        </label>

        <button
          v-if="hayFiltrosProductos"
          type="button"
          class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="limpiarFiltrosProductos"
        >
          Limpiar
        </button>
      </div>

      <div v-if="cargandoProductos" class="mt-6 space-y-3" role="status" aria-live="polite">
        <span class="sr-only">Cargando ventas de productos...</span>
        <div
          v-for="n in 3"
          :key="n"
          class="h-16 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"
        ></div>
      </div>

      <div
        v-else-if="errorProductos"
        class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center"
      >
        <p class="text-sm text-rojizo">{{ errorProductos }}</p>
        <button
          type="button"
          class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargarProductos"
        >
          Reintentar
        </button>
      </div>

      <div
        v-else-if="!ventasProductos.length"
        class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
      >
        <p class="text-lg font-semibold text-granate">
          {{ hayFiltrosProductos
            ? 'No hay ventas de productos con esos filtros'
            : 'Todavía no tenés ventas de productos' }}
        </p>
        <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/60">
          {{ hayFiltrosProductos
            ? 'Probá con otro mes u otro producto.'
            : 'Cuando un cliente compre uno de tus productos, la venta va a aparecer acá.' }}
        </p>
        <button
          v-if="hayFiltrosProductos"
          type="button"
          class="mt-5 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="limpiarFiltrosProductos"
        >
          Limpiar filtros
        </button>
      </div>

      <div v-else class="mt-6">
        <div class="hidden overflow-x-auto rounded-2xl border border-rosa/60 bg-blanco md:block">
          <table class="min-w-full text-sm">
            <thead class="bg-rosa/20 text-left text-xs uppercase tracking-wide text-cafe/60">
              <tr>
                <th class="px-4 py-3 font-semibold">Fecha</th>
                <th class="px-4 py-3 font-semibold">Cliente</th>
                <th class="px-4 py-3 font-semibold">Producto</th>
                <th class="px-4 py-3 text-right font-semibold">El cliente pagó</th>
                <th class="px-4 py-3 text-right font-semibold">Comisión Temipora</th>
                <th class="px-4 py-3 text-right font-semibold">Tu neto</th>
                <th class="px-4 py-3 text-right font-semibold">Arancel MercadoPago</th>
                <th class="px-4 py-3 text-right font-semibold">Recibís</th>
                <th class="px-4 py-3 font-semibold">Pago</th>
                <th class="px-4 py-3 font-semibold">Estado</th>
              </tr>
            </thead>
            <tbody>
              <template
                v-for="venta in ventasProductos"
                :key="venta.idCompraGrupo"
              >
                <tr class="border-t border-rosa/40 transition hover:bg-rosa/10">
                  <td class="px-4 py-3 whitespace-nowrap text-cafe/70">
                    {{ formatearFecha(venta.fecha) }}
                  </td>
                  <td class="px-4 py-3 font-semibold text-cafe/80">
                    @{{ venta.nombreUsuarioCliente || 'usuario' }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-3">
                      <img
                        v-if="imagenPrincipal(venta)"
                        :src="imagenPrincipal(venta)"
                        :alt="tituloPrincipal(venta)"
                        class="h-11 w-11 shrink-0 rounded-lg border border-rosa/50 object-cover"
                        loading="lazy"
                      />
                      <div
                        v-else
                        class="h-11 w-11 shrink-0 rounded-lg border border-rosa/50 bg-rosa/20"
                      ></div>
                      <div class="min-w-0">
                        <p class="truncate font-medium text-granate">
                          {{ tituloPrincipal(venta) }}
                        </p>
                        <p class="truncate text-xs text-cafe/55">
                          {{ variantePrincipal(venta) }}
                        </p>
                      </div>
                    </div>
                    <button
                      v-if="cantidadProductos(venta) > 1"
                      type="button"
                      class="mt-1 text-[11px] font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
                      @click="alternarFila(venta.idCompraGrupo)"
                    >
                      {{ filaExpandida === venta.idCompraGrupo
                        ? 'Ocultar detalle'
                        : `Ver los ${cantidadProductos(venta)} productos` }}
                    </button>
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap text-cafe/80">
                    {{ formatearPrecio(venta.montoCobro, venta.monedaCobro) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap text-cafe/60">
                    {{ formatearPrecio(venta.comisionMontoCobro, venta.monedaCobro) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap font-semibold text-granate">
                    {{ formatearNetoMostradoProducto(venta) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap text-cafe/70">
                    {{ formatearPrecio(venta.arancelMp, venta.monedaCobro) }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap font-semibold text-granate">
                    {{ formatearNetoProducto(venta) }}
                  </td>
                  <td class="px-4 py-3 whitespace-nowrap text-cafe/70">
                    {{ proveedorDe(venta) }}
                  </td>
                  <td class="px-4 py-3">
                    <span
                      class="inline-flex whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                      :class="estadoVentaProductoDe(venta.estado).clase"
                    >
                      {{ estadoVentaProductoDe(venta.estado).etiqueta }}
                    </span>
                  </td>
                </tr>
                <tr
                  v-if="filaExpandida === venta.idCompraGrupo"
                  class="border-t border-rosa/40 bg-rosa/5"
                >
                  <td colspan="10" class="px-4 py-3">
                    <ul class="grid gap-2 sm:grid-cols-2">
                      <li
                        v-for="item in venta.items"
                        :key="item.idCompraItem"
                        class="flex items-center gap-3 rounded-xl border border-rosa/40 bg-blanco px-3 py-2"
                      >
                        <img
                          v-if="item.imagenUrl"
                          :src="item.imagenUrl"
                          :alt="item.tituloProducto"
                          class="h-10 w-10 shrink-0 rounded-lg border border-rosa/50 object-cover"
                          loading="lazy"
                        />
                        <div
                          v-else
                          class="h-10 w-10 shrink-0 rounded-lg border border-rosa/50 bg-rosa/20"
                        ></div>
                        <div class="min-w-0 flex-1">
                          <p class="truncate text-sm font-medium text-granate">
                            {{ item.tituloProducto }}
                          </p>
                          <p class="truncate text-xs text-cafe/55">
                            {{ item.nombreVariante }}
                          </p>
                        </div>
                        <span class="shrink-0 text-sm text-cafe/70">
                          {{ formatearPrecio(item.precioTotalCobro, venta.monedaCobro) }}
                        </span>
                      </li>
                    </ul>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <ul class="space-y-3 md:hidden">
          <li
            v-for="venta in ventasProductos"
            :key="venta.idCompraGrupo"
            class="rounded-2xl border border-rosa/60 bg-blanco p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex min-w-0 items-center gap-3">
                <img
                  v-if="imagenPrincipal(venta)"
                  :src="imagenPrincipal(venta)"
                  :alt="tituloPrincipal(venta)"
                  class="h-12 w-12 shrink-0 rounded-lg border border-rosa/50 object-cover"
                  loading="lazy"
                />
                <div
                  v-else
                  class="h-12 w-12 shrink-0 rounded-lg border border-rosa/50 bg-rosa/20"
                ></div>
                <div class="min-w-0">
                  <p class="truncate font-semibold text-granate">
                    {{ tituloPrincipal(venta) }}
                  </p>
                  <p class="truncate text-xs text-cafe/55">
                    {{ variantePrincipal(venta) }} · {{ formatearFecha(venta.fecha) }}
                  </p>
                </div>
              </div>
              <span
                class="shrink-0 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                :class="estadoVentaProductoDe(venta.estado).clase"
              >
                {{ estadoVentaProductoDe(venta.estado).etiqueta }}
              </span>
            </div>

            <dl class="mt-3 grid grid-cols-2 gap-2 text-sm">
              <div>
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Cliente</dt>
                <dd class="text-cafe/75">@{{ venta.nombreUsuarioCliente || 'usuario' }}</dd>
              </div>
              <div class="text-right">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Recibís</dt>
                <dd class="font-semibold text-granate">
                  {{ formatearNetoProducto(venta) }}
                </dd>
              </div>
              <div>
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">El cliente pagó</dt>
                <dd class="text-cafe/70">
                  {{ formatearPrecio(venta.montoCobro, venta.monedaCobro) }}
                </dd>
              </div>
              <div class="text-right">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Tu neto</dt>
                <dd class="text-cafe/80">
                  {{ formatearNetoMostradoProducto(venta) }}
                </dd>
              </div>
              <div>
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Comisión Temipora</dt>
                <dd class="text-cafe/60">
                  {{ formatearPrecio(venta.comisionMontoCobro, venta.monedaCobro) }}
                </dd>
              </div>
              <div class="text-right">
                <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Arancel MercadoPago</dt>
                <dd class="text-cafe/60">
                  {{ formatearPrecio(venta.arancelMp, venta.monedaCobro) }}
                </dd>
              </div>
            </dl>

            <p class="mt-2 text-xs text-cafe/55">
              Pago: {{ proveedorDe(venta) }}
            </p>

            <button
              v-if="cantidadProductos(venta) > 1"
              type="button"
              class="mt-3 inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
              @click="alternarFila(venta.idCompraGrupo)"
            >
              {{ filaExpandida === venta.idCompraGrupo
                ? 'Ocultar detalle'
                : `Ver los ${cantidadProductos(venta)} productos` }}
            </button>

            <ul
              v-if="filaExpandida === venta.idCompraGrupo"
              class="mt-2 space-y-2"
            >
              <li
                v-for="item in venta.items"
                :key="item.idCompraItem"
                class="flex items-center gap-2 rounded-xl border border-rosa/40 px-2 py-1.5"
              >
                <img
                  v-if="item.imagenUrl"
                  :src="item.imagenUrl"
                  :alt="item.tituloProducto"
                  class="h-8 w-8 shrink-0 rounded-md border border-rosa/50 object-cover"
                  loading="lazy"
                />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-xs font-medium text-granate">
                    {{ item.tituloProducto }}
                  </p>
                  <p class="truncate text-[11px] text-cafe/55">{{ item.nombreVariante }}</p>
                </div>
                <span class="shrink-0 text-xs text-cafe/70">
                  {{ formatearPrecio(item.precioTotalCobro, venta.monedaCobro) }}
                </span>
              </li>
            </ul>
          </li>
        </ul>

        <p class="mt-3 text-xs leading-relaxed text-cafe/50">
          “Recibís” usa el arancel real de MercadoPago cuando está disponible; si no,
          es una estimación. Cada fila es una orden de pago; si incluye varios productos,
          podés abrir el detalle.
        </p>

        <div
          v-if="totalPaginasProductos > 1"
          class="mt-5 flex flex-wrap items-center justify-between gap-3"
        >
          <p class="text-xs text-cafe/55">
            Página {{ paginaMostradaProductos }} de {{ totalPaginasProductos }}
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              :disabled="!puedeAnteriorProductos"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
              @click="irAnteriorProductos"
            >
              Anterior
            </button>
            <button
              type="button"
              :disabled="!puedeSiguienteProductos"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
              @click="irSiguienteProductos"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </template>

    <ModalComoSeCalcula
      :abierto="modalCalculoAbierto"
      contexto="venta"
      :moneda="dinero.monedaMostrada || 'ARS'"
      @cerrar="modalCalculoAbierto = false"
    />
  </div>
</template>
