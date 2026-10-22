<script setup>
defineProps({
  preguntas: { type: Array, default: () => [] },
  mostrarAviso: { type: Boolean, default: true },
  mostrarEnviar: { type: Boolean, default: false },
})
</script>

<template>
  <div class="space-y-5">
    <p v-if="mostrarAviso" class="text-xs text-cafe/50">
      Así lo verá el cliente. Es una vista previa: no se envían respuestas.
    </p>

    <p
      v-if="!preguntas.length"
      class="rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center text-sm text-cafe/50"
    >
      Todavía no hay preguntas para previsualizar.
    </p>

    <div v-for="(p, i) in preguntas" :key="p.uid || i" class="rounded-xl border border-rosa/50 bg-blanco p-4">
      <template v-if="p.codigoTipo === 'ENCABEZADO'">
        <h3 class="text-base font-medium text-granate">{{ p.enunciado }}</h3>
        <p v-if="p.ayuda" class="mt-1 text-sm text-cafe/60">{{ p.ayuda }}</p>
      </template>

      <template v-else>
        <label class="block text-sm text-cafe mb-1">
          {{ p.enunciado }}<span v-if="p.obligatoria" class="text-rojizo"> *</span>
        </label>
        <p v-if="p.ayuda" class="text-xs text-cafe/60 mb-2">{{ p.ayuda }}</p>

        <textarea
          v-if="p.codigoTipo === 'TEXTO'"
          rows="3"
          placeholder="Escribí tu respuesta..."
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>

        <div v-else-if="p.codigoTipo === 'CASILLAS'" class="space-y-2">
          <label v-for="(o, oi) in p.opciones" :key="oi" class="flex items-center gap-2 text-sm text-cafe">
            <input type="checkbox" class="accent-coral" />
            <span>{{ o }}</span>
          </label>
        </div>

        <div v-else-if="p.codigoTipo === 'OPCIONES'" class="space-y-2">
          <label v-for="(o, oi) in p.opciones" :key="oi" class="flex items-center gap-2 text-sm pb-1 pt-1 text-cafe">
            <input type="radio" :name="'preview-' + (p.uid || i)" class="accent-coral" />
            <span>{{ o }}</span>
          </label>
        </div>

        <div
          v-else-if="p.codigoTipo === 'SUBIR_IMAGEN'"
          class="flex flex-col items-center justify-center rounded-xl border border-dashed border-rosa px-4 py-6 text-center text-sm text-cafe/60"
        >
          <svg class="h-6 w-6 mb-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 7.5m0 0L7.5 12M12 7.5V21" />
          </svg>
          <span>Subir imagen</span>
        </div>
      </template>
    </div>

    <button
      v-if="mostrarEnviar"
      type="button"
      disabled
      class="w-full cursor-not-allowed rounded-lg bg-coral px-4 py-2.5 text-sm text-white opacity-70"
    >
      Enviar solicitud
    </button>
  </div>
</template>
