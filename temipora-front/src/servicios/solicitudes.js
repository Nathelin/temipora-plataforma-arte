export const ESTADOS_SOLICITUD = {
  PENDIENTE: { texto: 'Pendiente', clase: 'border-mantequilla bg-mantequilla/40 text-granate' },
  EN_LISTA_ESPERA: { texto: 'En lista de espera', clase: 'border-durazno bg-durazno/50 text-granate' },
  PRESUPUESTADA: { texto: 'Presupuestada', clase: 'border-coral/30 bg-coral/10 text-coral' },
  PAGADA: { texto: 'Pagada', clase: 'border-rosa bg-rosa/35 text-granate' },
  FINALIZADA: { texto: 'Finalizada', clase: 'border-rosa bg-rosa/35 text-granate' },
  RECHAZADA: { texto: 'Rechazada', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
  CANCELADA: { texto: 'Cancelada', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
  VENCIDA: { texto: 'Vencida', clase: 'border-cafe/25 bg-cafe/10 text-cafe' },
}

export const UNIDADES_ENTREGA = {
  DIAS: { singular: 'día', plural: 'días' },
  SEMANAS: { singular: 'semana', plural: 'semanas' },
  MESES: { singular: 'mes', plural: 'meses' },
}

export function textoTiempoEntrega(valor, unidad) {
  if (valor === null || valor === undefined || valor === '') return '—'
  const info = UNIDADES_ENTREGA[unidad]
  if (!info) return `${valor} ${unidad || ''}`.trim()
  return `${valor} ${valor === 1 ? info.singular : info.plural}`
}

export const GRUPOS_ESTADO = [
  { clave: 'todas', texto: 'Todas', estados: [] },
  { clave: 'pendientes', texto: 'Pendientes', estados: ['PENDIENTE'] },
  { clave: 'espera', texto: 'En espera', estados: ['EN_LISTA_ESPERA'] },
  { clave: 'presupuestadas', texto: 'Presupuestadas', estados: ['PRESUPUESTADA'] },
  { clave: 'pagadas', texto: 'Pagadas', estados: ['PAGADA'] },
  { clave: 'historial', texto: 'Historial', estados: ['RECHAZADA', 'CANCELADA', 'VENCIDA'] },
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

export function gruposDeRespuestas(item) {
  const respuestas = [...(item?.respuestas || [])].sort((a, b) => {
    const orden = (a.orden ?? 0) - (b.orden ?? 0)
    return orden !== 0 ? orden : (a.suborden ?? 0) - (b.suborden ?? 0)
  })
  const grupos = new Map()

  for (const respuesta of respuestas) {
    const valor = respuesta.valor
    if (valor === null || valor === undefined || String(valor).trim() === '') continue

    const clave = respuesta.idPregunta != null
      ? `pregunta-${respuesta.idPregunta}`
      : `respuesta-${respuesta.orden}-${respuesta.enunciado}`
    if (!grupos.has(clave)) {
      grupos.set(clave, {
        clave,
        enunciado: respuesta.enunciado,
        codigoTipo: respuesta.codigoTipo,
        valores: [],
      })
    }
    grupos.get(clave).valores.push(String(valor))
  }

  return [...grupos.values()]
}
