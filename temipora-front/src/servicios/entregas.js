import api from './api'

export const MAX_ARCHIVOS_ENTREGA = 20
export const MAX_BYTES_ENTREGA = 25 * 1024 * 1024
export const MAX_BYTES_TOTAL_ENTREGA = 100 * 1024 * 1024

export function validarArchivoEntrega(archivo) {
  if (!archivo) return 'Seleccioná un archivo.'
  if (!String(archivo.type || '').startsWith('image/')) {
    return 'Cada archivo tiene que ser una imagen.'
  }
  if (archivo.size > MAX_BYTES_ENTREGA) {
    return 'Cada archivo puede pesar como máximo 25 MB.'
  }
  return ''
}

export async function crearEntrega(idPedido, idHito, descripcion, archivos) {
  const datos = {
    descripcion: descripcion?.trim() || null,
    archivos: archivos.map((archivo) => ({
      idItemSolicitud: archivo.idItemSolicitud || null,
      nombreVisible: archivo.file?.name || null,
    })),
  }

  const formData = new FormData()
  formData.append(
    'datos',
    new Blob([JSON.stringify(datos)], { type: 'application/json' }),
  )
  archivos.forEach((archivo) => formData.append('archivos', archivo.file))

  return api.post(`/pedidos/${idPedido}/hitos/${idHito}/entregas`, formData)
}
