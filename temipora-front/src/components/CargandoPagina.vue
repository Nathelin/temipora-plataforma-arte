<script setup>
defineProps({
  mensaje: { type: String, default: 'Cargando contenido…' },
  tarjetas: { type: Number, default: 8 },
  // catalogo | detalle | lista | perfil
  tipo: { type: String, default: 'catalogo' },
})
</script>

<template>
  <!-- Reserva el alto de la pantalla para que el footer no quede visible
       mientras llegan los datos y no salte después (CLS). -->
  <div class="min-h-[calc(100vh-4rem)]" role="status" aria-busy="true">
    <div class="animate-pulse" aria-hidden="true">
      <template v-if="tipo === 'perfil'">
        <div class="h-44 w-full rounded-3xl bg-superficie-alt sm:h-64"></div>
        <div class="mt-6 flex items-center gap-4">
          <div class="h-28 w-28 shrink-0 rounded-full bg-superficie-alt"></div>
          <div class="space-y-2">
            <div class="h-6 w-48 max-w-full rounded bg-superficie-alt"></div>
            <div class="h-4 w-32 rounded bg-superficie-alt"></div>
          </div>
        </div>
      </template>

      <template v-else-if="tipo === 'detalle'">
        <div class="h-64 w-full rounded-3xl bg-superficie-alt sm:h-80"></div>
        <div class="mt-6 h-8 w-2/3 max-w-md rounded bg-superficie-alt"></div>
        <div class="mt-3 h-4 w-1/3 max-w-xs rounded bg-superficie-alt"></div>
        <div class="mt-6 space-y-2">
          <div class="h-4 w-full max-w-2xl rounded bg-superficie-alt"></div>
          <div class="h-4 w-5/6 max-w-2xl rounded bg-superficie-alt"></div>
          <div class="h-4 w-3/4 max-w-2xl rounded bg-superficie-alt"></div>
        </div>
      </template>

      <template v-else-if="tipo === 'lista'">
        <div class="h-8 w-56 max-w-full rounded-lg bg-superficie-alt"></div>
        <div class="mt-6 space-y-3">
          <div
            v-for="n in tarjetas"
            :key="n"
            class="flex items-center gap-3 rounded-2xl border border-borde/60 bg-superficie p-4"
          >
            <div class="h-12 w-12 shrink-0 rounded-xl bg-superficie-alt"></div>
            <div class="min-w-0 flex-1 space-y-2">
              <div class="h-4 w-1/2 rounded bg-superficie-alt"></div>
              <div class="h-3 w-1/3 rounded bg-superficie-alt"></div>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="h-8 w-56 max-w-full rounded-lg bg-superficie-alt"></div>
        <div class="mt-3 h-4 w-80 max-w-full rounded bg-superficie-alt"></div>
        <div class="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
          <div
            v-for="n in tarjetas"
            :key="n"
            class="overflow-hidden rounded-2xl border border-borde/60 bg-superficie"
          >
            <div class="aspect-4/3 bg-superficie-alt"></div>
            <div class="space-y-2 p-4">
              <div class="h-4 w-3/4 rounded bg-superficie-alt"></div>
              <div class="h-3 w-1/2 rounded bg-superficie-alt"></div>
            </div>
          </div>
        </div>
      </template>
    </div>
    <span class="sr-only">{{ mensaje }}</span>
  </div>
</template>
