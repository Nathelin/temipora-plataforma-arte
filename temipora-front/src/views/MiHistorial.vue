<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ActividadTabs from '../components/ActividadTabs.vue'
import EmptyState from '../components/EmptyState.vue'
import FilaHistorial from '../components/FilaHistorial.vue'
import PageHeader from '../components/PageHeader.vue'
import { guardarArchivo } from '../servicios/descargas'
import { formatearPrecio } from '../servicios/moneda'
import {
  descargarHistorialPdf,
  obtenerHistorial,
} from '../servicios/miHistorial'
import { useNotificaciones } from '../composables/useNotificaciones'

const { exito, error: notificarError } = useNotificaciones()

const TIPOS = [
  { valor: '', texto: 'Todo' },
  { valor: 'PEDIDO', texto: 'Servicios' },
  { valor: 'COMPRA', texto: 'Productos' },
]

const ESTADOS = [
  { valor: 'FINALIZADO', texto: 'Completados' },
  { valor: 'ABIERTO', texto: 'En curso y pendientes' },
  { valor: 'CANCELADO', texto: 'Cancelados' },
  { valor: 'TODOS', texto: 'Todos' },
]

const PRESETS = [
  { clave: '30d', texto: '30 días' },
  { clave: '90d', texto: '3 meses' },
  { clave: 'anio', texto: 'Este año' },
  { clave: 'todo', texto: 'Todo' },
]

function dosDigitos(valor) {
  return String(valor).padStart(2, '0')
}

function iso(fecha) {
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`
}

function sumarDias(base, dias) {
  const fecha = new Date(base.getFullYear(), base.getMonth(), base.getDate())
  fecha.setDate(fecha.getDate() + dias)
  return fecha
}

const hoy = new Date()
const desde = ref('')
const hasta = ref('')
const preset = ref('todo')
const tipo = ref('')
const estado = ref('FINALIZADO')
const q = ref('')
const pagina = ref(0)
const historial = ref(null)
const abiertos = ref(null)
const cancelados = ref(null)
const cargando = ref(true)
const error = ref('')
const descargando = ref(false)
let debounceTimer
let requestId = 0

const resumen = computed(() => historial.value?.resumen || {})
const contenido = computed(() => historial.value?.contenido || [])
const totalElementos = computed(() => Number(historial.value?.totalElementos || 0))
const totalPaginas = computed(() => Number(historial.value?.totalPaginas || 0))
const moneda = computed(() => resumen.value.moneda || 'ARS')
const totalOtros = computed(() => Number(abiertos.value?.totalElementos || 0))
const totalCanceladas = computed(
  () => Number(cancelados.value?.totalElementos || 0),
)
const rangoInvalido = computed(
  () => Boolean(desde.value && hasta.value && desde.value > hasta.value),
)

const tarjetas = computed(() => {
  const r = resumen.value
  return [
    {
      etiqueta: 'Operaciones',
      valor: Number(r.operaciones || 0),
      detalle: `${r.pedidos || 0} servicios · ${r.compras || 0} productos`,
      destacada: true,
    },
    {
      etiqueta: 'Servicios',
      valor: Number(r.pedidos || 0),
      detalle: 'Pedidos a artistas',
    },
    {
      etiqueta: 'Productos',
      valor: Number(r.compras || 0),
      detalle: 'Compras de la tienda',
    },
    {
      etiqueta: 'Descuentos aplicados',
      valor: formatearPrecio(r.descuentos || 0, moneda.value),
      detalle: 'Ahorro real en servicios',
    },
  ]
})

const etiquetaImporte = computed(() => {
  if (estado.value === 'FINALIZADO') return 'Importe completado'
  if (estado.value === 'ABIERTO') return 'Importe en curso y pendiente'
  if (estado.value === 'CANCELADO') return 'Importe cancelado'
  return 'Importe del período'
})

const tituloLista = computed(() => {
  if (estado.value === 'FINALIZADO') return 'Completados'
  if (estado.value === 'ABIERTO') return 'En curso y pendientes'
  if (estado.value === 'CANCELADO') return 'Cancelados'
  return 'Operaciones'
})

const emptyState = computed(() => {
  if (q.value.trim() || desde.value || hasta.value || tipo.value) {
    return {
      titulo: 'No encontramos operaciones con esos filtros',
      detalle: 'Probá con otro período, otro tipo o quitá la búsqueda.',
    }
  }
  if (estado.value === 'FINALIZADO') {
    return {
      titulo: 'Todavía no tenés operaciones completadas',
      detalle: 'Cuando termines un pedido o se acredite una compra, va a aparecer acá.',
    }
  }
  return {
    titulo: 'No hay operaciones para este filtro',
    detalle: 'Probá con otro estado o mirá todas tus operaciones.',
  }
})

const hayFiltros = computed(() => Boolean(
  q.value.trim() || desde.value || hasta.value || tipo.value
    || (estado.value !== 'FINALIZADO'),
))

function aplicarPreset(clave) {
  clearTimeout(debounceTimer)
  preset.value = clave

  if (clave === '30d') {
    desde.value = iso(sumarDias(hoy, -29))
    hasta.value = iso(hoy)
  } else if (clave === '90d') {
    desde.value = iso(sumarDias(hoy, -89))
    hasta.value = iso(hoy)
  } else if (clave === 'anio') {
    desde.value = iso(new Date(hoy.getFullYear(), 0, 1))
    hasta.value = iso(hoy)
  } else {
    desde.value = ''
    hasta.value = ''
  }
}

function marcarPersonalizado() {
  preset.value = 'personalizado'
}

function limpiarFiltros() {
  tipo.value = ''
  estado.value = 'FINALIZADO'
  q.value = ''
  desde.value = ''
  hasta.value = ''
  preset.value = 'todo'
}

function parametros(estadoParam = estado.value) {
  return {
    desde: desde.value || undefined,
    hasta: hasta.value || undefined,
    tipo: tipo.value || 'TODOS',
    estado: estadoParam,
    q: q.value.trim() || undefined,
  }
}

async function cargar(nuevaPagina = 0) {
  const actual = ++requestId
  if (rangoInvalido.value) {
    error.value = 'La fecha de inicio debe ser anterior a la de fin.'
    cargando.value = false
    return
  }
  cargando.value = true
  error.value = ''

  try {
    const data = await obtenerHistorial({
      ...parametros(),
      pagina: nuevaPagina,
      tamanio: 12,
    })
    if (actual !== requestId) return

    historial.value = data
    pagina.value = Number(data?.pagina || 0)

    if (nuevaPagina === 0) {
      abiertos.value = null
      cancelados.value = null
      if (estado.value === 'FINALIZADO') {
        try {
          const [pendientesRes, canceladasRes] = await Promise.all([
            obtenerHistorial({
              ...parametros('ABIERTO'),
              pagina: 0,
              tamanio: 1,
            }),
            obtenerHistorial({
              ...parametros('CANCELADO'),
              pagina: 0,
              tamanio: 1,
            }),
          ])
          if (actual !== requestId) return
          abiertos.value = pendientesRes
          cancelados.value = canceladasRes
        } catch {
          abiertos.value = null
          cancelados.value = null
        }
      }
    }
  } catch (e) {
    if (actual !== requestId) return
    error.value = e.response?.data?.message
      || 'No se pudo cargar tu historial.'
  } finally {
    if (actual === requestId) {
      cargando.value = false
    }
  }
}

async function descargar() {
  if (descargando.value || rangoInvalido.value) return
  descargando.value = true
  try {
    const { blob, nombre } = await descargarHistorialPdf(parametros())
    guardarArchivo(blob, nombre)
    exito('Descargamos tu historial en PDF.')
  } catch {
    notificarError('No se pudo descargar el historial.')
  } finally {
    descargando.value = false
  }
}

watch([desde, hasta, tipo, estado, q], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => cargar(0), 250)
})

onMounted(() => cargar(0))
onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <div>
    <PageHeader
      eyebrow="Tu actividad"
      titulo="Mi historial"
      descripcion="Todos tus pedidos de servicio y compras de producto en un solo lugar. Podés descargar una constancia en PDF."
      :nivel="2"
    >
      <template #acciones>
        <span
          v-if="!cargando && !error"
          class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral tabular-nums"
        >
          {{ totalElementos }} {{ totalElementos === 1 ? 'operación' : 'operaciones' }}
        </span>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo disabled:opacity-50"
          :disabled="descargando || cargando || rangoInvalido"
          @click="descargar"
        >
          {{ descargando ? 'Generando...' : 'Descargar PDF' }}
        </button>
      </template>
    </PageHeader>

    <ActividadTabs class="mt-6" />

    <section class="rounded-2xl border border-rosa/60 bg-blanco p-4 sm:p-5">
      <div class="flex flex-wrap items-center gap-2">
        <div
          class="flex gap-1 rounded-xl border border-rosa/60 bg-crema/60 p-1"
          role="group"
          aria-label="Filtrar por tipo de operación"
        >
          <button
            v-for="item in TIPOS"
            :key="item.valor"
            type="button"
            class="inline-flex min-h-8 items-center rounded-lg px-3 text-[13px] font-semibold transition"
            :class="tipo === item.valor
              ? 'bg-coral text-white shadow-sm'
              : 'text-cafe/60 hover:bg-rosa/20 hover:text-granate'"
            :aria-pressed="tipo === item.valor"
            @click="tipo = item.valor"
          >
            {{ item.texto }}
          </button>
        </div>

        <label class="ml-auto flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Estado
          <select
            v-model="estado"
            aria-label="Filtrar por estado"
            class="rounded-lg border-0 bg-blanco px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          >
            <option v-for="item in ESTADOS" :key="item.valor" :value="item.valor">
              {{ item.texto }}
            </option>
          </select>
        </label>
      </div>

      <div class="mt-4 flex flex-wrap gap-2" role="group" aria-label="Períodos rápidos">
        <button
          v-for="item in PRESETS"
          :key="item.clave"
          type="button"
          class="inline-flex min-h-9 items-center rounded-full border px-4 text-[13px] font-semibold transition-colors motion-reduce:transition-none"
          :class="preset === item.clave
            ? 'border-coral bg-coral text-white'
            : 'border-rosa bg-blanco text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
          :aria-pressed="preset === item.clave"
          @click="aplicarPreset(item.clave)"
        >
          {{ item.texto }}
        </button>
      </div>

      <div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-[auto_auto_minmax(0,1fr)_auto]">
        <label class="flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Desde
          <input
            v-model="desde"
            type="date"
            class="min-w-0 rounded-lg border-0 px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="marcarPersonalizado"
          />
        </label>
        <label class="flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Hasta
          <input
            v-model="hasta"
            type="date"
            class="min-w-0 rounded-lg border-0 px-3 py-2 text-sm font-normal text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="marcarPersonalizado"
          />
        </label>
        <input
          v-model="q"
          type="search"
          placeholder="Buscar por servicio, producto o artista..."
          class="min-w-0 rounded-lg border-0 px-4 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa placeholder:text-cafe/35 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        />
        <button
          v-if="hayFiltros"
          type="button"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>

      <p class="mt-3 text-right text-[11px] text-cafe/45">
        Los importes están en ARS.
      </p>
    </section>

    <div v-if="cargando" class="mt-6 space-y-4" role="status" aria-live="polite">
      <span class="sr-only">Cargando tu historial...</span>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 4" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
      <div class="space-y-3">
        <div v-for="n in 3" :key="n" class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco" aria-hidden="true"></div>
      </div>
    </div>

    <p
      v-else-if="error"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-6 text-center text-sm text-rojizo"
    >
      {{ error }}
    </p>

    <template v-else>
      <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <article
          v-for="tarjeta in tarjetas"
          :key="tarjeta.etiqueta"
          class="rounded-2xl border border-rosa/60 bg-blanco p-5"
        >
          <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/50">
            {{ tarjeta.etiqueta }}
          </p>
          <p
            class="mt-2 font-display text-2xl font-extrabold tabular-nums"
            :class="tarjeta.destacada ? 'text-coral' : 'text-granate'"
          >
            {{ tarjeta.valor }}
          </p>
          <p class="mt-1 text-xs leading-relaxed text-cafe/55">{{ tarjeta.detalle }}</p>
        </article>
      </div>

      <div class="mt-6 flex flex-wrap items-center gap-x-6 gap-y-2 rounded-2xl border border-rosa/50 bg-crema/60 px-5 py-4 text-sm text-cafe/70">
        <span class="font-semibold text-granate">{{ etiquetaImporte }}</span>
        <span class="font-display text-lg font-extrabold text-granate tabular-nums">
          {{ formatearPrecio(resumen.importeTotal || 0, moneda) }}
        </span>
        <template v-if="estado === 'FINALIZADO'">
          <span><strong class="text-granate">{{ resumen.completadas || 0 }}</strong> completadas</span>
          <span v-if="totalOtros">
            <strong class="text-granate">{{ totalOtros }}</strong> en curso o pendientes
          </span>
          <span v-if="totalCanceladas">
            <strong class="text-granate">{{ totalCanceladas }}</strong> canceladas
          </span>
        </template>
        <template v-else>
          <span><strong class="text-granate">{{ resumen.completadas || 0 }}</strong> completadas</span>
          <span><strong class="text-granate">{{ resumen.enCurso || 0 }}</strong> en curso</span>
          <span><strong class="text-granate">{{ resumen.pendientes || 0 }}</strong> pendientes</span>
          <span><strong class="text-granate">{{ resumen.canceladas || 0 }}</strong> canceladas</span>
        </template>
      </div>

      <section class="mt-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">{{ tituloLista }}</h2>
          <span class="text-xs text-cafe/50 tabular-nums">
            {{ totalElementos }} {{ totalElementos === 1 ? 'operación' : 'operaciones' }}
          </span>
        </div>

        <ul v-if="contenido.length" class="mt-4 space-y-3">
          <li v-for="item in contenido" :key="`${item.tipo}-${item.id}`">
            <FilaHistorial :item="item" />
          </li>
        </ul>

        <EmptyState
          v-else
          class="mt-4"
          :titulo="emptyState.titulo"
          :detalle="emptyState.detalle"
        >
          <template #accion>
            <button
              v-if="estado === 'FINALIZADO'"
              type="button"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
              @click="estado = 'TODOS'"
            >
              Ver todas las operaciones
            </button>
            <button
              v-else-if="hayFiltros"
              type="button"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
              @click="limpiarFiltros"
            >
              Limpiar filtros
            </button>
          </template>
        </EmptyState>

        <div
          v-if="totalPaginas > 1"
          class="mt-6 flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
        >
          <p class="text-xs text-cafe/50 tabular-nums">
            Página {{ pagina + 1 }} de {{ totalPaginas }}
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
              :disabled="pagina <= 0"
              @click="cargar(pagina - 1)"
            >
              Anterior
            </button>
            <button
              type="button"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
              :disabled="pagina >= totalPaginas - 1"
              @click="cargar(pagina + 1)"
            >
              Siguiente
            </button>
          </div>
        </div>
      </section>

      <div
        v-if="estado === 'FINALIZADO' && (totalOtros || totalCanceladas)"
        class="mt-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-rosa/60 bg-blanco px-5 py-4"
      >
        <div>
          <h2 class="font-display text-base font-bold text-granate">
            Otros movimientos del período
          </h2>
          <!--p class="mt-1 text-sm text-cafe/60">
            No forman parte de los completados.
            <span v-if="totalOtros">{{ totalOtros }} en curso o pendientes</span>
            <span v-if="totalOtros && totalCanceladas"> · </span>
            <span v-if="totalCanceladas">{{ totalCanceladas }} canceladas</span>
          </p-->
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            v-if="totalOtros"
            type="button"
            class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-granate transition hover:bg-rosa/20"
            @click="estado = 'ABIERTO'"
          >
            Ver abiertas ({{ totalOtros }})
          </button>
          <button
            v-if="totalCanceladas"
            type="button"
            class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-granate transition hover:bg-rosa/20"
            @click="estado = 'CANCELADO'"
          >
            Ver canceladas ({{ totalCanceladas }})
          </button>
        </div>
      </div>

      <p class="mt-8 text-xs leading-relaxed text-cafe/50">
        Cada fila es un pedido de servicio o una compra de producto. Los importes
        están en ARS y el detalle de cada operación queda en su pantalla original.
      </p>
    </template>
  </div>
</template>
