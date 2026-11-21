import api from './api'

export async function conciliarMercadoPago() {
  const res = await api.post('/admin/pagos/conciliacion')
  return res.data
}
