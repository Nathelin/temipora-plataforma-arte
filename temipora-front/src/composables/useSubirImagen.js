import { reactive, ref, computed } from 'vue'

const estado = reactive({
  abierto: false,
  modo: 'crear',
  imagen: null,
  archivoEntrega: null,
  descripcionEntrega: '',
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
    estado.archivoEntrega = null
    estado.descripcionEntrega = ''
    estado.abierto = true
  }

  function abrirVerificada(archivoEntrega, descripcionEntrega = '') {
    estado.modo = 'verificar'
    estado.imagen = null
    estado.archivoEntrega = archivoEntrega
    estado.descripcionEntrega = descripcionEntrega
    estado.abierto = true
  }

  function cerrar() {
    estado.abierto = false
    estado.imagen = null
    estado.archivoEntrega = null
    estado.descripcionEntrega = ''
    estado.modo = 'crear'
    subidaEnCurso.value = false
    publicacionesPendientes.value = 0
    publicadasEnSesion.value = 0
  }

  return {
    estado,
    abrir,
    abrirVerificada,
    cerrar,
    subidaEnCurso,
    publicacionesPendientes,
    publicadasEnSesion,
    proteccionActiva,
  }
}
