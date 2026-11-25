<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatearFecha } from '../servicios/fechas'
import { formatearPrecio } from '../servicios/moneda'
import { guardarArchivo } from '../servicios/descargas'
import {
  descargarHistorialSustitucionesPdf,
  obtenerDetalleSustitucion,
  obtenerHistorialSustituciones,
} from '../servicios/adminSustituciones'
import { useNotificaciones } from '../composables/useNotificaciones'

const { exito, error: notificarError } = useNotificaciones()

const PRESETS = [
  { clave: '7d', texto: '7 días' },
  { clave: '30d', texto: '30 días' },
  { clave: '90d', texto: '90 días' },
  { clave: 'anio', texto: 'Este año' },
]

const ESTADOS = [
  { valor: '', texto: 'Todos los estados' },
  { valor: 'ABIERTA', texto: 'Abierta' },
  { valor: 'ALTERNATIVA_ELEGIDA', texto: 'Alternativa elegida' },
  { valor: 'RESUELTA_ALTERNATIVA', texto: 'Resuelta con alternativa' },
  { valor: 'RESUELTA_COLA', texto: 'Resuelta en cola' },
  { valor: 'DESCARTADA', texto: 'Descartada' },
  { valor: 'EXPIRADA', texto: 'Expirada' },
]

const MOTIVOS = [
  { valor: '', texto: 'Todos los motivos' },
  { valor: 'SIN_CUPOS', texto: 'Sin cupos' },
  { valor: 'SOLO_ESPERA', texto: 'Sólo lista de espera' },
  { valor: 'CERRADA', texto: 'Recepción cerrada' },
  { valor: 'PAUSADO', texto: 'Servicio pausado' },
  { valor: 'ARCHIVADO', texto: 'Servicio archivado' },
  { valor: 'SOLICITUD_RECHAZADA', texto: 'Solicitud rechazada' },
  { valor: 'PRESUPUESTO_RETIRADO', texto: 'Presupuesto retirado' },
  { valor: 'PRESUPUESTO_VENCIDO', texto: 'Presupuesto vencido' },
  { valor: 'SERVICIO_DADO_DE_BAJA', texto: 'Servicio dado de baja' },
]

const DISPARADORES = [
  { valor: '', texto: 'Todos los disparadores' },
  { valor: 'DETALLE', texto: 'Desde el detalle' },
  { valor: 'EXPLORAR', texto: 'Desde Explorar' },
  { valor: 'PERFIL', texto: 'Desde el perfil' },
  { valor: 'FAVORITOS', texto: 'Desde favoritos' },
  { valor: 'INTENTO_SOLICITUD', texto: 'Al intentar solicitar' },
  { valor: 'CIERRE', texto: 'Al cerrarse la solicitud o el pedido' },
]

const CLASES_ESTADO = {
  ABIERTA: 'bg-mantequilla text-tinta',
  ALTERNATIVA_ELEGIDA: 'bg-coral/10 text-coral',
  RESUELTA_ALTERNATIVA: 'bg-green-500/10 text-green-700',
  RESUELTA_COLA: 'bg-rosa/30 text-granate',
  DESCARTADA: 'bg-cafe/10 text-cafe/70',
  EXPIRADA: 'bg-rojizo/10 text-rojizo',
}

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
const desde = ref(iso(sumarDias(hoy, -29)))
const hasta = ref(iso(hoy))
const preset = ref('30d')
const q = ref('')
const estado = ref('')
const motivo = ref('')
const disparador = ref('')
const pagina = ref(0)
const informe = ref(null)
const cargando = ref(true)
const error = ref('')
const descargando = ref(false)
const abierto = ref(null)
const detalles = ref({})
const cargandoDetalle = ref(null)
const errorDetalle = ref('')
let debounceTimer
let requestId = 0

const resumen = computed(() => informe.value?.resumen || {})
const eventos = computed(() => informe.value?.eventos || [])
const motivos = computed(() => resumen.value.motivos || [])
const totalPaginas = computed(() => Number(informe.value?.totalPaginas || 0))
const hayMas = computed(() => Boolean(informe.value?.hayMas))
const totalElementos = computed(() => Number(informe.value?.totalElementos || 0))
const rangoInvalido = computed(
  () => !desde.value || !hasta.value || desde.value > hasta.value,
)

const tarjetas = computed(() => {
  const r = resumen.value
  return [
    {
      etiqueta: 'Eventos',
      valor: Number(r.total || 0),
      detalle: 'Búsquedas de alternativas en el período',
      destacada: true,
    },
    {
      etiqueta: 'Resueltas con otra artista',
      valor: Number(r.resueltasConAlternativa || 0),
      detalle: `${r.tasaSustitucion || 0}% de las resueltas`,
    },
    {
      etiqueta: 'Quedaron en lista de espera',
      valor: Number(r.resueltasConCola || 0),
      detalle: 'El cliente siguió con el servicio original',
    },
    {
      etiqueta: 'Opciones por evento',
      valor: Number(r.opcionesPromedio || 0).toLocaleString('es-AR', {
        maximumFractionDigits: 2,
      }),
      detalle: 'Promedio de alternativas mostradas',
    },
  ]
})

function claseEstado(valor) {
  return CLASES_ESTADO[valor] || 'bg-rosa/30 text-granate'
}

function participacion(item) {
  const total = Number(resumen.value.total || 0)
  if (!total) return 0
  return Math.round((Number(item.total || 0) * 100) / total)
}

function scoreTexto(score) {
  const numero = Number(score)
  if (!Number.isFinite(numero)) return '—'
  return `${(numero * 100).toFixed(0)}%`
}

function detalleDe(id) {
  return detalles.value[id] || null
}

function aplicarPreset(clave) {
  const ahora = new Date()
  preset.value = clave

  if (clave === '7d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -6))
  } else if (clave === '30d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -29))
  } else if (clave === '90d') {
    hasta.value = iso(ahora)
    desde.value = iso(sumarDias(ahora, -89))
  } else if (clave === 'anio') {
    desde.value = iso(new Date(ahora.getFullYear(), 0, 1))
    hasta.value = iso(ahora)
  }
}

function marcarPersonalizado() {
  preset.value = 'personalizado'
}

function limpiarFiltros() {
  q.value = ''
  estado.value = ''
  motivo.value = ''
  disparador.value = ''
}

function parametros() {
  return {
    desde: desde.value,
    hasta: hasta.value,
    q: q.value.trim() || undefined,
    estado: estado.value || undefined,
    motivo: motivo.value || undefined,
    disparador: disparador.value || undefined,
  }
}

async function cargar(nuevaPagina = 0) {
  if (rangoInvalido.value) {
    error.value = 'La fecha de inicio debe ser anterior a la de fin.'
    return
  }
  const actual = ++requestId
  cargando.value = true
  error.value = ''
  abierto.value = null
  errorDetalle.value = ''
  try {
    const data = await obtenerHistorialSustituciones({
      ...parametros(),
      pagina: nuevaPagina,
      tamanio: 20,
    })
    if (actual !== requestId) return
    informe.value = data
    pagina.value = Number(data?.pagina || 0)
  } catch (e) {
    if (actual !== requestId) return
    error.value = e.response?.data?.message
      || 'No se pudo cargar el historial de sustituciones.'
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
    const { blob, nombre } = await descargarHistorialSustitucionesPdf(parametros())
    guardarArchivo(blob, nombre)
    exito('Descargamos el historial en PDF.')
  } catch {
    notificarError('No se pudo descargar el historial.')
  } finally {
    descargando.value = false
  }
}

async function alternarDetalle(evento) {
  const id = evento.idSustitucion
  if (abierto.value === id) {
    abierto.value = null
    return
  }
  abierto.value = id
  errorDetalle.value = ''
  if (detalles.value[id]) return

  cargandoDetalle.value = id
  try {
    const data = await obtenerDetalleSustitucion(id)
    detalles.value = { ...detalles.value, [id]: data }
  } catch (e) {
    if (abierto.value === id) {
      errorDetalle.value = e.response?.data?.message
        || 'No se pudo cargar el detalle del evento.'
    }
  } finally {
    if (cargandoDetalle.value === id) {
      cargandoDetalle.value = null
    }
  }
}

watch([desde, hasta, q, estado, motivo, disparador], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => cargar(0), 250)
})

onMounted(() => cargar(0))
onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Comercio"
      titulo="Historial de sustituciones"
      descripcion="Cada fila es una búsqueda de alternativas. Expandidla para ver las opciones que se mostraron, con el precio congelado al momento y cuál eligió el cliente."
    >
      <template #acciones>
        <span
          v-if="!cargando && !error"
          class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral tabular-nums"
        >
          {{ totalElementos }} {{ totalElementos === 1 ? 'evento' : 'eventos' }}
        </span>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
          :disabled="descargando || cargando || rangoInvalido"
          @click="descargar"
        >
          {{ descargando ? 'Generando...' : 'Descargar PDF' }}
        </button>
      </template>
    </AdminPageHeader>

    <div class="mt-6">
      <AdminTabs />
    </div>

    <section class="rounded-2xl border border-rosa/60 bg-blanco p-4 sm:p-5">
      <div class="flex flex-wrap gap-2" role="group" aria-label="Períodos rápidos">
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

      <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[auto_auto_minmax(0,1fr)]">
        <label class="flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Desde
          <input
            v-model="desde"
            type="date"
            class="min-w-0 rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="marcarPersonalizado"
          />
        </label>
        <label class="flex items-center gap-2 text-xs font-semibold text-cafe/60">
          Hasta
          <input
            v-model="hasta"
            type="date"
            class="min-w-0 rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            @change="marcarPersonalizado"
          />
        </label>
        <input
          v-model="q"
          type="search"
          placeholder="Buscar por cliente, servicio o artista..."
          class="min-w-0 rounded-lg border-0 px-4 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        />
      </div>

      <div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
        <select
          v-model="estado"
          aria-label="Filtrar por estado"
          class="rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option v-for="item in ESTADOS" :key="item.valor" :value="item.valor">
            {{ item.texto }}
          </option>
        </select>
        <select
          v-model="motivo"
          aria-label="Filtrar por motivo"
          class="rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option v-for="item in MOTIVOS" :key="item.valor" :value="item.valor">
            {{ item.texto }}
          </option>
        </select>
        <select
          v-model="disparador"
          aria-label="Filtrar por disparador"
          class="rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option v-for="item in DISPARADORES" :key="item.valor" :value="item.valor">
            {{ item.texto }}
          </option>
        </select>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 motion-reduce:transition-none"
          @click="limpiarFiltros"
        >
          Limpiar filtros
        </button>
      </div>
    </section>

    <div v-if="cargando" class="mt-6 space-y-4" role="status" aria-live="polite">
      <span class="sr-only">Cargando historial de sustituciones...</span>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 4" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
      <div class="h-64 animate-pulse rounded-3xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
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
        <span class="font-semibold text-granate">Estado del período</span>
        <span><strong class="text-granate">{{ resumen.alternativaElegida || 0 }}</strong> alternativa elegida</span>
        <span><strong class="text-granate">{{ resumen.descartadas || 0 }}</strong> descartadas</span>
        <span><strong class="text-granate">{{ resumen.abiertas || 0 }}</strong> abiertas</span>
        <span><strong class="text-granate">{{ resumen.expiradas || 0 }}</strong> expiradas</span>
        <span><strong class="text-granate">{{ resumen.anonimas || 0 }}</strong> anónimas</span>
      </div>

      <section
        v-if="motivos.length"
        class="mt-6 rounded-2xl border border-rosa/60 bg-blanco p-5"
      >
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-lg font-bold text-granate">Motivos de la búsqueda</h2>
          <span class="text-xs text-cafe/50 tabular-nums">
            {{ resumen.total || 0 }} {{ Number(resumen.total) === 1 ? 'evento' : 'eventos' }}
          </span>
        </div>
        <ul class="mt-4 space-y-2">
          <li
            v-for="item in motivos"
            :key="item.clave"
            class="flex items-center gap-3 text-xs"
          >
            <span class="w-40 shrink-0 text-cafe/70">{{ item.etiqueta }}</span>
            <span class="h-2 min-w-0 flex-1 overflow-hidden rounded-full bg-rosa/30">
              <span
                class="block h-full rounded-full bg-coral"
                :style="{ width: `${participacion(item)}%` }"
              ></span>
            </span>
            <span class="w-10 shrink-0 text-right font-semibold text-granate tabular-nums">
              {{ item.total }}
            </span>
          </li>
        </ul>
      </section>

      <section class="mt-6 overflow-hidden rounded-3xl border border-rosa/60 bg-blanco shadow-xl shadow-rosa/30">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[62rem] text-left text-sm">
            <caption class="sr-only">
              Eventos históricos de sustitución de artistas
            </caption>
            <thead class="bg-rosa/30 text-granate">
              <tr>
                <th class="w-10 px-3 py-3 font-normal"></th>
                <th class="px-3 py-3 font-normal">Fecha</th>
                <th class="px-3 py-3 font-normal">Cliente</th>
                <th class="px-3 py-3 font-normal">Servicio / artista origen</th>
                <th class="px-3 py-3 font-normal">Motivo</th>
                <th class="px-3 py-3 font-normal">Disparador</th>
                <th class="px-3 py-3 font-normal">Opciones</th>
                <th class="px-3 py-3 font-normal">Resultado</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="evento in eventos" :key="evento.idSustitucion">
                <tr class="border-t border-rosa/40 align-top transition-colors hover:bg-rosa/10">
                  <td class="px-3 py-3">
                    <button
                      type="button"
                      class="inline-flex h-8 w-8 items-center justify-center rounded-full border border-rosa/60 text-granate transition hover:bg-rosa/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
                      :aria-expanded="abierto === evento.idSustitucion"
                      :aria-controls="`detalle-sustitucion-${evento.idSustitucion}`"
                      aria-label="Ver alternativas mostradas"
                      @click="alternarDetalle(evento)"
                    >
                      <svg
                        class="h-4 w-4 transition-transform"
                        :class="abierto === evento.idSustitucion ? 'rotate-180' : ''"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                        aria-hidden="true"
                      >
                        <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </td>
                  <td class="whitespace-nowrap px-3 py-3 text-cafe/70">
                    {{ formatearFecha(evento.fechaCreacion) }}
                  </td>
                  <td class="px-3 py-3">
                    <p class="font-semibold text-granate">
                      {{ evento.clienteUsuario ? `@${evento.clienteUsuario}` : 'Visitante' }}
                    </p>
                  </td>
                  <td class="px-3 py-3">
                    <p class="max-w-[16rem] text-cafe">{{ evento.servicioOrigen || '—' }}</p>
                    <p class="text-xs text-cafe/50">@{{ evento.artistaOrigen }}</p>
                  </td>
                  <td class="px-3 py-3">
                    <span class="inline-flex rounded-full bg-coral/10 px-2.5 py-1 text-[11px] font-semibold text-coral">
                      {{ evento.motivoEtiqueta }}
                    </span>
                  </td>
                  <td class="px-3 py-3 text-xs text-cafe/60">
                    {{ evento.disparadorEtiqueta }}
                  </td>
                  <td class="whitespace-nowrap px-3 py-3 text-center font-semibold text-granate tabular-nums">
                    {{ evento.cantidadOpciones }}
                  </td>
                  <td class="px-3 py-3">
                    <span
                      class="inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold"
                      :class="claseEstado(evento.estado)"
                    >
                      {{ evento.estadoEtiqueta }}
                    </span>
                    <p class="mt-1 max-w-[16rem] text-xs text-cafe/55">
                      {{ evento.resultado }}
                    </p>
                    <p v-if="evento.idPedido" class="mt-1 text-xs">
                      <router-link
                        :to="{ name: 'admin-pedido-detalle', params: { idPedido: evento.idPedido } }"
                        class="font-semibold text-coral transition hover:text-rojizo"
                      >
                        Pedido #{{ evento.idPedido }}
                      </router-link>
                    </p>
                  </td>
                </tr>

                <tr
                  v-if="abierto === evento.idSustitucion"
                  :id="`detalle-sustitucion-${evento.idSustitucion}`"
                  class="border-t border-rosa/30"
                >
                  <td colspan="8" class="bg-crema/50 px-4 py-4">
                    <div
                      v-if="cargandoDetalle === evento.idSustitucion"
                      class="py-4 text-center text-sm text-cafe/60"
                    >
                      Cargando alternativas...
                    </div>
                    <p
                      v-else-if="errorDetalle"
                      class="py-2 text-sm text-rojizo"
                    >
                      {{ errorDetalle }}
                    </p>
                    <template v-else-if="detalleDe(evento.idSustitucion)">
                      <div class="flex flex-wrap items-center gap-x-5 gap-y-1 text-xs text-cafe/60">
                        <span>Evento #{{ evento.idSustitucion }}</span>
                        <span>Solicitud #{{ evento.idSolicitud || '—' }}</span>
                        <span v-if="evento.idPedido">
                          Pedido
                          <router-link
                            :to="{ name: 'admin-pedido-detalle', params: { idPedido: evento.idPedido } }"
                            class="font-semibold text-coral hover:text-rojizo"
                          >
                            #{{ evento.idPedido }}
                          </router-link>
                        </span>
                        <span v-else>Pedido —</span>
                        <span>Resuelto: {{ formatearFecha(evento.fechaResolucion) || '—' }}</span>
                      </div>

                      <p
                        v-if="!detalleDe(evento.idSustitucion).opciones?.length"
                        class="mt-3 text-sm text-cafe/60"
                      >
                        No se registraron alternativas para este evento.
                      </p>

                      <div
                        v-else
                        class="mt-3 overflow-x-auto rounded-xl border border-rosa/40 bg-blanco"
                      >
                        <table class="w-full min-w-[44rem] text-left text-xs">
                          <thead class="bg-rosa/20 text-granate">
                            <tr>
                              <th class="px-3 py-2 font-normal">#</th>
                              <th class="px-3 py-2 font-normal">Servicio</th>
                              <th class="px-3 py-2 font-normal">Artista</th>
                              <th class="px-3 py-2 font-normal">Precio al momento</th>
                              <th class="px-3 py-2 font-normal">Puntaje</th>
                              <th class="px-3 py-2 font-normal">Motivo</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr
                              v-for="opcion in detalleDe(evento.idSustitucion).opciones"
                              :key="opcion.idSustitucionOpcion"
                              class="border-t border-rosa/30"
                              :class="opcion.elegida ? 'bg-green-500/5' : ''"
                            >
                              <td class="px-3 py-2 text-cafe/50 tabular-nums">
                                {{ opcion.orden + 1 }}
                              </td>
                              <td class="px-3 py-2 text-cafe">
                                {{ opcion.servicio }}
                                <span
                                  v-if="opcion.elegida"
                                  class="ml-2 inline-flex rounded-full bg-green-500/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-green-700"
                                >
                                  Elegida
                                </span>
                              </td>
                              <td class="px-3 py-2 text-cafe/70">@{{ opcion.artista }}</td>
                              <td class="whitespace-nowrap px-3 py-2 text-cafe">
                                {{ formatearPrecio(opcion.precioMomento, opcion.moneda || 'ARS') }}
                              </td>
                              <td class="px-3 py-2 text-cafe/70 tabular-nums">
                                {{ scoreTexto(opcion.score) }}
                              </td>
                              <td class="px-3 py-2 text-cafe/60">
                                {{ opcion.motivo || '—' }}
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </template>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <div v-if="!eventos.length" class="border-t border-rosa/40 px-6 py-12 text-center">
          <p class="font-semibold text-granate">No hay eventos de sustitución para estos filtros.</p>
          <p class="mt-1 text-sm text-cafe/60">
            Probá con otro período o quitá los filtros.
          </p>
        </div>
      </section>

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
            :disabled="!hayMas"
            @click="cargar(pagina + 1)"
          >
            Siguiente
          </button>
        </div>
      </div>

      <p class="mt-6 text-xs leading-relaxed text-cafe/50">
        Cada fila es una búsqueda de alternativas. El detalle conserva las opciones
        mostradas, el precio con el que se ofrecieron y cuál eligió el cliente.
      </p>
    </template>
  </div>
</template>
