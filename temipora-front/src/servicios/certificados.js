import api from './api'

/** Certificado público de autenticidad de una pieza única. */
export async function cargarCertificado(codigo) {
  const res = await api.get(`/certificados/${codigo}`)
  return res.data
}

/** Prueba OpenTimestamps (.ots) del anclaje de la pieza (blob descargable). */
export async function descargarPruebaCertificado(codigo) {
  const res = await api.get(`/certificados/${codigo}/prueba`, {
    responseType: 'blob',
  })
  return res.data
}

/** Documento sellado (raíz de Merkle) para verificar en OpenTimestamps (blob). */
export async function descargarDocumentoCertificado(codigo) {
  const res = await api.get(`/certificados/${codigo}/documento`, {
    responseType: 'blob',
  })
  return res.data
}
