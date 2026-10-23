import { computed, ref } from 'vue'

const CLAVE = 'temipora-tema'
const FAMILIAS = ['calida', 'azul']
const MODOS = ['claro', 'oscuro', 'sistema']

export const TEMAS = [
  {
    id: 'amanecer',
    nombre: 'Amanecer',
    familia: 'calida',
    oscuro: false,
    colorBarra: '#fff6f1',
    muestra: { fondo: '#fff6f1', superficie: '#ffffff', acento: '#c2185b' },
  },
  {
    id: 'medianoche',
    nombre: 'Medianoche',
    familia: 'calida',
    oscuro: true,
    colorBarra: '#241018',
    muestra: { fondo: '#241018', superficie: '#321923', acento: '#ff9fbc' },
  },
  {
    id: 'cielo',
    nombre: 'Cielo',
    familia: 'azul',
    oscuro: false,
    colorBarra: '#eef4ff',
    muestra: { fondo: '#eef4ff', superficie: '#ffffff', acento: '#1f6feb' },
  },
  {
    id: 'oceano',
    nombre: 'Océano',
    familia: 'azul',
    oscuro: true,
    colorBarra: '#0b1726',
    muestra: { fondo: '#0b1726', superficie: '#122239', acento: '#8ec6ff' },
  },
]

function prefiereOscuro() {
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  } catch (error) {
    return false
  }
}

function leerPreferencia() {
  try {
    const guardado = localStorage.getItem(CLAVE)
    if (!guardado) return null

    // Compatibilidad con la versión anterior, que guardaba 'claro'/'noche'.
    if (guardado === 'claro' || guardado === 'noche') {
      return { familia: 'calida', modo: guardado === 'noche' ? 'oscuro' : 'claro' }
    }

    const datos = JSON.parse(guardado)
    if (FAMILIAS.includes(datos?.familia) && MODOS.includes(datos?.modo)) {
      return { familia: datos.familia, modo: datos.modo }
    }
  } catch (error) {
    /* almacenamiento no disponible o dato inválido */
  }
  return null
}

function idDeTema(familia, oscuro) {
  return TEMAS.find((t) => t.familia === familia && t.oscuro === oscuro)?.id || 'amanecer'
}

function actualizarMeta(color) {
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', color)
}

function persistir(familia, modo) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify({ familia, modo }))
  } catch (error) {
    /* almacenamiento no disponible */
  }
}

const preferencia = leerPreferencia() || { familia: 'calida', modo: 'sistema' }

const familia = ref(preferencia.familia)
const modo = ref(preferencia.modo)
const tema = ref(idDeTema(familia.value, modo.value === 'sistema' ? prefiereOscuro() : modo.value === 'oscuro'))
const esOscuro = ref(false)

const esSistema = computed(() => modo.value === 'sistema')
const modoEfectivo = computed(() => (esOscuro.value ? 'oscuro' : 'claro'))

function aplicarEstado(guardar = true) {
  const oscuro = modo.value === 'sistema' ? prefiereOscuro() : modo.value === 'oscuro'
  const id = idDeTema(familia.value, oscuro)
  const definicion = TEMAS.find((t) => t.id === id)

  esOscuro.value = oscuro
  tema.value = id

  const raiz = document.documentElement
  raiz.dataset.theme = id
  raiz.dataset.oscuro = oscuro ? 'true' : 'false'
  raiz.style.colorScheme = oscuro ? 'dark' : 'light'

  actualizarMeta(definicion?.colorBarra || '#fff6f1')
  if (guardar) persistir(familia.value, modo.value)
}

function aplicarTema(nuevoTema) {
  const definicion = TEMAS.find((t) => t.id === nuevoTema)
  if (!definicion) return
  familia.value = definicion.familia
  modo.value = definicion.oscuro ? 'oscuro' : 'claro'
  aplicarEstado()
}

function seleccionar(nuevaFamilia, nuevoModo) {
  if (!FAMILIAS.includes(nuevaFamilia) || !MODOS.includes(nuevoModo)) return
  familia.value = nuevaFamilia
  modo.value = nuevoModo
  aplicarEstado()
}

function usarSistema() {
  modo.value = 'sistema'
  aplicarEstado()
}

function alternarTema() {
  modo.value = esOscuro.value ? 'claro' : 'oscuro'
  aplicarEstado()
}

if (typeof window !== 'undefined' && window.matchMedia) {
  const consulta = window.matchMedia('(prefers-color-scheme: dark)')
  const alCambiarSistema = () => {
    if (modo.value === 'sistema') aplicarEstado(false)
  }
  if (consulta.addEventListener) consulta.addEventListener('change', alCambiarSistema)
  else if (consulta.addListener) consulta.addListener(alCambiarSistema)
}

aplicarEstado(false)

export function useTema() {
  return {
    tema,
    familia,
    modo,
    modoEfectivo,
    esOscuro,
    esSistema,
    temas: TEMAS,
    aplicarTema,
    seleccionar,
    usarSistema,
    alternarTema,
  }
}
