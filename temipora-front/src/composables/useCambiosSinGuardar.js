import { ref } from 'vue'

// Estado global de "cambios sin guardar" para las pantallas de configuración.
// Cada vista marca su estado y el guard del router lo consulta antes de salir.
const hayCambios = ref(false)
const mensaje = ref('Tenés cambios sin guardar. Si salís, se perderán.')

export function useCambiosSinGuardar() {
  function actualizar(pendiente, texto) {
    hayCambios.value = pendiente
    if (texto) mensaje.value = texto
  }

  function limpiar() {
    hayCambios.value = false
  }

  return { hayCambios, mensaje, actualizar, limpiar }
}
