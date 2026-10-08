<script setup>
import { ref } from 'vue'
import api from '../servicios/api'

const categorias = ref([])
const cargando = ref(true)
const error = ref('')

const guardando = ref(false)

const modalAbierto = ref(false)
const editando = ref(false)
const idEditando = ref(null)
const nombre = ref('')
const descripcion = ref('')
const observaciones = ref('')
const errorForm = ref('')

const modalConfirmacion = ref(null)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/categorias-etiqueta')
    categorias.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar las categorías.'
  } finally {
    cargando.value = false
  }
}

function abrirAlta() {
  editando.value = false
  idEditando.value = null
  nombre.value = ''
  descripcion.value = ''
  observaciones.value = ''
  errorForm.value = ''
  modalAbierto.value = true
}

function abrirEdicion(c) {
  editando.value = true
  idEditando.value = c.idCategoriaEtiqueta
  nombre.value = c.nombre
  descripcion.value = c.descripcion
  observaciones.value = c.observaciones || ''
  errorForm.value = ''
  modalAbierto.value = true
}

async function guardar() {
  if (!nombre.value.trim()) {
    errorForm.value = 'El nombre es obligatorio.'
    return
  }
  guardando.value = true
  errorForm.value = ''
  const payload = {
    nombre: nombre.value.trim(),
    descripcion: descripcion.value.trim() || null,
    observaciones: observaciones.value.trim() || null,
  }
  try {
    if (editando.value) {
      await api.put(`/admin/categorias-etiqueta/${idEditando.value}`, payload)
    } else {
      await api.post('/admin/categorias-etiqueta', payload)
    }
    modalAbierto.value = false
    await cargar()
  } catch (e) {
    errorForm.value = e.response?.data?.message || 'No se pudo guardar la categoría.'
  } finally {
    guardando.value = false
  }
}

function cerrarModal() {
  if (!guardando.value) modalAbierto.value = false
}

function pedirConfirmacion({ titulo, mensaje, boton, ejecutar }) {
  modalConfirmacion.value = { titulo, mensaje, boton, ejecutar, confirmando: false, error: '' }
}

function cerrarConfirmacion() {
  if (modalConfirmacion.value && !modalConfirmacion.value.confirmando) {
    modalConfirmacion.value = null
  }
}

async function confirmarAccion() {
  const c = modalConfirmacion.value
  if (!c || c.confirmando) return
  c.confirmando = true
  c.error = ''
  try {
    await c.ejecutar()
    modalConfirmacion.value = null
    await cargar()
  } catch (err) {
    c.error = err.response?.data?.message || 'No se pudo completar la acción.'
    c.confirmando = false
  }
}

function confirmarBaja(c) {
  pedirConfirmacion({
    titulo: '¿Dar de baja la categoría?',
    mensaje: `La categoría "${c.nombre}" dejará de estar disponible para crear o editar etiquetas. Podrás reactivarla más adelante.`,
    boton: 'Dar de baja',
    ejecutar: () => api.delete(`/admin/categorias-etiqueta/${c.idCategoriaEtiqueta}`),
  })
}

function confirmarAlta(c) {
  pedirConfirmacion({
    titulo: '¿Reactivar la categoría?',
    mensaje: `La categoría "${c.nombre}" volverá a estar disponible para crear o editar etiquetas.`,
    boton: 'Reactivar',
    ejecutar: () => api.post(`/admin/categorias-etiqueta/${c.idCategoriaEtiqueta}/activar`),
  })
}

cargar()
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-normal text-granate">Categorías</h1>
      <button
        class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
        @click="abrirAlta"
      >
        Dar de alta
      </button>
    </div>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm min-w-160">
          <thead class="bg-rosa/40 text-granate">
            <tr>
              <th class="px-5 py-3 font-normal text-sm">Nombre</th>
              <th class="px-5 py-3 font-normal text-sm">Descripción</th>
              <th class="px-5 py-3 font-normal text-sm">Estado</th>
              <th class="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categorias" :key="c.idCategoriaEtiqueta" class="border-t border-rosa/50 hover:bg-rosa/20 transition">
              <td class="px-5 py-3 text-granate font-normal">{{ c.nombre }}</td>
              <td class="px-5 py-3 text-cafe">{{ c.descripcion }}</td>
              <td class="px-5 py-3">
                <span
                  class="px-2.5 py-0.5 rounded-full text-xs font-normal"
                  :class="c.activo ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
                >
                  {{ c.activo ? 'Activa' : 'Inactiva' }}
                </span>
              </td>
              <td class="px-5 py-3 text-right whitespace-nowrap">
                <button class="text-rojizo hover:text-coral transition mr-3" @click="abrirEdicion(c)">
                  Editar
                </button>
                <button
                  class="text-cafe/50 hover:text-rojizo transition"
                  @click="c.activo ? confirmarBaja(c) : confirmarAlta(c)"
                >
                  {{ c.activo ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="!categorias.length" class="p-8 text-cafe/50 text-center">
        No hay categorías registradas.
      </p>
    </div>

    <!-- Modal de categoría -->
    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">
          {{ editando ? 'Modificar categoría' : 'Dar de alta categoría' }}
        </h2>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2">Nombre <span class="text-rojizo">*</span></label>
            <input
              v-model="nombre"
              type="text"
              maxlength="50"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Descripción <span class="text-rojizo">*</span></label>
            <textarea
              v-model="descripcion"
              rows="3"
              maxlength="250"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
            <p class="text-right text-xs text-cafe/50 mt-1">{{ descripcion.length }}/250</p>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Observaciones (opcional)</label>
            <textarea
              v-model="observaciones"
              rows="3"
              maxlength="500"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
            <p class="text-right text-xs text-cafe/50 mt-1">{{ observaciones.length }}/500</p>
          </div>

          <p v-if="errorForm" class="text-rojizo text-sm">{{ errorForm }}</p>
        </div>

        <div class="flex items-center justify-end gap-3 mt-6">
          <button
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
            @click="cerrarModal"
          >
            Cancelar
          </button>
          <button
            :disabled="guardando"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de confirmación de acciones -->
    <div
      v-if="modalConfirmacion"
      class="fixed inset-0 z-[80] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
    >
      <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrarConfirmacion"></div>

      <div class="relative w-full max-w-md bg-blanco rounded-xl shadow-xl shadow-granate/20 border border-rosa p-6">
        <h2 class="text-lg text-granate">{{ modalConfirmacion.titulo }}</h2>
        <p class="mt-2 text-sm text-cafe/70">{{ modalConfirmacion.mensaje }}</p>
        <p v-if="modalConfirmacion.error" class="mt-3 text-sm text-rojizo">{{ modalConfirmacion.error }}</p>

        <div class="mt-6 flex flex-col-reverse sm:flex-row sm:justify-end gap-3">
          <button
            type="button"
            :disabled="modalConfirmacion.confirmando"
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 disabled:opacity-50 transition"
            @click="cerrarConfirmacion"
          >
            Cancelar
          </button>
          <button
            type="button"
            :disabled="modalConfirmacion.confirmando"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
            @click="confirmarAccion"
          >
            {{ modalConfirmacion.confirmando ? 'Procesando...' : modalConfirmacion.boton }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
