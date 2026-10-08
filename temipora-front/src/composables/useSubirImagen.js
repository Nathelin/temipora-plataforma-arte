import { reactive, ref, computed } from 'vue'

const estado = reactive({
  abierto: false,
  modo: 'crear',
  imagen: null,
})

const subidaEnCurso = ref(false)
const publicacionesPendientes = ref(0)
const publicadasEnSesion = ref(0)
const proteccionActiva = computed(
  () => subidaEnCurso.value || publicacionesPendientes.value > 0,
)

export function useSubirImagen() {
  function abrir(modo = 'crear', imagen = null) {
    estado.modo = modo
    estado.imagen = modo === 'editar' ? imagen : null
    estado.abierto = true
  }

  function cerrar() {
    estado.abierto = false
    estado.imagen = null
    estado.modo = 'crear'
    subidaEnCurso.value = false
    publicacionesPendientes.value = 0
    publicadasEnSesion.value = 0
  }

  return {
    estado,
    abrir,
    cerrar,
    subidaEnCurso,
    publicacionesPendientes,
    publicadasEnSesion,
    proteccionActiva,
  }
}
