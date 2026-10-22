<script setup>
import { useNotificaciones } from '../composables/useNotificaciones'

const { notificaciones, cerrar } = useNotificaciones()
</script>

<template>
  <div
    class="fixed right-4 top-20 z-[100] flex w-[min(22rem,calc(100vw-2rem))] flex-col gap-2"
    aria-live="polite"
    aria-atomic="false"
  >
    <div
      v-for="notificacion in notificaciones"
      :key="notificacion.id"
      class="flex items-start gap-3 rounded-2xl border bg-superficie px-4 py-3 shadow-[0_14px_34px_rgba(74,29,46,0.18)]"
      :class="notificacion.tipo === 'error' ? 'border-rojizo/40' : 'border-green-700/30'"
      :role="notificacion.tipo === 'error' ? 'alert' : 'status'"
    >
      <span
        class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold"
        :class="notificacion.tipo === 'error' ? 'bg-rojizo/10 text-rojizo' : 'bg-green-700/10 text-green-700'"
        aria-hidden="true"
      >
        {{ notificacion.tipo === 'error' ? '!' : '✓' }}
      </span>

      <p class="flex-1 text-sm leading-relaxed text-cafe">{{ notificacion.mensaje }}</p>

      <button
        type="button"
        class="shrink-0 rounded-full p-1 text-lg leading-none text-cafe/50 transition hover:bg-rosa/30 hover:text-granate"
        aria-label="Cerrar notificación"
        @click="cerrar(notificacion.id)"
      >
        ×
      </button>
    </div>
  </div>
</template>
