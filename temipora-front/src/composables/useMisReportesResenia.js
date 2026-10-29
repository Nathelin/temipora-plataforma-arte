import { ref } from 'vue'
import { cargarMisReportes } from '../servicios/resenias'

const ids = ref(new Set())
const cargando = ref(false)
const cargado = ref(false)
let cargaEnCurso = null
let tokenCargado = null

async function asegurarCarga() {
  const token = localStorage.getItem('token')
  if (token !== tokenCargado) {
    cargado.value = false
    ids.value = new Set()
    cargaEnCurso = null
    tokenCargado = token
  }

  if (cargado.value) return
  if (cargando.value) return cargaEnCurso
  if (!token) {
    cargado.value = true
    return
  }

  cargando.value = true
  cargaEnCurso = cargarMisReportes()
    .then((res) => {
      if (localStorage.getItem('token') === token) {
        ids.value = new Set((res.data || []).map(Number))
      }
    })
    .catch(() => {
      ids.value = new Set()
    })
    .finally(() => {
      cargando.value = false
      cargado.value = true
      cargaEnCurso = null
    })
  return cargaEnCurso
}

function marcarReportada(idResenia) {
  const siguiente = new Set(ids.value)
  siguiente.add(Number(idResenia))
  ids.value = siguiente
}

function estaReportada(idResenia) {
  return ids.value.has(Number(idResenia))
}

export function useMisReportesResenia() {
  return {
    ids,
    cargando,
    asegurarCarga,
    marcarReportada,
    estaReportada,
  }
}
