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

export const MAX_PLAZO_POR_UNIDAD = {
  DIAS: 365,
  SEMANAS: 52,
  MESES: 12,
}

export function aFechaISO(fecha) {
  if (!(fecha instanceof Date) || Number.isNaN(fecha.getTime())) return ''
  const anio = fecha.getFullYear()
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${anio}-${mes}-${dia}`
}

export function fechaISOlocal(valor) {
  if (valor instanceof Date) {
    if (Number.isNaN(valor.getTime())) return null
    return new Date(valor.getFullYear(), valor.getMonth(), valor.getDate())
  }
  if (!valor) return null
  const texto = String(valor).slice(0, 10)
  const [anio, mes, dia] = texto.split('-').map(Number)
  if (!anio || !mes || !dia) return null
  const fecha = new Date(anio, mes - 1, dia)
  return Number.isNaN(fecha.getTime()) ? null : fecha
}

function sumarPlazoAFecha(fecha, valor, unidad) {
  const cantidad = Number(valor)
  if (!fecha || !Number.isFinite(cantidad) || cantidad < 1) return null

  if (unidad === 'MESES') {
    const dia = fecha.getDate()
    const destino = new Date(fecha.getFullYear(), fecha.getMonth() + cantidad, 1)
    const ultimoDia = new Date(
      destino.getFullYear(),
      destino.getMonth() + 1,
      0,
    ).getDate()
    destino.setDate(Math.min(dia, ultimoDia))
    return destino
  }

  const dias = unidad === 'SEMANAS' ? cantidad * 7 : cantidad
  const resultado = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate())
  resultado.setDate(resultado.getDate() + dias)
  return resultado
}

export function sumarPlazo(fechaISO, valor, unidad) {
  const fecha = fechaISOlocal(fechaISO)
  if (!fecha) return null
  return sumarPlazoAFecha(fecha, valor, unidad)
}

export function fechaEntregaEstimada({
  fechaPago,
  fechaInicioEstimada,
  valor,
  unidad,
} = {}) {
  const pago = fechaISOlocal(fechaPago)
  const inicio = fechaISOlocal(fechaInicioEstimada)
  if (!pago) return null
  const inicioReal = inicio && inicio > pago ? inicio : pago
  return sumarPlazoAFecha(inicioReal, valor, unidad)
}

export function formatearFechaISO(valor, { larga = false } = {}) {
  const fecha = valor instanceof Date ? valor : fechaISOlocal(valor)
  if (!fecha) return ''
  return new Intl.DateTimeFormat('es', larga
    ? { dateStyle: 'long' }
    : { dateStyle: 'medium' }).format(fecha)
}

export function formatearFechaDDMMAAAA(valor) {
  const fecha = valor instanceof Date ? valor : fechaISOlocal(valor)
  if (!fecha) return ''
  const dia = String(fecha.getDate()).padStart(2, '0')
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  return `${dia}/${mes}/${fecha.getFullYear()}`
}

const GRUPO_TODAS = { clave: 'todas', texto: 'Todas', estados: [] }
const GRUPO_POR_RESPONDER = {
  clave: 'por-responder',
  texto: 'Por responder',
  estados: ['PENDIENTE', 'EN_LISTA_ESPERA'],
}
const GRUPO_PENDIENTES = { clave: 'pendientes', texto: 'Pendientes', estados: ['PENDIENTE'] }
const GRUPO_ESPERA = { clave: 'espera', texto: 'En espera', estados: ['EN_LISTA_ESPERA'] }
const GRUPO_PRESUPUESTADAS = { clave: 'presupuestadas', texto: 'Presupuestadas', estados: ['PRESUPUESTADA'] }
const GRUPO_PAGADAS = { clave: 'pagadas', texto: 'Pagadas', estados: ['PAGADA'] }
const GRUPO_HISTORIAL = { clave: 'historial', texto: 'Historial', estados: ['RECHAZADA', 'CANCELADA', 'VENCIDA'] }

export const GRUPOS_ESTADO = [
  GRUPO_TODAS,
  GRUPO_PENDIENTES,
  GRUPO_ESPERA,
  GRUPO_PRESUPUESTADAS,
  GRUPO_PAGADAS,
  GRUPO_HISTORIAL,
]

export const GRUPOS_RECIBIDAS = [
  GRUPO_TODAS,
  GRUPO_POR_RESPONDER,
  GRUPO_PRESUPUESTADAS,
  GRUPO_PAGADAS,
  GRUPO_HISTORIAL,
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
