<script setup>
import { useAvisoActualizacion } from '../composables/useAvisoActualizacion'

const { estado, ocultar } = useAvisoActualizacion()
</script>

<template>
  <div
    v-if="estado.visible"
    class="fixed bottom-4 right-4 z-[70] w-80 max-w-[calc(100vw-2rem)] bg-blanco rounded-xl shadow-xl shadow-granate/20 border border-rosa p-4"
    role="status"
    aria-live="polite"
  >
    <div class="flex items-start gap-3">
      <span
        v-if="estado.fase === 'exito'"
        class="text-green-600 text-xl leading-none shrink-0 mt-0.5"
        aria-hidden="true"
      >
        ✓
      </span>
      <span
        v-else-if="estado.fase === 'error'"
        class="text-rojizo text-xl leading-none shrink-0 mt-0.5"
        aria-hidden="true"
      >
        ⚠
      </span>
      <span
        v-else
        class="inline-block w-5 h-5 border-2 border-coral border-t-transparent rounded-full animate-spin shrink-0 mt-0.5"
        aria-hidden="true"
      ></span>

      <div class="flex-1 min-w-0">
        <p class="text-sm font-medium text-granate">
          <template v-if="estado.fase === 'exito'">Listo</template>
          <template v-else-if="estado.fase === 'error'">No se pudo completar</template>
          <template v-else>{{ estado.titulo }}</template>
        </p>
        <p class="text-xs text-cafe/70 mt-1">
          <template v-if="estado.fase === 'exito'">{{ estado.mensajeExito }}</template>
          <template v-else-if="estado.fase === 'error'">{{ estado.mensajeError }}</template>
          <template v-else>{{ estado.detalle }}</template>
        </p>

        <template v-if="estado.fase === 'subiendo'">
          <div class="mt-2 h-1.5 bg-rosa rounded-full overflow-hidden">
            <div
              class="h-full bg-coral transition-all duration-150"
              :style="{ width: estado.progreso + '%' }"
            ></div>
          </div>
          <p class="text-[11px] text-cafe/60 mt-1">{{ estado.progreso }}%</p>
        </template>
      </div>

      <button
        type="button"
        class="text-cafe/50 hover:text-granate leading-none shrink-0"
        aria-label="Cerrar aviso"
        @click="ocultar"
      >
        ✕
      </button>
    </div>
  </div>
</template>
