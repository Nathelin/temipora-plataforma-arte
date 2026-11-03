<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'

const route = useRoute()

const etiquetas = ref([])
const categorias = ref([])
const cargando = ref(true)
const error = ref('')
const errorAccion = ref('')

const ESTADOS = [
  { valor: '', etiqueta: 'Todos los estados' },
  { valor: 'PENDIENTE', etiqueta: 'Pendientes' },
  { valor: 'VALIDA', etiqueta: 'Válidas' },
  { valor: 'RECHAZADA', etiqueta: 'Rechazadas' },
  { valor: 'INACTIVA', etiqueta: 'Dadas de baja' },
]

const estadoInicial = String(route.query.estado || '')
const filtroEstado = ref(
  ESTADOS.some((op) => op.valor === estadoInicial) ? estadoInicial : '',
)
const filtroCategoria = ref('')
const pagina = ref(0)
const totalPaginas = ref(0)
const hayMas = ref(false)

const modalEtiquetaAbierto = ref(false)
const editandoEtiqueta = ref(false)
const guardando = ref(false)
const errorEtiqueta = ref('')
const idEtiquetaEditando = ref(null)
const nombre = ref('')
const descripcion = ref('')
const observaciones = ref('')
const idCategoria = ref('')

async function cargar(nuevaPagina = 0) {
  cargando.value = true
  error.value = ''
  try {
    const [resEtiquetas, resCategorias] = await Promise.all([
      api.get('/admin/etiquetas', {
        params: {
          estado: filtroEstado.value || undefined,
          idCategoria: filtroCategoria.value || undefined,
          pagina: nuevaPagina,
          tamanio: 10,
        },
      }),
      api.get('/admin/categorias-etiqueta'),
    ])
    const data = resEtiquetas.data ?? {}
    etiquetas.value = data.contenido ?? []
    pagina.value = data.pagina ?? 0
    totalPaginas.value = data.totalPaginas ?? 0
    hayMas.value = Boolean(data.hayMas)
    categorias.value = resCategorias.data

    if (!etiquetas.value.length && pagina.value > 0) {
      await cargar(pagina.value - 1)
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar los datos.'
  } finally {
    cargando.value = false
  }
}

watch([filtroEstado, filtroCategoria], () => {
  errorAccion.value = ''
  cargar(0)
})

watch(
  () => route.query.estado,
  (valor) => {
    const estado = String(valor || '')
    const valido = ESTADOS.some((op) => op.valor === estado) ? estado : ''
    if (valido !== filtroEstado.value) filtroEstado.value = valido
  },
)

// ---------------------------------------------------------------
// etiquetas
// ---------------------------------------------------------------

const categoriasParaEtiqueta = computed(() =>
  editandoEtiqueta.value ? categorias.value : categorias.value.filter(c => c.activo),
)

function abrirAltaEtiqueta() {
  editandoEtiqueta.value = false
  idEtiquetaEditando.value = null
  nombre.value = ''
  descripcion.value = ''
  observaciones.value = ''
  idCategoria.value = ''
  errorEtiqueta.value = ''
  modalEtiquetaAbierto.value = true
}

function abrirEdicionEtiqueta(e) {
  editandoEtiqueta.value = true
  idEtiquetaEditando.value = e.idEtiqueta
  nombre.value = e.nombre
  descripcion.value = e.descripcion
  observaciones.value = e.observaciones || ''
  idCategoria.value = String(e.idCategoria)
  errorEtiqueta.value = ''
  modalEtiquetaAbierto.value = true
}

async function guardarEtiqueta() {
  if (!idCategoria.value) {
    errorEtiqueta.value = 'Selecciona una categoría.'
    return
  }
  guardando.value = true
  errorEtiqueta.value = ''
  const payload = {
    nombre: nombre.value,
    descripcion: descripcion.value,
    observaciones: observaciones.value || null,
    idCategoria: Number(idCategoria.value),
  }
  try {
    if (editandoEtiqueta.value) {
      await api.put(`/admin/etiquetas/${idEtiquetaEditando.value}`, payload)
    } else {
      await api.post('/admin/etiquetas', payload)
    }
    modalEtiquetaAbierto.value = false
    await cargar(pagina.value)
  } catch (e) {
    errorEtiqueta.value = e.response?.data?.message || 'No se pudo guardar la etiqueta.'
  } finally {
    guardando.value = false
  }
}

const modalConfirmacion = ref(null)

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
    await cargar(pagina.value)
  } catch (err) {
    c.error = err.response?.data?.message || 'No se pudo completar la acción.'
    c.confirmando = false
  }
}

function confirmarAccionEtiqueta(e, accion) {
  const acciones = {
    aprobar: {
      titulo: '¿Aprobar la etiqueta?',
      mensaje: `La etiqueta "${e.nombre}" pasará a estado Válida y quedará disponible en el selector y los filtros de toda la plataforma.`,
      boton: 'Aprobar',
    },
    rechazar: {
      titulo: '¿Rechazar la etiqueta?',
      mensaje: `La etiqueta "${e.nombre}" pasará a estado Rechazada y se quitará de las imágenes donde ya fue asignada.`,
      boton: 'Rechazar',
    },
    baja: {
      titulo: '¿Dar de baja la etiqueta?',
      mensaje: `La etiqueta "${e.nombre}" dejará de estar disponible para nuevas asignaciones. Podrás reactivarla más adelante desde esta lista.`,
      boton: 'Dar de baja',
    },
  }
  const def = acciones[accion]
  if (!def) return
  pedirConfirmacion({
    titulo: def.titulo,
    mensaje: def.mensaje,
    boton: def.boton,
    ejecutar: () => api.post(`/admin/etiquetas/${e.idEtiqueta}/${accion}`),
  })
}

async function reactivarEtiqueta(e) {
  errorAccion.value = ''
  try {
    await api.post(`/admin/etiquetas/${e.idEtiqueta}/reactivar`)
    await cargar(pagina.value)
  } catch (err) {
    errorAccion.value = err.response?.data?.message || 'No se pudo reactivar la etiqueta.'
  }
}

function chipEstado(estado) {
  return {
    VALIDA: 'bg-green-500/10 text-green-700',
    PENDIENTE: 'bg-coral/15 text-coral',
    RECHAZADA: 'bg-rojizo/10 text-rojizo',
    INACTIVA: 'bg-cafe/10 text-cafe/60',
  }[estado] || 'bg-durazno/40 text-cafe'
}

function textoEstado(estado) {
  return {
    VALIDA: 'Válida',
    PENDIENTE: 'Pendiente',
    RECHAZADA: 'Rechazada',
    INACTIVA: 'Dada de baja',
  }[estado] || estado
}

function cerrarModal() {
  modalEtiquetaAbierto.value = false
}

cargar(0)
</script>

<template>
  <div class="flex-1 w-full mx-auto px-4 sm:px-8 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-normal text-granate">Etiquetas</h1>
      <button
        class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
        @click="abrirAltaEtiqueta"
      >
        Dar de alta
      </button>
    </div>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else>
      <p v-if="errorAccion" class="text-rojizo text-sm mb-4">{{ errorAccion }}</p>

      <!-- ------------------------------------------------------- -->
      <!-- Etiquetas -->
      <!-- ------------------------------------------------------- -->
      <div class="flex flex-wrap items-end gap-4 mb-5">
          <div>
            <label class="block text-xs text-cafe/60 mb-1">Estado</label>
            <select
              v-model="filtroEstado"
              class="rounded-lg border-0 py-2 pl-3 pr-8 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            >
              <option v-for="op in ESTADOS" :key="op.valor" :value="op.valor">{{ op.etiqueta }}</option>
            </select>
          </div>
          <div>
            <label class="block text-xs text-cafe/60 mb-1">Categoría</label>
            <select
              v-model="filtroCategoria"
              class="rounded-lg border-0 py-2 pl-3 pr-8 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            >
              <option value="">Todas las categorías</option>
              <option v-for="c in categorias" :key="c.idCategoriaEtiqueta" :value="String(c.idCategoriaEtiqueta)">
                {{ c.nombre }}
              </option>
            </select>
          </div>
        </div>

        <div class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm min-w-160">
              <thead class="bg-rosa/40 text-granate">
                <tr>
                  <th class="px-5 py-3 font-normal text-sm">Nombre</th>
                  <th class="px-5 py-3 font-normal text-sm">Categoría</th>
                  <th class="px-5 py-3 font-normal text-sm">Estado</th>
                  <th class="px-5 py-3 font-normal text-sm">Demanda</th>
                  <th class="px-5 py-3 font-normal text-sm">Creado por</th>
                  <th class="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in etiquetas" :key="e.idEtiqueta" class="border-t border-rosa/50 hover:bg-rosa/20 transition">
                  <td class="px-5 py-3 text-granate font-normal">
                    {{ e.nombre }}
                    <span class="block text-xs text-cafe/50 font-normal truncate max-w-64" :title="e.descripcion">
                      {{ e.descripcion }}
                    </span>
                  </td>
                  <td class="px-5 py-3 text-cafe">{{ e.nombreCategoria }}</td>
                  <td class="px-5 py-3">
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-normal" :class="chipEstado(e.estado)">
                      {{ textoEstado(e.estado) }}
                    </span>
                  </td>
                  <td class="px-5 py-3 text-cafe/70">{{ e.vecesSolicitada }}</td>
                  <td class="px-5 py-3 text-cafe/70">{{ e.creadoPor || '—' }}</td>
                  <td class="px-5 py-3 text-right whitespace-nowrap">
                    <button
                      v-if="e.estado === 'PENDIENTE' || e.estado === 'RECHAZADA'"
                      class="text-green-700 hover:text-coral transition mr-3"
                      @click="confirmarAccionEtiqueta(e, 'aprobar')"
                    >
                      Aprobar
                    </button>
                    <button
                      v-if="e.estado === 'PENDIENTE'"
                      class="text-rojizo hover:text-coral transition mr-3"
                      @click="confirmarAccionEtiqueta(e, 'rechazar')"
                    >
                      Rechazar
                    </button>
                    <button class="text-rojizo hover:text-coral transition mr-3" @click="abrirEdicionEtiqueta(e)">
                      Editar
                    </button>
                    <button
                      v-if="e.estado === 'INACTIVA'"
                      class="text-green-700 hover:text-coral transition"
                      @click="reactivarEtiqueta(e)"
                    >
                      Reactivar
                    </button>
                    <button
                      v-if="e.estado === 'VALIDA'"
                      class="text-cafe/50 hover:text-rojizo transition"
                      @click="confirmarAccionEtiqueta(e, 'baja')"
                    >
                      Dar de baja
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <p v-if="!etiquetas.length" class="p-8 text-cafe/50 text-center">
            No hay etiquetas con los filtros seleccionados.
          </p>
        </div>

        <div
          v-if="totalPaginas > 1"
          class="mt-5 flex items-center justify-between gap-3"
        >
          <p class="text-xs text-cafe/50">
            Página {{ pagina + 1 }} de {{ totalPaginas }}
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              class="rounded-md border border-rosa px-3 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:opacity-40"
              :disabled="pagina <= 0"
              @click="cargar(pagina - 1)"
            >
              Anterior
            </button>
            <button
              type="button"
              class="rounded-md border border-rosa px-3 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:opacity-40"
              :disabled="!hayMas"
              @click="cargar(pagina + 1)"
            >
              Siguiente
            </button>
          </div>
        </div>
    </template>

    <!-- Modal de etiqueta -->
    <div
      v-if="modalEtiquetaAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg text-granate mb-4">
          {{ editandoEtiqueta ? 'Modificar etiqueta' : 'Dar de alta etiqueta' }}
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
            <label class="block text-sm text-cafe mb-2">Categoría <span class="text-rojizo">*</span></label>
            <select
              v-model="idCategoria"
              class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            >
              <option value="" disabled>Selecciona una categoría</option>
              <option v-for="c in categoriasParaEtiqueta" :key="c.idCategoriaEtiqueta" :value="String(c.idCategoriaEtiqueta)">
                {{ c.nombre }}{{ c.activo ? '' : ' (inactiva)' }}
              </option>
            </select>
            <p v-if="!categoriasParaEtiqueta.length" class="text-xs text-cafe/50 mt-1">
              No hay categorías disponibles. Créala primero en la sección "Categorías".
            </p>
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

          <p v-if="errorEtiqueta" class="text-rojizo text-sm">{{ errorEtiqueta }}</p>
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
            @click="guardarEtiqueta"
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
