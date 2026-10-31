<script setup>
import { computed } from 'vue'
import ValoracionEstrellas from './ValoracionEstrellas.vue'
import { formatearPromedio } from '../servicios/resenias'

const props = defineProps({
  resumen: { type: Object, default: null },
  compacto: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
  filtrable: { type: Boolean, default: false },
})

const emit = defineEmits(['ver-todas', 'filtrar-opcion'])

const MINIMO_RESENIAS_DESTACADOS = 10
const MINIMO_PORCENTAJE_DESTACADO = 50
const MAX_DESTACADOS = 2

const hayDatos = computed(() => Boolean(
  props.resumen?.hayDatos && Number(props.resumen?.cantidad) > 0,
))
const cantidad = computed(() => Number(props.resumen?.cantidad || 0))
const estrellas = computed(() => Math.round(Number(props.resumen?.promedio || 0)))

const destacadosVisibles = computed(() => {
  if (cantidad.value < MINIMO_RESENIAS_DESTACADOS) return []
  return (props.resumen?.destacados || [])
    .filter((destacado) => Number(destacado?.porcentaje || 0) >= MINIMO_PORCENTAJE_DESTACADO)
    .slice(0, MAX_DESTACADOS)
})

function porcentaje(fila) {
  return Math.min(100, Math.max(0, Number(fila?.porcentaje || 0)))
}

function prefijoDestacado(destacado) {
  if (destacado?.polaridad === 'NEGATIVA') return 'Varios usuarios señalan'
  if (destacado?.polaridad === 'POSITIVA') return 'Muchos usuarios resaltan'
  return 'Varios usuarios mencionan'
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

      <div v-if="resumen.criterios?.length" class="space-y-1.5">
        <div
          v-for="criterio in resumen.criterios"
          :key="criterio.idCriterio"
          class="flex items-center justify-between gap-2 rounded-lg bg-rosa/10 px-3 py-1.5 text-xs"
        >
          <span class="text-cafe/70">{{ criterio.nombre }}</span>
          <span class="flex shrink-0 items-center gap-1 font-semibold text-granate">
            <span class="text-coral" aria-hidden="true">★</span>
            {{ formatearPromedio(criterio.promedio) }}
          </span>
        </div>
      </div>

      <div v-if="destacadosVisibles.length" class="space-y-1.5 border-t border-rosa/30 pt-3">
        <p class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">
          Lo que más se repite
        </p>
        <p
          v-for="destacado in destacadosVisibles"
          :key="destacado.idOpcion"
          class="text-sm leading-relaxed text-cafe/80"
        >
          <button
            v-if="filtrable"
            type="button"
            class="text-left transition hover:text-coral focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50"
            @click="emit('filtrar-opcion', destacado)"
          >
            {{ prefijoDestacado(destacado) }}
            <span class="font-semibold text-granate underline decoration-coral/40 underline-offset-4">
              “{{ destacado.etiqueta }}”
            </span>.
          </button>
          <span v-else>
            {{ prefijoDestacado(destacado) }}
            <span class="font-semibold text-granate">“{{ destacado.etiqueta }}”</span>.
          </span>
        </p>
      </div>
    </div>
  </div>
</template>
