import api from './api'

export const CONTACTO_FALLBACK = 'temiporasupport@gmail.com'

let cache = null
let enCurso = null

export async function obtenerContacto() {
  if (cache) return cache
  if (!enCurso) {
    enCurso = api
      .get('/contacto')
      .then((res) => {
        cache = res.data?.email || CONTACTO_FALLBACK
        return cache
      })
      .catch(() => CONTACTO_FALLBACK)
      .finally(() => {
        enCurso = null
      })
  }
  return enCurso
}
