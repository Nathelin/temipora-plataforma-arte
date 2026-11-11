<script setup>
import { ref } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { cargarLedgerCompraItem } from '../servicios/compras'

const props = defineProps({
  idCompra: { type: Number, required: true },
  item: { type: Object, required: true },
})

const abierto = ref(false)
const cargando = ref(false)
const error = ref('')
const ledger = ref(null)

async function alternar() {
  abierto.value = !abierto.value
  if (!abierto.value || ledger.value || cargando.value) return
  cargando.value = true
  error.value = ''
  try {
    ledger.value = await cargarLedgerCompraItem(
      props.idCompra,
      props.item.idCompraItem,
    )
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el historial.'
  } finally {
    cargando.value = false
  }
}

function fecha(valor) {
  if (!valor) return '—'
  try {
    return new Intl.DateTimeFormat('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(valor))
  } catch {
    return valor
  }
}
</script>

<template>
  <div class="mt-4 rounded-xl border border-rosa/40 bg-rosa/5 px-4 py-3">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-2">
        <p class="text-xs font-semibold text-granate">Certificado de procedencia</p>
        <span
          v-if="ledger"
          class="rounded-full px-2 py-0.5 text-[10px] font-bold"
          :class="ledger.integridad
            ? 'bg-green-500/10 text-green-700'
            : 'bg-rojizo/10 text-rojizo'"
        >
          {{ ledger.integridad ? 'Verificado' : 'Revisar' }}
        </span>
      </div>
      <button
        type="button"
        class="rounded-full border border-rosa bg-blanco px-3 py-1.5 text-[11px] font-semibold text-granate transition hover:bg-rosa/20"
        @click="alternar"
      >
        {{ abierto ? 'Ocultar historial' : 'Ver historial completo' }}
      </button>
    </div>

    <div v-if="abierto" class="mt-3">
      <p v-if="cargando" class="text-xs text-cafe/60">Cargando historial...</p>
      <p v-else-if="error" class="text-xs text-rojizo">{{ error }}</p>
      <template v-else-if="ledger">
        <p class="text-xs text-cafe/60">{{ ledger.mensaje }}</p>

        <p v-if="!ledger.eventos.length" class="mt-2 text-xs text-cafe/55">
          Esta pieza no tiene eventos registrados.
        </p>

        <ol v-else class="mt-3 space-y-2">
          <li
            v-for="evento in ledger.eventos"
            :key="evento.idEventoLedger"
            class="rounded-lg bg-blanco px-3 py-2 ring-1 ring-rosa/40"
          >
            <div class="flex flex-wrap items-center justify-between gap-2">
              <p class="text-[11px] font-bold uppercase tracking-wide text-coral">
                #{{ evento.secuencia }} · {{ evento.tipo }}
                <span
                  v-if="evento.esEventoDeEstaCompra"
                  class="ml-1 rounded-full bg-coral/10 px-1.5 py-0.5 text-[9px] font-semibold text-coral"
                >
                  Tu compra
                </span>
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
              {{ fecha(evento.ocurridoEn) }}
            </p>
            <p class="mt-1 text-[11px] text-cafe/55">
              @{{ evento.nombreOrigen || '—' }} → @{{ evento.nombreDestino || '—' }}
            </p>
            <p v-if="evento.monto != null" class="mt-1 text-[11px] font-semibold text-granate">
              Monto pagado: {{ formatearPrecio(evento.monto, evento.moneda || 'ARS') }}
            </p>
            <p v-else class="mt-1 text-[10px] text-cafe/40">
              Monto no disponible
            </p>

            <div class="mt-2 grid gap-1 text-[10px] text-cafe/50 sm:grid-cols-2">
              <p class="break-all">
                <span class="font-semibold">ID de evento:</span> {{ evento.idEventoLedger }}
              </p>
              <p v-if="evento.idCompraItem" class="break-all">
                <span class="font-semibold">ID de ítem:</span> {{ evento.idCompraItem }}
              </p>
              <p class="break-all sm:col-span-2">
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
              <p>
                <span class="font-semibold">Versión de hash:</span>
                {{ evento.versionHash }}
              </p>
            </div>
          </li>
        </ol>
      </template>
    </div>
  </div>
</template>
