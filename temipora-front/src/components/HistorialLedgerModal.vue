<script setup>
import { computed, ref, watch } from 'vue'
import { formatearFechaHora } from '../servicios/fechas'
import { formatearPrecio } from '../servicios/moneda'
import { etiquetaEvento } from '../servicios/ledger'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  titulo: { type: String, default: 'Historial de la pieza' },
  eventos: { type: Array, default: () => [] },
  integridad: { type: Boolean, default: true },
  mensaje: { type: String, default: '' },
})

const emit = defineEmits(['cerrar'])

const POR_PAGINA = 5
const pagina = ref(0)
const totalPaginas = computed(() => Math.max(
  1,
  Math.ceil(props.eventos.length / POR_PAGINA),
))
const eventosPagina = computed(() => props.eventos.slice(
  pagina.value * POR_PAGINA,
  (pagina.value + 1) * POR_PAGINA,
))

watch(() => props.abierto, (abierto) => {
  if (abierto) pagina.value = 0
})
watch(() => props.eventos, () => {
  pagina.value = 0
})

function tieneDatosTecnicos(evento) {
  return Boolean(
    evento.idEventoLedger
    || evento.idCompraItem
    || evento.hashEvento
    || evento.hashPrevio
    || evento.hashDatos
    || evento.hashImagen,
  )
}

function cerrar() {
  emit('cerrar')
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-historial-ledger"
  >
    <div class="absolute inset-0 bg-granate/40 backdrop-blur-sm" aria-hidden="true" @click="cerrar"></div>

    <div class="relative flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-rosa bg-blanco shadow-2xl">
      <div class="flex items-start justify-between gap-3 border-b border-rosa/40 px-5 py-4">
        <div class="min-w-0">
          <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
            Historial de autenticidad
          </p>
          <h2 id="titulo-historial-ledger" class="mt-1 font-display text-lg font-bold text-granate">
            {{ titulo }}
          </h2>
          <p v-if="mensaje" class="mt-1 text-xs text-cafe/60">{{ mensaje }}</p>
        </div>
        <button
          type="button"
          class="shrink-0 rounded-full border border-rosa px-3 py-1 text-xs font-semibold text-cafe transition hover:bg-rosa/20"
          @click="cerrar"
        >
          Cerrar
        </button>
      </div>

      <div class="overflow-y-auto px-5 py-4">
        <p v-if="!eventos.length" class="py-8 text-center text-sm text-cafe/55">
          Esta pieza no tiene eventos registrados.
        </p>

        <ol v-else class="space-y-3">
          <li
            v-for="evento in eventosPagina"
            :key="evento.idEventoLedger"
            class="rounded-xl bg-crema/40 px-4 py-3 ring-1 ring-rosa/40"
          >
            <div class="flex flex-wrap items-center justify-between gap-2">
              <p class="text-[11px] font-bold uppercase tracking-wide text-coral">
                #{{ evento.secuencia }} · {{ etiquetaEvento(evento.tipo) }}
              </p>
              <span
                class="rounded-full px-2 py-0.5 text-[10px] font-bold"
                :class="evento.verificado
                  ? 'bg-green-500/10 text-green-700'
                  : 'bg-rojizo/10 text-rojizo'"
              >
                {{ evento.verificado ? 'Verificado' : 'Con diferencias' }}
              </span>
            </div>

            <p class="mt-1 text-[11px] text-cafe/60">
              {{ formatearFechaHora(evento.ocurridoEn) }}
            </p>
            <p
              v-if="evento.nombreOrigen || evento.nombreDestino"
              class="mt-1 text-[11px] text-cafe/55"
            >
              @{{ evento.nombreOrigen || '—' }} → @{{ evento.nombreDestino || '—' }}
            </p>
            <p v-if="evento.monto != null" class="mt-1 text-[11px] font-semibold text-granate">
              Monto: {{ formatearPrecio(evento.monto, evento.moneda || 'ARS') }}
            </p>

            <details
              v-if="tieneDatosTecnicos(evento)"
              class="mt-2 text-[10px] text-cafe/50"
            >
              <summary class="cursor-pointer font-semibold text-cafe/50">
                Ver detalles técnicos
              </summary>
              <div class="mt-1 grid gap-1 sm:grid-cols-2">
                <p class="break-all">
                  <span class="font-semibold">ID de evento:</span>
                  {{ evento.idEventoLedger }}
                </p>
                <p v-if="evento.idCompraItem" class="break-all">
                  <span class="font-semibold">ID de ítem:</span>
                  {{ evento.idCompraItem }}
                </p>
                <p v-if="evento.hashEvento" class="break-all sm:col-span-2">
                  <span class="font-semibold">Hash del evento:</span>
                  <span class="font-mono">{{ evento.hashEvento }}</span>
                </p>
                <p v-if="evento.hashPrevio" class="break-all sm:col-span-2">
                  <span class="font-semibold">Hash previo:</span>
                  <span class="font-mono">{{ evento.hashPrevio }}</span>
                </p>
                <p v-if="evento.hashDatos" class="break-all sm:col-span-2">
                  <span class="font-semibold">Hash de datos:</span>
                  <span class="font-mono">{{ evento.hashDatos }}</span>
                </p>
                <p v-if="evento.hashImagen" class="break-all sm:col-span-2">
                  <span class="font-semibold">Hash de imagen:</span>
                  <span class="font-mono">{{ evento.hashImagen }}</span>
                </p>
              </div>
            </details>
          </li>
        </ol>
      </div>

      <div
        v-if="totalPaginas > 1"
        class="flex items-center justify-between gap-3 border-t border-rosa/40 px-5 py-3"
      >
        <button
          type="button"
          class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="pagina === 0"
          @click="pagina -= 1"
        >
          ← Anterior
        </button>
        <span class="text-xs text-cafe/55">
          Página {{ pagina + 1 }} de {{ totalPaginas }}
        </span>
        <button
          type="button"
          class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="pagina >= totalPaginas - 1"
          @click="pagina += 1"
        >
          Siguiente →
        </button>
      </div>
    </div>
  </div>
</template>
