export const ESTADOS_SOLICITUD = {
  PENDIENTE: { texto: 'Pendiente', clase: 'border-mantequilla bg-mantequilla/40 text-granate' },
  EN_LISTA_ESPERA: { texto: 'En lista de espera', clase: 'border-durazno bg-durazno/50 text-granate' },
  PRESUPUESTADA: { texto: 'Presupuestada', clase: 'border-coral/30 bg-coral/10 text-coral' },
  PAGADA: { texto: 'Pagada', clase: 'border-rosa bg-rosa/35 text-granate' },
  FINALIZADA: { texto: 'Finalizada', clase: 'border-rosa bg-rosa/35 text-granate' },
  RECHAZADA: { texto: 'Rechazada', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
  CANCELADA: { texto: 'Cancelada', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
}

export const GRUPOS_ESTADO = [
  { clave: 'todas', texto: 'Todas', estados: [] },
  { clave: 'pendientes', texto: 'Pendientes', estados: ['PENDIENTE'] },
  { clave: 'espera', texto: 'En espera', estados: ['EN_LISTA_ESPERA'] },
  { clave: 'presupuestadas', texto: 'Presupuestadas', estados: ['PRESUPUESTADA'] },
  { clave: 'pagadas', texto: 'Pagadas', estados: ['PAGADA'] },
  { clave: 'historial', texto: 'Historial', estados: ['RECHAZADA', 'CANCELADA'] },
]

export function estadoDe(codigo) {
  return ESTADOS_SOLICITUD[codigo] || {
    texto: codigo || 'Sin estado',
    clase: 'border-rosa bg-rosa/20 text-cafe',
  }
}

export function formatearFechaSolicitud(valor, { corta = false } = {}) {
  if (!valor) return ''
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  return new Intl.DateTimeFormat('es', corta
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'long', timeStyle: 'short' }).format(fecha)
}

export function datosContraparte(usuario) {
  if (!usuario) return null
  const nombre = usuario.nombreDisplay || usuario.nombreUsuario || 'No disponible'
  return {
    idUsuario: usuario.idUsuario,
    nombre,
    usuario: usuario.nombreUsuario || '',
    foto: usuario.urlFotoPerfilMiniatura || usuario.urlFotoPerfil || '',
    inicial: nombre.charAt(0).toUpperCase(),
    pais: usuario.nombrePais || '',
    biografia: usuario.biografia || '',
  }
}

export function contraparteDe(solicitud, esArtista) {
  return esArtista ? solicitud?.cliente : solicitud?.artista
}

export function solicitudesEnGrupo(solicitudes, grupo) {
  if (!grupo?.estados?.length) return solicitudes
  return solicitudes.filter((solicitud) => grupo.estados.includes(solicitud.estado))
}
