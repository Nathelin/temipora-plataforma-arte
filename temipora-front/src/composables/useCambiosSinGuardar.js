import { ref } from 'vue'

// Estado global de "cambios sin guardar" para las pantallas de configuración.
// Cada vista marca su estado y el guard del router lo consulta antes de salir.
const hayCambios = ref(false)
const mensaje = ref('Tenés cambios sin guardar. Si salís, se perderán.')

// Acción alternativa opcional que ofrece la pantalla que bloquea la salida.
// El alta de producto la usa para ofrecer "Guardar borrador y salir".
const textoAlternativo = ref('')
let accionAlternativa = null

export function useCambiosSinGuardar() {
  function actualizar(pendiente, texto) {
    hayCambios.value = pendiente
    if (texto) mensaje.value = texto
  }

  function limpiar() {
    hayCambios.value = false
  }

  function ofrecerAlternativa(texto, accion) {
    textoAlternativo.value = texto
    accionAlternativa = accion
  }

  function retirarAlternativa() {
    textoAlternativo.value = ''
    accionAlternativa = null
  }

  async function ejecutarAlternativa() {
    if (!accionAlternativa) return false
    const accion = accionAlternativa
    retirarAlternativa()
    await accion()
    return true
  }

  return {
    hayCambios,
    mensaje,
    textoAlternativo,
    actualizar,
    limpiar,
    ofrecerAlternativa,
    retirarAlternativa,
    ejecutarAlternativa,
  }
}
