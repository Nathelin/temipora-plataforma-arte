<script setup>
import { computed } from 'vue'
import { formatearMonto, parsearMonto } from '../../servicios/moneda'

const props = defineProps({
  precioMin: { type: [String, Number], default: '' },
  precioMax: { type: [String, Number], default: '' },
  moneda: { type: String, default: 'ARS' },
})

const emit = defineEmits(['cambiar'])

// Los montos sugeridos dependen de la escala de la moneda: 100.000 tiene
// sentido en ARS y ninguno en USD.
const sugerenciasPorMoneda = {
  ARS: [0, 5000, 10000, 25000, 50000, 100000],
  USD: [0, 5, 10, 25, 50, 100],
}

const sugerencias = computed(
  () => sugerenciasPorMoneda[props.moneda] || sugerenciasPorMoneda.USD,
)

const activo = computed(() => props.precioMin !== '' || props.precioMax !== '')

// Un mínimo mayor que el máximo deja la lista vacía sin explicar por qué.
const invertido = computed(() => {
  if (props.precioMin === '' || props.precioMax === '') return false
  const min = Number(props.precioMin)
  const max = Number(props.precioMax)
  return Number.isFinite(min) && Number.isFinite(max) && min > max
})

function alEscribir(clave, evento) {
  const valor = parsearMonto(evento.target.value)
  emit('cambiar', {
    precioMin: clave === 'precioMin' ? (valor == null ? '' : String(valor)) : props.precioMin,
    precioMax: clave === 'precioMax' ? (valor == null ? '' : String(valor)) : props.precioMax,
  })
}

function aplicarSugerencia(valor) {
  emit('cambiar', {
    precioMin: valor === '' ? '' : String(valor),
    precioMax: props.precioMax,
  })
}

function limpiar() {
  emit('cambiar', { precioMin: '', precioMax: '' })
}
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center gap-2">
      <label class="flex-1">
        <span class="mb-1 block text-[11px] font-semibold uppercase tracking-[0.1em] text-cafe/50">Desde</span>
        <input
          :value="precioMin === '' ? '' : formatearMonto(precioMin)"
          type="text"
          inputmode="decimal"
          placeholder="Sin mínimo"
          class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          @change="alEscribir('precioMin', $event)"
        />
      </label>
      <label class="flex-1">
        <span class="mb-1 block text-[11px] font-semibold uppercase tracking-[0.1em] text-cafe/50">Hasta</span>
        <input
          :value="precioMax === '' ? '' : formatearMonto(precioMax)"
          type="text"
          inputmode="decimal"
          placeholder="Sin máximo"
          class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          @change="alEscribir('precioMax', $event)"
        />
      </label>
    </div>

    <p class="text-[11px] leading-relaxed text-cafe/50">
      Precio en <strong class="font-semibold text-cafe/65">{{ moneda }}</strong>,
      convirtiendo los servicios publicados en otra moneda.
    </p>

    <p
      v-if="invertido"
      role="alert"
      class="rounded-lg border border-rojizo/30 bg-rojizo/10 px-2.5 py-1.5 text-[11px] font-medium leading-relaxed text-rojizo"
    >
      El mínimo es mayor que el máximo: no vas a obtener resultados.
    </p>

    <div class="flex flex-wrap gap-1.5">
      <button
        v-for="sugerencia in sugerencias"
        :key="sugerencia"
        type="button"
        class="rounded-full border border-borde/60 px-2.5 py-1 text-[11px] font-semibold text-cafe/70 transition hover:border-coral/40 hover:text-granate"
        :class="String(precioMin) === String(sugerencia) ? 'border-coral bg-coral/10 text-granate' : ''"
        @click="aplicarSugerencia(sugerencia)"
      >
        {{ sugerencia === 0 ? 'Cualquiera' : `+${formatearMonto(sugerencia)}` }}
      </button>
    </div>

    <button
      v-if="activo"
      type="button"
      class="text-[11px] font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
      @click="limpiar"
    >
      Quitar rango
    </button>
  </div>
</template>
