import api from './api'

/** Listado de anclajes (sellos OpenTimestamps) del ledger. */
export async function listarAnclajes() {
  const res = await api.get('/admin/anclaje')
  return res.data
}

/** Fuerza el sellado de los eventos nuevos y reintenta confirmar pendientes. */
export async function ejecutarAnclaje() {
  const res = await api.post('/admin/anclaje/ejecutar')
  return res.data
}

/** Descarga el archivo .ots (prueba OpenTimestamps) de un anclaje. */
export async function descargarPruebaAnclaje(idAnclaje) {
  const res = await api.get(`/admin/anclaje/${idAnclaje}/prueba`, {
    responseType: 'blob',
  })
  return res.data
}
