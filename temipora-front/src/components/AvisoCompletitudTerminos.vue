<script setup>
import { computed } from 'vue'
import { calcularCompletitudTerminos } from '../servicios/terminos'

const props = defineProps({
  terminos: { type: Object, required: true },
})

const completitud = computed(() => calcularCompletitudTerminos(props.terminos))
</script>

<template>
  <div
    v-if="completitud.tieneContenido"
    class="rounded-2xl border px-4 py-4"
    :class="completitud.requiereAviso ? 'border-coral/40 bg-coral/5' : 'border-green-600/30 bg-green-50'"
  >
    <div class="flex flex-wrap items-center justify-between gap-2">
      <p class="text-sm font-medium" :class="completitud.requiereAviso ? 'text-granate' : 'text-green-800'">
        {{ completitud.requiereAviso ? 'Tus términos pueden estar incompletos' : 'Tus términos tienen la información principal' }}
      </p>
      <span class="text-sm" :class="completitud.requiereAviso ? 'text-coral' : 'text-green-700'">
        {{ completitud.porcentaje }}%
      </span>
    </div>

    <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-rosa/40">
      <div
        class="h-full rounded-full transition-all"
        :class="completitud.requiereAviso ? 'bg-coral' : 'bg-green-600'"
        :style="{ width: `${completitud.porcentaje}%` }"
      ></div>
    </div>

    <template v-if="completitud.requiereAviso">
      <p class="mt-3 text-xs leading-relaxed text-cafe/70">
        Completaste {{ completitud.seccionesCompletas }} de {{ completitud.totalSecciones }} secciones.
        Cuanto más claro sea el acuerdo, menos consultas y conflictos vas a tener.
      </p>
      <p v-if="completitud.faltantesClave.length" class="mt-1.5 text-xs leading-relaxed text-cafe/70">
        Para cubrir lo esencial, completá:
        <span class="text-cafe">{{ completitud.faltantesClave.join(', ') }}</span>.
      </p>
      <p v-if="completitud.faltantes.length" class="mt-1.5 text-xs leading-relaxed text-cafe/60">
        Secciones vacías: {{ completitud.faltantes.join(', ') }}.
      </p>
    </template>
  </div>
</template>
