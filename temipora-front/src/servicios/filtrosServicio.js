// Filtros del catálogo de servicios, definidos sobre el motor compartido.
// La API pública se mantiene igual que antes para no tocar los consumidores.

import api from './api'
import {
  aParams as aParamsBase,
  aQueryUrl as aQueryUrlBase,
  cantidadActivos as cantidadActivosBase,
  desdeQuery as desdeQueryBase,
  estadoInicial as estadoInicialBase,
  quitarChip as quitarChipBase,
  simboloMoneda,
  textoRangoPrecio,
} from './filtrosCatalogo'

export { simboloMoneda }

export const ORDENES = [
  { valor: 'RELEVANCIA', texto: 'Para vos' },
  { valor: 'RECIENTES', texto: 'Más recientes' },
  { valor: 'NOMBRE', texto: 'Nombre' },
  { valor: 'PRECIO_ASC', texto: 'Precio: menor a mayor' },
  { valor: 'PRECIO_DESC', texto: 'Precio: mayor a menor' },
]

export const CONTENIDOS_SENSIBLES = [
  { valor: 'VIOLENCIA', texto: 'Violencia' },
  { valor: 'DESNUDOS', texto: 'Desnudos' },
  { valor: 'GENERAL', texto: 'Contenido sensible general' },
]

const DEFINICION = {
  textos: ['nombre', 'nombreArtista', 'textoLibre', 'sensible', 'monedaDeclarada'],
  numericos: ['precioMin', 'precioMax', 'tiempoMaxDias', 'valoracionMinima'],
  listas: [
    'idsMedios',
    'idsTiposServicio',
    'idsEtiquetas',
    'idsEtiquetasExcluidas',
    'contenidosSensibles',
    'excluirContenidosSensibles',
  ],
  booleanos: ['soloConListaEspera', 'soloConCupos'],
  ordenes: ORDENES,
  ordenPorDefecto: 'RELEVANCIA',
}

export function estadoInicial() {
  return estadoInicialBase(DEFINICION)
}

export function desdeQuery(query) {
  const estado = desdeQueryBase(DEFINICION, query)
  // "Solo con cupos" es la ausencia de servicios sin cupo, no un filtro aparte.
  if (query && query.incluirSinCupos === 'false') estado.soloConCupos = true
  return estado
}

function mapearServicio(params, estado) {
  // El motor serializa los booleanos con su nombre; "solo con cupos" es en
  // realidad la ausencia de servicios sin cupo, así que se traduce y se quita.
  params.delete('soloConCupos')
  if (estado.soloConCupos) params.set('incluirSinCupos', 'false')
}

export function aParams(estado, opciones = {}) {
  return aParamsBase(DEFINICION, estado, {
    ...opciones,
    mapear: mapearServicio,
  })
}

export function aQueryUrl(estado) {
  return aQueryUrlBase(DEFINICION, estado, { mapear: mapearServicio })
}

export function cantidadActivos(estado) {
  return cantidadActivosBase(DEFINICION, estado)
}

/** Quita un chip del estado sin tocar el resto. */
export function quitarChip(estado, chip) {
  if (chip.clave === 'rangoPrecio') {
    return { ...estado, precioMin: '', precioMax: '' }
  }
  return quitarChipBase(DEFINICION, estado, chip)
}

/** Chips removibles que se muestran arriba de la grilla. */
export function chipsActivos(estado, {
  tipos = [],
  medios = [],
  etiquetas = [],
  moneda = 'ARS',
} = {}) {
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
  const rango = textoRangoPrecio(estado, moneda)
  if (rango) agregar('rangoPrecio', '', rango)
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
