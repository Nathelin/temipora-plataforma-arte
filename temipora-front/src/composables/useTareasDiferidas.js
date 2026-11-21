const TIEMPO_MAXIMO_MS = 1200

/**
 * Ejecuta una tarea cuando el navegador queda libre (o como máximo en 1,2 s).
 * Sirve para que los contadores, el carrito y el WebSocket no compitan con la
 * carga de la pantalla que el usuario está mirando.
 */
export function cuandoEsteLibre(tarea, opciones = {}) {
  if (typeof window === 'undefined') {
    return () => {}
  }

  const tiempoMaximo = opciones.tiempoMaximo ?? TIEMPO_MAXIMO_MS
  let cancelada = false
  let identificador = null

  const ejecutar = () => {
    if (cancelada) return
    cancelada = true
    tarea()
  }

  if (typeof window.requestIdleCallback === 'function') {
    identificador = window.requestIdleCallback(ejecutar, { timeout: tiempoMaximo })
    return () => {
      cancelada = true
      window.cancelIdleCallback(identificador)
    }
  }

  identificador = window.setTimeout(ejecutar, tiempoMaximo)
  return () => {
    cancelada = true
    window.clearTimeout(identificador)
  }
}
