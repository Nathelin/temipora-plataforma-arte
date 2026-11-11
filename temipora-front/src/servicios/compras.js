import api from './api'

export const ESTADOS_COMPRA = {
  PENDIENTE_PAGO: {
    texto: 'Pendiente de pago',
    clase: 'border-mantequilla/70 bg-mantequilla/40 text-granate',
  },
  PARCIALMENTE_PAGA: {
    texto: 'Parcialmente paga',
    clase: 'border-coral/30 bg-coral/10 text-granate',
  },
  PAGA: {
    texto: 'Paga',
    clase: 'border-green-700/30 bg-green-500/10 text-green-700',
  },
  VENCIDA: {
    texto: 'Vencida',
    clase: 'border-cafe/20 bg-cafe/10 text-cafe/60',
  },
  CANCELADA: {
    texto: 'Cancelada',
    clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
  },
}

export const ESTADOS_GRUPO = {
  PENDIENTE: {
    texto: 'Pendiente',
    clase: 'border-mantequilla/70 bg-mantequilla/40 text-granate',
  },
  PAGADO: {
    texto: 'Pagado',
    clase: 'border-green-700/30 bg-green-500/10 text-green-700',
  },
  VENCIDO: {
    texto: 'Vencido',
    clase: 'border-cafe/20 bg-cafe/10 text-cafe/60',
  },
  CANCELADO: {
    texto: 'Cancelado',
    clase: 'border-rojizo/30 bg-rojizo/10 text-rojizo',
  },
}

export const ESTADOS_ENTREGA = {
  PENDIENTE: 'Pendiente',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
}

export const GRUPOS_COMPRA = [
  { clave: 'pagadas', texto: 'Pagadas' },
  { clave: 'pendientes', texto: 'Pendientes' },
  { clave: 'archivo', texto: 'Archivo' },
  { clave: 'todas', texto: 'Todas' },
]

export const GRUPO_COMPRA_DEFAULT = 'pagadas'

export function grupoCompraDeQuery(valor) {
  return GRUPOS_COMPRA.some((grupo) => grupo.clave === valor)
    ? valor
    : GRUPO_COMPRA_DEFAULT
}

export function estadoCompraDe(estado) {
  return ESTADOS_COMPRA[estado] || {
    texto: estado || 'Sin estado',
    clase: 'border-cafe/20 bg-cafe/10 text-cafe/60',
  }
}

export function estadoGrupoDe(estado) {
  return ESTADOS_GRUPO[estado] || {
    texto: estado || 'Sin estado',
    clase: 'border-cafe/20 bg-cafe/10 text-cafe/60',
  }
}

export async function listarCompras(params = {}) {
  const res = await api.get('/compras', { params })
  return res.data
}

export async function contarCompras() {
  const res = await api.get('/compras/contadores')
  return res.data
}

export async function cargarCompra(idCompra) {
  const res = await api.get(`/compras/${idCompra}`)
  return res.data
}

export async function cargarLedgerCompraItem(idCompra, idCompraItem) {
  const res = await api.get(
    `/compras/${idCompra}/items/${idCompraItem}/ledger`,
  )
  return res.data
}

export async function pagarGrupo(idCompra, idGrupo) {
  const res = await api.post(`/compras/${idCompra}/grupos/${idGrupo}/pagar`)
  return res.data
}

export async function sincronizarCompra(idCompra) {
  const res = await api.post(`/compras/${idCompra}/sincronizar`)
  return res.data
}

export async function confirmarPagoSimulado(idIntento) {
  const res = await api.post(`/pagos/simulado/compras/${idIntento}/confirmar`)
  return res.data
}

export async function rechazarPagoSimulado(idIntento) {
  const res = await api.post(`/pagos/simulado/compras/${idIntento}/rechazar`)
  return res.data
}

export async function descargarCompra(idCompra) {
  const res = await api.get(`/compras/${idCompra}/archivo`, {
    responseType: 'blob',
  })
  return {
    blob: res.data,
    nombre: nombreDesdeDisposicion(
      res.headers?.['content-disposition'],
      `compra-${idCompra}`,
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
