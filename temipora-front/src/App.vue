<script setup>
import AvisoActualizacion from './components/AvisoActualizacion.vue'
import ConfirmacionSalida from './components/ConfirmacionSalida.vue'
import NotificacionToast from './components/NotificacionToast.vue'
</script>

<template>
  <router-view />
  <AvisoActualizacion />
  <ConfirmacionSalida />
  <NotificacionToast />
</template>
