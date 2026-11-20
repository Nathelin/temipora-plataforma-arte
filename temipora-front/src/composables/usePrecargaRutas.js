import { onBeforeUnmount, onMounted } from 'vue'
import { precargarRuta } from '../router'

const ESPERA_MS = 80

/**
 * Precarga la vista de un enlace interno cuando el puntero se detiene sobre él
 * o cuando recibe foco por teclado. Usa delegación de eventos para cubrir todos
 * los enlaces sin tocar cada componente.
 */
export function usePrecargaRutas() {
  let temporizador = null
  let ultimoEnlace = null

  function cancelar() {
    if (temporizador) {
      clearTimeout(temporizador)
      temporizador = null
    }
  }

  function alPasar(evento) {
    const objetivo = evento.target
    if (!(objetivo instanceof Element)) return

    const enlace = objetivo.closest('a[href]')
    if (!enlace || enlace === ultimoEnlace) return
    ultimoEnlace = enlace

    if (enlace.target === '_blank' || enlace.hasAttribute('download')) return
    const href = enlace.getAttribute('href')
    if (!href
        || href.startsWith('#')
        || href.startsWith('http')
        || href.startsWith('mailto:')
        || href.startsWith('tel:')) {
      return
    }

    cancelar()
    temporizador = setTimeout(() => {
      temporizador = null
      precargarRuta(href)
    }, ESPERA_MS)
  }

  onMounted(() => {
    document.addEventListener('pointerover', alPasar, { passive: true })
    document.addEventListener('focusin', alPasar)
  })

  onBeforeUnmount(() => {
    cancelar()
    document.removeEventListener('pointerover', alPasar)
    document.removeEventListener('focusin', alPasar)
  })
}
