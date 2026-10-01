const formatosImagenPermitidos = ['image/jpeg', 'image/png', 'image/webp']

export function esImagenPermitida(archivo) {
  return Boolean(archivo) && formatosImagenPermitidos.includes(archivo.type)
}

export const acceptImagenes = '.jpg,.jpeg,.png,.webp'
