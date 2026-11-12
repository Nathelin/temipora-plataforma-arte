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
