<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFecha } from '../servicios/pedidos'
import { FILTROS_ESTADO_VENTA, estadoVentaDe } from '../servicios/ventas'
import { usePanelArtista } from '../composables/usePanelArtista'

const router = useRouter()

const TAMANIO = 10

const { dinero, cargar: cargarPanel } = usePanelArtista()

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
const servicios = ref([])

const tarjetas = computed(() => [
  {
    etiqueta: 'Cobrado total',
    valor: formatearPrecio(dinero.value.cobradoTotal, dinero.value.monedaMostrada),
    detalle: 'Neto de todos tus pagos acreditados.',
  },
  {
    etiqueta: 'Cobrado este mes',
    valor: formatearPrecio(dinero.value.cobradoMes, dinero.value.monedaMostrada),
    detalle: 'Neto acreditado en el mes corriente.',
  },
  {
    etiqueta: 'Por cobrar',
    valor: formatearPrecio(dinero.value.porCobrar, dinero.value.monedaMostrada),
    detalle: 'Cuotas pendientes de pago en pedidos activos.',
  },
  {
    etiqueta: 'Pendiente de acreditar',
    valor: formatearPrecio(dinero.value.pendienteAcreditar, dinero.value.monedaMostrada),
    detalle: 'Pagos iniciados que todavía no se acreditaron.',
  },
])

const hayFiltros = computed(
  () => Boolean(estado.value || mes.value || idServicio.value),
)
const paginaMostrada = computed(() => (totalPaginas.value ? pagina.value + 1 : 0))
const puedeAnterior = computed(() => pagina.value > 0)
const puedeSiguiente = computed(() => hayMas.value)

function monedaDe(venta) {
  return venta.monedaMostrada || venta.monedaCobro
}

function netoDe(venta) {
  return venta.netoMostrado ?? venta.montoNetoCobro
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

async function cargarServicios() {
  try {
    const res = await api.get('/mis-servicios')
    servicios.value = res.data || []
  } catch {
    servicios.value = []
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

onMounted(() => {
  cargar()
  cargarServicios()
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
          Todo lo que cobraste por tus pedidos: lo que pagó el cliente, la comisión de
          Temipora y lo que te queda.
        </p>
      </div>
      <span
        v-if="!cargando && !error"
        class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral sm:self-auto"
      >
        {{ totalElementos }} {{ totalElementos === 1 ? 'venta' : 'ventas' }}
      </span>
    </header>

    <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
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
      <div class="hidden overflow-hidden rounded-2xl border border-rosa/60 bg-blanco md:block">
        <table class="min-w-full text-sm">
          <thead class="bg-rosa/20 text-left text-xs uppercase tracking-wide text-cafe/60">
            <tr>
              <th class="px-4 py-3 font-semibold">Fecha</th>
              <th class="px-4 py-3 font-semibold">Cliente</th>
              <th class="px-4 py-3 font-semibold">Servicio / hito</th>
              <th class="px-4 py-3 text-right font-semibold">Cobrado</th>
              <th class="px-4 py-3 text-right font-semibold">Comisión</th>
              <th class="px-4 py-3 text-right font-semibold">Neto</th>
              <th class="px-4 py-3 font-semibold">Estado</th>
              <th class="px-4 py-3 font-semibold">Acreditado</th>
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
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap text-cafe/60">
                {{ formatearPrecio(venta.comisionMontoCobro, venta.monedaCobro) }}
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap font-semibold text-granate">
                {{ formatearPrecio(netoDe(venta), monedaDe(venta)) }}
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
                {{ formatearFecha(venta.fechaAcreditacion) }}
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
              <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Neto</dt>
              <dd class="font-semibold text-granate">
                {{ formatearPrecio(netoDe(venta), monedaDe(venta)) }}
              </dd>
            </div>
            <div>
              <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Cobrado</dt>
              <dd class="text-cafe/70">
                {{ formatearPrecio(venta.montoCobro, venta.monedaCobro) }}
              </dd>
            </div>
            <div class="text-right">
              <dt class="text-[11px] uppercase tracking-wide text-cafe/45">Comisión</dt>
              <dd class="text-cafe/60">
                {{ formatearPrecio(venta.comisionMontoCobro, venta.monedaCobro) }}
              </dd>
            </div>
          </dl>

          <router-link
            :to="{ name: 'panel-pedido-detalle', params: { idPedido: venta.idPedido } }"
            class="mt-3 inline-flex text-sm font-semibold text-coral transition hover:text-rojizo"
          >
            Ver pedido
          </router-link>
        </li>
      </ul>

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
  </div>
</template>
