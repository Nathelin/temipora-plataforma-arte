import api from './api'

export async function obtenerInformeVentas(params = {}) {
  const res = await api.get('/panel-artista/informes/ventas', { params })
  return res.data
}

export async function descargarInformeVentasPdf(params = {}) {
  const res = await api.get('/panel-artista/informes/ventas/pdf', {
    params,
    responseType: 'blob',
  })
  return {
    blob: res.data,
    nombre: nombreDesdeDisposicion(
      res.headers?.['content-disposition'],
      'informe-ventas.pdf',
    ),
  }
}

function nombreDesdeDisposicion(header, fallback) {
  if (!header) return fallback
  const utf8 = header.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8) {
    try {
      return decodeURIComponent(utf8[1])
    } catch {
      return fallback
    }
  }
  const simple = header.match(/filename="?([^";]+)"?/i)
  return simple ? simple[1] : fallback
}
