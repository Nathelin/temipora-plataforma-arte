import api from './api'

export const MOTIVOS = {
  SIN_CUPOS: {
    titulo: 'Sin cupos por ahora',
    detalle:
      '',
  },
  SOLO_ESPERA: {
    titulo: 'Solo lista de espera',
    detalle:
      'El artista recibe solicitudes únicamente en lista de espera. Podés anotarte o buscar a alguien con cupo ahora.',
  },
  CERRADA: {
    titulo: 'Recepción cerrada',
    detalle: 'El artista no está recibiendo solicitudes para este servicio.',
  },
  PAUSADO: {
    titulo: 'Servicio pausado',
    detalle: 'El artista pausó este servicio y no recibe solicitudes nuevas.',
  },
  ARCHIVADO: {
    titulo: 'Servicio no disponible',
    detalle: 'Este servicio ya no está disponible en la plataforma.',
  },
  SOLICITUD_RECHAZADA: {
    titulo: 'El artista rechazó tu solicitud',
    detalle:
      'Podés buscar otro artista con características parecidas y volver a intentarlo.',
  },
  PRESUPUESTO_RETIRADO: {
    titulo: 'El artista retiró el presupuesto',
    detalle: 'El presupuesto ya no está vigente. Estas son otras propuestas con cupo.',
  },
  PRESUPUESTO_VENCIDO: {
    titulo: 'El presupuesto venció',
    detalle:
      'El cupo quedó liberado. Si todavía querés la pieza, podés elegir otra propuesta.',
  },
  SERVICIO_DADO_DE_BAJA: {
    titulo: 'El servicio ya no está disponible',
    detalle: 'El artista dio de baja este servicio. Te dejamos alternativas parecidas.',
  },
  PEDIDO_IMPAGO: {
    titulo: 'El pedido se cerró por falta de pago',
    detalle: 'Se venció el pago de un hito. Podés retomar con otro artista.',
  },
}

export function datosMotivo(motivo) {
  return MOTIVOS[motivo] || {
    titulo: 'El proceso terminó',
    detalle: 'Podés buscar otra propuesta con cupo disponible.',
  }
}

export async function buscarAlternativas(idServicioOrigen, disparador = 'DETALLE') {
  const { data } = await api.post('/sustituciones', {
    idServicioOrigen,
    disparador,
  })
  return data
}

export async function elegirAlternativa(idSustitucion, idServicio) {
  await api.post(`/sustituciones/${idSustitucion}/elegir`, { idServicio })
}

export async function descartarSustitucion(idSustitucion) {
  await api.post(`/sustituciones/${idSustitucion}/descartar`)
}

export async function detalleCierreSolicitud(idSolicitud) {
  const { data } = await api.post(`/sustituciones/por-solicitud/${idSolicitud}`)
  return data
}

export async function detalleCierrePedido(idPedido) {
  const { data } = await api.post(`/sustituciones/por-pedido/${idPedido}`)
  return data
}
