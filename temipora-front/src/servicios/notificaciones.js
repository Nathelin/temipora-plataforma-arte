/**
 * Destino y texto de cada notificación. Vive en un solo lugar para que la
 * campana y la página completa no diverjan.
 */

export const CATEGORIAS_NOTIFICACION = [
  { clave: 'SOLICITUDES', texto: 'Solicitudes' },
  { clave: 'PEDIDOS', texto: 'Pedidos y pagos' },
  { clave: 'MENSAJES', texto: 'Mensajes' },
  { clave: 'RESENIAS', texto: 'Reseñas' },
  { clave: 'PROMOCIONES', texto: 'Promociones' },
]

const CODIGOS_CIERRE = [
  'SOLICITUD_RECHAZADA',
  'PRESUPUESTO_VENCIDO',
  'PEDIDO_IMPAGO',
]

export function esCierre(notificacion) {
  return CODIGOS_CIERRE.includes(notificacion?.codigoTipo)
}

export function destinoDe(notificacion, { rol, pathActual = '' } = {}) {
  const tipo = notificacion?.tipoReferencia
  if (tipo !== 'SOLICITUD' && tipo !== 'PEDIDO' && tipo !== 'RESENIA'
    && tipo !== 'CONVERSACION' && tipo !== 'OFERTA_REACTIVACION') return null

  if (rol !== 'ARTISTA' && rol !== 'CLIENTE' && rol !== 'ADMIN') return null

  if (tipo === 'RESENIA') {
    return rol === 'ADMIN' ? { name: 'admin-resenias-reportadas' } : null
  }

  if (tipo === 'OFERTA_REACTIVACION') {
    return rol === 'ADMIN' ? null : { name: 'ofertas' }
  }

  if (tipo === 'CONVERSACION') {
    if (rol === 'ADMIN') return null
    const usuario = notificacion.nombreUsuarioReferencia
    if (!usuario) return null
    return pathActual.startsWith('/panel')
      ? { name: 'panel-conversacion', params: { nombreUsuario: `@${usuario}` } }
      : { name: 'conversacion', params: { nombreUsuario: `@${usuario}` } }
  }

  const id = Number(notificacion.idReferencia)
  if (!Number.isInteger(id) || id <= 0) return null

  if (tipo === 'SOLICITUD') {
    if (rol === 'ADMIN') return null
    if (rol === 'CLIENTE' && esCierre(notificacion)) {
      return { name: 'solicitud-resultado', params: { idSolicitud: id } }
    }
    return notificacion.rolReferencia === 'ARTISTA'
      ? { name: 'panel-solicitud-detalle', params: { idSolicitud: id } }
      : { name: 'solicitud-detalle', params: { idSolicitud: id } }
  }

  if (rol === 'ADMIN') {
    return { name: 'admin-pedido-detalle', params: { idPedido: id } }
  }
  if (rol === 'CLIENTE' && esCierre(notificacion)) {
    return { name: 'pedido-resultado', params: { idPedido: id } }
  }
  return notificacion.rolReferencia === 'ARTISTA'
    ? { name: 'panel-pedido-detalle', params: { idPedido: id } }
    : { name: 'pedido-detalle', params: { idPedido: id } }
}

export function textoDestino(notificacion, rol) {
  if (rol === 'CLIENTE' && esCierre(notificacion)) return 'Ver alternativas →'
  if (notificacion?.tipoReferencia === 'PEDIDO') return 'Ver pedido →'
  if (notificacion?.tipoReferencia === 'SOLICITUD') return 'Ver solicitud →'
  if (notificacion?.tipoReferencia === 'RESENIA') return 'Ver reportes →'
  if (notificacion?.tipoReferencia === 'CONVERSACION') return 'Ver conversación →'
  if (notificacion?.tipoReferencia === 'OFERTA_REACTIVACION') return 'Ver ofertas →'
  return ''
}
