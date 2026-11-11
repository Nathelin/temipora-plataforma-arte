import api from './api'

export async function cargarCarrito() {
  const res = await api.get('/carrito')
  return res.data
}

export async function agregarItem(idVariante, cantidad = 1) {
  const res = await api.post('/carrito/items', { idVariante, cantidad })
  return res.data
}

export async function actualizarItem(idItem, cantidad) {
  const res = await api.patch(`/carrito/items/${idItem}`, { cantidad })
  return res.data
}

export async function quitarItem(idItem) {
  const res = await api.delete(`/carrito/items/${idItem}`)
  return res.data
}

export async function vaciarCarrito() {
  const res = await api.delete('/carrito')
  return res.data
}

export async function checkout(claveIdempotencia) {
  const res = await api.post('/carrito/checkout', { claveIdempotencia })
  return res.data
}
