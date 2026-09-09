<script setup>
import { ref, watch, computed } from 'vue'
import api from '../servicios/api'

const props = defineProps({
  tipo: { type: String, required: true },
})

const endpointBase = computed(() => props.tipo === 'producto' ? 'tipos-producto' : 'tipos-servicio')
const campoId = computed(() => props.tipo === 'producto' ? 'idTipoProducto' : 'idTipoServicio')
const etiquetaSingular = computed(() => props.tipo === 'producto' ? 'tipo de producto' : 'tipo de servicio')
const titulo = computed(() => props.tipo === 'producto' ? 'Gestión de productos' : 'Gestión de servicios')

const tipos = ref([])
const cargando = ref(true)
const error = ref('')

const modalAbierto = ref(false)
const editando = ref(false)
const guardando = ref(false)
const errorEdicion = ref('')

const idEditando = ref(null)
const nombre = ref('')
const descripcion = ref('')
const observaciones = ref('')

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/admin/${endpointBase.value}`)
    tipos.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar los tipos.'
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
  errorEdicion.value = ''
  modalAbierto.value = true
}

function abrirEdicion(t) {
  editando.value = true
  idEditando.value = t[campoId.value]
  nombre.value = t.nombre
  descripcion.value = t.descripcion
  observaciones.value = t.observaciones || ''
  errorEdicion.value = ''
  modalAbierto.value = true
}

function cerrarModal() {
  modalAbierto.value = false
}

async function guardar() {
  guardando.value = true
  errorEdicion.value = ''
  const payload = {
    nombre: nombre.value,
    descripcion: descripcion.value,
    observaciones: observaciones.value,
  }
  try {
    if (editando.value) {
      await api.put(`/admin/${endpointBase.value}/${idEditando.value}`, payload)
    } else {
      await api.post(`/admin/${endpointBase.value}`, payload)
    }
    modalAbierto.value = false
    await cargar()
  } catch (e) {
    errorEdicion.value = e.response?.data?.message || 'No se pudo guardar.'
  } finally {
    guardando.value = false
  }
}

async function alternarEstado(t) {
  const id = t[campoId.value]
  try {
    if (t.activo) {
      await api.delete(`/admin/${endpointBase.value}/${id}`)
    } else {
      await api.post(`/admin/${endpointBase.value}/${id}/activar`)
    }
    await cargar()
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo actualizar el estado.'
  }
}

watch(() => props.tipo, cargar, { immediate: true })
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-normal text-granate">{{ titulo }}</h1>
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
              <th class="px-5 py-3 font-normal text-sm">Observaciones</th>
              <th class="px-5 py-3 font-normal text-sm">Estado</th>
              <th class="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in tipos" :key="t[campoId]" class="border-t border-rosa/50 hover:bg-rosa/20 transition">
              <td class="px-5 py-3 text-granate font-normal">{{ t.nombre }}</td>
              <td class="px-5 py-3 text-cafe">{{ t.descripcion }}</td>
              <td class="px-5 py-3 text-cafe/60">{{ t.observaciones || '—' }}</td>
              <td class="px-5 py-3">
                <span
                  class="px-2.5 py-0.5 rounded-full text-xs font-normal"
                  :class="t.activo ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
                >
                  {{ t.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td class="px-5 py-3 text-right whitespace-nowrap">
                <button class="text-rojizo hover:text-coral transition mr-3" @click="abrirEdicion(t)">
                  Editar
                </button>
                <button
                  class="text-cafe/50 hover:text-rojizo transition"
                  @click="alternarEstado(t)"
                >
                  {{ t.activo ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="!tipos.length" class="p-8 text-cafe/50 text-center">
        No hay {{ etiquetaSingular }} registradas.
      </p>
    </div>

    <!-- Modal -->
    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">
          {{ editando ? 'Modificar' : 'Dar de alta' }} {{ etiquetaSingular }}
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

          <p v-if="errorEdicion" class="text-rojizo text-sm">{{ errorEdicion }}</p>
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
  </div>
</template>