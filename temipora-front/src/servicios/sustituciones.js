import api from './api'

export const MOTIVOS = {
  SIN_CUPOS: {
    titulo: 'Sin cupos por ahora',
    detalle:
      'El artista tiene su carga laboral llena.',
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
}

export function datosMotivo(motivo) {
  return MOTIVOS[motivo] || MOTIVOS.SIN_CUPOS
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
