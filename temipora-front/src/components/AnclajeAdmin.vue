<script setup>
import { computed, onMounted, ref } from 'vue'
import { RefreshCw, Download, ShieldCheck } from '@lucide/vue'
import AdminPageHeader from './admin/AdminPageHeader.vue'
import { formatearFechaHora } from '../servicios/fechas'
import { hashCorto } from '../servicios/ledger'
import {
  listarAnclajes,
  ejecutarAnclaje,
  descargarPruebaAnclaje,
} from '../servicios/anclaje'

const anclajes = ref([])
const cargando = ref(true)
const error = ref('')
const ejecutando = ref(false)
const mensaje = ref('')
const errorEjecutar = ref('')
const descargando = ref(null)

const totalEventos = computed(() => anclajes.value
  .reduce((suma, a) => suma + Number(a.cantidadEventos || 0), 0))
const pendientes = computed(() => anclajes.value
  .filter((a) => a.estado === 'PENDIENTE').length)
const confirmados = computed(() => anclajes.value
  .filter((a) => a.estado === 'CONFIRMADO').length)

const ESTADO = {
  CONFIRMADO: { texto: 'Confirmado en Bitcoin', clase: 'bg-green-500/10 text-green-700' },
  PENDIENTE: { texto: 'Pendiente de minar', clase: 'bg-amber-500/10 text-amber-700' },
  FALLIDO: { texto: 'Fallido', clase: 'bg-rojizo/10 text-rojizo' },
}

function infoEstado(estado) {
  return ESTADO[estado] || { texto: estado || '—', clase: 'bg-rosa/40 text-granate' }
}

function fecha(valor) {
  return valor ? formatearFechaHora(valor) : '—'
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    anclajes.value = await listarAnclajes()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar los anclajes.'
  } finally {
    cargando.value = false
  }
}

async function sellarAhora() {
  ejecutando.value = true
  mensaje.value = ''
  errorEjecutar.value = ''
  try {
    const res = await ejecutarAnclaje()
    if (res && res.idAnclaje) {
      mensaje.value = `Se sellaron ${res.cantidadEventos} evento(s). El sello se confirmará en Bitcoin en unos minutos.`
    } else {
      mensaje.value = res?.mensaje || 'No hay eventos nuevos para sellar.'
    }
    await cargar()
  } catch (e) {
    errorEjecutar.value = e.response?.data?.message || 'No se pudo ejecutar el sellado.'
  } finally {
    ejecutando.value = false
  }
}

async function descargar(anclaje) {
  descargando.value = anclaje.idAnclaje
  try {
    const blob = await descargarPruebaAnclaje(anclaje.idAnclaje)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `anclaje-${anclaje.idAnclaje}.ots`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    errorEjecutar.value = 'No se pudo descargar la prueba .ots.'
  } finally {
    descargando.value = null
  }
}

onMounted(cargar)
</script>

<template>
  <div class="px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Sistema"
      titulo="Anclaje en Bitcoin"
      descripcion="Sello periódico de la raíz de Merkle del registro de autenticidad en la blockchain de Bitcoin con OpenTimestamps. No usa criptomonedas; solo publica un hash."
    >
      <template #acciones>
        <button
          type="button"
          class="inline-flex min-h-10 items-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
          :disabled="ejecutando"
          @click="sellarAhora"
        >
          <ShieldCheck class="h-4 w-4" aria-hidden="true" />
          {{ ejecutando ? 'Sellando…' : 'Sellar ahora' }}
        </button>
        <button
          type="button"
          class="inline-flex min-h-10 items-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
          :disabled="cargando"
          @click="cargar"
        >
          <RefreshCw
            class="h-4 w-4"
            :class="cargando ? 'animate-spin motion-reduce:animate-none' : ''"
            aria-hidden="true"
          />
          Actualizar
        </button>
      </template>
    </AdminPageHeader>

    <p
      v-if="mensaje"
      role="status"
      class="mt-6 rounded-xl border border-green-600/30 bg-green-500/10 px-4 py-2 text-sm font-medium text-green-700"
    >
      {{ mensaje }}
    </p>
    <p
      v-if="errorEjecutar"
      role="alert"
      class="mt-6 rounded-xl border border-coral/30 bg-coral/5 px-4 py-2 text-sm font-medium text-coral"
    >
      {{ errorEjecutar }}
    </p>

    <div class="mt-6 grid gap-3 sm:grid-cols-3">
      <div class="rounded-2xl border border-rosa/60 bg-blanco p-4">
        <p class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Anclajes</p>
        <p class="mt-2 font-display text-2xl font-extrabold tabular-nums text-granate">
          {{ anclajes.length }}
        </p>
      </div>
      <div class="rounded-2xl border border-rosa/60 bg-blanco p-4">
        <p class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Confirmados en Bitcoin</p>
        <p class="mt-2 font-display text-2xl font-extrabold tabular-nums text-green-700">
          {{ confirmados }}
        </p>
      </div>
      <div class="rounded-2xl border border-rosa/60 bg-blanco p-4">
        <p class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Eventos sellados</p>
        <p class="mt-2 font-display text-2xl font-extrabold tabular-nums text-granate">
          {{ totalEventos }}
        </p>
      </div>
    </div>

    <div v-if="cargando" class="mt-6 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando anclajes…</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-14 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70 motion-reduce:animate-none"
        aria-hidden="true"
      ></div>
    </div>

    <div
      v-else-if="error"
      class="mt-6 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center"
    >
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <div
      v-else-if="!anclajes.length"
      class="mt-6 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center text-sm text-cafe/65"
    >
      Todavía no hay anclajes. Presioná “Sellar ahora” para sellar los eventos
      nuevos del ledger en Bitcoin.
    </div>

    <div v-else class="mt-6 overflow-hidden rounded-2xl border border-rosa/60 bg-blanco">
      <table class="w-full text-sm">
        <thead class="bg-crema/60 text-left text-[11px] uppercase tracking-wide text-cafe/55">
          <tr>
            <th class="px-4 py-3">Anclaje</th>
            <th class="px-4 py-3">Estado</th>
            <th class="px-4 py-3 text-right">Eventos</th>
            <th class="px-4 py-3">Raíz (Merkle)</th>
            <th class="px-4 py-3">Solicitado</th>
            <th class="px-4 py-3">Bloque de Bitcoin</th>
            <th class="px-4 py-3 text-right">Prueba</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-rosa/30">
          <tr v-for="a in anclajes" :key="a.idAnclaje" class="align-top">
            <td class="px-4 py-3 font-semibold text-granate">#{{ a.idAnclaje }}</td>
            <td class="px-4 py-3">
              <span
                class="rounded-full px-2.5 py-0.5 text-[10px] font-bold"
                :class="infoEstado(a.estado).clase"
              >
                {{ infoEstado(a.estado).texto }}
              </span>
            </td>
            <td class="px-4 py-3 text-right tabular-nums text-cafe/70">{{ a.cantidadEventos }}</td>
            <td class="px-4 py-3 font-mono text-[11px] text-cafe/60" :title="a.hashRaiz">
              {{ hashCorto(a.hashRaiz) }}
            </td>
            <td class="px-4 py-3 text-[11px] text-cafe/60">{{ fecha(a.fechaSolicitud) }}</td>
            <td class="px-4 py-3 text-[11px] text-cafe/60">
              <template v-if="a.bitcoinAltura">
                <span class="font-semibold text-green-700">#{{ a.bitcoinAltura }}</span>
                <span class="block text-[10px] text-cafe/45">{{ fecha(a.fechaConfirmacion) }}</span>
              </template>
              <span v-else class="text-cafe/45">Esperando…</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button
                type="button"
                class="inline-flex items-center gap-1 rounded-full border border-coral/40 bg-coral/10 px-3 py-1 text-[11px] font-semibold text-coral transition-colors hover:bg-coral/20 disabled:opacity-50"
                :disabled="descargando === a.idAnclaje"
                @click="descargar(a)"
              >
                <Download class="h-3.5 w-3.5" aria-hidden="true" />
                {{ descargando === a.idAnclaje ? 'Descargando…' : '.ots' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-6 rounded-2xl border border-borde/60 bg-crema/30 p-4 text-xs leading-relaxed text-cafe/60">
      <p>
        Cada anclaje sella en Bitcoin la <strong class="text-cafe/75">raíz de Merkle</strong>
        de un lote de eventos del ledger. La prueba <code>.ots</code> es verificable de
        forma independiente en <span class="font-semibold">opentimestamps.org</span>
        (o con la herramienta oficial). Mientras un anclaje esté
        <em>pendiente</em>, el sello existe pero Bitcoin todavía no confirmó el bloque;
        se actualiza solo.
      </p>
    </div>
  </div>
</template>
