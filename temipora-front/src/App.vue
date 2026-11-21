<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import AvisoActualizacion from './components/AvisoActualizacion.vue'
import ConfirmacionSalida from './components/ConfirmacionSalida.vue'
import NotificacionToast from './components/NotificacionToast.vue'
import { useNotificacionesSistema } from './composables/useNotificacionesSistema'
import { usePrecargaRutas } from './composables/usePrecargaRutas'

const router = useRouter()
const { detenerPolling, limpiar } = useNotificacionesSistema()

usePrecargaRutas()

function alInhabilitar() {
  detenerPolling()
  limpiar()
  if (router.currentRoute.value.name !== 'login') {
    router.push({ name: 'login', query: { inhabilitada: 1 } })
  }
}

onMounted(() => {
  window.addEventListener('cuenta-inhabilitada', alInhabilitar)
})

onBeforeUnmount(() => {
  window.removeEventListener('cuenta-inhabilitada', alInhabilitar)
})
</script>

<template>
  <router-view />
  <AvisoActualizacion />
  <ConfirmacionSalida />
  <NotificacionToast />
</template>
