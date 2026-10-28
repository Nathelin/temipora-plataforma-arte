export const ESTADOS_PEDIDO = {
  PENDIENTE_PAGO: {
    etiqueta: 'Pendiente de pago',
    clase: 'border-durazno bg-durazno/30 text-cafe',
  },
  EN_CURSO: {
    etiqueta: 'En curso',
    clase: 'border-coral/30 bg-coral/10 text-coral',
  },
  FINALIZADO: {
    etiqueta: 'Finalizado',
    clase: 'border-green-700/30 bg-green-700/10 text-green-700',
  },
  CANCELADO: {
    etiqueta: 'Cancelado',
    clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
  },
  IMPAGO: {
    etiqueta: 'Sin pago',
    clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
  },
}

export const ESTADOS_PAGO_HITO = {
  PENDIENTE: 'Pendiente',
  PAGADO: 'Pagado',
  VENCIDO: 'Vencido',
  REEMBOLSADO: 'Reembolsado',
  CANCELADO: 'Cancelado',
}

export const ESTADOS_ENTREGA = {
  NO_INICIADO: 'No iniciado',
  EN_PROCESO: 'En proceso',
  ENTREGADO: 'Entregado',
  APROBADO: 'Aprobado',
  APROBADO_AUTOMATICAMENTE: 'Aprobado automáticamente',
  CON_REVISION: 'Con revisión',
}

export function estadoPedidoDe(estado) {
  return ESTADOS_PEDIDO[estado] || {
    etiqueta: estado || 'Sin estado',
    clase: 'border-rosa bg-blanco text-cafe/60',
  }
}

export function etiquetaPago(estado) {
  return ESTADOS_PAGO_HITO[estado] || estado || '—'
}

export function etiquetaEntrega(estado) {
  return ESTADOS_ENTREGA[estado] || estado || '—'
}

export function etiquetaTipoHito(tipo) {
  if (tipo === 'SENA') return 'Seña'
  if (tipo === 'ENTREGA') return 'Entrega final'
  return 'Cuota'
}

export function descripcionHito(hito) {
  if (hito?.tipo === 'ENTREGA') {
    return 'Se paga contra entrega, al ver el preview protegido.'
  }
  if (hito?.tipo === 'CUOTA') {
    return 'Se paga después de aprobar el avance anterior.'
  }
  return hito?.requiereEntrega
    ? 'Se paga al aceptar; incluye un avance para aprobar.'
    : 'Se paga al aceptar el presupuesto.'
}

export function pagoPendiente(hito) {
  return (hito?.pagos || []).find((pago) => pago.estado === 'PENDIENTE') || null
}

export function formatearFecha(valor) {
  if (!valor) return '—'
  try {
    return new Date(valor).toLocaleDateString('es-AR')
  } catch {
    return valor
  }
}

export function formatearFechaHora(valor) {
  if (!valor) return '—'
  try {
    return new Date(valor).toLocaleString('es-AR', {
      dateStyle: 'short',
      timeStyle: 'short',
    })
  } catch {
    return valor
  }
}
