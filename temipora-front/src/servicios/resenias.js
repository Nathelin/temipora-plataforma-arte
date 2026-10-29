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

export const AMBITOS_RESENIA = [
  { valor: 'SERVICIO', etiqueta: 'Servicio (cliente → artista)' },
  { valor: 'CLIENTE', etiqueta: 'Cliente (artista → cliente)' },
  { valor: 'PRODUCTO', etiqueta: 'Producto (sin flujo todavía)' },
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
