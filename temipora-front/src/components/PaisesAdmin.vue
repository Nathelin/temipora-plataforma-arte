<script setup>
import { ref, onMounted } from 'vue'
import api from '../servicios/api'

const paises = ref([])
const cargando = ref(true)
const error = ref('')

const guardando = ref(false)

const modalAbierto = ref(false)
const editando = ref(false)
const idEditando = ref(null)
const nombre = ref('')
const codigoIso = ref('')
const prefijoTelefonico = ref('')
const errorForm = ref('')

const modalConfirmacion = ref(null)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/paises')
    paises.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar los países.'
  } finally {
    cargando.value = false
  }
}

function abrirAlta() {
  editando.value = false
  idEditando.value = null
  nombre.value = ''
  codigoIso.value = ''
  prefijoTelefonico.value = ''
  errorForm.value = ''
  modalAbierto.value = true
}

function abrirEdicion(p) {
  editando.value = true
  idEditando.value = p.idPais
  nombre.value = p.nombre
  codigoIso.value = p.codigoIso || ''
  prefijoTelefonico.value = p.prefijoTelefonico || ''
  errorForm.value = ''
  modalAbierto.value = true
}

async function guardar() {
  const iso = codigoIso.value.trim().toUpperCase()
  const prefijo = prefijoTelefonico.value.trim()
  if (!nombre.value.trim()) {
    errorForm.value = 'El nombre es obligatorio.'
    return
  }
  if (!/^[A-Za-z]{2}$/.test(iso)) {
    errorForm.value = 'El código ISO debe tener 2 letras.'
    return
  }
  if (!/^\+\d{1,4}$/.test(prefijo)) {
    errorForm.value = 'El prefijo telefónico debe tener el formato +NNN.'
    return
  }
  guardando.value = true
  errorForm.value = ''
  const payload = {
    nombre: nombre.value.trim(),
    codigoIso: iso,
    prefijoTelefonico: prefijo,
  }
  try {
    if (editando.value) {
      await api.put(`/admin/paises/${idEditando.value}`, payload)
    } else {
      await api.post('/admin/paises', payload)
    }
    modalAbierto.value = false
    await cargar()
  } catch (e) {
    errorForm.value = e.response?.data?.message || 'No se pudo guardar el país.'
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

function confirmarBaja(p) {
  pedirConfirmacion({
    titulo: '¿Dar de baja el país?',
    mensaje: `El país "${p.nombre}" dejará de estar disponible para seleccionarse en los perfiles. Los usuarios que ya lo tienen lo conservan.`,
    boton: 'Dar de baja',
    ejecutar: () => api.delete(`/admin/paises/${p.idPais}`),
  })
}

function confirmarAlta(p) {
  pedirConfirmacion({
    titulo: '¿Reactivar el país?',
    mensaje: `El país "${p.nombre}" volverá a estar disponible para seleccionarse en los perfiles.`,
    boton: 'Reactivar',
    ejecutar: () => api.post(`/admin/paises/${p.idPais}/activar`),
  })
}

onMounted(cargar)
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Sistema"
      titulo="Países"
      descripcion="Países y prefijos telefónicos disponibles en los perfiles."
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

    <div v-else class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm min-w-160">
          <thead class="bg-rosa/40 text-granate">
            <tr>
              <th class="px-5 py-3 font-normal text-sm">Nombre</th>
              <th class="px-5 py-3 font-normal text-sm">Código ISO</th>
              <th class="px-5 py-3 font-normal text-sm">Prefijo</th>
              <th class="px-5 py-3 font-normal text-sm">Estado</th>
              <th class="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in paises" :key="p.idPais" class="border-t border-rosa/50 hover:bg-rosa/20 transition">
              <td class="px-5 py-3 text-granate font-normal">{{ p.nombre }}</td>
              <td class="px-5 py-3 text-cafe font-mono">{{ p.codigoIso || '—' }}</td>
              <td class="px-5 py-3 text-cafe font-mono">{{ p.prefijoTelefonico || '—' }}</td>
              <td class="px-5 py-3">
                <span
                  class="px-2.5 py-0.5 rounded-full text-xs font-normal"
                  :class="p.activo ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
                >
                  {{ p.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td class="px-5 py-3 text-right whitespace-nowrap">
                <button class="text-rojizo hover:text-coral transition mr-3" @click="abrirEdicion(p)">
                  Editar
                </button>
                <button
                  class="text-cafe/50 hover:text-rojizo transition"
                  @click="p.activo ? confirmarBaja(p) : confirmarAlta(p)"
                >
                  {{ p.activo ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="!paises.length" class="p-8 text-cafe/50 text-center">
        No hay países registrados.
      </p>
    </div>

    <!-- Modal de país -->
    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">
          {{ editando ? 'Modificar país' : 'Dar de alta país' }}
        </h2>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2">Nombre <span class="text-rojizo">*</span></label>
            <input
              v-model="nombre"
              type="text"
              maxlength="60"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm text-cafe mb-2">Código ISO <span class="text-rojizo">*</span></label>
              <input
                v-model="codigoIso"
                type="text"
                maxlength="2"
                placeholder="AR"
                class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base uppercase text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              />
            </div>
            <div>
              <label class="block text-sm text-cafe mb-2">Prefijo <span class="text-rojizo">*</span></label>
              <input
                v-model="prefijoTelefonico"
                type="text"
                maxlength="6"
                placeholder="+54"
                class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              />
            </div>
          </div>
          <p class="text-xs text-cafe/50">
            El código ISO (2 letras) y el prefijo (+NNN) se usan para validar el teléfono del perfil.
          </p>

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
