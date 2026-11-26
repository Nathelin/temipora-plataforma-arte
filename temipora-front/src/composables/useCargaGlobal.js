import { computed, ref } from 'vue'

const RETRASO_MS = 150

let pendientes = 0
let temporizador = null

const activa = ref(false)

function iniciarCarga() {
  pendientes += 1
  if (temporizador || activa.value) return
  temporizador = window.setTimeout(() => {
    temporizador = null
    activa.value = pendientes > 0
  }, RETRASO_MS)
}

function finalizarCarga() {
  pendientes = Math.max(0, pendientes - 1)
  if (pendientes > 0) return
  if (temporizador) {
    window.clearTimeout(temporizador)
    temporizador = null
  }
  activa.value = false
}

export function useCargaGlobal() {
  return {
    activa: computed(() => activa.value),
    iniciarCarga,
    finalizarCarga,
  }
}
