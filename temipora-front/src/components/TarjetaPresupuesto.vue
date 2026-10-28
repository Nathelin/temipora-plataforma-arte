<script setup>
import { computed } from 'vue'
import { formatearMonto, formatearPrecio } from '../servicios/moneda'
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
const hitos = computed(() => props.presupuesto?.hitos || [])
const monedaMostrada = computed(
  () => props.presupuesto?.monedaMostrada || props.presupuesto?.moneda,
)
const totalMostrado = computed(
  () => props.presupuesto?.precioFinalMostrado ?? props.presupuesto?.precioFinal,
)
const baseMostrada = computed(
  () => props.presupuesto?.precioBaseTotalMostrado ?? props.presupuesto?.precioBaseTotal,
)
const cobroDistinto = computed(
  () => props.presupuesto?.precioFinalCobro != null
    && props.presupuesto?.monedaCobro
    && props.presupuesto.monedaCobro !== monedaMostrada.value,
)
const cotizacionAlEmitir = computed(() => {
  const cotizacion = Number(props.presupuesto?.cotizacionCobro)
  if (!Number.isFinite(cotizacion) || cotizacion <= 0) return null
  if (props.presupuesto?.monedaCobro === props.presupuesto?.moneda) return null
  return cotizacion
})
const comisionPorcentaje = computed(() => {
  const valor = Number(props.presupuesto?.comisionPorcentaje)
  return Number.isFinite(valor) ? valor : null
})
const comisionMostrada = computed(
  () => props.presupuesto?.comisionMontoMostrado ?? props.presupuesto?.comisionMonto,
)
const netoMostrado = computed(
  () => props.presupuesto?.montoNetoArtistaMostrado ?? props.presupuesto?.montoNetoArtista,
)
const muestraComision = computed(
  () => comisionPorcentaje.value != null && comisionMostrada.value != null,
)

function formatearMoneda(valor) {
  return formatearPrecio(valor, monedaMostrada.value)
}

function descripcionHito(hito) {
  if (hito.tipo === 'ENTREGA') {
    return 'Se paga contra entrega, al ver el preview protegido'
  }
  if (hito.tipo === 'CUOTA') {
    return 'Se paga después de aprobar el avance anterior'
  }
  return hito.requiereEntrega
    ? 'Se paga al aceptar; incluye un avance para aprobar'
    : 'Se paga al aceptar el presupuesto'
}

function formatearRecargo(recargo) {
  if (recargo?.tipo === 'PORCENTAJE') {
    return `${Number(recargo.valor)}%`
  }
  return formatearMoneda(recargo?.valorMostrado ?? recargo?.valor)
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
      {{ formatearMoneda(totalMostrado) }}
    </p>
    <p class="mt-1 text-xs text-cafe/50">
      Precio base {{ formatearMoneda(baseMostrada) }} + recargos
    </p>
    <p
      v-if="cobroDistinto"
      class="mt-2 text-sm font-semibold text-granate"
    >
      Total a pagar: {{ formatearPrecio(presupuesto.precioFinalCobro, presupuesto.monedaCobro) }}
    </p>
    <p
      v-if="cotizacionAlEmitir"
      class="mt-1 text-xs leading-relaxed text-cafe/50"
    >
      Cotización al emitir: 1 {{ presupuesto.moneda }} =
      {{ formatearMonto(cotizacionAlEmitir) }} {{ presupuesto.monedaCobro }}.
      El importe en {{ presupuesto.monedaCobro }} queda congelado.
    </p>

    <div
      v-if="muestraComision && esArtista"
      class="mt-4 rounded-xl border border-cafe/15 bg-cafe/5 p-4"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
        Comisión de la plataforma
      </p>
      <dl class="mt-2 space-y-1.5 text-sm">
        <div class="flex items-center justify-between gap-3">
          <dt class="text-cafe/60">
            Comisión ({{ formatearMonto(comisionPorcentaje) }}%)
          </dt>
          <dd class="text-cafe">− {{ formatearMoneda(comisionMostrada) }}</dd>
        </div>
        <div
          v-if="esArtista"
          class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5"
        >
          <dt class="font-semibold text-granate">Recibís</dt>
          <dd class="text-base font-bold text-granate">{{ formatearMoneda(netoMostrado) }}</dd>
        </div>
      </dl>
      <p class="mt-2 text-xs leading-relaxed text-cafe/50">
        La comisión se descuenta de tu precio. El cliente paga el total sin cambios.
      </p>
    </div>

    <div v-if="hitos.length" class="mt-4 rounded-xl border border-rosa/40 p-4">
      <h3 class="text-sm font-semibold text-granate">Plan de pagos</h3>
      <ol class="mt-3 space-y-2">
        <li
          v-for="hito in hitos"
          :key="hito.idHitoPresupuesto || hito.orden"
          class="flex items-start justify-between gap-3 rounded-lg bg-rosa/10 px-3 py-2"
        >
          <div class="min-w-0">
            <p class="text-sm font-medium text-cafe">
              {{ hito.nombre }}
              <span class="text-xs text-cafe/45">
                ({{ formatearMonto(hito.porcentaje) }}%)
              </span>
            </p>
            <p class="text-xs text-cafe/50">
              {{ descripcionHito(hito) }}
            </p>
          </div>
          <div class="shrink-0 text-right">
            <p class="text-sm font-semibold text-granate">
              {{ formatearMoneda(hito.montoMostrado ?? hito.monto) }}
            </p>
            <p v-if="esArtista" class="text-xs text-cafe/50">
              Recibís {{ formatearMoneda(hito.montoNetoMostrado ?? hito.montoNeto) }}
            </p>
          </div>
        </li>
      </ol>
      <p class="mt-3 text-xs leading-relaxed text-cafe/50">
        La primera cuota se paga al aceptar el presupuesto. Cada cuota siguiente se habilita
        cuando el cliente aprueba el entregable anterior.
      </p>
    </div>

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
          <p class="text-sm font-semibold text-granate">{{ formatearMoneda(item.precioFinalMostrado ?? item.precioFinal) }}</p>
        </div>

        <dl class="mt-3 space-y-1.5 text-xs">
          <div class="flex items-center justify-between gap-3">
            <dt class="text-cafe/55">Base</dt>
            <dd class="text-cafe">{{ formatearMoneda(item.precioBaseMostrado ?? item.precioBase) }}</dd>
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
