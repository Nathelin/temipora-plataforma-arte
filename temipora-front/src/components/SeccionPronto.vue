<script setup>
defineProps({
  titulo: { type: String, required: true },
})
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8 text-center">
    <h2 class="text-lg text-granate mb-2">{{ titulo }}</h2>
    <p class="text-cafe/50 text-sm">Esta sección está en proceso.</p>
  </div>
</template>