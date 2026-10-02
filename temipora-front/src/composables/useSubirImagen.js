import { reactive } from 'vue'

const estado = reactive({
  abierto: false,
  modo: 'crear',
  imagen: null,
})

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
  }

  return { estado, abrir, cerrar }
}
