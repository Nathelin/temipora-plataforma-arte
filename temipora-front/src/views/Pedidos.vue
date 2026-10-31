<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  ESTADOS_PEDIDO,
  estadoPedidoDe,
  etiquetaTipoHito,
  formatearFecha,
} from '../servicios/pedidos'

const route = useRoute()
const router = useRouter()
const esArtista = localStorage.getItem('rol') === 'ARTISTA'

const bandejaActiva = ref(esArtista ? 'recibidos' : 'enviados')
const estadoFiltro = ref(route.query.estado || '')
const pedidos = ref([])
const pagina = ref(0)
const hayMas = ref(false)
const total = ref(0)
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')

const enPanel = computed(() => route.path.startsWith('/panel'))
const bandejas = computed(() => (esArtista
  ? [
      { clave: 'recibidos', texto: 'Recibidos' },
      { clave: 'enviados', texto: 'Enviados' },
    ]
  : [{ clave: 'enviados', texto: 'Enviados' }]))

function estadoInfo(pedido) {
  return estadoPedidoDe(pedido.estado)
}

function contraparteTexto(pedido) {
  const persona = pedido.contraparte
  if (!persona) return '—'
  const rol = pedido.rolVista === 'ARTISTA' ? 'Cliente' : 'Artista'
  return `${rol}: @${persona.nombreUsuario || persona.nombreDisplay || '—'}`
}

function montoProximo(pedido) {
  if (pedido.proximoHitoMontoMostrado == null) return null
  return formatearPrecio(
    pedido.proximoHitoMontoMostrado,
    pedido.monedaMostrada || pedido.monedaCobro,
  )
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
    const params = { pagina: pagina.value, tamanio: 12 }
    if (estadoFiltro.value) params.estado = estadoFiltro.value
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

function cargarMas() {
  pagina.value += 1
  cargar(false)
}

function cambiarBandeja(clave) {
  if (bandejaActiva.value === clave) return
  bandejaActiva.value = clave
  cargar(true)
}

function cambiarEstado() {
  cargar(true)
}

watch(estadoFiltro, (valor) => {
  const query = { ...route.query }
  if (valor) {
    query.estado = valor
  } else {
    delete query.estado
  }
  router.replace({ query })
})

watch(() => route.query.estado, (valor) => {
  const nuevo = valor || ''
  if (nuevo !== estadoFiltro.value) {
    estadoFiltro.value = nuevo
    cargar(true)
  }
})

function verPedido(idPedido) {
  router.push({
    name: enPanel.value ? 'panel-pedido-detalle' : 'pedido-detalle',
    params: { idPedido },
  })
}

function abrirChat(pedido) {
  if (!pedido?.idConversacion) return
  router.push({
    name: enPanel.value ? 'panel-conversacion' : 'conversacion',
    params: { idConversacion: pedido.idConversacion },
    query: { volverA: 'pedido', idPedido: pedido.idPedido },
  })
}

onMounted(() => cargar(true))
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

    <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div v-if="esArtista" class="inline-flex rounded-full border border-rosa bg-blanco p-1">
        <button
          v-for="bandeja in bandejas"
          :key="bandeja.clave"
          type="button"
          class="rounded-full px-4 py-1.5 text-sm font-semibold transition"
          :class="bandejaActiva === bandeja.clave
            ? 'bg-coral text-white'
            : 'text-cafe/65 hover:text-granate'"
          @click="cambiarBandeja(bandeja.clave)"
        >
          {{ bandeja.texto }}
        </button>
      </div>
      <span v-else></span>

      <label class="flex items-center gap-2 text-xs text-cafe/60">
        Estado
        <select
          v-model="estadoFiltro"
          class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          @change="cambiarEstado"
        >
          <option value="">Todos</option>
          <option
            v-for="(info, clave) in ESTADOS_PEDIDO"
            :key="clave"
            :value="clave"
          >
            {{ info.etiqueta }}
          </option>
        </select>
      </label>
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
      class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center"
    >
      <p class="text-lg font-semibold text-granate">Todavía no hay pedidos</p>
      <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/60">
        {{ bandejaActiva === 'recibidos'
          ? 'Cuando un cliente acepte y pague un presupuesto, el pedido va a aparecer acá.'
          : 'Cuando aceptes y pagues un presupuesto, el pedido va a aparecer acá.' }}
      </p>
      <router-link
        v-if="bandejaActiva === 'enviados'"
        to="/solicitudes"
        class="mt-5 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
      >
        Ver mis solicitudes
      </router-link>
    </div>

    <div v-else class="mt-6 space-y-3">
      <article
        v-for="pedido in pedidos"
        :key="pedido.idPedido"
        class="rounded-2xl border border-rosa/50 bg-blanco p-5 transition hover:border-coral/40"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
              Pedido #{{ pedido.idPedido }}
            </p>
            <h2 class="mt-1 truncate text-lg font-semibold text-granate">
              {{ pedido.nombreServicio || 'Servicio' }}
            </h2>
            <p class="mt-1 text-sm text-cafe/60">{{ contraparteTexto(pedido) }}</p>
          </div>
          <span
            class="rounded-full border px-3 py-1 text-xs font-semibold"
            :class="estadoInfo(pedido).clase"
          >
            {{ estadoInfo(pedido).etiqueta }}
          </span>
        </div>

        <dl class="mt-4 grid gap-2 text-sm sm:grid-cols-3">
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Próximo hito</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ pedido.proximoHitoNombre || 'Sin pendientes' }}
            </dd>
            <dd v-if="pedido.proximoHitoTipo" class="text-xs text-cafe/45">
              {{ etiquetaTipoHito(pedido.proximoHitoTipo) }}
            </dd>
          </div>
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Monto</dt>
            <dd class="mt-0.5 font-semibold text-granate">
              {{ montoProximo(pedido) || '—' }}
            </dd>
          </div>
          <div class="rounded-xl bg-rosa/10 px-3 py-2">
            <dt class="text-xs text-cafe/50">Límite de entrega</dt>
            <dd class="mt-0.5 font-medium text-cafe">
              {{ pedido.limiteEntrega ? formatearFecha(pedido.limiteEntrega) : 'A confirmar' }}
            </dd>
          </div>
        </dl>

        <div class="mt-4 flex flex-wrap items-center gap-2">
          <button
            type="button"
            class="inline-flex min-h-9 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo"
            @click="verPedido(pedido.idPedido)"
          >
            Ver pedido
          </button>
          <button
            v-if="pedido.idConversacion"
            type="button"
            class="inline-flex min-h-9 items-center justify-center gap-1.5 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
            @click="abrirChat(pedido)"
          >
            Chat
            <span
              v-if="pedido.noLeidos > 0"
              class="inline-flex min-w-5 items-center justify-center rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold text-white"
            >
              {{ pedido.noLeidos > 9 ? '9+' : pedido.noLeidos }}
            </span>
          </button>
          <span
            v-if="pedido.puedePagar"
            class="rounded-full bg-durazno/40 px-3 py-1 text-xs font-semibold text-cafe"
          >
            Tiene un pago pendiente
          </span>
          <span
            v-else-if="pedido.puedeEntregar"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
          >
            Podés entregar
          </span>
          <span
            v-else-if="pedido.puedeAprobar"
            class="rounded-full bg-green-700/10 px-3 py-1 text-xs font-semibold text-green-700"
          >
            Entregable para aprobar
          </span>
        </div>
      </article>

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
