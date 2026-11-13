// Estado de los filtros del catálogo de servicios.
// Única fuente de verdad para: leerse de la URL, viajar al backend y volver.

import api from './api'

export const ORDENES = [
  { valor: 'RELEVANCIA', texto: 'Para vos' },
  { valor: 'NOMBRE', texto: 'Nombre' },
  { valor: 'PRECIO_ASC', texto: 'Precio: menor a mayor' },
  { valor: 'PRECIO_DESC', texto: 'Precio: mayor a menor' },
]

export const CONTENIDOS_SENSIBLES = [
  { valor: 'VIOLENCIA', texto: 'Violencia' },
  { valor: 'DESNUDOS', texto: 'Desnudos' },
  { valor: 'GENERAL', texto: 'Contenido sensible general' },
]

const LISTAS = [
  'idsMedios',
  'idsTiposServicio',
  'idsEtiquetas',
  'idsEtiquetasExcluidas',
  'idsCategoriasEtiqueta',
  'contenidosSensibles',
  'excluirContenidosSensibles',
]

const TEXTO_NUMERICO = [
  'precioMin',
  'precioMax',
  'tiempoMaxDias',
  'valoracionMinima',
]

export function estadoInicial() {
  return {
    nombre: '',
    nombreArtista: '',
    textoLibre: '',
    sensible: '',
    precioMin: '',
    precioMax: '',
    tiempoMaxDias: '',
    idsMedios: [],
    idsTiposServicio: [],
    idsEtiquetas: [],
    idsEtiquetasExcluidas: [],
    idsCategoriasEtiqueta: [],
    contenidosSensibles: [],
    excluirContenidosSensibles: [],
    soloConListaEspera: false,
    valoracionMinima: '',
    monedaDeclarada: '',
    soloConCupos: false,
    orden: 'RELEVANCIA',
  }
}

function listaDesde(valor) {
  if (valor === undefined || valor === null || valor === '') return []
  const crudo = Array.isArray(valor) ? valor : [valor]
  return crudo
    .flatMap((v) => String(v).split(','))
    .map((v) => v.trim())
    .filter(Boolean)
}

export function desdeQuery(query) {
  const estado = estadoInicial()
  if (!query) return estado

  for (const clave of ['nombre', 'nombreArtista', 'textoLibre',
    'sensible', 'monedaDeclarada']) {
    if (query[clave] !== undefined && query[clave] !== null) {
      estado[clave] = String(Array.isArray(query[clave]) ? query[clave][0] : query[clave])
    }
  }
  for (const clave of TEXTO_NUMERICO) {
    if (query[clave] !== undefined && query[clave] !== null) {
      estado[clave] = String(Array.isArray(query[clave]) ? query[clave][0] : query[clave])
    }
  }
  for (const clave of LISTAS) {
    estado[clave] = listaDesde(query[clave])
  }
  estado.soloConListaEspera = query.soloConListaEspera === 'true'
  estado.soloConCupos = query.incluirSinCupos === 'false'
  if (query.orden) {
    const valor = String(Array.isArray(query.orden) ? query.orden[0] : query.orden)
    if (ORDENES.some((o) => o.valor === valor)) estado.orden = valor
  }
  return estado
}

/**
 * Traduce el estado a la query string que consume el backend.
 * Las listas viajan como parámetros repetidos, que es lo que Spring
 * enlaza a `List<Long>` / `List<Enum>`.
 */
export function aParams(estado, { pagina = 0, tamanio = 24 } = {}) {
  const params = new URLSearchParams()
  params.set('pagina', String(pagina))
  params.set('tamanio', String(tamanio))

  for (const clave of ['nombre', 'nombreArtista', 'textoLibre']) {
    const valor = (estado[clave] || '').trim()
    if (valor) params.set(clave, valor)
  }
  if (estado.sensible !== '') params.set('sensible', estado.sensible)
  for (const clave of ['precioMin', 'precioMax', 'tiempoMaxDias', 'valoracionMinima']) {
    if (estado[clave] !== '' && estado[clave] !== null && estado[clave] !== undefined) {
      params.set(clave, estado[clave])
    }
  }
  for (const clave of LISTAS) {
    for (const valor of estado[clave] || []) {
      params.append(clave, valor)
    }
  }
  if (estado.soloConListaEspera) params.set('soloConListaEspera', 'true')
  if (estado.monedaDeclarada) params.set('monedaDeclarada', estado.monedaDeclarada)

  // "Solo con cupos" es la ausencia de servicios sin cupo, no un filtro aparte.
  if (estado.soloConCupos) params.set('incluirSinCupos', 'false')
  if (estado.orden && estado.orden !== 'RELEVANCIA') params.set('orden', estado.orden)

  return params
}

/** Query de la URL, sin paginación: así "Atrás" y los links comparten estado. */
export function aQueryUrl(estado) {
  const params = aParams(estado, { pagina: 0, tamanio: 24 })
  params.delete('pagina')
  params.delete('tamanio')
  const salida = {}
  for (const [clave, valor] of params.entries()) {
    if (salida[clave] === undefined) {
      salida[clave] = valor
    } else if (Array.isArray(salida[clave])) {
      salida[clave].push(valor)
    } else {
      salida[clave] = [salida[clave], valor]
    }
  }
  return salida
}

export function cantidadActivos(estado) {
  let total = 0
  for (const clave of ['nombre', 'nombreArtista', 'textoLibre',
    'precioMin', 'precioMax', 'tiempoMaxDias', 'valoracionMinima', 'monedaDeclarada']) {
    const valor = estado[clave]
    if (valor !== '' && valor !== null && valor !== undefined) total++
  }
  if (estado.sensible !== '') total++
  for (const clave of LISTAS) {
    total += (estado[clave] || []).length
  }
  if (estado.soloConListaEspera) total++
  if (estado.soloConCupos) total++
  return total
}

/** Chips removibles que se muestran arriba de la grilla. */
export function chipsActivos(estado, { tipos = [], medios = [], etiquetas = [] } = {}) {
  const chips = []
  const agregar = (clave, valor, texto) => chips.push({ clave, valor, texto })

  if (estado.textoLibre.trim()) {
    agregar('textoLibre', '', `“${estado.textoLibre.trim()}”`)
  }
  if (estado.nombre.trim()) agregar('nombre', '', `Nombre: ${estado.nombre.trim()}`)
  if (estado.nombreArtista.trim()) {
    agregar('nombreArtista', '', `@${estado.nombreArtista.trim()}`)
  }
  for (const id of estado.idsTiposServicio || []) {
    const tipo = tipos.find((t) => String(t.idTipoServicio) === String(id))
    agregar('idsTiposServicio', id, tipo?.nombre || `Tipo ${id}`)
  }
  if (estado.sensible !== '') {
    agregar('sensible', '', estado.sensible === 'true' ? 'Con contenido sensible' : 'Sin contenido sensible')
  }
  if (estado.precioMin !== '' || estado.precioMax !== '') {
    const desde = estado.precioMin !== '' ? estado.precioMin : null
    const hasta = estado.precioMax !== '' ? estado.precioMax : null
    const texto = desde && hasta
      ? `Precio: ${desde} – ${hasta}`
      : (desde ? `Precio desde ${desde}` : `Precio hasta ${hasta}`)
    agregar('rangoPrecio', '', texto)
  }
  if (estado.tiempoMaxDias !== '') {
    agregar('tiempoMaxDias', '', `Entrega en ${estado.tiempoMaxDias} días o menos`)
  }
  if (estado.valoracionMinima !== '') {
    agregar('valoracionMinima', '', `★ ${estado.valoracionMinima} o más`)
  }
  if (estado.monedaDeclarada) {
    agregar('monedaDeclarada', '', `Precio original en ${estado.monedaDeclarada}`)
  }
  for (const id of estado.idsMedios || []) {
    const medio = medios.find((m) => String(m.idMedio) === String(id))
    agregar('idsMedios', id, medio?.nombre || `Medio ${id}`)
  }
  for (const id of estado.idsEtiquetas || []) {
    const etiqueta = etiquetas.find((e) => String(e.idEtiqueta) === String(id))
    agregar('idsEtiquetas', id, etiqueta?.nombre || `Etiqueta ${id}`)
  }
  for (const id of estado.idsEtiquetasExcluidas || []) {
    const etiqueta = etiquetas.find((e) => String(e.idEtiqueta) === String(id))
    agregar('idsEtiquetasExcluidas', id, `Sin ${etiqueta?.nombre || `etiqueta ${id}`}`)
  }
  for (const id of estado.contenidosSensibles || []) {
    const contenido = CONTENIDOS_SENSIBLES.find((c) => c.valor === id)
    agregar('contenidosSensibles', id, contenido?.texto || id)
  }
  for (const id of estado.excluirContenidosSensibles || []) {
    const contenido = CONTENIDOS_SENSIBLES.find((c) => c.valor === id)
    agregar('excluirContenidosSensibles', id, `Sin ${contenido?.texto || id}`)
  }
  if (estado.soloConListaEspera) agregar('soloConListaEspera', '', 'Acepta lista de espera')
  if (estado.soloConCupos) agregar('soloConCupos', '', 'Solo con cupos')

  return chips
}

/**
 * Conteo de resultados por opción de filtro. Usa exactamente la misma query que
 * el listado, así los números siempre coinciden con lo que devuelve la grilla.
 */
export async function cargarFacetas(estado) {
  const params = aParams(estado, { pagina: 0, tamanio: 24 })
  params.delete('pagina')
  params.delete('tamanio')
  const res = await api.get('/servicios/facetas', { params })
  return res.data || null
}

/** Quita un chip del estado sin tocar el resto. */
export function quitarChip(estado, chip) {
  const siguiente = { ...estado }
  if (chip.clave === 'rangoPrecio') {
    siguiente.precioMin = ''
    siguiente.precioMax = ''
    return siguiente
  }
  for (const clave of LISTAS) {
    if (chip.clave === clave) {
      siguiente[clave] = (estado[clave] || []).filter((v) => String(v) !== String(chip.valor))
      return siguiente
    }
  }
  if (chip.clave === 'soloConListaEspera' || chip.clave === 'soloConCupos') {
    siguiente[chip.clave] = false
    return siguiente
  }
  if (chip.clave === 'sensible') {
    siguiente.sensible = ''
    return siguiente
  }
  siguiente[chip.clave] = ''
  return siguiente
}
