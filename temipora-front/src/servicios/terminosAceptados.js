import api from './api'

/** Registro público de los términos aceptados en un pedido (por código). */
export async function cargarTerminosAceptados(codigo) {
  const res = await api.get(`/terminos-aceptados/${encodeURIComponent(codigo)}`)
  return res.data
}
