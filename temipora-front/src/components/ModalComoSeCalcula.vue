<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { X } from '@lucide/vue'
import { formatearPrecio } from '../servicios/moneda'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  contexto: { type: String, default: 'pedido' },
  moneda: { type: String, default: 'ARS' },
  precio: { type: [Number, String], default: null },
  comisionPorcentaje: { type: [Number, String], default: null },
  comisionMonto: { type: [Number, String], default: null },
  neto: { type: [Number, String], default: null },
  arancelPorcentaje: { type: [Number, String], default: null },
  arancelMonto: { type: [Number, String], default: null },
  recibis: { type: [Number, String], default: null },
  descuentoPorcentaje: { type: [Number, String], default: null },
  descuentoMonto: { type: [Number, String], default: null },
  clientePaga: { type: [Number, String], default: null },
  estimado: { type: Boolean, default: true },
})

const emit = defineEmits(['cerrar'])
const id = useId()
const cerrarRef = ref(null)

const tieneDatos = computed(
  () => props.precio != null && props.precio !== '',
)
const tieneDescuento = computed(() => Number(props.descuentoMonto) > 0)
const esProducto = computed(() => props.contexto === 'producto')
const etiquetaPrecio = computed(() => {
  if (props.contexto === 'presupuesto') return 'Precio de la propuesta'
  if (esProducto.value) return 'Precio de la publicación'
  return 'Precio del pedido'
})

function formatear(valor) {
  return formatearPrecio(valor, props.moneda)
}

function porcentaje(valor) {
  if (valor == null || valor === '') return ''
  return new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 })
    .format(Number(valor))
}

function cerrar() {
  emit('cerrar')
}

function alTeclear(event) {
  if (event.key === 'Escape') cerrar()
}

watch(() => props.abierto, async (abierto) => {
  if (abierto) {
    document.addEventListener('keydown', alTeclear)
    await nextTick()
    cerrarRef.value?.focus?.()
  } else {
    document.removeEventListener('keydown', alTeclear)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  document.removeEventListener('keydown', alTeclear)
})
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    :aria-labelledby="id"
  >
    <div
      class="absolute inset-0 bg-granate/40"
      aria-hidden="true"
      @click="cerrar"
    ></div>

    <div
      class="relative max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-rosa bg-blanco p-5 shadow-xl shadow-granate/20 sm:p-6"
    >
      <div class="flex items-start justify-between gap-3">
        <h2 :id="id" class="font-display text-xl font-bold text-cafe">
          {{ esProducto ? '¿Cómo se calcula lo que vas a recibir?' : '¿Cómo se calcula tu liquidación?' }}
        </h2>
        <button
          ref="cerrarRef"
          type="button"
          class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-cafe transition hover:bg-rosa/30 hover:text-coral focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
          aria-label="Cerrar"
          @click="cerrar"
        >
          <X class="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <template v-if="tieneDatos">
        <dl class="mt-4 space-y-1.5 text-sm">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-cafe/80">{{ etiquetaPrecio }}</dt>
            <dd class="text-cafe">{{ formatear(precio) }}</dd>
          </div>
          <div
            v-if="comisionMonto != null"
            class="flex items-center justify-between gap-3"
          >
            <dt class="text-cafe/80">
              Comisión Temipora<span v-if="comisionPorcentaje != null">
                ({{ porcentaje(comisionPorcentaje) }}%)</span>
            </dt>
            <dd class="text-cafe">− {{ formatear(comisionMonto) }}</dd>
          </div>
          <div
            v-if="neto != null"
            class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5"
          >
            <dt class="font-semibold text-cafe">Tu neto</dt>
            <dd class="font-semibold text-cafe">{{ formatear(neto) }}</dd>
          </div>
          <div
            v-if="arancelMonto != null"
            class="flex items-center justify-between gap-3"
          >
            <dt class="text-cafe/80">
              Arancel MercadoPago<span v-if="arancelPorcentaje != null">
                ({{ porcentaje(arancelPorcentaje) }}%)</span>
              (estimado)
            </dt>
            <dd class="text-cafe">− {{ formatear(arancelMonto) }}</dd>
          </div>
          <div
            v-if="recibis != null"
            class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5"
          >
            <dt class="font-semibold text-cafe">
              Recibís{{ estimado ? ' (estimado)' : '' }}
            </dt>
            <dd class="text-base font-bold text-cafe">{{ formatear(recibis) }}</dd>
          </div>
        </dl>
      </template>

      <template v-else>
        <p class="mt-4 text-sm leading-relaxed text-cafe/90">
          {{ esProducto ? 'Cada venta de tu producto se calcula así:' : 'Cada venta se calcula así:' }}
        </p>
        <ul class="mt-2 space-y-1 text-sm leading-relaxed text-cafe/90">
          <template v-if="esProducto">
            <li>
              <strong class="text-cafe">El comprador paga</strong>:
              el precio que publicás.
            </li>
            <li>
              <strong class="text-cafe">Comisión Temipora</strong>:
              el porcentaje que retiene la plataforma.
            </li>
            <li>
              <strong class="text-cafe">Arancel MercadoPago</strong>:
              el costo del cobro; varía según el medio de pago y las cuotas (estimado).
            </li>
            <li>
              <strong class="text-cafe">Recibís</strong>:
              el precio menos la comisión Temipora y el arancel de MercadoPago.
            </li>
          </template>
          <template v-else>
            <li>
              <strong class="text-cafe">El cliente paga</strong>:
              el precio del pedido menos el descuento.
            </li>
            <li>
              <strong class="text-cafe">Comisión Temipora</strong>:
              el porcentaje que retiene la plataforma.
            </li>
            <li>
              <strong class="text-cafe">Tu neto</strong>:
              lo que queda después de la comisión Temipora.
            </li>
            <li>
              <strong class="text-cafe">Arancel MercadoPago</strong>:
              el costo del cobro; puede variar según el medio de pago y las cuotas.
            </li>
            <li>
              <strong class="text-cafe">Recibís</strong>:
              tu neto menos el arancel de MercadoPago.
            </li>
          </template>
        </ul>
      </template>

      <div
        v-if="tieneDescuento"
        class="mt-4 rounded-xl border border-coral/70 bg-granate px-3 py-2 text-xs leading-relaxed text-white"
      >
        <div class="flex items-center justify-between gap-3">
          <span class="font-bold">
            Descuento del cliente<span v-if="descuentoPorcentaje != null">
              ({{ porcentaje(descuentoPorcentaje) }}%)</span>
          </span>
          <span class="font-semibold">{{ formatear(descuentoMonto) }}</span>
        </div>
        <p class="mt-1">
          <template v-if="clientePaga != null">
            El cliente paga {{ formatear(clientePaga) }}
            (descuento financiado por Temipora).
          </template>
          <template v-else>
            El descuento lo financia Temipora.
          </template>
          No reduce tu neto.
        </p>
      </div>

      <div class="mt-4 space-y-1.5 text-xs leading-relaxed text-cafe/80">
        <p v-if="esProducto">
          El arancel de MercadoPago es una estimación: el valor real depende del
          medio de pago y de las cuotas, y se conoce al confirmarse el pago.
        </p>
        <p v-else>
          El arancel de MercadoPago es una estimación y puede variar según el
          medio de pago y las cuotas. Cuando MercadoPago informa el arancel
          real, “Recibís” se actualiza con ese valor.
        </p>
        <p v-if="esProducto">
          Si publicás en una moneda distinta de ARS, el cobro se procesa en ARS
          al tipo de cambio del momento. El porcentaje no cambia, pero el monto
          final puede variar por la cotización.
        </p>
        <p v-else>
          Si el plan de pagos tiene varios hitos, la comisión y el arancel se
          prorratean según el porcentaje de cada hito.
        </p>
        <p>Los montos se muestran en {{ moneda }}.</p>
      </div>

      <div class="mt-5 flex justify-end">
        <button
          type="button"
          class="rounded-full bg-coral px-5 py-2 text-sm font-semibold text-white transition hover:bg-rojizo focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-coral"
          @click="cerrar"
        >
          Entendido
        </button>
      </div>
    </div>
  </div>
</template>
