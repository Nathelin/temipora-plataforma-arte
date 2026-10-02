const formatosImagenPermitidos = ['image/jpeg', 'image/png', 'image/webp']

export const TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024

export const LIMITES_IMAGEN = {
  avatar: 512,
  banner: 1920,
  obra: 1600,
}

export function esImagenPermitida(archivo) {
  return Boolean(archivo) && formatosImagenPermitidos.includes(archivo.type)
}

export function esTamanoPermitido(archivo) {
  return Boolean(archivo) && archivo.size <= TAMANO_MAXIMO_BYTES
}

export function validarImagen(archivo) {
  if (!archivo) return 'Selecciona una imagen.'
  if (!esImagenPermitida(archivo)) return 'El formato de la imagen es inválido.'
  if (!esTamanoPermitido(archivo)) return 'La imagen no puede superar 5MB.'
  return ''
}

export const acceptImagenes = '.jpg,.jpeg,.png,.webp'

function decodificarConImg(archivo) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(archivo)
    const img = new Image()
    img.onload = () => {
      resolve(img)
      URL.revokeObjectURL(url)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('No se pudo leer la imagen.'))
    }
    img.src = url
  })
}

async function decodificarImagen(archivo) {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(archivo, { imageOrientation: 'from-image' })
    } catch {
      try {
        return await createImageBitmap(archivo)
      } catch {
        // continúa con el fallback por <img>
      }
    }
  }
  return decodificarConImg(archivo)
}

function aBlob(canvas, tipo, calidad) {
  return new Promise((resolve) => canvas.toBlob(resolve, tipo, calidad))
}

export async function procesarImagen(archivo, maxLado) {
  const fuente = await decodificarImagen(archivo)
  const anchoOriginal = fuente.width
  const altoOriginal = fuente.height

  const escala = Math.min(1, maxLado / Math.max(anchoOriginal, altoOriginal))
  const ancho = Math.max(1, Math.round(anchoOriginal * escala))
  const alto = Math.max(1, Math.round(altoOriginal * escala))

  const canvas = document.createElement('canvas')
  canvas.width = ancho
  canvas.height = alto
  const contexto = canvas.getContext('2d')
  contexto.drawImage(fuente, 0, 0, ancho, alto)
  if (typeof fuente.close === 'function') fuente.close()

  const base = (archivo.name || 'imagen').replace(/\.[^.]+$/, '') || 'imagen'

  let blob = await aBlob(canvas, 'image/webp', 0.82)
  if (blob && blob.type === 'image/webp') {
    return new File([blob], `${base}.webp`, { type: 'image/webp' })
  }

  blob = await aBlob(canvas, 'image/jpeg', 0.85)
  if (!blob) {
    throw new Error('No se pudo procesar la imagen.')
  }
  return new File([blob], `${base}.jpg`, { type: 'image/jpeg' })
}
