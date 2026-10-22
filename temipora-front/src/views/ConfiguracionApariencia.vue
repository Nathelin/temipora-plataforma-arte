<script setup>
import SelectorTema from '../components/SelectorTema.vue'
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <h2 class="text-lg font-bold text-granate">Apariencia</h2>
    <p class="mt-1 text-sm text-cafe/70">
      Elegí cómo querés ver Temipora. La preferencia se guarda en este navegador.
    </p>

    <div class="mt-5 max-w-xl">
      <SelectorTema />
    </div>

    <p class="mt-4 max-w-xl text-xs text-cafe/60">
      Con "Sistema" el tema cambia automáticamente según la configuración de tu dispositivo,
      manteniendo la familia de colores que tengas elegida.
    </p>
  </div>
</template>
