<script setup>
import { ref, onMounted } from 'vue'
import api from '../servicios/api'

const monedas = ref([])
const cargando = ref(true)
const error = ref('')

const guardando = ref(false)

const modalAbierto = ref(false)
const editando = ref(false)
const idEditando = ref(null)
const codigo = ref('')
const nombre = ref('')
const simbolo = ref('')
const predeterminada = ref(false)
const errorForm = ref('')

const modalConfirmacion = ref(null)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/monedas')
    monedas.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar las monedas.'
  } finally {
    cargando.value = false
  }
}

function abrirAlta() {
  editando.value = false
  idEditando.value = null
  codigo.value = ''
  nombre.value = ''
  simbolo.value = ''
  predeterminada.value = false
  errorForm.value = ''
  modalAbierto.value = true
}

function abrirEdicion(m) {
  editando.value = true
  idEditando.value = m.idMoneda
  codigo.value = m.codigo || ''
  nombre.value = m.nombre || ''
  simbolo.value = m.simbolo || ''
  predeterminada.value = !!m.predeterminada
  errorForm.value = ''
  modalAbierto.value = true
}

async function guardar() {
  const code = codigo.value.trim().toUpperCase()
  if (!nombre.value.trim()) {
    errorForm.value = 'El nombre es obligatorio.'
    return
  }
  if (!/^[A-Za-z]{3}$/.test(code)) {
    errorForm.value = 'El código debe tener 3 letras (ISO 4217).'
    return
  }
  guardando.value = true
  errorForm.value = ''
  const payload = {
    codigo: code,
    nombre: nombre.value.trim(),
    simbolo: simbolo.value.trim(),
    predeterminada: predeterminada.value,
  }
  try {
    if (editando.value) {
      await api.put(`/admin/monedas/${idEditando.value}`, payload)
    } else {
      await api.post('/admin/monedas', payload)
    }
    modalAbierto.value = false
    await cargar()
  } catch (e) {
    errorForm.value = e.response?.data?.message || 'No se pudo guardar la moneda.'
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

function confirmarBaja(m) {
  pedirConfirmacion({
    titulo: '¿Dar de baja la moneda?',
    mensaje: `La moneda "${m.codigo}" dejará de estar disponible para cargar precios o como preferencia. Los datos existentes que la usan se conservan.`,
    boton: 'Dar de baja',
    ejecutar: () => api.delete(`/admin/monedas/${m.idMoneda}`),
  })
}

function confirmarAlta(m) {
  pedirConfirmacion({
    titulo: '¿Reactivar la moneda?',
    mensaje: `La moneda "${m.codigo}" volverá a estar disponible para cargar precios o como preferencia.`,
    boton: 'Reactivar',
    ejecutar: () => api.post(`/admin/monedas/${m.idMoneda}/activar`),
  })
}

function confirmarPredeterminada(m) {
  pedirConfirmacion({
    titulo: '¿Marcar como predeterminada?',
    mensaje: `La moneda "${m.codigo}" será la moneda por defecto para nuevos servicios y usuarios.`,
    boton: 'Marcar predeterminada',
    ejecutar: () => api.post(`/admin/monedas/${m.idMoneda}/predeterminada`),
  })
}

onMounted(cargar)
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Monedas"
      descripcion="Monedas disponibles para publicar precios y definir la preferencia del usuario."
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

    <AdminTabs />

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm min-w-160">
          <thead class="bg-rosa/40 text-granate">
            <tr>
              <th class="px-5 py-3 font-normal text-sm">Código</th>
              <th class="px-5 py-3 font-normal text-sm">Nombre</th>
              <th class="px-5 py-3 font-normal text-sm">Símbolo</th>
              <th class="px-5 py-3 font-normal text-sm">Predeterminada</th>
              <th class="px-5 py-3 font-normal text-sm">Estado</th>
              <th class="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in monedas" :key="m.idMoneda" class="border-t border-rosa/50 hover:bg-rosa/20 transition">
              <td class="px-5 py-3 text-granate font-mono">{{ m.codigo }}</td>
              <td class="px-5 py-3 text-cafe">{{ m.nombre }}</td>
              <td class="px-5 py-3 text-cafe">{{ m.simbolo || '—' }}</td>
              <td class="px-5 py-3">
                <span
                  v-if="m.predeterminada"
                  class="px-2.5 py-0.5 rounded-full text-xs font-normal bg-coral/10 text-granate"
                >
                  Predeterminada
                </span>
                <button
                  v-else-if="m.activo"
                  class="text-xs text-cafe/60 hover:text-granate transition"
                  @click="confirmarPredeterminada(m)"
                >
                  Marcar como predeterminada
                </button>
                <span v-else class="text-cafe/40">—</span>
              </td>
              <td class="px-5 py-3">
                <span
                  class="px-2.5 py-0.5 rounded-full text-xs font-normal"
                  :class="m.activo ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
                >
                  {{ m.activo ? 'Activa' : 'Inactiva' }}
                </span>
              </td>
              <td class="px-5 py-3 text-right whitespace-nowrap">
                <button class="text-rojizo hover:text-coral transition mr-3" @click="abrirEdicion(m)">
                  Editar
                </button>
                <button
                  class="text-cafe/50 hover:text-rojizo transition disabled:opacity-40"
                  :disabled="m.predeterminada"
                  :title="m.predeterminada ? 'No se puede dar de baja la moneda predeterminada' : ''"
                  @click="m.activo ? confirmarBaja(m) : confirmarAlta(m)"
                >
                  {{ m.activo ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="!monedas.length" class="p-8 text-cafe/50 text-center">
        No hay monedas registradas.
      </p>
    </div>

    <!-- Modal de moneda -->
    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">
          {{ editando ? 'Modificar moneda' : 'Dar de alta moneda' }}
        </h2>

        <div class="space-y-5">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm text-cafe mb-2">Código ISO <span class="text-rojizo">*</span></label>
              <input
                v-model="codigo"
                type="text"
                maxlength="3"
                placeholder="ARS"
                class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base uppercase text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              />
            </div>
            <div>
              <label class="block text-sm text-cafe mb-2">Símbolo</label>
              <input
                v-model="simbolo"
                type="text"
                maxlength="6"
                placeholder="$"
                class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Nombre <span class="text-rojizo">*</span></label>
            <input
              v-model="nombre"
              type="text"
              maxlength="60"
              placeholder="Peso argentino"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <label class="flex items-start gap-2 text-sm text-cafe">
            <input v-model="predeterminada" type="checkbox" class="mt-0.5 accent-coral" />
            <span>
              Moneda predeterminada
              <span class="block text-xs text-cafe/50 mt-0.5">
                Se usa por defecto al crear servicios y cuentas nuevas.
              </span>
            </span>
          </label>

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

    <!-- Modal de confirmación -->
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
