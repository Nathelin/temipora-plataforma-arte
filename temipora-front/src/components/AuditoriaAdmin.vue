<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Activity,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Info,
  LogIn,
  RefreshCw,
  Search,
  ShieldAlert,
  X,
} from '@lucide/vue'
import api from '../servicios/api'
import { useAdminMenu } from '../composables/useAdminMenu'
import AuditoriaDetalle from './admin/AuditoriaDetalle.vue'
import {
  actorVisible,
  formatearFecha,
  formatearFechaRelativa,
  nombreAccion,
  nombreCategoria,
  nombreEntidad,
  nombreOrigen,
  resumenCambios,
} from '../servicios/auditoriaCatalogo'

const route = useRoute()
const router = useRouter()
const { colapsada: menuColapsada } = useAdminMenu()

const RANGOS = [
  { id: '1h', etiqueta: '1 h' },
  { id: 'hoy', etiqueta: 'Hoy' },
  { id: '7d', etiqueta: '7 días' },
  { id: '30d', etiqueta: '30 días' },
  { id: 'custom', etiqueta: 'Personalizado' },
  { id: 'todo', etiqueta: 'Todo' },
]

const ORIGENES = [
  { codigo: 'ADMIN', etiqueta: 'Administración' },
  { codigo: 'ARTISTA', etiqueta: 'Artistas' },
  { codigo: 'CLIENTE', etiqueta: 'Clientes' },
  { codigo: 'SISTEMA', etiqueta: 'Sistema' },
]

const claseCampo =
  'block w-full rounded-lg border-0 bg-blanco px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral'
const claseEtiqueta =
  'mb-1 block text-[11px] font-semibold uppercase tracking-wide text-cafe/55'
const claseBotonRango =
  'inline-flex min-h-9 items-center justify-center rounded-full px-3 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none'

const eventos = ref([])
const opciones = ref({ entidades: [], acciones: [], categorias: [] })
const resumen = ref({ total: 0, criticas: 0, loginFallido: 0 })
const cargando = ref(true)
const cargandoResumen = ref(true)
const error = ref('')
const pagina = ref(0)
const totalPaginas = ref(0)
const totalElementos = ref(0)
const hayMas = ref(false)
const seleccionado = ref(null)

const filtroActor = ref('')
const filtroOrigen = ref('')
const filtroEntidad = ref('')
const filtroEntidadId = ref('')
const filtroAccion = ref('')
const filtroTexto = ref('')
const filtroCategoria = ref('')
const filtroCritica = ref(false)
const filtroRequestId = ref('')
const rango = ref('7d')
const desde = ref('')
const hasta = ref('')
const desdeCustom = ref('')
const hastaCustom = ref('')

let debounceTimer
let cargaProgramada
let inicializado = false

function isoLocal(fecha) {
  const dos = (valor) => String(valor).padStart(2, '0')
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`
    + `T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}:${dos(fecha.getSeconds())}`
}

function aplicarRango() {
  if (rango.value === 'todo') {
    desde.value = ''
    hasta.value = ''
    return
  }
  if (rango.value === 'custom') {
    desde.value = desdeCustom.value ? `${desdeCustom.value}:00` : ''
    hasta.value = hastaCustom.value ? `${hastaCustom.value}:59` : ''
    return
  }
  const ahora = new Date()
  let inicio
  if (rango.value === '1h') {
    inicio = new Date(ahora.getTime() - 60 * 60 * 1000)
  } else if (rango.value === 'hoy') {
    inicio = new Date(ahora)
    inicio.setHours(0, 0, 0, 0)
  } else if (rango.value === '7d') {
    inicio = new Date(ahora.getTime() - 7 * 24 * 60 * 60 * 1000)
  } else {
    inicio = new Date(ahora.getTime() - 30 * 24 * 60 * 60 * 1000)
  }
  desde.value = isoLocal(inicio)
  hasta.value = isoLocal(ahora)
}

function params(nuevaPagina = 0) {
  return {
    actor: filtroActor.value.trim() || undefined,
    origen: filtroOrigen.value || undefined,
    entidad: filtroEntidad.value || undefined,
    entidadId: filtroEntidadId.value.trim() || undefined,
    accion: filtroAccion.value || undefined,
    requestId: filtroRequestId.value || undefined,
    texto: filtroTexto.value.trim() || undefined,
    categoria: filtroCategoria.value || undefined,
    critica: filtroCritica.value ? true : undefined,
    desde: desde.value || undefined,
    hasta: hasta.value || undefined,
    pagina: nuevaPagina,
    tamanio: 25,
  }
}

async function cargar(nuevaPagina = 0) {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/auditoria', { params: params(nuevaPagina) })
    const data = res.data ?? {}
    eventos.value = data.contenido ?? []
    pagina.value = data.pagina ?? 0
    totalPaginas.value = data.totalPaginas ?? 0
    totalElementos.value = data.totalElementos ?? 0
    hayMas.value = Boolean(data.hayMas)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar la auditoría.'
  } finally {
    cargando.value = false
  }
}

async function cargarResumen() {
  cargandoResumen.value = true
  try {
    const fin = new Date()
    const inicio = new Date(fin.getTime() - 24 * 60 * 60 * 1000)
    const res = await api.get('/admin/auditoria/resumen', {
      params: { desde: isoLocal(inicio), hasta: isoLocal(fin) },
    })
    resumen.value = {
      total: res.data?.total ?? 0,
      criticas: res.data?.criticas ?? 0,
      loginFallido: res.data?.loginFallido ?? 0,
    }
  } catch {
    resumen.value = { total: 0, criticas: 0, loginFallido: 0 }
  } finally {
    cargandoResumen.value = false
  }
}

async function cargarOpciones() {
  try {
    const res = await api.get('/admin/auditoria/opciones')
    opciones.value = {
      entidades: res.data?.entidades ?? [],
      acciones: res.data?.acciones ?? [],
      categorias: res.data?.categorias ?? [],
    }
  } catch {
    opciones.value = { entidades: [], acciones: [], categorias: [] }
  }
}

function programarCarga() {
  if (cargaProgramada) return
  cargaProgramada = setTimeout(() => {
    cargaProgramada = null
    cargar(0)
  }, 0)
}

function programarCargaConDebounce() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    cargar(0)
  }, 300)
}

watch(rango, () => {
  if (!inicializado) return
  aplicarRango()
  programarCarga()
})

watch([desdeCustom, hastaCustom], () => {
  if (!inicializado) return
  if (rango.value === 'custom') {
    aplicarRango()
    programarCarga()
  }
})

watch(
  [
    filtroOrigen,
    filtroEntidad,
    filtroAccion,
    filtroCategoria,
    filtroCritica,
    filtroRequestId,
  ],
  () => {
    if (inicializado) programarCarga()
  },
)

watch([filtroActor, filtroEntidadId, filtroTexto], () => {
  if (inicializado) programarCargaConDebounce()
})

function esSeleccionado(evento) {
  return seleccionado.value?.idEventoAuditoria === evento.idEventoAuditoria
}

function seleccionar(evento) {
  seleccionado.value = evento
  router.replace({
    query: { ...route.query, evento: String(evento.idEventoAuditoria) },
  })
}

function cerrarDetalle() {
  seleccionado.value = null
  const query = { ...route.query }
  delete query.evento
  router.replace({ query })
}

async function abrirPorId(id) {
  const enLista = eventos.value.find(
    (evento) => String(evento.idEventoAuditoria) === String(id),
  )
  if (enLista) {
    seleccionado.value = enLista
    return
  }
  try {
    const res = await api.get(`/admin/auditoria/${id}`)
    seleccionado.value = res.data
  } catch {
    seleccionado.value = null
  }
}

function moverSeleccion(indice) {
  if (indice < 0 || indice >= eventos.value.length) return
  const evento = eventos.value[indice]
  seleccionar(evento)
  const fila = document.getElementById(
    `fila-auditoria-${evento.idEventoAuditoria}`,
  )
  fila?.focus()
}

const etiquetaRango = computed(
  () => RANGOS.find((item) => item.id === rango.value)?.etiqueta ?? 'Todo',
)

const grillaContenido = computed(() =>
  menuColapsada.value
    ? 'xl:grid-cols-[minmax(0,1fr)_460px] 2xl:grid-cols-[minmax(0,1fr)_540px]'
    : 'xl:grid-cols-[minmax(0,1fr)_400px] 2xl:grid-cols-[minmax(0,1fr)_460px]',
)

const chips = computed(() => {
  const activos = []
  if (rango.value !== 'todo') {
    activos.push({ clave: 'rango', etiqueta: etiquetaRango.value })
  }
  if (filtroActor.value) {
    activos.push({ clave: 'actor', etiqueta: `Actor: ${filtroActor.value}` })
  }
  if (filtroOrigen.value) {
    activos.push({
      clave: 'origen',
      etiqueta: `Origen: ${nombreOrigen(filtroOrigen.value)}`,
    })
  }
  if (filtroEntidad.value) {
    activos.push({
      clave: 'entidad',
      etiqueta: `Entidad: ${nombreEntidad(filtroEntidad.value)}`,
    })
  }
  if (filtroEntidadId.value) {
    activos.push({
      clave: 'entidadId',
      etiqueta: `ID de entidad: ${filtroEntidadId.value}`,
    })
  }
  if (filtroAccion.value) {
    activos.push({
      clave: 'accion',
      etiqueta: `Acción: ${nombreAccion(filtroAccion.value)}`,
    })
  }
  if (filtroCategoria.value) {
    activos.push({
      clave: 'categoria',
      etiqueta: `Categoría: ${nombreCategoria(filtroCategoria.value)}`,
    })
  }
  if (filtroCritica.value) {
    activos.push({ clave: 'critica', etiqueta: 'Solo críticas' })
  }
  if (filtroRequestId.value) {
    activos.push({ clave: 'requestId', etiqueta: 'Operación relacionada' })
  }
  if (filtroTexto.value) {
    activos.push({ clave: 'texto', etiqueta: `Buscar: ${filtroTexto.value}` })
  }
  return activos
})

function quitarChip(clave) {
  if (clave === 'rango') rango.value = 'todo'
  if (clave === 'actor') filtroActor.value = ''
  if (clave === 'origen') filtroOrigen.value = ''
  if (clave === 'entidad') filtroEntidad.value = ''
  if (clave === 'entidadId') filtroEntidadId.value = ''
  if (clave === 'accion') filtroAccion.value = ''
  if (clave === 'categoria') filtroCategoria.value = ''
  if (clave === 'critica') filtroCritica.value = false
  if (clave === 'requestId') filtroRequestId.value = ''
  if (clave === 'texto') filtroTexto.value = ''
}

function limpiar() {
  filtroActor.value = ''
  filtroOrigen.value = ''
  filtroEntidad.value = ''
  filtroEntidadId.value = ''
  filtroAccion.value = ''
  filtroTexto.value = ''
  filtroCategoria.value = ''
  filtroCritica.value = false
  filtroRequestId.value = ''
  rango.value = '7d'
  desdeCustom.value = ''
  hastaCustom.value = ''
  aplicarRango()
  cerrarDetalle()
  programarCarga()
}

function aplicarFiltro(payload) {
  if (payload.tipo === 'actor') {
    filtroActor.value = payload.valor || ''
  } else if (payload.tipo === 'entidad') {
    filtroEntidad.value = payload.entidad || ''
    filtroEntidadId.value = payload.entidadId || ''
  } else if (payload.tipo === 'requestId') {
    filtroRequestId.value = payload.valor || ''
  }
  rango.value = 'todo'
  aplicarRango()
}

function recargar() {
  cargar(pagina.value)
  cargarResumen()
}

watch(
  () => route.query.evento,
  (valor) => {
    if (!valor) {
      seleccionado.value = null
      return
    }
    if (String(seleccionado.value?.idEventoAuditoria) === String(valor)) {
      return
    }
    abrirPorId(valor)
  },
)

function aInputFechaHora(valor) {
  return valor ? String(valor).slice(0, 16) : ''
}

function inicializarDesdeUrl() {
  const q = route.query
  if (q.actor) filtroActor.value = String(q.actor)
  if (q.origen) filtroOrigen.value = String(q.origen)
  if (q.entidad) filtroEntidad.value = String(q.entidad)
  if (q.entidadId) filtroEntidadId.value = String(q.entidadId)
  if (q.accion) filtroAccion.value = String(q.accion)
  if (q.requestId) filtroRequestId.value = String(q.requestId)
  if (q.texto) filtroTexto.value = String(q.texto)
  if (q.categoria) filtroCategoria.value = String(q.categoria)
  if (String(q.critica) === 'true') filtroCritica.value = true
  if (q.desde || q.hasta) {
    rango.value = 'custom'
    desdeCustom.value = aInputFechaHora(q.desde)
    hastaCustom.value = aInputFechaHora(q.hasta)
  } else if (q.rango) {
    rango.value = String(q.rango)
  }
  aplicarRango()
}

onMounted(async () => {
  inicializarDesdeUrl()
  await Promise.all([cargar(0), cargarOpciones(), cargarResumen()])
  if (route.query.evento) {
    abrirPorId(route.query.evento)
  }
  inicializado = true
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  clearTimeout(cargaProgramada)
})
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Sistema"
      titulo="Auditoría"
      descripcion="Quién hizo qué, sobre qué registro y con qué valores anteriores y nuevos."
    >
      <template #acciones>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
          :disabled="cargando"
          @click="recargar"
        >
          <RefreshCw class="h-4 w-4" :class="cargando ? 'animate-spin' : ''" aria-hidden="true" />
          Actualizar
        </button>
      </template>
    </AdminPageHeader>

    <div class="mb-5 grid gap-3 sm:grid-cols-3">
      <div class="rounded-2xl border border-rosa/50 bg-blanco px-4 py-3">
        <div class="flex items-center justify-between gap-2">
          <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Eventos (24 h)</span>
          <Activity class="h-4 w-4 text-cafe/30" aria-hidden="true" />
        </div>
        <p class="mt-1 font-display text-2xl font-extrabold tabular-nums text-granate">
          {{ cargandoResumen ? '—' : resumen.total }}
        </p>
      </div>
      <div class="rounded-2xl border border-rosa/50 bg-blanco px-4 py-3">
        <div class="flex items-center justify-between gap-2">
          <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Acciones críticas (24 h)</span>
          <ShieldAlert class="h-4 w-4 text-coral/60" aria-hidden="true" />
        </div>
        <p class="mt-1 font-display text-2xl font-extrabold tabular-nums text-coral">
          {{ cargandoResumen ? '—' : resumen.criticas }}
        </p>
      </div>
      <div class="rounded-2xl border border-rosa/50 bg-blanco px-4 py-3">
        <div class="flex items-center justify-between gap-2">
          <span class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Logins fallidos (24 h)</span>
          <LogIn class="h-4 w-4 text-cafe/30" aria-hidden="true" />
        </div>
        <p class="mt-1 font-display text-2xl font-extrabold tabular-nums text-cafe">
          {{ cargandoResumen ? '—' : resumen.loginFallido }}
        </p>
      </div>
    </div>

    <section class="rounded-2xl border border-rosa/50 bg-blanco p-4">
      <div class="flex flex-wrap items-end gap-3">
        <div class="w-full">
          <span :class="claseEtiqueta">Rango</span>
          <div class="flex flex-wrap gap-1.5" role="group" aria-label="Rango de tiempo">
            <button
              v-for="item in RANGOS"
              :key="item.id"
              type="button"
              :class="[
                claseBotonRango,
                rango === item.id
                  ? 'bg-granate text-blanco'
                  : 'border border-rosa bg-blanco text-cafe hover:bg-rosa/20',
              ]"
              :aria-pressed="rango === item.id"
              @click="rango = item.id"
            >
              {{ item.etiqueta }}
            </button>
          </div>
        </div>

        <div v-if="rango === 'custom'" class="grid w-full gap-3 sm:grid-cols-2">
          <label class="block">
            <span :class="claseEtiqueta">Desde</span>
            <input v-model="desdeCustom" type="datetime-local" :class="claseCampo" />
          </label>
          <label class="block">
            <span :class="claseEtiqueta">Hasta</span>
            <input v-model="hastaCustom" type="datetime-local" :class="claseCampo" />
          </label>
        </div>
      </div>

      <div class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <label class="block">
          <span :class="claseEtiqueta">Actor</span>
          <input v-model="filtroActor" type="search" placeholder="Usuario" :class="claseCampo" />
        </label>
        <label class="block">
          <span :class="claseEtiqueta">Origen</span>
          <select v-model="filtroOrigen" :class="claseCampo">
            <option value="">Todos</option>
            <option v-for="origen in ORIGENES" :key="origen.codigo" :value="origen.codigo">
              {{ origen.etiqueta }}
            </option>
          </select>
        </label>
        <label class="block">
          <span :class="claseEtiqueta">Entidad</span>
          <select v-model="filtroEntidad" :class="claseCampo">
            <option value="">Todas</option>
            <option v-for="entidad in opciones.entidades" :key="entidad" :value="entidad">
              {{ nombreEntidad(entidad) }}
            </option>
          </select>
        </label>
        <label class="block">
          <span :class="claseEtiqueta">ID de entidad</span>
          <input v-model="filtroEntidadId" type="search" placeholder="Ej. 318" :class="claseCampo" />
        </label>
        <label class="block">
          <span :class="claseEtiqueta">Acción</span>
          <select v-model="filtroAccion" :class="claseCampo">
            <option value="">Todas</option>
            <option v-for="accion in opciones.acciones" :key="accion" :value="accion">
              {{ nombreAccion(accion) }}
            </option>
          </select>
        </label>
        <label class="block">
          <span :class="claseEtiqueta">Categoría</span>
          <select v-model="filtroCategoria" :class="claseCampo">
            <option value="">Todas</option>
            <option v-for="categoria in opciones.categorias" :key="categoria.codigo" :value="categoria.codigo">
              {{ categoria.etiqueta }}
            </option>
          </select>
        </label>
        <label class="block">
          <span :class="claseEtiqueta">Buscar</span>
          <span class="relative block">
            <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-cafe/35" aria-hidden="true" />
            <input v-model="filtroTexto" type="search" placeholder="Resumen, actor o ID" :class="[claseCampo, 'pl-9']" />
          </span>
        </label>
        <div class="flex items-end gap-2">
          <button
            type="button"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full px-3 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
            :class="filtroCritica ? 'bg-granate text-blanco' : 'border border-rosa bg-blanco text-cafe hover:bg-rosa/20'"
            :aria-pressed="filtroCritica"
            @click="filtroCritica = !filtroCritica"
          >
            <ShieldAlert class="h-4 w-4" aria-hidden="true" />
            Solo críticas
          </button>
          <span class="group relative inline-flex">
            <button
              type="button"
              class="inline-flex h-9 w-9 items-center justify-center rounded-full text-cafe/45 transition-colors hover:bg-rosa/20 hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
              aria-label="Qué se considera un evento crítico"
              aria-describedby="ayuda-critica"
            >
              <Info class="h-4 w-4" aria-hidden="true" />
            </button>
            <span
              id="ayuda-critica"
              role="tooltip"
              class="pointer-events-none absolute bottom-full right-0 z-20 mb-2 hidden w-72 rounded-lg bg-cafe px-3 py-2 text-xs leading-relaxed text-blanco shadow-lg group-hover:block group-focus-within:block"
            >
              Se consideran críticas las acciones sobre pagos, pedidos, presupuestos, entregas, términos y cambios de seguridad de la cuenta.
            </span>
          </span>
        </div>
      </div>

      <div v-if="chips.length" class="mt-3 flex flex-wrap items-center gap-2">
        <span
          v-for="chip in chips"
          :key="chip.clave"
          class="inline-flex max-w-full items-center gap-1 rounded-full bg-rosa/20 py-1 pl-2.5 pr-1 text-xs font-medium text-granate"
        >
          <span class="truncate">{{ chip.etiqueta }}</span>
          <button
            type="button"
            class="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-granate/60 transition-colors hover:bg-granate/10 hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
            :aria-label="`Quitar filtro ${chip.etiqueta}`"
            @click="quitarChip(chip.clave)"
          >
            <X class="h-3 w-3" aria-hidden="true" />
          </button>
        </span>
        <button
          type="button"
          class="ml-1 rounded-full px-2 py-1 text-xs font-semibold text-coral transition-colors hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
          @click="limpiar"
        >
          Limpiar filtros
        </button>
      </div>
    </section>

    <p v-if="cargando" class="py-10 text-center text-sm text-cafe/55">Cargando auditoría...</p>
    <p v-else-if="error" class="py-10 text-center text-sm text-rojizo" role="alert">{{ error }}</p>
    <p
      v-else-if="!eventos.length"
      class="mt-5 rounded-2xl border border-dashed border-rosa/60 py-10 text-center text-sm text-cafe/55"
    >
      No hay acciones registradas con estos filtros.
    </p>

    <div v-else class="mt-5 grid items-start gap-4" :class="grillaContenido">
      <div class="min-w-0">
        <div class="hidden overflow-hidden rounded-2xl border border-rosa/50 bg-blanco md:block">
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-rosa/40 text-[10px] uppercase tracking-wide text-cafe/50">
                  <th class="px-3 py-2.5 font-semibold">Fecha</th>
                  <th class="px-3 py-2.5 font-semibold">Actor</th>
                  <th class="px-3 py-2.5 font-semibold">Acción</th>
                  <th class="px-3 py-2.5 font-semibold">Entidad</th>
                  <th class="px-3 py-2.5 font-semibold">Categoría</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(evento, indice) in eventos"
                  :id="`fila-auditoria-${evento.idEventoAuditoria}`"
                  :key="evento.idEventoAuditoria"
                  tabindex="0"
                  :aria-current="esSeleccionado(evento) ? 'true' : undefined"
                  class="cursor-pointer border-b border-rosa/25 transition-colors last:border-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60 motion-reduce:transition-none"
                  :class="esSeleccionado(evento) ? 'bg-coral/5' : 'hover:bg-rosa/5'"
                  @click="seleccionar(evento)"
                  @keydown.enter.prevent="seleccionar(evento)"
                  @keydown.space.prevent="seleccionar(evento)"
                  @keydown.down.prevent="moverSeleccion(indice + 1)"
                  @keydown.up.prevent="moverSeleccion(indice - 1)"
                  @keydown.esc="cerrarDetalle"
                >
                  <td class="whitespace-nowrap px-3 py-3 align-top text-xs text-cafe/55">
                    <span :title="formatearFecha(evento.ocurridoEn)">
                      {{ formatearFechaRelativa(evento.ocurridoEn) }}
                    </span>
                  </td>
                  <td class="px-3 py-3 align-top">
                    <p class="text-xs font-medium text-cafe">{{ actorVisible(evento) }}</p>
                    <span
                      v-if="evento.actorRol"
                      class="mt-1 inline-flex rounded-full bg-rosa/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-granate"
                    >
                      {{ evento.actorRol }}
                    </span>
                  </td>
                  <td class="px-3 py-3 align-top">
                    <div class="flex items-start gap-1.5">
                      <AlertTriangle
                        v-if="evento.critica"
                        class="mt-0.5 h-3.5 w-3.5 shrink-0 text-coral"
                        aria-hidden="true"
                      />
                      <span class="text-xs font-semibold text-granate">{{ nombreAccion(evento.accion) }}</span>
                    </div>
                    <p class="mt-0.5 text-[11px] text-cafe/50">{{ resumenCambios(evento) }}</p>
                  </td>
                  <td class="px-3 py-3 align-top text-xs text-cafe/70">
                    {{ nombreEntidad(evento.entidad) }}
                    <span v-if="evento.entidadId" class="text-cafe/45">#{{ evento.entidadId }}</span>
                  </td>
                  <td class="px-3 py-3 align-top">
                    <span class="inline-flex rounded-full bg-rosa/15 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-cafe/70">
                      {{ nombreCategoria(evento.categoria) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="space-y-2 md:hidden">
          <button
            v-for="evento in eventos"
            :key="evento.idEventoAuditoria"
            type="button"
            class="w-full rounded-2xl border border-rosa/50 bg-blanco p-3 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
            :class="esSeleccionado(evento) ? 'bg-coral/5' : 'hover:bg-rosa/5'"
            :aria-current="esSeleccionado(evento) ? 'true' : undefined"
            @click="seleccionar(evento)"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="text-xs text-cafe/50">{{ formatearFechaRelativa(evento.ocurridoEn) }}</span>
              <span class="rounded-full bg-rosa/15 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-cafe/70">
                {{ nombreCategoria(evento.categoria) }}
              </span>
            </div>
            <p class="mt-1 flex items-start gap-1.5 text-sm font-semibold text-granate">
              <AlertTriangle v-if="evento.critica" class="mt-0.5 h-3.5 w-3.5 shrink-0 text-coral" aria-hidden="true" />
              {{ nombreAccion(evento.accion) }}
            </p>
            <p class="mt-1 text-xs text-cafe/60">{{ evento.resumen }}</p>
            <p class="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-cafe/55">
              <span class="font-medium text-cafe/75">{{ actorVisible(evento) }}</span>
              <span v-if="evento.entidad">· {{ nombreEntidad(evento.entidad) }}<span v-if="evento.entidadId"> #{{ evento.entidadId }}</span></span>
            </p>
          </button>
        </div>

        <div
          v-if="totalPaginas > 1"
          class="mt-4 flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
        >
          <p class="text-xs text-cafe/50 tabular-nums">
            Página {{ pagina + 1 }} de {{ totalPaginas }} · {{ totalElementos }} eventos
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              class="inline-flex min-h-9 items-center justify-center gap-1 rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
              :disabled="pagina <= 0"
              @click="cargar(pagina - 1)"
            >
              <ChevronLeft class="h-4 w-4" aria-hidden="true" />
              Anterior
            </button>
            <button
              type="button"
              class="inline-flex min-h-9 items-center justify-center gap-1 rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
              :disabled="!hayMas"
              @click="cargar(pagina + 1)"
            >
              Siguiente
              <ChevronRight class="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>

      <aside class="sticky top-24 hidden max-h-[calc(100vh-7rem)] self-start overflow-y-auto xl:block">
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-4">
          <AuditoriaDetalle
            :evento="seleccionado"
            @cerrar="cerrarDetalle"
            @filtrar="aplicarFiltro"
          />
        </div>
      </aside>
    </div>

    <div
      v-if="seleccionado"
      class="fixed inset-0 z-40 xl:hidden"
      role="dialog"
      aria-modal="true"
      aria-label="Detalle del evento"
    >
      <div class="absolute inset-0 bg-cafe/30" @click="cerrarDetalle" />
      <div class="absolute inset-x-0 bottom-0 max-h-[85vh] overflow-y-auto rounded-t-2xl bg-blanco p-4 shadow-xl">
        <AuditoriaDetalle
          :evento="seleccionado"
          @cerrar="cerrarDetalle"
          @filtrar="aplicarFiltro"
        />
      </div>
    </div>
  </div>
</template>
