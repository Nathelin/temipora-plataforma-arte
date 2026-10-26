<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import {
  aFechaISO,
  fechaEntregaEstimada,
  formatearFechaISO,
  formatearFechaSolicitud,
  textoTiempoEntrega,
} from '../servicios/solicitudes'

const props = defineProps({
  presupuesto: { type: Object, required: true },
  estado: { type: String, default: '' },
  esArtista: { type: Boolean, default: false },
})

const hoy = aFechaISO(new Date())
const entrega = computed(() => textoTiempoEntrega(
  props.presupuesto?.tiempoEntregaValor,
  props.presupuesto?.tiempoEntregaUnidad,
))
const inicioEstimado = computed(
  () => props.presupuesto?.fechaInicioEstimada || null,
)
const inicioEstimadoTexto = computed(() => inicioEstimado.value
  ? formatearFechaISO(inicioEstimado.value, { larga: true })
  : 'Al confirmarse el pago')
const entregaSiPagaHoy = computed(() => fechaEntregaEstimada({
  fechaPago: hoy,
  fechaInicioEstimada: inicioEstimado.value,
  valor: props.presupuesto?.tiempoEntregaValor,
  unidad: props.presupuesto?.tiempoEntregaUnidad,
}))
const entregaSiPagaHoyTexto = computed(
  () => formatearFechaISO(entregaSiPagaHoy.value, { larga: true }),
)
const entregaAlVencer = computed(() => {
  if (!props.presupuesto?.fechaVencimiento) return null
  return fechaEntregaEstimada({
    fechaPago: props.presupuesto.fechaVencimiento,
    fechaInicioEstimada: inicioEstimado.value,
    valor: props.presupuesto?.tiempoEntregaValor,
    unidad: props.presupuesto?.tiempoEntregaUnidad,
  })
})
const entregaAlVencerTexto = computed(
  () => formatearFechaISO(entregaAlVencer.value, { larga: true }),
)
const vencido = computed(() => props.estado === 'VENCIDA')
const retirado = computed(() => props.estado === 'RECHAZADA')
const cancelado = computed(() => props.estado === 'CANCELADA')
const vigente = computed(() => props.estado === 'PRESUPUESTADA')
const items = computed(() => props.presupuesto?.items || [])

function formatearMoneda(valor) {
  return formatearPrecio(valor, props.presupuesto?.moneda)
}

function formatearRecargo(recargo) {
  if (recargo?.tipo === 'PORCENTAJE') {
    return `${Number(recargo.valor)}%`
  }
  return formatearMoneda(recargo?.valor)
}
</script>

<template>
  <section
    class="rounded-2xl border bg-blanco p-5 sm:p-6"
    :class="vencido ? 'border-cafe/25' : cancelado ? 'border-rojizo/25' : 'border-coral/30'"
  >
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-coral">Presupuesto</p>
        <h2 class="mt-1 text-xl font-semibold text-granate">
          {{ esArtista ? 'Tu presupuesto' : 'Presupuesto del artista' }}
        </h2>
      </div>
      <span
        v-if="vencido"
        class="rounded-full border border-cafe/25 bg-cafe/10 px-3 py-1 text-xs font-semibold text-cafe"
      >
        Vencido
      </span>
      <span
        v-else-if="retirado"
        class="rounded-full border border-rojizo/25 bg-rojizo/10 px-3 py-1 text-xs font-semibold text-rojizo"
      >
        Retirado
      </span>
      <span
        v-else-if="cancelado"
        class="rounded-full border border-rojizo/25 bg-rojizo/10 px-3 py-1 text-xs font-semibold text-rojizo"
      >
        Sin efecto
      </span>
    </div>

    <p class="mt-4 text-3xl font-bold text-granate">
      {{ formatearMoneda(presupuesto.precioFinal) }}
    </p>
    <p class="mt-1 text-xs text-cafe/50">
      Precio base {{ formatearMoneda(presupuesto.precioBaseTotal) }} + recargos
    </p>

    <dl class="mt-5 grid gap-3 text-sm sm:grid-cols-2">
      <div class="flex items-start justify-between gap-3 rounded-xl bg-rosa/10 px-3 py-2 sm:block">
        <dt class="text-cafe/55">Entrega máxima</dt>
        <dd class="text-right font-semibold text-cafe sm:mt-1 sm:text-left">{{ entrega }}</dd>
      </div>
      <div class="flex items-start justify-between gap-3 rounded-xl bg-rosa/10 px-3 py-2 sm:block">
        <dt class="text-cafe/55">Oferta válida hasta</dt>
        <dd class="text-right font-semibold text-cafe sm:mt-1 sm:text-left">
          {{ presupuesto.fechaVencimiento
            ? formatearFechaSolicitud(presupuesto.fechaVencimiento, { corta: true })
            : '—' }}
        </dd>
      </div>
    </dl>

    <div class="mt-4 rounded-xl border border-coral/20 bg-coral/5 p-4">
      <p class="text-xs font-semibold uppercase tracking-[0.14em] text-coral">
        Fecha de entrega estimada
      </p>
      <dl class="mt-3 space-y-2 text-sm">
        <div class="flex items-start justify-between gap-4">
          <dt class="text-cafe/55">Inicio estimado</dt>
          <dd class="text-right font-medium text-cafe">{{ inicioEstimadoTexto }}</dd>
        </div>
        <div v-if="vigente && entregaSiPagaHoy" class="flex items-start justify-between gap-4">
          <dt class="text-cafe/55">
            {{ esArtista ? 'Si el cliente paga hoy' : 'Si pagás hoy' }}
          </dt>
          <dd class="text-right font-semibold text-granate">{{ entregaSiPagaHoyTexto }}</dd>
        </div>
        <div v-if="vigente && entregaAlVencer" class="flex items-start justify-between gap-4">
          <dt class="text-cafe/55">
            {{ esArtista ? 'Si el cliente paga al vencer' : 'Si pagás al vencer' }}
          </dt>
          <dd class="text-right font-semibold text-granate">{{ entregaAlVencerTexto }}</dd>
        </div>
      </dl>
      <p v-if="vigente" class="mt-3 text-xs leading-relaxed text-cafe/50">
        La fecha se recalcula al confirmarse el pago.
      </p>
    </div>

    <p
      v-if="presupuesto.mensaje"
      class="mt-4 whitespace-pre-line rounded-xl border border-rosa/40 bg-rosa/5 px-3 py-2 text-sm leading-relaxed text-cafe/75"
    >
      {{ presupuesto.mensaje }}
    </p>

    <div class="mt-5 space-y-3">
      <h3 class="text-sm font-semibold text-granate">Desglose por dibujo</h3>

      <article
        v-for="item in items"
        :key="item.idItemPresupuesto"
        class="rounded-xl border border-rosa/40 p-4"
      >
        <div class="flex flex-wrap items-start justify-between gap-2">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">
              Dibujo {{ (item.orden ?? 0) + 1 }}
            </p>
            <p class="mt-0.5 text-sm font-semibold text-granate">{{ item.nombre || 'Dibujo' }}</p>
          </div>
          <p class="text-sm font-semibold text-granate">{{ formatearMoneda(item.precioFinal) }}</p>
        </div>

        <dl class="mt-3 space-y-1.5 text-xs">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-cafe/55">Base</dt>
            <dd class="text-cafe">{{ formatearMoneda(item.precioBase) }}</dd>
          </div>
          <div
            v-for="recargo in item.recargos || []"
            :key="recargo.idRecargo"
            class="flex items-center justify-between gap-3"
          >
            <dt class="text-cafe/55">
              {{ recargo.descripcion }}
              <span class="text-cafe/35">
                ({{ recargo.tipo === 'PORCENTAJE' ? 'porcentaje' : 'fijo' }})
              </span>
            </dt>
            <dd class="text-cafe">+ {{ formatearRecargo(recargo) }}</dd>
          </div>
        </dl>

        <p
          v-if="item.nota"
          class="mt-3 whitespace-pre-line rounded-lg bg-durazno/25 px-3 py-2 text-xs leading-relaxed text-cafe/75"
        >
          {{ item.nota }}
        </p>
      </article>
    </div>

    <div class="mt-5 flex flex-wrap items-center gap-3">
      <template v-if="!esArtista">
        <template v-if="vigente">
          <button
            type="button"
            disabled
            class="inline-flex min-h-10 cursor-not-allowed items-center justify-center rounded-full bg-coral/40 px-5 text-sm font-semibold text-white/70"
          >
            Aceptar y pagar
          </button>
          <p class="text-xs text-cafe/50">El pago va a estar disponible en el próximo paso.</p>
        </template>
        <p v-else-if="vencido" class="text-xs leading-relaxed text-cafe/60">
          El presupuesto venció y ya no se puede pagar.
        </p>
        <p v-else-if="retirado" class="text-xs leading-relaxed text-cafe/60">
          El artista retiró este presupuesto.
        </p>
        <p v-else-if="cancelado" class="text-xs leading-relaxed text-cafe/60">
          Cancelaste esta solicitud; el presupuesto quedó sin efecto.
        </p>
      </template>
    </div>
  </section>
</template>
