import { reactive } from 'vue'

const estado = reactive({
  visible: false,
  titulo: '',
  detalle: '',
  fase: '',
  progreso: 0,
  mensajeExito: '',
  mensajeError: '',
})

let temporizadorExito = null

function limpiarTemporizador() {
  if (temporizadorExito) {
    clearTimeout(temporizadorExito)
    temporizadorExito = null
  }
}

export function useAvisoActualizacion() {
  function mostrar(titulo, detalle = '') {
    limpiarTemporizador()
    estado.titulo = titulo
    estado.detalle = detalle
    estado.fase = 'procesando'
    estado.progreso = 0
    estado.mensajeExito = ''
    estado.mensajeError = ''
    estado.visible = true
  }

  function fase(nuevaFase, progreso = null) {
    estado.fase = nuevaFase
    if (progreso !== null) estado.progreso = progreso
  }

  function detalle(texto) {
    estado.detalle = texto
  }

  function exito(mensaje) {
    limpiarTemporizador()
    estado.fase = 'exito'
    estado.mensajeExito = mensaje
    estado.progreso = 100
    temporizadorExito = setTimeout(() => {
      estado.visible = false
      temporizadorExito = null
    }, 6000)
  }

  function fallo(mensaje) {
    limpiarTemporizador()
    estado.fase = 'error'
    estado.mensajeError = mensaje
  }

  function ocultar() {
    limpiarTemporizador()
    estado.visible = false
  }

  return { estado, mostrar, fase, detalle, exito, fallo, ocultar }
}
