import api from './api'

/** Certificado público de autenticidad de una pieza única. */
export async function cargarCertificado(codigo) {
  const res = await api.get(`/certificados/${codigo}`)
  return res.data
}
