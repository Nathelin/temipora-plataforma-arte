import { ref } from 'vue'

const pendiente = ref(false)
const mensaje = ref('')
const titulo = ref('¿Salir sin publicar?')
const textoConfirmar = ref('Salir sin publicar')
const textoCancelar = ref('Quedarme')
const textoAlternativo = ref('')
const procesandoAlternativa = ref(false)
const errorAlternativa = ref('')
let resolutor = null
let alAlternar = null

export function useConfirmacionSalida() {
  function pedir(texto, opciones = {}) {
    mensaje.value = texto
    titulo.value = opciones.titulo || '¿Salir sin publicar?'
    textoConfirmar.value = opciones.textoConfirmar || 'Salir sin publicar'
    textoCancelar.value = opciones.textoCancelar || 'Quedarme'
    textoAlternativo.value = opciones.textoAlternativo || ''
    procesandoAlternativa.value = false
    errorAlternativa.value = ''
    alAlternar = opciones.alAlternar || null
    pendiente.value = true
    return new Promise((resolver) => {
      resolutor = resolver
    })
  }

  function responder(continuar) {
    if (!pendiente.value) return
    pendiente.value = false
    alAlternar = null
    const resolver = resolutor
    resolutor = null
    if (resolver) resolver(continuar)
  }

  /**
   * Ejecuta la acción alternativa (por ejemplo "guardar borrador y salir").
   * Solo se abandona la pantalla si la acción termina bien: si falla, el
   * modal queda abierto para que el usuario pueda reintentar o cancelar.
   */
  async function responderAlternativa() {
    if (!pendiente.value || !alAlternar || procesandoAlternativa.value) return
    const accion = alAlternar
    procesandoAlternativa.value = true
    errorAlternativa.value = ''
    try {
      await accion()
    } catch (e) {
      errorAlternativa.value = e?.response?.data?.message
        || e?.message
        || 'No se pudo guardar. Revisá los datos e intentá de nuevo.'
      procesandoAlternativa.value = false
      return
    }
    procesandoAlternativa.value = false
    pendiente.value = false
    alAlternar = null
    const resolver = resolutor
    resolutor = null
    if (resolver) resolver(true)
  }

  return {
    pendiente,
    mensaje,
    titulo,
    textoConfirmar,
    textoCancelar,
    textoAlternativo,
    procesandoAlternativa,
    errorAlternativa,
    pedir,
    responder,
    responderAlternativa,
  }
}
