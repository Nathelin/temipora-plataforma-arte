<script setup>
import { useTema } from '../composables/useTema'

defineProps({
  compacto: { type: Boolean, default: false },
  conSistema: { type: Boolean, default: true },
})

const emit = defineEmits(['seleccionar'])

const { tema, esSistema, temas, aplicarTema, usarSistema } = useTema()

function elegir(id) {
  aplicarTema(id)
  emit('seleccionar', id)
}

function elegirSistema() {
  usarSistema()
  emit('seleccionar', 'sistema')
}
</script>

<template>
  <div role="group" aria-label="Tema de la interfaz">
    <div class="grid gap-2" :class="compacto ? 'grid-cols-2' : 'grid-cols-1 sm:grid-cols-2'">
      <button
        v-for="t in temas"
        :key="t.id"
        type="button"
        class="group relative overflow-hidden rounded-xl border-2 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-acento/60"
        :class="[
          compacto ? 'p-1.5' : 'p-2.5',
          tema === t.id && !esSistema
            ? 'border-acento bg-acento/10'
            : 'border-borde/50 hover:border-acento/40 hover:bg-superficie-alt/60',
        ]"
        :aria-pressed="tema === t.id && !esSistema"
        @click="elegir(t.id)"
      >
        <span
          class="flex w-full overflow-hidden rounded-lg border border-black/10"
          :class="compacto ? 'h-8' : 'h-12'"
          aria-hidden="true"
        >
          <span class="flex-1" :style="{ backgroundColor: t.muestra.fondo }"></span>
          <span class="w-1/3" :style="{ backgroundColor: t.muestra.superficie }"></span>
          <span class="w-1/4" :style="{ backgroundColor: t.muestra.acento }"></span>
        </span>

        <span class="mt-1.5 flex items-center justify-between gap-2">
          <span
            class="font-semibold text-texto"
            :class="compacto ? 'text-xs' : 'text-sm'"
          >
            {{ t.nombre }}
          </span>
          <svg
            v-if="tema === t.id && !esSistema"
            class="h-4 w-4 shrink-0 text-acento"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="m5 12 4 4L19 6" />
          </svg>
        </span>
      </button>
    </div>

    <button
      v-if="conSistema"
      type="button"
      class="mt-2 flex w-full items-center gap-2 rounded-xl border-2 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-acento/60"
      :class="[
        compacto ? 'px-2 py-2' : 'px-3 py-3',
        esSistema
          ? 'border-acento bg-acento/10'
          : 'border-borde/50 hover:border-acento/40 hover:bg-superficie-alt/60',
      ]"
      :aria-pressed="esSistema"
      @click="elegirSistema"
    >
      <svg
        class="h-4 w-4 shrink-0 text-texto-suave"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <rect x="3" y="4" width="18" height="12" rx="2" />
        <path d="M8 20h8M12 16v4" />
      </svg>
      <span class="flex flex-1 flex-col">
        <span class="font-semibold text-texto" :class="compacto ? 'text-xs' : 'text-sm'">
          Sistema
        </span>
        <span class="text-[11px] text-texto-suave">Sigue la configuración del dispositivo</span>
      </span>
      <svg
        v-if="esSistema"
        class="h-4 w-4 shrink-0 text-acento"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="m5 12 4 4L19 6" />
      </svg>
    </button>
  </div>
</template>
