import api from './api'

const ESTADOS_PRODUCTO = {
  BORRADOR: {
    texto: 'Borrador',
    clase: 'border-cafe/20 bg-cafe/10 text-cafe',
  },
  DISPONIBLE: {
    texto: 'Disponible',
    clase: 'border-green-600/20 bg-green-500/10 text-green-700',
  },
  PAUSADO: {
    texto: 'Pausado',
    clase: 'border-cafe/15 bg-mantequilla/50 text-granate',
  },
  ARCHIVADO: {
    texto: 'Archivado',
    clase: 'border-cafe/15 bg-cafe/10 text-cafe',
  },
}

const ESTADOS_VARIANTE = {
  DISPONIBLE: {
    texto: 'Disponible',
    clase: 'border-green-600/20 bg-green-500/10 text-green-700',
  },
  RESERVADO: {
    texto: 'Reservado',
    clase: 'border-cafe/15 bg-mantequilla/50 text-granate',
  },
  AGOTADO: {
    texto: 'Agotado',
    clase: 'border-cafe/15 bg-cafe/10 text-cafe',
  },
  PAUSADO: {
    texto: 'Pausado',
    clase: 'border-cafe/15 bg-mantequilla/50 text-granate',
  },
  ARCHIVADO: {
    texto: 'Archivado',
    clase: 'border-cafe/15 bg-cafe/10 text-cafe',
  },
  EN_REVENTA: {
    texto: 'En reventa',
    clase: 'border-coral/20 bg-rosa/40 text-granate',
  },
}

const FALLBACK = {
  texto: 'Sin estado',
  clase: 'border-cafe/15 bg-cafe/10 text-cafe',
}

export function estadoProductoDe(estado) {
  return ESTADOS_PRODUCTO[estado] || FALLBACK
}

export function estadoVarianteDe(estado) {
  return ESTADOS_VARIANTE[estado] || FALLBACK
}

export function esIlimitada(variante) {
  return variante?.stockDisponible === null || variante?.stockDisponible === undefined
}

export function etiquetaStock(variante) {
  if (esIlimitada(variante)) return 'Ilimitado'
  const stock = Number(variante.stockDisponible)
  if (stock === 0) return 'Sin stock'
  if (stock === 1) return 'Pieza única'
  return `${stock} disponibles`
}

export function tieneLedger(producto) {
  return (producto?.variantes || []).some((variante) => variante.tieneLedger)
}

export function precioDesdeDe(producto) {
  const precios = (producto?.variantes || [])
    .map((variante) => Number(variante.precio))
    .filter((precio) => Number.isFinite(precio) && precio > 0)
  return precios.length ? Math.min(...precios) : null
}

export async function subirImagenProducto(archivo) {
  const fd = new FormData()
  fd.append('archivo', archivo)
  const res = await api.post('/mis-productos/imagenes', fd)
  return res.data
}

// ---------------------------------------------------------------------
// Catálogo público
// ---------------------------------------------------------------------

export async function cargarCatalogoProductos(params = {}) {
  const res = await api.get('/productos', { params })
  return res.data
}

export async function cargarProductoPublico(idProducto) {
  const res = await api.get(`/productos/${idProducto}`)
  return res.data
}

export async function cargarVariantesProductoPublico(idProducto, params = {}) {
  const res = await api.get(`/productos/${idProducto}/variantes`, { params })
  return res.data
}

export async function cargarLedgerProductoPublico(idProducto, params = {}) {
  const res = await api.get(`/productos/${idProducto}/ledger`, { params })
  return res.data
}

export async function cargarProductosDeArtista(nombreUsuario) {
  const res = await api.get(`/productos/artista/${nombreUsuario}`)
  return res.data
}

// ---------------------------------------------------------------------
// Favoritos de producto
// ---------------------------------------------------------------------

export async function cargarIdsProductosFavoritos() {
  const res = await api.get('/favoritos/productos/ids')
  return res.data
}

export async function agregarProductoFavorito(idProducto) {
  await api.post(`/favoritos/productos/${idProducto}`)
}

export async function eliminarProductoFavorito(idProducto) {
  await api.delete(`/favoritos/productos/${idProducto}`)
}

export function etiquetaDisponibilidad(producto) {
  if (!producto) return ''
  if (!producto.disponible) return 'Sin stock'
  if (producto.esPiezaUnica) return 'Pieza única'
  if (producto.tieneStockIlimitado) return 'Disponible'
  return `${producto.stockTotal} disponibles`
}
