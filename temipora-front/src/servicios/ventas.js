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

export const ESTADOS_VENTA_PRODUCTO = {
  PAGADO: {
    etiqueta: 'Pagado',
    clase: 'border-green-700/30 bg-green-700/10 text-green-700',
  },
  PENDIENTE: {
    etiqueta: 'Pendiente',
    clase: 'border-durazno bg-durazno/30 text-cafe',
  },
  VENCIDO: {
    etiqueta: 'Vencido',
    clase: 'border-cafe/25 bg-cafe/5 text-cafe/70',
  },
  CANCELADO: {
    etiqueta: 'Cancelado',
    clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
  },
}

export function estadoVentaProductoDe(estado) {
  return ESTADOS_VENTA_PRODUCTO[estado] || {
    etiqueta: estado || 'Sin estado',
    clase: 'border-rosa bg-blanco text-cafe/60',
  }
}

const TIPOS_MEDIO_PAGO = {
  credit_card: 'Tarjeta de crédito',
  debit_card: 'Tarjeta de débito',
  account_money: 'Dinero en cuenta',
  bank_transfer: 'Transferencia',
  ticket: 'Efectivo',
  atm: 'Cajero automático',
}

const MARCAS = {
  visa: 'Visa',
  master: 'Mastercard',
  amex: 'American Express',
  debvisa: 'Visa débito',
  debmaster: 'Mastercard débito',
  maestro: 'Maestro',
  cabal: 'Cabal',
  naranja: 'Naranja X',
}

function capitalizar(texto) {
  if (!texto) return ''
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

export function descripcionMedioPago(venta) {
  if (!venta) return '—'
  const tipo = TIPOS_MEDIO_PAGO[venta.tipoMedioPago] || null
  const marca = MARCAS[venta.medioPago]
    || (venta.medioPago ? capitalizar(venta.medioPago) : null)

  if (venta.tipoMedioPago === 'credit_card'
    || venta.tipoMedioPago === 'debit_card') {
    const partes = [marca]
    if (venta.ultimosCuatro) partes.push(`•••• ${venta.ultimosCuatro}`)
    if (Number(venta.cuotas) > 1) partes.push(`${venta.cuotas} cuotas`)
    return partes.filter(Boolean).join(' · ') || tipo || '—'
  }

  return tipo || marca || '—'
}

export function necesitaSincronizar(venta) {
  return Boolean(venta?.puedeSincronizar)
    && venta.estado === 'APROBADO'
    && !venta.fechaAprobacion
}
