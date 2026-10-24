export const ESTADOS_SERVICIO = {
  PUBLICADO: { texto: 'Publicado', clase: 'border-green-600/25 bg-green-500/10 text-green-700' },
  PAUSADO: { texto: 'Pausado', clase: 'border-durazno bg-durazno/50 text-granate' },
  ARCHIVADO: { texto: 'Archivado', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
}

export function estadoServicioDe(codigo) {
  return ESTADOS_SERVICIO[codigo] || {
    texto: codigo || 'Sin estado',
    clase: 'border-rosa bg-rosa/20 text-cafe',
  }
}

export function servicioDisponible(estadoServicio) {
  return !estadoServicio || estadoServicio === 'PUBLICADO'
}
