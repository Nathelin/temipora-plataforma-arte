import { computed, ref } from 'vue'
import {
  actualizarItem,
  agregarItem,
  cargarCarrito,
  checkout,
  quitarItem,
  vaciarCarrito,
} from '../servicios/carrito'

const carrito = ref(null)
const cargando = ref(false)
const error = ref('')

const cantidad = computed(() => Number(carrito.value?.cantidadItems) || 0)
const grupos = computed(() => carrito.value?.grupos || [])
const items = computed(() => grupos.value.flatMap((grupo) => grupo.items || []))
const total = computed(() => carrito.value?.total ?? 0)
const puedeCheckout = computed(() => Boolean(carrito.value?.puedeCheckout))
const avisos = computed(() => carrito.value?.avisos || [])

function limpiar() {
  carrito.value = null
  error.value = ''
}

async function cargar() {
  if (!localStorage.getItem('token')) {
    limpiar()
    return null
  }
  cargando.value = true
  error.value = ''
  try {
    carrito.value = await cargarCarrito()
    return carrito.value
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el carrito.'
    return null
  } finally {
    cargando.value = false
  }
}

async function agregar(idVariante, cantidadItem = 1, idPublicacionReventa = null) {
  error.value = ''
  try {
    carrito.value = await agregarItem(idVariante, cantidadItem, idPublicacionReventa)
    return carrito.value
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo agregar al carrito.'
    throw e
  }
}

async function actualizar(idItem, cantidadItem) {
  error.value = ''
  try {
    carrito.value = await actualizarItem(idItem, cantidadItem)
    return carrito.value
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo actualizar el carrito.'
    throw e
  }
}

async function quitar(idItem) {
  error.value = ''
  try {
    carrito.value = await quitarItem(idItem)
    return carrito.value
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo quitar el producto.'
    throw e
  }
}

async function vaciar(idArtista = null) {
  error.value = ''
  try {
    carrito.value = await vaciarCarrito(idArtista)
    return carrito.value
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo vaciar el carrito.'
    throw e
  }
}

async function confirmarCheckout(claveIdempotencia, idArtista = null) {
  error.value = ''
  try {
    const compra = await checkout(claveIdempotencia, idArtista)
    await cargar()
    return compra
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo iniciar la compra.'
    throw e
  }
}

export function useCarrito() {
  return {
    carrito,
    cargando,
    error,
    cantidad,
    grupos,
    items,
    total,
    puedeCheckout,
    avisos,
    cargar,
    agregar,
    actualizar,
    quitar,
    vaciar,
    limpiar,
    confirmarCheckout,
  }
}
