import api from './api'

export function cargarCriterios(ambito) {
  return api.get('/criterios-resenia', { params: { ambito } })
}

export function cargarEstadoResenia(idPedido) {
  return api.get(`/pedidos/${idPedido}/resenia`)
}

export function crearResenia(idPedido, payload) {
  return api.post(`/pedidos/${idPedido}/resenia`, payload)
}

export function editarResenia(idPedido, payload) {
  return api.put(`/pedidos/${idPedido}/resenia`, payload)
}

export function eliminarResenia(idPedido) {
  return api.delete(`/pedidos/${idPedido}/resenia`)
}

export function cargarReputacionCliente(idCliente) {
  return api.get(`/clientes/${idCliente}/reputacion`)
}

export function cargarEstadoReseniaProducto(idProducto) {
  return api.get(`/productos/${idProducto}/resenia`)
}

export function crearReseniaProducto(idProducto, payload) {
  return api.post(`/productos/${idProducto}/resenia`, payload)
}

export function editarReseniaProducto(idProducto, payload) {
  return api.put(`/productos/${idProducto}/resenia`, payload)
}

export function eliminarReseniaProducto(idProducto) {
  return api.delete(`/productos/${idProducto}/resenia`)
}

export function cargarReseniasProducto(idProducto, params = {}) {
  return api.get(`/productos/${idProducto}/resenias`, { params })
}

export function cargarReseniasServicio(idServicio, params = {}) {
  return api.get(`/servicios/${idServicio}/resenias`, { params })
}

export function cargarReseniasArtista(nombreUsuario, params = {}) {
  return api.get(`/perfil/${encodeURIComponent(nombreUsuario)}/resenias`, { params })
}

export function cargarResumenReseniasArtista(nombreUsuario) {
  return api.get(`/perfil/${encodeURIComponent(nombreUsuario)}/resumen-resenias`)
}

export const ORDENES_RESENIAS = [
  { valor: 'RECIENTES', etiqueta: 'Más recientes' },
  { valor: 'MEJOR', etiqueta: 'Mejor calificadas' },
  { valor: 'PEOR', etiqueta: 'Peor calificadas' },
]

export function etiquetaOrden(codigo) {
  return ORDENES_RESENIAS.find((item) => item.valor === codigo)?.etiqueta || 'Más recientes'
}

export const AMBITOS_RESENIA = [
  { valor: 'SERVICIO', etiqueta: 'Servicio (cliente → artista)' },
  { valor: 'CLIENTE', etiqueta: 'Cliente (artista → cliente)' },
  { valor: 'PRODUCTO', etiqueta: 'Producto (comprador → pieza)' },
]

export const ESCALAS_CRITERIO = [
  { valor: 'ESTRELLAS', etiqueta: 'Estrellas (1 a 5)' },
  { valor: 'OPCIONES_MULTIPLES', etiqueta: 'Chips (opciones múltiples)' },
]

export const POLARIDADES_OPCION = [
  { valor: 'POSITIVA', etiqueta: 'Positiva' },
  { valor: 'NEGATIVA', etiqueta: 'Negativa' },
]

export const ETIQUETAS_REPUTACION = {
  EXCELENTE: {
    etiqueta: 'Excelente',
    clase: 'border-green-700/25 bg-green-500/10 text-green-700',
  },
  BUENA: {
    etiqueta: 'Buena',
    clase: 'border-coral/25 bg-coral/10 text-coral',
  },
  REGULAR: {
    etiqueta: 'Regular',
    clase: 'border-durazno bg-durazno/40 text-granate',
  },
  SIN_DATOS: {
    etiqueta: 'Sin historial suficiente',
    clase: 'border-cafe/15 bg-cafe/5 text-cafe/60',
  },
}

export function etiquetaReputacionDe(codigo) {
  return ETIQUETAS_REPUTACION[codigo] || ETIQUETAS_REPUTACION.SIN_DATOS
}

export function etiquetaAmbito(codigo) {
  return AMBITOS_RESENIA.find((item) => item.valor === codigo)?.etiqueta || codigo
}

export function etiquetaEscala(codigo) {
  return ESCALAS_CRITERIO.find((item) => item.valor === codigo)?.etiqueta || codigo
}

export function etiquetaPolaridad(codigo) {
  return POLARIDADES_OPCION.find((item) => item.valor === codigo)?.etiqueta || codigo
}

export function formatearPromedio(valor) {
  if (valor == null) return '—'
  return new Intl.NumberFormat('es-AR', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(Number(valor))
}

export const MOTIVOS_REPORTE = [
  { valor: 'OFENSIVA', etiqueta: 'Contenido ofensivo' },
  { valor: 'SPAM', etiqueta: 'Spam o publicidad' },
  { valor: 'DATOS_PERSONALES', etiqueta: 'Datos personales' },
  { valor: 'FALSA', etiqueta: 'Información falsa' },
  { valor: 'OTRO', etiqueta: 'Otro motivo' },
]

export const ESTADOS_REPORTE = [
  { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
  { valor: 'RESUELTO', etiqueta: 'Resuelto' },
  { valor: 'DESCARTADO', etiqueta: 'Descartado' },
]

export const ACCIONES_REPORTE = [
  { valor: 'OCULTAR', etiqueta: 'Reseña ocultada' },
  { valor: 'DESCARTAR', etiqueta: 'Reporte descartado' },
]

export function etiquetaMotivoReporte(codigo) {
  return MOTIVOS_REPORTE.find((item) => item.valor === codigo)?.etiqueta || codigo
}

export function etiquetaEstadoReporte(codigo) {
  return ESTADOS_REPORTE.find((item) => item.valor === codigo)?.etiqueta || codigo
}

export function etiquetaAccionReporte(codigo) {
  return ACCIONES_REPORTE.find((item) => item.valor === codigo)?.etiqueta || codigo
}

export function reportarResenia(idResenia, payload) {
  return api.post(`/resenias/${idResenia}/reportes`, payload)
}

export function cargarMisReportes() {
  return api.get('/resenias/reportes/mios')
}

export function cargarReseniasReportadas(params = {}) {
  return api.get('/admin/resenias/reportadas', { params })
}

export function cargarReseniasOcultas(params = {}) {
  return api.get('/admin/resenias/ocultas', { params })
}

export function cargarHistorialReportes(params = {}) {
  return api.get('/admin/resenias/historial', { params })
}

export function ocultarResenia(idResenia, payload = {}) {
  return api.post(`/admin/resenias/${idResenia}/ocultar`, payload)
}

export function descartarReportesResenia(idResenia) {
  return api.post(`/admin/resenias/${idResenia}/descartar-reportes`)
}

export function restaurarResenia(idResenia) {
  return api.post(`/admin/resenias/${idResenia}/restaurar`)
}
