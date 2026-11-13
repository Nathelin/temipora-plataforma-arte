// Motor compartido de filtros de catálogo.
// Sirve a cualquier catálogo que describa sus dimensiones: se ocupa de leer el
// estado desde la URL, serializarlo al backend, contar activos y armar chips.

import { formatearMonto } from './moneda'

/**
 * Símbolo de la moneda según Intl, para no hardcodear "$": en USD debe verse
 * "US$" y en ARS "$". Si el entorno no lo resuelve, cae al código.
 */
export function simboloMoneda(codigo = 'ARS') {
  const iso = (codigo || 'ARS').toUpperCase()
  try {
    const partes = new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: iso,
      currencyDisplay: 'narrowSymbol',
    }).formatToParts(0)
    const simbolo = partes.find((p) => p.type === 'currency')?.value
    return simbolo || `${iso} `
  } catch {
    return `${iso} `
  }
}

/**
 * Definición de un catálogo.
 *
 * - `listas`: claves cuyo estado es un array de ids.
 * - `textos`: claves de texto libre.
 * - `booleanos`: claves de checkbox simple.
 * - `ordenes`: opciones del select de orden.
 * - `ordenPorDefecto`: valor inicial del orden.
 * - `chips`: función que, dado el estado y los catálogos auxiliares, devuelve
 *   los chips activos `{ clave, valor, texto }`. Cada catálogo decide sus
 *   etiquetas legibles.
 */
export function estadoInicial(definicion) {
  const estado = {}
  for (const clave of definicion.textos) estado[clave] = ''
  for (const clave of definicion.listas) estado[clave] = []
  for (const clave of definicion.booleanos) estado[clave] = false
  for (const clave of definicion.numericos || []) estado[clave] = ''
  estado.orden = definicion.ordenPorDefecto
  return estado
}

function listaDesde(valor) {
  if (valor === undefined || valor === null || valor === '') return []
  const crudo = Array.isArray(valor) ? valor : [valor]
  return crudo
    .flatMap((v) => String(v).split(','))
    .map((v) => v.trim())
    .filter(Boolean)
}

function primerValor(valor) {
  if (valor === undefined || valor === null) return null
  return String(Array.isArray(valor) ? valor[0] : valor)
}

export function desdeQuery(definicion, query) {
  const estado = estadoInicial(definicion)
  if (!query) return estado

  for (const clave of definicion.textos) {
    const valor = primerValor(query[clave])
    if (valor !== null) estado[clave] = valor
  }
  for (const clave of definicion.numericos || []) {
    const valor = primerValor(query[clave])
    if (valor !== null) estado[clave] = valor
  }
  for (const clave of definicion.listas) {
    estado[clave] = listaDesde(query[clave])
  }
  for (const clave of definicion.booleanos) {
    estado[clave] = query[clave] === 'true'
  }
  const orden = primerValor(query.orden)
  if (orden && definicion.ordenes.some((o) => o.valor === orden)) {
    estado.orden = orden
  }
  return estado
}

/**
 * Traduce el estado a la query string que consume el backend.
 * Las listas viajan como parámetros repetidos, que es lo que Spring
 * enlaza a `List<Long>` / `List<Enum>`.
 *
 * `mapear` permite que cada catálogo transforme claves antes de enviarlas
 * (por ejemplo, un booleano de UI que viaja como el opuesto).
 */
export function aParams(definicion, estado, {
  pagina = 0,
  tamanio = 24,
  mapear = null,
} = {}) {
  const params = new URLSearchParams()
  params.set('pagina', String(pagina))
  params.set('tamanio', String(tamanio))

  for (const clave of definicion.textos) {
    const valor = (estado[clave] || '').trim()
    if (valor) params.set(clave, valor)
  }
  for (const clave of definicion.numericos || []) {
    const valor = estado[clave]
    if (valor !== '' && valor !== null && valor !== undefined) {
      params.set(clave, valor)
    }
  }
  for (const clave of definicion.listas) {
    for (const valor of estado[clave] || []) params.append(clave, valor)
  }
  for (const clave of definicion.booleanos) {
    if (estado[clave]) params.set(clave, 'true')
  }

  if (mapear) mapear(params, estado)

  if (estado.orden && estado.orden !== definicion.ordenPorDefecto) {
    params.set('orden', estado.orden)
  }
  return params
}

/** Query de la URL, sin paginación: así "Atrás" y los links comparten estado. */
export function aQueryUrl(definicion, estado, opciones = {}) {
  const params = aParams(definicion, estado, opciones)
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

export function cantidadActivos(definicion, estado) {
  let total = 0
  for (const clave of definicion.textos) {
    const valor = estado[clave]
    if (valor !== '' && valor !== null && valor !== undefined) total++
  }
  for (const clave of definicion.numericos || []) {
    const valor = estado[clave]
    if (valor !== '' && valor !== null && valor !== undefined) total++
  }
  for (const clave of definicion.listas) {
    total += (estado[clave] || []).length
  }
  for (const clave of definicion.booleanos) {
    if (estado[clave]) total++
  }
  return total
}

/** Quita un chip del estado sin tocar el resto. */
export function quitarChip(definicion, estado, chip) {
  const siguiente = { ...estado }
  for (const clave of definicion.listas) {
    if (chip.clave === clave) {
      siguiente[clave] = (estado[clave] || [])
        .filter((v) => String(v) !== String(chip.valor))
      return siguiente
    }
  }
  if (definicion.booleanos.includes(chip.clave)) {
    siguiente[chip.clave] = false
    return siguiente
  }
  siguiente[chip.clave] = ''
  return siguiente
}

/** Helper de chips: formatea un rango de precio con el símbolo de la moneda. */
export function textoRangoPrecio(estado, moneda, prefijos = ['precioMin', 'precioMax']) {
  const [claveMin, claveMax] = prefijos
  const simbolo = simboloMoneda(moneda)
  const desde = estado[claveMin] !== '' ? formatearMonto(estado[claveMin]) : null
  const hasta = estado[claveMax] !== '' ? formatearMonto(estado[claveMax]) : null
  if (!desde && !hasta) return ''
  if (desde && hasta) return `Precio ${simbolo}${desde} – ${simbolo}${hasta}`
  return desde
    ? `Precio desde ${simbolo}${desde}`
    : `Precio hasta ${simbolo}${hasta}`
}
