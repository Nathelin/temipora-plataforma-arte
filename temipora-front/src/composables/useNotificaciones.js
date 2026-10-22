import { reactive } from 'vue'

const notificaciones = reactive([])
let siguienteId = 0

export function useNotificaciones() {
  function cerrar(id) {
    const indice = notificaciones.findIndex((notificacion) => notificacion.id === id)
    if (indice !== -1) {
      notificaciones.splice(indice, 1)
    }
  }

  function mostrar(mensaje, tipo = 'exito', duracion = 3500) {
    const id = ++siguienteId
    notificaciones.push({ id, mensaje, tipo })
    window.setTimeout(() => cerrar(id), duracion)
    return id
  }

  function exito(mensaje) {
    return mostrar(mensaje, 'exito')
  }

  function error(mensaje) {
    return mostrar(mensaje, 'error', 5000)
  }

  return { notificaciones, mostrar, exito, error, cerrar }
}
