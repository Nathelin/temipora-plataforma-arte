import { ref } from 'vue'

const pendiente = ref(false)
const mensaje = ref('')
const titulo = ref('¿Salir sin publicar?')
const textoConfirmar = ref('Salir sin publicar')
const textoCancelar = ref('Quedarme')
let resolutor = null

export function useConfirmacionSalida() {
  function pedir(texto, opciones = {}) {
    mensaje.value = texto
    titulo.value = opciones.titulo || '¿Salir sin publicar?'
    textoConfirmar.value = opciones.textoConfirmar || 'Salir sin publicar'
    textoCancelar.value = opciones.textoCancelar || 'Quedarme'
    pendiente.value = true
    return new Promise((resolver) => {
      resolutor = resolver
    })
  }

  function responder(continuar) {
    if (!pendiente.value) return
    pendiente.value = false
    const resolver = resolutor
    resolutor = null
    if (resolver) resolver(continuar)
  }

  return { pendiente, mensaje, titulo, textoConfirmar, textoCancelar, pedir, responder }
}
