<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { datosContraparte } from '../servicios/solicitudes'
import { estadoPedidoDe, etiquetaTipoHito } from '../servicios/pedidos'

const props = defineProps({
  pedido: { type: Object, required: true },
})

const contraparte = computed(() => datosContraparte(props.pedido.contraparte))
const estado = computed(() => estadoPedidoDe(props.pedido.estado))
const progreso = computed(() => {
  const valor = Number(props.pedido.porcentajeAvance)
  return Number.isFinite(valor) ? Math.min(100, Math.max(0, valor)) : 0
})
const monto = computed(() => {
  if (props.pedido.proximoHitoMontoMostrado == null) return null
  return formatearPrecio(
    props.pedido.proximoHitoMontoMostrado,
    props.pedido.monedaMostrada || props.pedido.monedaCobro,
  )
})
const accion = computed(() => {
  if (props.pedido.estado === 'IMPAGO') return 'Pedido sin pago'
  if (props.pedido.puedeEntregar) return 'Subir entrega'
  if (props.pedido.estado === 'PENDIENTE_PAGO') return 'Esperando pago del cliente'
  return 'Ver pedido'
})
const accionDestacada = computed(
  () => props.pedido.puedeEntregar || props.pedido.estado === 'IMPAGO',
)
</script>

<template>
  <router-link
    :to="{ name: 'panel-pedido-detalle', params: { idPedido: pedido.idPedido } }"
    class="group block rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
  >
    <div class="flex items-start gap-3">
      <img
        v-if="contraparte?.foto"
        :src="contraparte.foto"
        alt=""
        class="h-9 w-9 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
      />
      <span
        v-else
        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate"
        aria-hidden="true"
      >
        {{ contraparte?.inicial || '?' }}
      </span>

      <div class="min-w-0 flex-1">
        <div class="flex items-start justify-between gap-2">
          <p class="truncate text-sm font-semibold text-granate">
            {{ contraparte?.nombre || 'Usuario no disponible' }}
          </p>
          <span
            class="shrink-0 whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
            :class="estado.clase"
          >
            {{ estado.etiqueta }}
          </span>
        </div>
        <p class="truncate text-xs text-cafe/60">{{ pedido.nombreServicio || 'Servicio' }}</p>

        <div class="mt-2 flex items-center gap-2">
          <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-rosa/30">
            <div class="h-full rounded-full bg-coral" :style="{ width: `${progreso}%` }"></div>
          </div>
          <span class="shrink-0 text-[11px] font-semibold text-cafe/50">{{ progreso }}%</span>
        </div>

        <div class="mt-2 flex flex-wrap items-center justify-between gap-2">
          <span v-if="pedido.proximoHitoNombre" class="text-[11px] text-cafe/60">
            <template v-if="pedido.proximoHitoTipo">{{ etiquetaTipoHito(pedido.proximoHitoTipo) }} · </template>{{ pedido.proximoHitoNombre }}
            <span v-if="monto" class="font-semibold text-granate"> · {{ monto }}</span>
          </span>
          <span v-else class="text-[11px] text-cafe/50">Sin hitos pendientes</span>

          <span
            class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold"
            :class="accionDestacada
              ? 'bg-coral text-white'
              : 'border border-rosa bg-blanco text-cafe/70'"
          >
            {{ accion }}
          </span>
        </div>

        <p v-if="pedido.noLeidos" class="mt-1 text-[11px] font-semibold text-coral">
          {{ pedido.noLeidos }} mensaje(s) sin leer
        </p>
      </div>
    </div>
  </router-link>
</template>
