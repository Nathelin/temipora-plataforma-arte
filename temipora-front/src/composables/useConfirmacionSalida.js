import { ref } from 'vue'

const pendiente = ref(false)
const mensaje = ref('')
let resolutor = null

export function useConfirmacionSalida() {
  function pedir(texto) {
    mensaje.value = texto
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

  return { pendiente, mensaje, pedir, responder }
}
