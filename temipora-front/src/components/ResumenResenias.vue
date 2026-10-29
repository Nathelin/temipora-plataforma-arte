<script setup>
import { computed } from 'vue'
import ValoracionEstrellas from './ValoracionEstrellas.vue'
import { formatearPromedio } from '../servicios/resenias'

const props = defineProps({
  resumen: { type: Object, default: null },
  compacto: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
})

const emit = defineEmits(['ver-todas'])

const hayDatos = computed(() => Boolean(
  props.resumen?.hayDatos && Number(props.resumen?.cantidad) > 0,
))
const cantidad = computed(() => Number(props.resumen?.cantidad || 0))
const estrellas = computed(() => Math.round(Number(props.resumen?.promedio || 0)))

function porcentaje(fila) {
  return Math.min(100, Math.max(0, Number(fila?.porcentaje || 0)))
}
</script>

<template>
  <div>
    <div v-if="!hayDatos" class="text-sm text-cafe/55">
      Todavía no hay reseñas.
    </div>

    <div v-else-if="compacto" class="flex flex-wrap items-center justify-between gap-3">
      <div class="min-w-0">
        <div class="flex items-center gap-2">
          <span class="font-display text-3xl font-extrabold text-granate">
            {{ formatearPromedio(resumen.promedio) }}
          </span>
          <ValoracionEstrellas
            :model-value="estrellas"
            readonly
            tamano="sm"
            etiqueta="Promedio de reseñas"
          />
        </div>
        <p class="mt-0.5 text-xs text-cafe/55">
          {{ cantidad }} {{ cantidad === 1 ? 'reseña' : 'reseñas' }}
        </p>
      </div>
      <button
        v-if="clickable"
        type="button"
        class="rounded-full border border-coral/40 px-3 py-1.5 text-xs font-semibold text-coral transition hover:bg-coral/10"
        @click="emit('ver-todas')"
      >
        Ver reseñas
      </button>
    </div>

    <div v-else class="space-y-5">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <div class="flex items-center gap-2">
            <span class="font-display text-4xl font-extrabold text-granate">
              {{ formatearPromedio(resumen.promedio) }}
            </span>
            <ValoracionEstrellas
              :model-value="estrellas"
              readonly
              tamano="md"
              etiqueta="Promedio de reseñas"
            />
          </div>
          <p class="mt-1 text-sm text-cafe/60">
            {{ cantidad }} {{ cantidad === 1 ? 'reseña' : 'reseñas' }}
          </p>
        </div>
        <button
          v-if="clickable"
          type="button"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo"
          @click="emit('ver-todas')"
        >
          Ver las {{ cantidad }} reseñas
        </button>
      </div>

      <div class="space-y-1.5">
        <div
          v-for="fila in resumen.distribucion || []"
          :key="fila.estrellas"
          class="flex items-center gap-2 text-xs"
        >
          <span class="w-9 shrink-0 text-cafe/60">{{ fila.estrellas }} ★</span>
          <div class="h-2 flex-1 overflow-hidden rounded-full bg-rosa/30">
            <div
              class="h-full rounded-full bg-coral"
              :style="{ width: `${porcentaje(fila)}%` }"
            ></div>
          </div>
          <span class="w-7 shrink-0 text-right text-cafe/50">{{ fila.cantidad }}</span>
        </div>
      </div>

      <div v-if="resumen.criterios?.length" class="grid gap-2 sm:grid-cols-2">
        <div
          v-for="criterio in resumen.criterios"
          :key="criterio.idCriterio"
          class="flex items-center justify-between gap-2 rounded-lg bg-rosa/10 px-3 py-2 text-xs"
        >
          <span class="text-cafe/70">{{ criterio.nombre }}</span>
          <span class="font-semibold text-granate">
            {{ formatearPromedio(criterio.promedio) }}
          </span>
        </div>
      </div>

      <div v-if="resumen.destacados?.length" class="flex flex-wrap gap-2">
        <span
          v-for="destacado in resumen.destacados"
          :key="destacado.idOpcion"
          class="rounded-full border px-3 py-1 text-[11px] font-medium"
          :class="destacado.polaridad === 'NEGATIVA'
            ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
            : 'border-rosa bg-blanco text-cafe'"
        >
          {{ destacado.etiqueta }} · {{ Math.round(destacado.porcentaje || 0) }}%
        </span>
      </div>
    </div>
  </div>
</template>
