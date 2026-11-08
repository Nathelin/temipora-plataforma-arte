function dosDigitos(valor) {
  return String(valor).padStart(2, '0')
}

export function isoLocal(fecha) {
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`
    + `T${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}:${dosDigitos(fecha.getSeconds())}`
}

export function formatearFecha(valor) {
  if (!valor) return ''
  const fecha = valor instanceof Date ? valor : new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  return `${dosDigitos(fecha.getDate())}/${dosDigitos(fecha.getMonth() + 1)}/${fecha.getFullYear()}`
}

export function formatearFechaHora(valor) {
  if (!valor) return ''
  const fecha = valor instanceof Date ? valor : new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  return `${formatearFecha(fecha)} ${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`
}

/**
 * Convierte "dd/mm/aaaa HH:mm" (o "dd/mm/aaaa") a ISO local
 * "aaaa-mm-ddTHH:mm:ss". Devuelve '' si el texto no es válido.
 */
export function parsearFechaHora(texto, { finDelDia = false } = {}) {
  if (!texto) return ''
  const match = String(texto)
    .trim()
    .match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})(?:[ ,]+(\d{1,2}):(\d{2})(?::(\d{2}))?)?$/)
  if (!match) return ''

  const dia = Number(match[1])
  const mes = Number(match[2])
  const anio = Number(match[3])
  const hora = match[4] !== undefined ? Number(match[4]) : (finDelDia ? 23 : 0)
  const minuto = match[5] !== undefined ? Number(match[5]) : (finDelDia ? 59 : 0)
  const segundo = match[6] !== undefined ? Number(match[6]) : (finDelDia ? 59 : 0)

  const fecha = new Date(anio, mes - 1, dia, hora, minuto, segundo)
  const valido = fecha.getFullYear() === anio
    && fecha.getMonth() === mes - 1
    && fecha.getDate() === dia
    && fecha.getHours() === hora
    && fecha.getMinutes() === minuto

  return valido ? isoLocal(fecha) : ''
}
