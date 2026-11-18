<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'

const props = defineProps({
  serie: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
})

const ANCHO = 760
const ALTO = 250
const MARGEN_SUPERIOR = 20
const MARGEN_INFERIOR = 34
const MARGEN_LATERAL = 12
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

const puntos = computed(() => (props.serie || []).filter(Boolean))
const base = ALTO - MARGEN_INFERIOR
const altoDisponible = base - MARGEN_SUPERIOR

const hayDatos = computed(() =>
  puntos.value.some(
    (punto) =>
      (Number(punto.volumen) || 0) > 0 || (Number(punto.comision) || 0) > 0,
  ),
)

const maxVolumen = computed(() =>
  Math.max(1, ...puntos.value.map((punto) => Number(punto.volumen) || 0)),
)
const maxComision = computed(() =>
  Math.max(1, ...puntos.value.map((punto) => Number(punto.comision) || 0)),
)

const paso = computed(
  () => (ANCHO - MARGEN_LATERAL * 2) / Math.max(puntos.value.length, 1),
)
const anchoBarra = computed(
  () => Math.max(2, Math.min(26, paso.value * 0.55)),
)

function x(indice) {
  return MARGEN_LATERAL + paso.value * indice + (paso.value - anchoBarra.value) / 2
}

function centro(indice) {
  return x(indice) + anchoBarra.value / 2
}

function altura(punto) {
  const valor = Number(punto.volumen) || 0
  if (valor <= 0) return 0
  return Math.max(2, Math.round((valor / maxVolumen.value) * altoDisponible))
}

function yVolumen(punto) {
  return base - altura(punto)
}

function yComision(punto) {
  const valor = Number(punto.comision) || 0
  return base - (valor / maxComision.value) * altoDisponible
}

const linea = computed(() =>
  puntos.value
    .map((punto, indice) =>
      `${indice === 0 ? 'M' : 'L'} ${centro(indice)} ${yComision(punto)}`,
    )
    .join(' '),
)

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
          Volumen y comisión por período
        </p>
        <p class="mt-1 text-xs text-cafe/55">
          Barras: volumen procesado. Línea: ingresos de la plataforma (escala propia).
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-3 text-xs text-cafe/60">
        <span class="flex items-center gap-1.5">
          <span class="h-2.5 w-2.5 rounded-sm bg-coral/50" aria-hidden="true"></span>
          Volumen
        </span>
        <span class="flex items-center gap-1.5">
          <span class="h-0.5 w-4 rounded-full bg-granate" aria-hidden="true"></span>
          Comisión
        </span>
      </div>
    </figcaption>

    <svg
      :viewBox="`0 0 ${ANCHO} ${ALTO}`"
      class="mt-3 w-full"
      role="img"
      aria-label="Serie de volumen y comisión del período"
    >
      <line
        v-for="parte in [1, 2, 3]"
        :key="`guia-${parte}`"
        :x1="0"
        :x2="ANCHO"
        :y1="MARGEN_SUPERIOR + (altoDisponible / 4) * parte"
        :y2="MARGEN_SUPERIOR + (altoDisponible / 4) * parte"
        class="text-rosa"
        stroke="currentColor"
        stroke-opacity="0.35"
        stroke-width="1"
      />
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
          :y="yVolumen(punto)"
          :width="anchoBarra"
          :height="altura(punto)"
          rx="3"
          class="text-coral"
          fill="currentColor"
          fill-opacity="0.45"
        >
          <title>
            {{ etiquetaDe(punto.periodo) }}: volumen
            {{ formatearPrecio(punto.volumen, moneda) }}
          </title>
        </rect>
      </g>

      <path
        :d="linea"
        fill="none"
        class="text-granate"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
        stroke-linecap="round"
      />
      <g v-for="(punto, indice) in puntos" :key="`punto-${punto.periodo}`">
        <circle
          :cx="centro(indice)"
          :cy="yComision(punto)"
          r="2.5"
          class="text-granate"
          fill="currentColor"
        >
          <title>
            {{ etiquetaDe(punto.periodo) }}: comisión
            {{ formatearPrecio(punto.comision, moneda) }}
          </title>
        </circle>
      </g>

      <text
        v-for="etiqueta in etiquetas"
        :key="`etiqueta-${etiqueta.periodo}`"
        :x="centro(etiqueta.indice)"
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
    <p class="text-sm text-cafe/60">Todavía no hay ventas en el período.</p>
  </div>
</template>
