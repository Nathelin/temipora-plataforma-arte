import { computed, ref } from 'vue'

const CLAVE = 'temipora-tema'
const TEMAS = ['claro', 'noche']
const tema = ref(document.documentElement.dataset.theme === 'noche' ? 'noche' : 'claro')
const esOscuro = computed(() => tema.value === 'noche')

function actualizarMeta(nuevoTema) {
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) {
    meta.setAttribute('content', nuevoTema === 'noche' ? '#241018' : '#fff6f1')
  }
}

function aplicarTema(nuevoTema) {
  tema.value = TEMAS.includes(nuevoTema) ? nuevoTema : 'claro'
  document.documentElement.dataset.theme = tema.value
  document.documentElement.style.colorScheme = tema.value === 'noche' ? 'dark' : 'light'
  try {
    localStorage.setItem(CLAVE, tema.value)
  } catch (error) {
    /* almacenamiento no disponible */
  }
  actualizarMeta(tema.value)
}

function alternarTema() {
  aplicarTema(tema.value === 'noche' ? 'claro' : 'noche')
}

actualizarMeta(tema.value)

export function useTema() {
  return { tema, esOscuro, aplicarTema, alternarTema }
}
