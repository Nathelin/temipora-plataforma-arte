// Filtros del catálogo de productos, definidos sobre el motor compartido.

import api from './api'
import {
  aParams as aParamsBase,
  aQueryUrl as aQueryUrlBase,
  cantidadActivos as cantidadActivosBase,
  desdeQuery as desdeQueryBase,
  estadoInicial as estadoInicialBase,
  quitarChip as quitarChipBase,
  textoRangoPrecio,
} from './filtrosCatalogo'

export const ORDENES = [
  { valor: 'RELEVANCIA', texto: 'Para vos' },
  { valor: 'RECIENTES', texto: 'Más recientes' },
  { valor: 'TITULO', texto: 'Título' },
  { valor: 'PRECIO_ASC', texto: 'Precio: menor a mayor' },
  { valor: 'PRECIO_DESC', texto: 'Precio: mayor a menor' },
]

export const CONTENIDOS_SENSIBLES = [
  { valor: 'VIOLENCIA', texto: 'Violencia' },
  { valor: 'DESNUDOS', texto: 'Desnudos' },
  { valor: 'GENERAL', texto: 'Contenido sensible general' },
]

/** Origen del ítem en el catálogo mezclado de productos y reventas. */
export const ORIGENES = [
  { valor: 'TODAS', texto: 'Todo' },
  { valor: 'PRODUCTOS', texto: 'Solo productos' },
  { valor: 'REVENTAS', texto: 'Solo reventas' },
]

const DEFINICION = {
  textos: ['titulo', 'nombreArtista', 'textoLibre', 'sensible', 'origen'],
  numericos: ['precioMin', 'precioMax'],
  listas: [
    'idsTiposProducto',
    'idsMedios',
    'idsEtiquetas',
    'idsEtiquetasExcluidas',
    'contenidosSensibles',
    'excluirContenidosSensibles',
  ],
  booleanos: ['soloPiezaUnica', 'soloIlimitado'],
  // "Todas" no es un filtro: no cuenta como activo ni viaja al backend.
  valoresNeutros: { origen: 'TODAS' },
  ordenes: ORDENES,
  ordenPorDefecto: 'RELEVANCIA',
}

export function estadoInicial() {
  const estado = estadoInicialBase(DEFINICION)
  estado.origen = 'TODAS'
  return estado
}

export function desdeQuery(query) {
  const estado = desdeQueryBase(DEFINICION, query)
  if (!ORIGENES.some((o) => o.valor === estado.origen)) estado.origen = 'TODAS'
  return estado
}

function mapearProducto(params, estado) {
  // `incluirReventas` es la forma en que el backend entiende el origen.
  const origen = estado.origen || 'TODAS'
  if (origen === 'PRODUCTOS') params.set('incluirReventas', 'false')
  if (origen === 'REVENTAS') {
    params.set('incluirReventas', 'true')
    params.set('soloReventas', 'true')
  }
}

export function aParams(estado, opciones = {}) {
  return aParamsBase(DEFINICION, estado, {
    ...opciones,
    mapear: mapearProducto,
  })
}

export function aQueryUrl(estado) {
  return aQueryUrlBase(DEFINICION, estado, { mapear: mapearProducto })
}

export function cantidadActivos(estado) {
  return cantidadActivosBase(DEFINICION, estado)
}

export function quitarChip(estado, chip) {
  if (chip.clave === 'rangoPrecio') {
    return { ...estado, precioMin: '', precioMax: '' }
  }
  if (chip.clave === 'origen') {
    return { ...estado, origen: 'TODAS' }
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
  if (estado.titulo.trim()) agregar('titulo', '', `Título: ${estado.titulo.trim()}`)
  if (estado.nombreArtista.trim()) {
    agregar('nombreArtista', '', `@${estado.nombreArtista.trim()}`)
  }
  if (estado.origen && estado.origen !== 'TODAS') {
    const origen = ORIGENES.find((o) => o.valor === estado.origen)
    agregar('origen', '', origen?.texto || estado.origen)
  }
  for (const id of estado.idsTiposProducto || []) {
    const tipo = tipos.find((t) => String(t.idTipoProducto) === String(id))
    agregar('idsTiposProducto', id, tipo?.nombre || `Tipo ${id}`)
  }
  for (const id of estado.idsMedios || []) {
    const medio = medios.find((m) => String(m.idMedio) === String(id))
    agregar('idsMedios', id, medio?.nombre || `Medio ${id}`)
  }
  const rango = textoRangoPrecio(estado, moneda)
  if (rango) agregar('rangoPrecio', '', rango)
  if (estado.sensible !== '') {
    agregar('sensible', '', estado.sensible === 'true'
      ? 'Con contenido sensible' : 'Sin contenido sensible')
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
  if (estado.soloPiezaUnica) agregar('soloPiezaUnica', '', 'Solo piezas únicas')
  if (estado.soloIlimitado) agregar('soloIlimitado', '', 'Solo stock ilimitado')

  return chips
}

/** Conteo de resultados por opción de filtro, con la misma query que el listado. */
export async function cargarFacetas(estado) {
  const params = aParams(estado, { pagina: 0, tamanio: 24 })
  params.delete('pagina')
  params.delete('tamanio')
  const res = await api.get('/productos/facetas', { params })
  return res.data || null
}
