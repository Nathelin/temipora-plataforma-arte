<script setup>
import AdminPageHeader from '../components/admin/AdminPageHeader.vue'
import SancionPruebasAdmin from '../components/SancionPruebasAdmin.vue'
</script>

<template>
  <main class="mx-auto w-full px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Comunidad"
      titulo="Pruebas de sanciones"
      descripcion="Prepará el Nivel 1, forzá el Nivel 2, simulá la aprobación del cliente y revertí el estado de prueba sin esperar a los jobs."
    />

    <AdminTabs />

    <SancionPruebasAdmin />
  </main>
</template>
