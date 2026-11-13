export const ETIQUETAS_EVENTO = {
  GENESIS: 'Origen',
  VENTA: 'Venta',
  TRANSFERENCIA: 'Transferencia',
  REVENTA: 'Reventa',
  ANULACION: 'Anulación',
}

export function etiquetaEvento(tipo) {
  return ETIQUETAS_EVENTO[tipo] || tipo || '—'
}

export function hashCorto(hash) {
  if (!hash) return '—'
  return `${hash.slice(0, 10)}…${hash.slice(-6)}`
}

/** Estado del anclaje en Bitcoin de un evento o variante. */
export const ESTADO_ANCLAJE = {
  CONFIRMADO: {
    texto: 'Sellado en Bitcoin',
    clase: 'bg-green-500/10 text-green-700',
    punto: 'bg-green-600',
  },
  PENDIENTE: {
    texto: 'Sellado en Bitcoin (confirmando)',
    clase: 'bg-amber-500/10 text-amber-700',
    punto: 'bg-amber-500',
  },
}

/**
 * Devuelve la info de anclaje a mostrar, o null si no hay sello.
 * Confirmado cuando hay altura de bloque; pendiente mientras tanto.
 */
export function infoAnclaje(anclado, bitcoinAltura) {
  if (!anclado) return null
  const base = bitcoinAltura ? ESTADO_ANCLAJE.CONFIRMADO : ESTADO_ANCLAJE.PENDIENTE
  return { ...base, bitcoinAltura: bitcoinAltura || null }
}
