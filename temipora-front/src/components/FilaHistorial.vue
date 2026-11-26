<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFecha } from '../servicios/pedidos'

const props = defineProps({
  item: { type: Object, required: true },
})

const esPedido = computed(() => props.item.tipo === 'PEDIDO')
const to = computed(() => (esPedido.value
  ? { name: 'pedido-detalle', params: { idPedido: props.item.id } }
  : { name: 'compra-detalle', params: { id: props.item.id } }))

const CLASES_ESTADO = {
  FINALIZADO: 'border-green-700/30 bg-green-500/10 text-green-700',
  EN_CURSO: 'border-coral/30 bg-coral/10 text-coral',
  PENDIENTE: 'border-mantequilla/70 bg-mantequilla/40 text-granate',
  CANCELADO: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
}

const claseEstado = computed(
  () => CLASES_ESTADO[props.item.estadoGrupo]
    || 'border-rosa bg-blanco text-cafe/60',
)

const descuento = computed(() => Number(props.item.descuentoMonto || 0))
const porcentaje = computed(() => Number(props.item.descuentoPorcentaje || 0))
</script>

<template>
  <router-link
    :to="to"
    class="group block rounded-2xl border border-rosa/60 bg-blanco p-4 transition hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)] motion-reduce:transition-none motion-reduce:transform-none"
  >
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div class="min-w-0">
        <div class="flex flex-wrap items-center gap-2">
          <span
            class="inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold"
            :class="esPedido
              ? 'bg-coral/10 text-coral'
              : 'bg-rosa/30 text-granate'"
          >
            {{ item.tipoEtiqueta }}
          </span>
          <span
            class="inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold"
            :class="claseEstado"
          >
            {{ item.estadoEtiqueta }}
          </span>
          <span
            v-if="descuento > 0"
            class="inline-flex rounded-full bg-green-500/10 px-2.5 py-1 text-[11px] font-semibold text-green-700"
          >
            -{{ porcentaje }}%
          </span>
        </div>

        <p class="mt-2 line-clamp-2 font-display text-base font-bold leading-snug text-granate">
          {{ item.titulo || 'Sin detalle' }}
        </p>

        <p class="mt-1 text-xs text-cafe/55">
          {{ formatearFecha(item.fecha) }}
          <span v-if="item.contraparte"> · @{{ item.contraparte }}</span>
          <span v-if="!esPedido && item.cantidadItems > 1">
            · {{ item.cantidadItems }} ítems
          </span>
        </p>
      </div>

      <div class="flex shrink-0 flex-col items-start gap-1 sm:items-end">
        <p class="font-display text-lg font-extrabold text-granate tabular-nums">
          {{ formatearPrecio(item.monto, item.moneda || 'ARS') }}
        </p>
        <p v-if="descuento > 0" class="text-[11px] font-semibold text-green-700">
          Ahorraste {{ formatearPrecio(descuento, item.moneda || 'ARS') }}
        </p>
        <span class="text-xs font-semibold text-coral transition group-hover:text-rojizo">
          Ver detalle →
        </span>
      </div>
    </div>
  </router-link>
</template>
