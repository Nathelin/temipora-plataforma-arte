const LOCALE = 'es-AR'

export function formatearPrecio(valor, moneda = 'ARS') {
  if (valor === null || valor === undefined || valor === '') return '—'
  const numero = Number(valor)
  if (Number.isNaN(numero)) return '—'
  try {
    return new Intl.NumberFormat(LOCALE, {
      style: 'currency',
      currency: (moneda || 'ARS').toUpperCase(),
      maximumFractionDigits: 2,
    }).format(numero)
  } catch {
    return `${moneda || ''} ${numero}`.trim()
  }
}

export function etiquetaMoneda(m) {
  if (!m) return ''
  return m.nombre ? `${m.nombre} (${m.codigo})` : m.codigo
}
