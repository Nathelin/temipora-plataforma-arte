import api from './api'

export function cargarGaleria(nombreUsuario, params = {}) {
  return api.get(`/galeria/perfil/${encodeURIComponent(nombreUsuario)}`, { params })
}

export function autorizarPublicacionPedido(idPedido, autoriza) {
  return api.put(`/pedidos/${idPedido}/publicacion/artista`, { autoriza })
}

export function ocultarPublicacionPedido(idPedido, ocultar) {
  return api.put(`/pedidos/${idPedido}/publicacion/cliente`, { ocultar })
}
