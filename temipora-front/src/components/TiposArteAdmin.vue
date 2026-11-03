<script setup>
import { ref, onMounted } from 'vue'
import api from '../servicios/api'

const tipos = ref([])
const medios = ref([])
const cargando = ref(true)
const error = ref('')
const errorAccion = ref('')

const guardando = ref(false)

const modalAbierto = ref(false)
const editando = ref(false)
const idEditando = ref(null)
const nombre = ref('')
const descripcion = ref('')
const observaciones = ref('')
const idMedio = ref('')
const errorForm = ref('')

const modalConfirmacion = ref(null)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [resTipos, resMedios] = await Promise.all([
      api.get('/admin/tipos-arte'),
      api.get('/admin/medios'),
    ])
    tipos.value = resTipos.data
    medios.value = resMedios.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar los tipos de arte.'
  } finally {
    cargando.value = false
  }
}

const mediosParaSelect = () =>
  editando.value ? medios.value : medios.value.filter((m) => m.activo)

function textoMedio(m) {
  return m.activo ? m.nombre : `${m.nombre} (inactivo)`
}

function abrirAlta() {
  editando.value = false
  idEditando.value = null
  nombre.value = ''
  descripcion.value = ''
  observaciones.value = ''
  idMedio.value = ''
  errorForm.value = ''
  modalAbierto.value = true
}

function abrirEdicion(t) {
  editando.value = true
  idEditando.value = t.idTipoArte
  nombre.value = t.nombre
  descripcion.value = t.descripcion || ''
  observaciones.value = t.observaciones || ''
  idMedio.value = String(t.idMedio)
  errorForm.value = ''
  modalAbierto.value = true
}

async function guardar() {
  if (!nombre.value.trim()) {
    errorForm.value = 'El nombre es obligatorio.'
    return
  }
  if (!idMedio.value) {
    errorForm.value = 'Selecciona el medio.'
    return
  }
  guardando.value = true
  errorForm.value = ''
  const payload = {
    nombre: nombre.value.trim(),
    descripcion: descripcion.value.trim() || null,
    idMedio: Number(idMedio.value),
    observaciones: observaciones.value.trim() || null,
  }
  try {
    if (editando.value) {
      await api.put(`/admin/tipos-arte/${idEditando.value}`, payload)
    } else {
      await api.post('/admin/tipos-arte', payload)
    }
    modalAbierto.value = false
    await cargar()
  } catch (e) {
    errorForm.value = e.response?.data?.message || 'No se pudo guardar el tipo de arte.'
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

function confirmarBaja(t) {
  pedirConfirmacion({
    titulo: '¿Dar de baja el tipo de arte?',
    mensaje: `El tipo de arte "${t.nombre}" dejará de estar disponible para nuevas obras y no podrá recibir materiales nuevos. Las obras existentes lo conservan.`,
    boton: 'Dar de baja',
    ejecutar: () => api.delete(`/admin/tipos-arte/${t.idTipoArte}`),
  })
}

function confirmarAlta(t) {
  pedirConfirmacion({
    titulo: '¿Reactivar el tipo de arte?',
    mensaje: `El tipo de arte "${t.nombre}" volverá a estar disponible para nuevas obras y materiales.`,
    boton: 'Reactivar',
    ejecutar: () => api.post(`/admin/tipos-arte/${t.idTipoArte}/activar`),
  })
}

onMounted(cargar)
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Catálogo"
      titulo="Tipos de arte"
      descripcion="Cada tipo de arte agrupa los materiales que pueden usarse en una obra."
    >
      <template #acciones>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
          @click="abrirAlta"
        >
          Dar de alta
        </button>
      </template>
    </AdminPageHeader>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else>
      <p v-if="errorAccion" class="text-rojizo text-sm mb-4">{{ errorAccion }}</p>

      <div class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm min-w-160">
            <thead class="bg-rosa/40 text-granate">
              <tr>
                <th class="px-5 py-3 font-normal text-sm">Nombre</th>
                <th class="px-5 py-3 font-normal text-sm">Medio</th>
                <th class="px-5 py-3 font-normal text-sm">Observaciones</th>
                <th class="px-5 py-3 font-normal text-sm">Estado</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in tipos" :key="t.idTipoArte" class="border-t border-rosa/50 hover:bg-rosa/20 transition">
                <td class="px-5 py-3 text-granate font-normal">
                  {{ t.nombre }}
                  <span class="block text-xs text-cafe/50 font-normal truncate max-w-64" :title="t.descripcion">
                    {{ t.descripcion }}
                  </span>
                </td>
                <td class="px-5 py-3 text-cafe">{{ t.nombreMedio || '—' }}</td>
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
                    @click="t.activo ? confirmarBaja(t) : confirmarAlta(t)"
                  >
                    {{ t.activo ? 'Dar de baja' : 'Reactivar' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-if="!tipos.length" class="p-8 text-cafe/50 text-center">
          No hay tipos de arte registrados.
        </p>
      </div>
    </template>

    <!-- Modal de tipo de arte -->
    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">
          {{ editando ? 'Modificar tipo de arte' : 'Dar de alta tipo de arte' }}
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
            <label class="block text-sm text-cafe mb-2">Medio <span class="text-rojizo">*</span></label>
            <select
              v-model="idMedio"
              class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            >
              <option value="" disabled>Selecciona un medio</option>
              <option v-for="m in mediosParaSelect()" :key="m.idMedio" :value="String(m.idMedio)">
                {{ textoMedio(m) }}
              </option>
            </select>
            <p class="text-xs text-cafe/50 mt-1">
              Determina qué materiales podrán asignarse a las obras de este tipo.
            </p>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Descripción</label>
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
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            @click="cerrarModal"
          >
            Cancelar
          </button>
          <button
            :disabled="guardando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
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
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            @click="cerrarConfirmacion"
          >
            Cancelar
          </button>
          <button
            type="button"
            :disabled="modalConfirmacion.confirmando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            @click="confirmarAccion"
          >
            {{ modalConfirmacion.confirmando ? 'Procesando...' : modalConfirmacion.boton }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
