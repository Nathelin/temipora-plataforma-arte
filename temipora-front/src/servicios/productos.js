import api from './api'

const ESTADOS_PRODUCTO = {
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
    clase: 'border-cafe/15 bg-cafe/10 text-cafe/60',
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
    clase: 'border-cafe/15 bg-cafe/10 text-cafe/60',
  },
  PAUSADO: {
    texto: 'Pausado',
    clase: 'border-cafe/15 bg-mantequilla/50 text-granate',
  },
  ARCHIVADO: {
    texto: 'Archivado',
    clase: 'border-cafe/15 bg-cafe/10 text-cafe/60',
  },
  EN_REVENTA: {
    texto: 'En reventa',
    clase: 'border-coral/20 bg-rosa/40 text-granate',
  },
}

const FALLBACK = {
  texto: 'Sin estado',
  clase: 'border-cafe/15 bg-cafe/10 text-cafe/60',
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

export function esPiezaUnica(variante) {
  return !esIlimitada(variante) && Number(variante.stockDisponible) === 1
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

export function terminosComoForm(terminos) {
  return {
    form: {
      informacionGeneral: terminos?.informacionGeneral || '',
      politicaRevisiones: terminos?.politicaRevisiones || '',
      politicaCancelaciones: terminos?.politicaCancelaciones || '',
      usoArtista: terminos?.usoArtista || '',
      usoCliente: terminos?.usoCliente || '',
      queDibuja: terminos?.queDibuja || '',
      queNoDibuja: terminos?.queNoDibuja || '',
    },
  }
}

export function hayTerminos(terminos) {
  const claves = [
    'informacionGeneral',
    'politicaRevisiones',
    'politicaCancelaciones',
    'usoArtista',
    'usoCliente',
    'queDibuja',
    'queNoDibuja',
  ]
  return claves.some((clave) => String(terminos?.[clave] || '').trim())
}
