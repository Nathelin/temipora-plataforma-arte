import api from './api'

export async function obtenerHistorialSustituciones(params = {}) {
  const res = await api.get('/admin/sustituciones/historial', { params })
  return res.data
}

export async function descargarHistorialSustitucionesPdf(params = {}) {
  const res = await api.get('/admin/sustituciones/historial/pdf', {
    params,
    responseType: 'blob',
  })
  return {
    blob: res.data,
    nombre: nombreDesdeDisposicion(
      res.headers?.['content-disposition'],
      'historial-sustituciones.pdf',
    ),
  }
}

export async function obtenerDetalleSustitucion(idSustitucion) {
  const res = await api.get(`/admin/sustituciones/historial/${idSustitucion}`)
  return res.data
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
