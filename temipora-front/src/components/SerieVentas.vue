<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'

const props = defineProps({
  serie: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
  agrupacion: { type: String, default: 'DIA' },
})

const ANCHO = 760
const ALTO = 240
const MARGEN_SUPERIOR = 16
const MARGEN_INFERIOR = 34
const MARGEN_LATERAL = 12

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

const puntos = computed(() => (props.serie || []).filter(Boolean))
const base = ALTO - MARGEN_INFERIOR
const altoDisponible = base - MARGEN_SUPERIOR

const maximo = computed(() => {
  const valores = puntos.value.map((punto) => Number(punto.netoPercibido) || 0)
  return Math.max(1, ...valores)
})

const hayDatos = computed(() =>
  puntos.value.some((punto) => (Number(punto.netoPercibido) || 0) > 0),
)

const paso = computed(
  () => (ANCHO - MARGEN_LATERAL * 2) / Math.max(puntos.value.length, 1),
)

const anchoBarra = computed(
  () => Math.max(2, Math.min(28, paso.value * 0.62)),
)

const detalleAgrupacion = computed(() => {
  if (props.agrupacion === 'MES') return 'por mes'
  if (props.agrupacion === 'SEMANA') return 'por semana'
  return 'por día'
})

function x(indice) {
  return MARGEN_LATERAL + paso.value * indice + (paso.value - anchoBarra.value) / 2
}

function altura(punto) {
  const valor = Number(punto.netoPercibido) || 0
  if (valor <= 0) return 0
  return Math.max(2, Math.round((valor / maximo.value) * altoDisponible))
}

function y(punto) {
  return base - altura(punto)
}

const etiquetas = computed(() => {
  const total = puntos.value.length
  if (!total) return []
  const cada = Math.max(1, Math.ceil(total / 7))
  return puntos.value
    .map((punto, indice) => ({ periodo: punto.periodo, indice }))
    .filter((_, indice) => indice % cada === 0)
})

function etiquetaDe(periodo) {
  const valor = String(periodo || '')
  const mes = valor.match(/^(\d{4})-(\d{2})$/)
  if (mes) {
    const indice = Number(mes[2]) - 1
    return `${MESES[indice] || mes[2]} ${mes[1].slice(2)}`
  }
  const fecha = valor.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (fecha) return `${fecha[3]}/${fecha[2]}`
  return valor
}
</script>

<template>
  <figure v-if="hayDatos" class="rounded-2xl border border-rosa/60 bg-blanco p-4">
    <figcaption class="flex flex-wrap items-baseline justify-between gap-2">
      <div>
        <p class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">
          Neto percibido {{ detalleAgrupacion }}
        </p>
        <p class="mt-1 text-xs text-cafe/55">
          Pasá el cursor sobre una barra para ver el detalle.
        </p>
      </div>
      <p class="text-xs text-cafe/55">
        Máximo: {{ formatearPrecio(maximo, moneda) }}
      </p>
    </figcaption>

    <svg
      :viewBox="`0 0 ${ANCHO} ${ALTO}`"
      class="mt-3 w-full"
      role="img"
      aria-label="Serie de ventas netas del período"
    >
      <line
        :x1="0"
        :x2="ANCHO"
        :y1="base"
        :y2="base"
        class="text-rosa"
        stroke="currentColor"
        stroke-width="1"
      />
      <g v-for="(punto, indice) in puntos" :key="punto.periodo">
        <rect
          :x="x(indice)"
          :y="y(punto)"
          :width="anchoBarra"
          :height="altura(punto)"
          rx="3"
          class="text-coral"
          fill="currentColor"
          fill-opacity="0.8"
        >
          <title>
            {{ etiquetaDe(punto.periodo) }}: {{ formatearPrecio(punto.netoPercibido, moneda) }}
            · {{ punto.cobros }} cobro(s)
          </title>
        </rect>
      </g>
      <text
        v-for="etiqueta in etiquetas"
        :key="`etiqueta-${etiqueta.periodo}`"
        :x="x(etiqueta.indice) + anchoBarra / 2"
        :y="base + 18"
        text-anchor="middle"
        class="fill-current text-[10px] text-cafe/60"
      >
        {{ etiquetaDe(etiqueta.periodo) }}
      </text>
    </svg>
  </figure>

  <div
    v-else
    class="rounded-2xl border border-dashed border-rosa/70 bg-blanco px-6 py-10 text-center"
  >
    <p class="text-sm text-cafe/60">Todavía no hay cobros en el período.</p>
  </div>
</template>
