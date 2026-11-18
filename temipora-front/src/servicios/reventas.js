import api from './api'

export async function listarReventas(params = {}) {
  const res = await api.get('/reventas', { params })
  return res.data
}

export async function cargarReventa(id) {
  const res = await api.get(`/reventas/${id}`)
  return res.data
}

export async function misReventas() {
  const res = await api.get('/reventas/mias')
  return res.data
}

export async function piezasRevendibles() {
  const res = await api.get('/reventas/elegibles')
  return res.data
}

export async function publicarReventa(idCompraItem, precio) {
  const res = await api.post('/reventas', { idCompraItem, precio })
  return res.data
}

export async function editarPrecioReventa(idPublicacion, precio) {
  const res = await api.patch(`/reventas/${idPublicacion}`, { precio })
  return res.data
}

export async function archivarReventa(idPublicacion) {
  const res = await api.delete(`/reventas/${idPublicacion}`)
  return res.data
}
