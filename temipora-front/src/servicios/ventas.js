export const ESTADOS_VENTA = {
  PENDIENTE: {
    etiqueta: 'Pendiente',
    clase: 'border-durazno bg-durazno/30 text-cafe',
  },
  APROBADO: {
    etiqueta: 'Aprobado',
    clase: 'border-green-700/30 bg-green-700/10 text-green-700',
  },
  RECHAZADO: {
    etiqueta: 'Rechazado',
    clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
  },
  REEMBOLSADO: {
    etiqueta: 'Reembolsado',
    clase: 'border-cafe/25 bg-cafe/5 text-cafe/70',
  },
  CANCELADO: {
    etiqueta: 'Cancelado',
    clase: 'border-cafe/25 bg-cafe/5 text-cafe/60',
  },
}

export const FILTROS_ESTADO_VENTA = [
  { valor: '', etiqueta: 'Todos los estados' },
  { valor: 'APROBADO', etiqueta: 'Aprobados' },
  { valor: 'PENDIENTE', etiqueta: 'Pendientes' },
  { valor: 'RECHAZADO', etiqueta: 'Rechazados' },
  { valor: 'REEMBOLSADO', etiqueta: 'Reembolsados' },
  { valor: 'CANCELADO', etiqueta: 'Cancelados' },
]

export function estadoVentaDe(estado) {
  return ESTADOS_VENTA[estado] || {
    etiqueta: estado || 'Sin estado',
    clase: 'border-rosa bg-blanco text-cafe/60',
  }
}
