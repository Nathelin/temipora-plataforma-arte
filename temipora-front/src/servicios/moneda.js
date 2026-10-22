const LOCALE = 'es-AR'

export function formatearPrecio(valor, moneda = 'ARS') {
  if (valor === null || valor === undefined || valor === '') return '—'
  const numero = Number(valor)
  if (Number.isNaN(numero)) return '—'
  try {
    return new Intl.NumberFormat(LOCALE, {
      style: 'currency',
      currency: (moneda || 'ARS').toUpperCase(),
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(numero)
  } catch {
    return `${moneda || ''} ${numero}`.trim()
  }
}

export function formatearMonto(valor) {
  if (valor === null || valor === undefined || valor === '') return ''
  const numero = Number(valor)
  if (!Number.isFinite(numero)) return ''
  return new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 2 }).format(numero)
}

export function formatearMontoEnEdicion(texto) {
  const limpio = String(texto ?? '').replace(/\./g, '').replace(/[^\d,]/g, '')
  if (!limpio) return ''

  const [parteEntera, ...partesDecimales] = limpio.split(',')
  const entero = parteEntera.replace(/^0+(?=\d)/, '')
  const enteroConMiles = entero.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  if (!partesDecimales.length) return enteroConMiles

  const decimales = partesDecimales.join('').slice(0, 2)
  return `${enteroConMiles || '0'},${decimales}`
}

export function parsearMonto(texto) {
  if (texto === null || texto === undefined || texto === '') return null
  const limpio = String(texto).replace(/\./g, '').replace(',', '.').trim()
  if (!limpio) return null
  const numero = Number(limpio)
  return Number.isFinite(numero) ? numero : null
}

export function etiquetaMoneda(m) {
  if (!m) return ''
  return m.nombre ? `${m.nombre} (${m.codigo})` : m.codigo
}
