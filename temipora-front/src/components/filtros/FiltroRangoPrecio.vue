<script setup>
import { computed } from 'vue'
import { formatearMonto, parsearMonto } from '../../servicios/moneda'

const props = defineProps({
  precioMin: { type: [String, Number], default: '' },
  precioMax: { type: [String, Number], default: '' },
  moneda: { type: String, default: 'ARS' },
})

const emit = defineEmits(['cambiar'])

const sugerencias = [0, 5000, 10000, 25000, 50000, 100000]

const activo = computed(() => props.precioMin !== '' || props.precioMax !== '')

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
      En {{ moneda }}, convirtiendo los precios declarados en otra moneda.
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
