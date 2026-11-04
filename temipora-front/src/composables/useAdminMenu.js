import { ref, watch } from 'vue'

const CLAVE = 'adminMenuColapsada'
const colapsada = ref(localStorage.getItem(CLAVE) === '1')

watch(colapsada, (valor) => {
  try {
    localStorage.setItem(CLAVE, valor ? '1' : '0')
  } catch {
    /* sin persistencia disponible */
  }
})

export function useAdminMenu() {
  return { colapsada }
}
