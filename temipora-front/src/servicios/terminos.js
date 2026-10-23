export const SECCIONES_TERMINOS = [
  { key: 'informacionGeneral', label: 'Información general' },
  { key: 'politicaRevisiones', label: 'Política de revisiones' },
  { key: 'politicaCancelaciones', label: 'Política de cancelaciones' },
  { key: 'usoArtista', label: 'Uso del artista' },
  { key: 'usoCliente', label: 'Uso del cliente' },
  { key: 'queDibuja', label: 'Qué dibujo' },
  { key: 'queNoDibuja', label: 'Qué no dibujo' },
]

const SECCIONES_CLAVE = ['informacionGeneral', 'usoCliente']

export function hayContenidoTerminos(terminos) {
  const form = terminos?.form || {}
  return SECCIONES_TERMINOS.some((seccion) => (form[seccion.key] || '').trim())
}

export function calcularCompletitudTerminos(terminos) {
  const form = terminos?.form || {}
  const faltantes = SECCIONES_TERMINOS
    .filter((seccion) => !(form[seccion.key] || '').trim())
    .map((seccion) => seccion.label)
  const seccionesCompletas = SECCIONES_TERMINOS.length - faltantes.length
  const porcentaje = Math.round((seccionesCompletas / SECCIONES_TERMINOS.length) * 100)

  const faltantesClave = SECCIONES_TERMINOS
    .filter((seccion) => SECCIONES_CLAVE.includes(seccion.key) && !(form[seccion.key] || '').trim())
    .map((seccion) => seccion.label)

  const tieneContenido = hayContenidoTerminos(terminos)

  return {
    seccionesCompletas,
    totalSecciones: SECCIONES_TERMINOS.length,
    porcentaje,
    faltantes,
    faltantesClave,
    tieneContenido,
    requiereAviso: tieneContenido && (porcentaje < 70 || faltantesClave.length > 0),
  }
}
