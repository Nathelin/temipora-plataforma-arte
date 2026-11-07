<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { formatearPrecio } from '../servicios/moneda'
import { datosContraparte } from '../servicios/solicitudes'
import { estadoPedidoDe, etiquetaTipoHito, formatearFecha } from '../servicios/pedidos'

const props = defineProps({
  pedido: { type: Object, required: true },
})

const route = useRoute()
const enPanel = computed(() => route.path.startsWith('/panel'))

const detalle = computed(() => ({
  name: enPanel.value ? 'panel-pedido-detalle' : 'pedido-detalle',
  params: { idPedido: props.pedido.idPedido },
}))
const chat = computed(() => {
  if (!props.pedido.idConversacion) return null
  const usuario = props.pedido.contraparte?.nombreUsuario
  if (!usuario) return null
  return {
    name: enPanel.value ? 'panel-conversacion' : 'conversacion',
    params: { nombreUsuario: `@${usuario}` },
    query: { volverA: 'pedido', idPedido: props.pedido.idPedido },
  }
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
  if (props.pedido.puedePagar) return 'Pagar cuota'
  if (props.pedido.puedeAprobar) return 'Aprobar entrega'
  if (props.pedido.estado === 'PENDIENTE_PAGO') return 'Esperando pago del cliente'
  return 'Ver pedido'
})
const accionDestacada = computed(() => Boolean(
  props.pedido.puedeEntregar
  || props.pedido.puedePagar
  || props.pedido.puedeAprobar
  || props.pedido.estado === 'IMPAGO',
))
const noLeidos = computed(() => Number(props.pedido.noLeidos || 0))
</script>

<template>
  <div class="rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10">
    <router-link :to="detalle" class="group block">
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

          <div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
            <span v-if="pedido.proximoHitoNombre" class="text-[11px] text-cafe/60">
              <template v-if="pedido.proximoHitoTipo">{{ etiquetaTipoHito(pedido.proximoHitoTipo) }} · </template>{{ pedido.proximoHitoNombre }}
              <span v-if="monto" class="font-semibold text-granate"> · {{ monto }}</span>
            </span>
            <span v-else class="text-[11px] text-cafe/50">Sin hitos pendientes</span>
            <span v-if="pedido.limiteEntrega" class="text-[11px] text-cafe/45">
              Entrega: {{ formatearFecha(pedido.limiteEntrega) }}
            </span>
          </div>
        </div>
      </div>
    </router-link>

    <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
      <router-link
        :to="detalle"
        class="inline-flex min-h-8 items-center gap-1 rounded-full px-3 text-xs font-semibold transition"
        :class="accionDestacada
          ? 'bg-coral text-white hover:bg-rojizo'
          : 'border border-rosa bg-blanco text-cafe/70 hover:bg-rosa/20'"
      >
        {{ accion }}
      </router-link>

      <router-link
        v-if="chat"
        :to="chat"
        class="inline-flex min-h-8 items-center gap-1.5 rounded-full border border-rosa bg-blanco px-3 text-xs font-semibold text-granate transition hover:bg-rosa/20"
      >
        Chat
        <span
          v-if="noLeidos"
          class="inline-flex min-w-4 items-center justify-center rounded-full bg-coral px-1.5 py-0.5 text-[10px] font-bold text-white"
        >
          {{ noLeidos > 9 ? '9+' : noLeidos }}
        </span>
      </router-link>
    </div>
  </div>
</template>
