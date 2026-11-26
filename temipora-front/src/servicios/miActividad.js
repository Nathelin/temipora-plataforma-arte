import api from './api'

export async function cargarResumenActividad() {
  const res = await api.get('/panel-comprador/resumen')
  return res.data
}
