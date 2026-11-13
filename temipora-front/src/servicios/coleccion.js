import api from './api'

export function cargarColeccion(nombreUsuario, params = {}) {
  return api.get(`/coleccion/perfil/${encodeURIComponent(nombreUsuario)}`, { params })
}

export function cambiarVisibilidadCompra(idCompraItem, ocultar) {
  return api.put(`/coleccion/items/${idCompraItem}/visibilidad`, { ocultar })
}

export function marcarSugerenciaColeccionVista() {
  return api.post('/coleccion/sugerencia/vista')
}
