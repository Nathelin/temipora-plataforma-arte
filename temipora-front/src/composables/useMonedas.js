import { ref } from 'vue'
import api from '../servicios/api'

const monedas = ref([])
const cargando = ref(false)
let cargadas = false
let enCurso = null

async function cargar(forzar = false) {
  if (cargadas && !forzar) return monedas.value
  if (enCurso) return enCurso
  cargando.value = true
  enCurso = api
    .get('/monedas')
    .then((res) => {
      monedas.value = res.data
      cargadas = true
      return monedas.value
    })
    .finally(() => {
      cargando.value = false
      enCurso = null
    })
  return enCurso
}

function predeterminada() {
  return monedas.value.find((m) => m.predeterminada) || monedas.value[0] || null
}

export function useMonedas() {
  return { monedas, cargando, cargar, predeterminada }
}
