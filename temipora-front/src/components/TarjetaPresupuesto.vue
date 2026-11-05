<script setup>
import { computed, ref } from 'vue'
import { formatearMonto, formatearPrecio } from '../servicios/moneda'
import { descripcionHito } from '../servicios/pedidos'
import BurbujaInfo from './BurbujaInfo.vue'
import ModalComoSeCalcula from './ModalComoSeCalcula.vue'
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

const modalCalculoAbierto = ref(false)

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
const totalMostrado = computed(() => {
  const presupuesto = props.presupuesto
  if (!presupuesto) return null
  if (props.esArtista) {
    return presupuesto.precioFinalMostrado ?? presupuesto.precioFinal
  }
  return presupuesto.precioFinalClienteMostrado
    ?? presupuesto.precioFinalMostrado
    ?? presupuesto.precioFinal
})
const totalCobroMostrado = computed(() => {
  const presupuesto = props.presupuesto
  if (!presupuesto) return null
  return props.esArtista
    ? presupuesto.precioFinalCobro
    : (presupuesto.precioFinalClienteCobro ?? presupuesto.precioFinalCobro)
})
const descuentoPorcentaje = computed(() => {
  const valor = Number(props.presupuesto?.descuentoPorcentaje)
  return Number.isFinite(valor) ? valor : 0
})
const descuentoMostrado = computed(
  () => props.presupuesto?.descuentoMostrado
    ?? props.presupuesto?.descuentoMonto
    ?? 0,
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
const netoMostrado = computed(
  () => props.presupuesto?.montoNetoArtistaMostrado ?? props.presupuesto?.montoNetoArtista,
)
const comisionNominalMostrada = computed(() => {
  const directo = props.presupuesto?.comisionNominalMostrado
    ?? props.presupuesto?.comisionNominal
  if (directo != null) return directo
  const precio = Number(
    props.presupuesto?.precioFinalMostrado ?? props.presupuesto?.precioFinal,
  )
  const neto = Number(
    props.presupuesto?.montoNetoArtistaMostrado ?? props.presupuesto?.montoNetoArtista,
  )
  if (!Number.isFinite(precio) || !Number.isFinite(neto)) return null
  return Math.round((precio - neto) * 100) / 100
})
const muestraComision = computed(
  () => comisionPorcentaje.value != null && comisionNominalMostrada.value != null,
)
const tieneDescuento = computed(() => Number(descuentoMostrado.value) > 0)
const sinComision = computed(
  () => comisionNominalMostrada.value != null
    && Number(comisionNominalMostrada.value) <= 0,
)
const precioOriginalMostrado = computed(
  () => props.presupuesto?.precioFinalMostrado
    ?? props.presupuesto?.precioFinal
    ?? null,
)
const muestraPrecioOriginal = computed(() => (
  !props.esArtista
  && Number(descuentoMostrado.value) > 0
  && precioOriginalMostrado.value != null
  && Number(precioOriginalMostrado.value) > Number(totalMostrado.value)
))
const arancelMpPorcentaje = computed(() => {
  const valor = Number(props.presupuesto?.arancelMpPorcentaje)
  return Number.isFinite(valor) ? valor : null
})
const arancelMpEstimado = computed(
  () => props.presupuesto?.arancelMpEstimadoMostrado
    ?? props.presupuesto?.arancelMpEstimado
    ?? null,
)
const netoConArancel = computed(
  () => props.presupuesto?.montoNetoArtistaConArancelMostrado
    ?? props.presupuesto?.montoNetoArtistaConArancel
    ?? netoMostrado.value,
)
const muestraArancelMp = computed(
  () => arancelMpPorcentaje.value != null && arancelMpEstimado.value != null,
)

function formatearMoneda(valor) {
  return formatearPrecio(valor, monedaMostrada.value)
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
          {{ esArtista ? 'Tu propuesta' : 'Propuesta del artista' }}
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

    <p class="mt-4 text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
      {{ esArtista ? 'Precio' : 'Total a pagar' }}
    </p>
    <div class="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
      <p class="text-3xl font-bold text-granate">
        {{ formatearMoneda(totalMostrado) }}
      </p>
      <p
        v-if="muestraPrecioOriginal"
        class="text-lg font-semibold text-cafe/45 line-through decoration-cafe/50"
      >
        <span class="sr-only">Precio sin descuento: </span>
        {{ formatearMoneda(precioOriginalMostrado) }}
      </p>
    </div>
    <p class="mt-1 text-xs text-cafe/50">
      Precio base {{ formatearMoneda(baseMostrada) }} + recargos
    </p>
    <p
      v-if="!esArtista && Number(descuentoMostrado) > 0"
      class="mt-2 inline-flex items-center gap-2 rounded-full bg-mantequilla px-3 py-1 text-xs font-bold text-tinta"
    >
      Descuento de reactivación ({{ descuentoPorcentaje }}%):
      − {{ formatearMoneda(descuentoMostrado) }}
    </p>
    <p
      v-if="cobroDistinto"
      class="mt-2 text-sm font-semibold text-granate"
    >
      Total a pagar: {{ formatearPrecio(totalCobroMostrado, presupuesto.monedaCobro) }}
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
      <div class="flex flex-wrap items-center justify-between gap-2">
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
          Tu liquidación
        </p>
        <button
          type="button"
          class="text-xs font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
          @click="modalCalculoAbierto = true"
        >
          ¿Cómo se calcula?
        </button>
      </div>
      <dl class="mt-2 space-y-1.5 text-sm">
        <div class="flex items-center justify-between gap-3">
          <dt class="text-cafe/60">Precio</dt>
          <dd class="text-cafe">{{ formatearMoneda(totalMostrado) }}</dd>
        </div>
        <div v-if="!sinComision" class="flex items-center justify-between gap-3">
          <dt class="text-cafe/60">
            <span class="inline-flex items-center gap-1.5">
              Comisión Temipora ({{ formatearMonto(comisionPorcentaje) }}%)
              <BurbujaInfo
                v-if="tieneDescuento"
                tono="descuento"
                etiqueta="Descuento financiado por Temipora"
              >
                Este cliente aplicó un descuento de reactivación del
                {{ formatearMonto(descuentoPorcentaje) }}%
                ({{ formatearMoneda(descuentoMostrado) }}).
                El cliente paga
                {{ formatearMoneda(presupuesto.precioFinalClienteMostrado
                  ?? presupuesto.precioFinalCliente) }}
                (descuento financiado por Temipora) y tu liquidación no cambia.
              </BurbujaInfo>
            </span>
          </dt>
          <dd class="text-cafe">− {{ formatearMoneda(comisionNominalMostrada) }}</dd>
        </div>
        <div v-else class="flex items-center justify-between gap-3">
          <dt class="text-cafe/60">Comisión Temipora</dt>
          <dd class="text-cafe">Sin comisión en esta operación</dd>
        </div>
        <div class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5">
          <dt class="font-semibold text-granate">Tu neto</dt>
          <dd class="font-semibold text-granate">{{ formatearMoneda(netoMostrado) }}</dd>
        </div>
        <div v-if="muestraArancelMp" class="flex items-center justify-between gap-3">
          <dt class="text-cafe/60">
            Arancel MercadoPago (estimado) ({{ formatearMonto(arancelMpPorcentaje) }}%)
          </dt>
          <dd class="text-cafe">− {{ formatearMoneda(arancelMpEstimado) }}</dd>
        </div>
        <div class="flex items-center justify-between gap-3 border-t border-cafe/10 pt-1.5">
          <dt class="font-semibold text-granate">Recibís (estimado)</dt>
          <dd class="text-base font-bold text-granate">{{ formatearMoneda(netoConArancel) }}</dd>
        </div>
      </dl>
      <p v-if="!tieneDescuento" class="mt-2 text-xs leading-relaxed text-cafe/50">
        La comisión se descuenta de tu precio. El cliente paga el total sin cambios.
      </p>
      <p v-if="muestraArancelMp" class="mt-2 text-xs leading-relaxed text-cafe/50">
        El arancel de MercadoPago es una estimación y puede variar según el medio de pago
        y las cuotas.
      </p>
    </div>

    <div v-if="hitos.length" class="mt-4 rounded-xl border border-rosa/40 p-4">
      <div class="flex items-center gap-1.5">
        <h3 class="text-sm font-semibold text-granate">Plan de pagos</h3>
        <BurbujaInfo etiqueta="Cómo se prorratean los montos por hito">
          La comisión Temipora y el arancel de MercadoPago se prorratean según
          el porcentaje de cada hito. El último hito absorbe el redondeo.
        </BurbujaInfo>
      </div>
      <div
        v-if="esArtista && tieneDescuento"
        class="mt-3 rounded-xl border border-coral/60 bg-granate px-3 py-2 text-xs leading-relaxed text-white"
      >
        El plan de pagos suma
        {{ formatearMoneda(presupuesto.precioFinalClienteMostrado
          ?? presupuesto.precioFinalCliente) }},
        no el precio de {{ formatearMoneda(totalMostrado) }}, porque incluye el
        descuento de reactivación del {{ formatearMonto(descuentoPorcentaje) }}%
        ({{ formatearMoneda(descuentoMostrado) }}), financiado por Temipora.
        Tu liquidación no cambia.
      </div>
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
            <p v-if="hito.descripcion" class="text-xs leading-relaxed text-cafe/65">
              {{ hito.descripcion }}
            </p>
            <p class="text-xs text-cafe/50">
              {{ descripcionHito(hito) }}
            </p>
          </div>
          <div class="shrink-0 text-right">
            <p v-if="esArtista" class="text-[11px] uppercase tracking-wide text-cafe/45">
              El cliente paga
            </p>
            <p class="text-sm font-semibold text-granate">
              {{ formatearMoneda(hito.montoMostrado ?? hito.monto) }}
            </p>
            
            <p v-if="esArtista" class="text-xs text-cafe/50">
              Recibís est. {{ formatearMoneda(
                hito.montoNetoConArancelMostrado
                  ?? hito.montoNetoMostrado
                  ?? hito.montoNeto,
              ) }}
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
              <span v-if="recargo.automatico" class="ml-1 rounded-full bg-coral/10 px-2 py-0.5 text-[10px] font-semibold text-coral">
                Automático
              </span>
              <span v-else class="text-cafe/35">
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
        <p v-if="vencido" class="text-xs leading-relaxed text-cafe/60">
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

    <ModalComoSeCalcula
      :abierto="modalCalculoAbierto"
      contexto="presupuesto"
      :moneda="monedaMostrada || presupuesto.moneda || 'ARS'"
      :precio="totalMostrado"
      :comision-porcentaje="comisionPorcentaje"
      :comision-monto="comisionNominalMostrada"
      :neto="netoMostrado"
      :arancel-porcentaje="arancelMpPorcentaje"
      :arancel-monto="arancelMpEstimado"
      :recibis="netoConArancel"
      :descuento-porcentaje="descuentoPorcentaje"
      :descuento-monto="descuentoMostrado"
      :cliente-paga="presupuesto.precioFinalClienteMostrado
        ?? presupuesto.precioFinalCliente"
      estimado
      @cerrar="modalCalculoAbierto = false"
    />
  </section>
</template>
