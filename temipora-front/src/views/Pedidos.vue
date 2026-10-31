<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import {
  GRUPOS_PEDIDO,
  GRUPO_PEDIDO_DEFAULT,
  ORDENES_PEDIDO,
  ORDEN_PEDIDO_DEFAULT,
} from '../servicios/pedidos'
import FilaPedidoActivo from '../components/FilaPedidoActivo.vue'

const route = useRoute()
const router = useRouter()
const esArtista = localStorage.getItem('rol') === 'ARTISTA'

const CONTADORES_VACIOS = {
  todos: 0,
  enCurso: 0,
  accion: 0,
  finalizados: 0,
  cerrados: 0,
}

const bandejaActiva = ref(esArtista ? 'recibidos' : 'enviados')
const grupoActivo = ref(grupoDeQuery(route.query.grupo))
const idServicio = ref(route.query.servicio ? String(route.query.servicio) : '')
const busqueda = ref(String(route.query.q || ''))
const ordenActivo = ref(ordenDeQuery(route.query.orden))
const contadores = ref({ ...CONTADORES_VACIOS })
const servicios = ref([])
const pedidos = ref([])
const pagina = ref(0)
const hayMas = ref(false)
const total = ref(0)
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')

let temporizadorBusqueda = null

const enPanel = computed(() => route.path.startsWith('/panel'))
const bandejas = computed(() => (esArtista
  ? [
      { clave: 'recibidos', texto: 'Recibidos' },
      { clave: 'enviados', texto: 'Enviados' },
    ]
  : [{ clave: 'enviados', texto: 'Enviados' }]))

const mostrarFiltroServicio = computed(
  () => esArtista && bandejaActiva.value === 'recibidos',
)

const hayFiltros = computed(() => Boolean(idServicio.value)
  || busqueda.value.trim().length >= 2)

const gruposVisibles = computed(() => GRUPOS_PEDIDO.filter(
  (grupo) => !grupo.soloSiHay
    || contarGrupo(grupo) > 0
    || grupoActivo.value === grupo.clave,
))

const emptyState = computed(() => {
  if (hayFiltros.value) {
    return {
      titulo: 'No encontramos pedidos con esos filtros',
      detalle: 'Probá con otro nombre, otro servicio o quitá los filtros.',
    }
  }
  if (grupoActivo.value === 'accion') {
    return {
      titulo: 'Nada pendiente de tu parte',
      detalle: 'No hay entregas ni aprobaciones esperando tu acción.',
    }
  }
  if (grupoActivo.value === 'finalizados') {
    return {
      titulo: 'Todavía no hay pedidos finalizados',
      detalle: 'Cuando un pedido se complete, va a quedar archivado acá.',
    }
  }
  if (grupoActivo.value === 'cerrados') {
    return {
      titulo: 'No hay pedidos cancelados',
      detalle: 'Acá van a quedar los pedidos cancelados o dados de baja por falta de pago.',
    }
  }
  if (grupoActivo.value === 'en-curso') {
    return {
      titulo: 'No tenés pedidos en curso',
      detalle: bandejaActiva.value === 'recibidos'
        ? 'Cuando un cliente acepte y pague un presupuesto, el pedido va a aparecer acá.'
        : 'Cuando aceptes y pagues un presupuesto, el pedido va a aparecer acá.',
    }
  }
  return {
    titulo: 'Todavía no hay pedidos',
    detalle: 'Acá vas a ver todos tus pedidos, sin importar el estado.',
  }
})

function grupoDeQuery(valor) {
  const clave = String(valor || '')
  return GRUPOS_PEDIDO.some((grupo) => grupo.clave === clave)
    ? clave
    : GRUPO_PEDIDO_DEFAULT
}

function ordenDeQuery(valor) {
  const clave = String(valor || '')
  return ORDENES_PEDIDO.some((orden) => orden.clave === clave)
    ? clave
    : ORDEN_PEDIDO_DEFAULT
}

function contarGrupo(grupo) {
  return Number(contadores.value[grupo.contador] || 0)
}

function sincronizarUrl() {
  const query = {}
  if (grupoActivo.value !== GRUPO_PEDIDO_DEFAULT) {
    query.grupo = grupoActivo.value
  }
  if (idServicio.value) {
    query.servicio = String(idServicio.value)
  }
  if (busqueda.value.trim().length >= 2) {
    query.q = busqueda.value.trim()
  }
  if (ordenActivo.value !== ORDEN_PEDIDO_DEFAULT) {
    query.orden = ordenActivo.value
  }
  router.replace({ query })
}

async function cargar(reset = true) {
  if (reset) {
    pagina.value = 0
    pedidos.value = []
  } else {
    cargandoMas.value = true
  }
  cargando.value = reset
  error.value = ''
  try {
    const endpoint = bandejaActiva.value === 'recibidos'
      ? '/pedidos/recibidos'
      : '/pedidos/enviados'
    const params = {
      pagina: pagina.value,
      tamanio: 12,
      grupo: grupoActivo.value,
    }
    if (idServicio.value) params.idServicio = idServicio.value
    if (busqueda.value.trim().length >= 2) params.q = busqueda.value.trim()
    if (ordenActivo.value !== ORDEN_PEDIDO_DEFAULT) {
      params.orden = ordenActivo.value
    }
    const res = await api.get(endpoint, { params })
    const contenido = res.data?.contenido || []
    pedidos.value = reset ? contenido : [...pedidos.value, ...contenido]
    hayMas.value = Boolean(res.data?.hayMas)
    total.value = Number(res.data?.totalElementos || 0)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.data?.message || 'No se pudieron cargar tus pedidos.'
  } finally {
    cargando.value = false
    cargandoMas.value = false
  }
}

async function cargarContadores() {
  if (bandejaActiva.value === 'recibidos' && !esArtista) {
    contadores.value = { ...CONTADORES_VACIOS }
    return
  }
  try {
    const res = await api.get('/pedidos/contadores', {
      params: { bandeja: bandejaActiva.value },
    })
    contadores.value = { ...CONTADORES_VACIOS, ...(res.data || {}) }
  } catch {
    contadores.value = { ...CONTADORES_VACIOS }
  }
}

async function cargarServicios() {
  if (!esArtista) return
  try {
    const res = await api.get('/mis-servicios')
    servicios.value = res.data || []
  } catch {
    servicios.value = []
  }
}

function aplicarCambios() {
  window.clearTimeout(temporizadorBusqueda)
  sincronizarUrl()
  cargar(true)
}

function cambiarBandeja(clave) {
  if (bandejaActiva.value === clave) return
  bandejaActiva.value = clave
  idServicio.value = ''
  aplicarCambios()
  cargarContadores()
}

function cambiarGrupo(clave) {
  if (grupoActivo.value === clave) return
  grupoActivo.value = clave
  aplicarCambios()
}

function alBuscar() {
  window.clearTimeout(temporizadorBusqueda)
  temporizadorBusqueda = window.setTimeout(aplicarCambios, 300)
}

function limpiarFiltros() {
  idServicio.value = ''
  busqueda.value = ''
  ordenActivo.value = ORDEN_PEDIDO_DEFAULT
  aplicarCambios()
}

function cargarMas() {
  pagina.value += 1
  cargar(false)
}

onMounted(() => {
  cargar(true)
  cargarContadores()
  cargarServicios()
})

onUnmounted(() => {
  window.clearTimeout(temporizadorBusqueda)
})
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tu actividad</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Pedidos
        </h1>
        <p class="mt-2 text-sm text-cafe/65">
          {{ bandejaActiva === 'recibidos'
            ? 'Pedidos que recibiste de tus clientes.'
            : 'Pedidos que hiciste a otros artistas.' }}
        </p>
      </div>
      <span
        v-if="!cargando && !error"
        class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral sm:self-auto"
      >
        {{ total }} {{ total === 1 ? 'pedido' : 'pedidos' }}
      </span>
    </header>

    <div
      v-if="bandejas.length > 1"
      class="mt-5 flex gap-1 rounded-xl border border-rosa/60 bg-blanco p-1"
      role="group"
      aria-label="Elegir bandeja de pedidos"
    >
      <button
        v-for="bandeja in bandejas"
        :key="bandeja.clave"
        type="button"
        class="flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition"
        :class="bandejaActiva === bandeja.clave
          ? 'bg-coral text-white shadow-sm'
          : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
        :aria-pressed="bandejaActiva === bandeja.clave"
        @click="cambiarBandeja(bandeja.clave)"
      >
        {{ bandeja.texto }}
        <span
          v-if="bandejaActiva === bandeja.clave"
          class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
          :class="bandejaActiva === bandeja.clave ? 'bg-white/20 text-white' : 'bg-rosa/30 text-granate'"
        >
          {{ contadores.todos }}
        </span>
      </button>
    </div>

    <div
      class="flex gap-1 overflow-x-auto rounded-xl border border-rosa/60 bg-blanco p-1"
      :class="bandejas.length > 1 ? 'mt-3' : 'mt-5'"
      role="group"
      aria-label="Filtrar pedidos por grupo"
    >
      <button
        v-for="grupo in gruposVisibles"
        :key="grupo.clave"
        type="button"
        class="flex shrink-0 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition"
        :class="grupoActivo === grupo.clave
          ? 'bg-coral text-white shadow-sm'
          : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
        :aria-pressed="grupoActivo === grupo.clave"
        @click="cambiarGrupo(grupo.clave)"
      >
        {{ grupo.texto }}
        <span
          class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
          :class="grupoActivo === grupo.clave ? 'bg-white/20 text-white' : 'bg-rosa/30 text-granate'"
        >
          {{ contarGrupo(grupo) }}
        </span>
      </button>
    </div>

    <div class="mt-3 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
      <label
        v-if="mostrarFiltroServicio"
        class="flex flex-col gap-1 text-xs font-semibold text-cafe/60"
      >
        Servicio
        <select
          v-model="idServicio"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="aplicarCambios"
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

      <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60 sm:flex-1">
        Buscar
        <input
          v-model="busqueda"
          type="search"
          placeholder="Cliente o servicio"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa placeholder:text-cafe/35 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @input="alBuscar"
        />
      </label>

      <label class="flex flex-col gap-1 text-xs font-semibold text-cafe/60">
        Orden
        <select
          v-model="ordenActivo"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="aplicarCambios"
        >
          <option v-for="orden in ORDENES_PEDIDO" :key="orden.clave" :value="orden.clave">
            {{ orden.texto }}
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
      <span class="sr-only">Cargando pedidos...</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"
      ></div>
    </div>

    <p v-else-if="error" class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center text-sm text-rojizo">
      {{ error }}
    </p>

    <div
      v-else-if="!pedidos.length"
      class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
    >
      <p class="text-lg font-semibold text-granate">{{ emptyState.titulo }}</p>
      <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/60">
        {{ emptyState.detalle }}
      </p>
      <button
        v-if="hayFiltros"
        type="button"
        class="mt-5 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
        @click="limpiarFiltros"
      >
        Limpiar filtros
      </button>
      <router-link
        v-else-if="bandejaActiva === 'enviados'"
        to="/servicios"
        class="mt-5 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
      >
        Explorar servicios
      </router-link>
    </div>

    <div v-else class="mt-6 space-y-3">
      <FilaPedidoActivo
        v-for="pedido in pedidos"
        :key="pedido.idPedido"
        :pedido="pedido"
      />

      <div v-if="hayMas" class="pt-2 text-center">
        <button
          type="button"
          :disabled="cargandoMas"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
      </div>
    </div>
  </div>
</template>
