<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'
import { formatearFechaHora } from '../servicios/fechas'
import AdminPageHeader from './admin/AdminPageHeader.vue'

const route = useRoute()

const texto = ref(String(route.query.texto || ''))
const estado = ref('')
const tipo = ref('')
const pagina = ref(0)
const tamanio = ref(10)
const totalElementos = ref(0)
const totalPaginas = ref(0)
const filas = ref([])
const cargando = ref(true)
const error = ref('')

const seleccionada = ref(null)
const historial = ref([])
const cargandoHistorial = ref(false)

const porRevertir = ref(null)
const observacion = ref('')
const revirtiendo = ref(false)
const errorRevertir = ref('')

let debounceTimer
let secuencia = 0

const nombresTipo = {
  SUSPENSION: 'Suspensión temporal',
  INHABILITACION: 'Inhabilitación permanente',
}

const nombresEstado = {
  ACTIVA: 'Activa',
  VENCIDA: 'Vencida',
  REVERTIDA: 'Revertida',
}

function formatearFecha(valor) {
  if (!valor) return '—'
  return formatearFechaHora(valor) || '—'
}

function nombreTipo(valor) {
  return nombresTipo[valor] || valor || '—'
}

function nombreEstado(valor) {
  return nombresEstado[valor] || valor || '—'
}

function claseEstado(valor) {
  if (valor === 'ACTIVA') return 'bg-rojizo/10 text-rojizo'
  if (valor === 'VENCIDA') return 'bg-durazno/40 text-cafe'
  return 'bg-rosa/40 text-granate'
}

async function cargar() {
  const actual = ++secuencia
  cargando.value = true
  error.value = ''
  try {
    const params = { pagina: pagina.value, tamanio: tamanio.value }
    if (texto.value.trim()) params.texto = texto.value.trim()
    if (estado.value) params.estado = estado.value
    if (tipo.value) params.tipo = tipo.value

    const res = await api.get('/admin/sanciones', { params })
    if (actual !== secuencia) return
    const data = res.data || {}
    filas.value = Array.isArray(data.contenido) ? data.contenido : []
    totalElementos.value = Number(data.totalElementos || 0)
    totalPaginas.value = Number(data.totalPaginas || 0)
  } catch (e) {
    if (actual !== secuencia) return
    error.value = e.response?.data?.message || 'No se pudieron cargar las sanciones.'
    filas.value = []
    totalElementos.value = 0
    totalPaginas.value = 0
  } finally {
    if (actual === secuencia) cargando.value = false
  }
}

async function seleccionar(fila) {
  seleccionada.value = fila
  cargandoHistorial.value = true
  try {
    const res = await api.get(`/admin/sanciones/usuario/${fila.idUsuario}`)
    historial.value = Array.isArray(res.data) ? res.data : []
  } catch {
    historial.value = []
  } finally {
    cargandoHistorial.value = false
  }
}

function limpiarFiltros() {
  texto.value = ''
  estado.value = ''
  tipo.value = ''
  pagina.value = 0
  cargar()
}

function paginaAnterior() {
  if (pagina.value > 0) {
    pagina.value -= 1
    cargar()
  }
}

function paginaSiguiente() {
  if (pagina.value < totalPaginas.value - 1) {
    pagina.value += 1
    cargar()
  }
}

function pedirRevertir(fila) {
  porRevertir.value = fila
  observacion.value = ''
  errorRevertir.value = ''
}

async function confirmarRevertir() {
  if (!porRevertir.value) return
  revirtiendo.value = true
  errorRevertir.value = ''
  try {
    await api.post(`/admin/sanciones/${porRevertir.value.idSancion}/revertir`, {
      observacion: observacion.value.trim() || null,
    })
    porRevertir.value = null
    seleccionada.value = null
    historial.value = []
    await cargar()
  } catch (e) {
    errorRevertir.value = e.response?.data?.message || 'No se pudo revertir la sanción.'
  } finally {
    revirtiendo.value = false
  }
}

watch([texto, estado, tipo], () => {
  pagina.value = 0
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(cargar, 300)
})

watch(
  () => route.query.texto,
  (valor) => {
    texto.value = String(valor || '')
  },
)

onMounted(cargar)
onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <main class="mx-auto w-full px-4 py-8 sm:px-8">
    <div class="mb-6 flex flex-col gap-4">
      <AdminPageHeader
        eyebrow="Comunidad"
        titulo="Sanciones de cuentas"
        descripcion="Inhabilitá cuentas y revisá el historial. La persona mencionada es desconectada y no puede volver a iniciar sesión hasta que se revierta."
      >
        <template #acciones>
          <span class="text-sm tabular-nums text-cafe/60">
            {{ totalElementos }} sanción(es)
          </span>
        </template>
      </AdminPageHeader>

      <div class="grid grid-cols-1 gap-3 md:grid-cols-[minmax(0,1fr)_auto_auto_auto]">
        <input
          v-model="texto"
          type="search"
          placeholder="Buscar por usuario o correo…"
          class="min-w-0 rounded-lg border-0 px-4 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        />
        <select
          v-model="estado"
          aria-label="Filtrar por estado"
          class="rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        >
          <option value="">Todos los estados</option>
          <option value="ACTIVA">Activas</option>
          <option value="VENCIDA">Vencidas</option>
          <option value="REVERTIDA">Revertidas</option>
        </select>
        <select
          v-model="tipo"
          aria-label="Filtrar por tipo"
          class="rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        >
          <option value="">Todos los tipos</option>
          <option value="SUSPENSION">Suspensión temporal</option>
          <option value="INHABILITACION">Inhabilitación permanente</option>
        </select>
        <button
          type="button"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>
    </div>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando sanciones…</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <section class="overflow-hidden rounded-3xl bg-blanco shadow-xl shadow-rosa/40">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[46rem] text-left text-sm">
            <thead class="bg-rosa/40 text-granate">
              <tr>
                <th class="px-5 py-3 font-normal">Usuario</th>
                <th class="px-5 py-3 font-normal">Tipo</th>
                <th class="px-5 py-3 font-normal">Motivo</th>
                <th class="px-5 py-3 font-normal">Inicio</th>
                <th class="px-5 py-3 font-normal">Fin</th>
                <th class="px-5 py-3 font-normal">Estado</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="fila in filas"
                :key="fila.idSancion"
                class="cursor-pointer border-t border-rosa/50 transition hover:bg-rosa/20"
                :class="seleccionada?.idSancion === fila.idSancion ? 'bg-rosa/30' : ''"
                @click="seleccionar(fila)"
              >
                <td class="px-5 py-3">
                  <p class="font-semibold text-granate">@{{ fila.nombreUsuario }}</p>
                  <p class="text-xs text-cafe/55">{{ fila.correo }}</p>
                </td>
                <td class="px-5 py-3 text-cafe">{{ nombreTipo(fila.tipo) }}</td>
                <td class="max-w-[14rem] px-5 py-3">
                  <p class="line-clamp-2 whitespace-normal break-words text-cafe/80">
                    {{ fila.motivo }}
                  </p>
                </td>
                <td class="px-5 py-3 text-cafe/80">{{ formatearFecha(fila.fechaInicio) }}</td>
                <td class="px-5 py-3 text-cafe/80">
                  {{ fila.fechaFin ? formatearFecha(fila.fechaFin) : 'Permanente' }}
                </td>
                <td class="px-5 py-3">
                  <span
                    class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
                    :class="claseEstado(fila.estadoEfectivo)"
                  >
                    {{ nombreEstado(fila.estadoEfectivo) }}
                  </span>
                </td>
                <td class="px-5 py-3 text-right">
                  <button
                    v-if="fila.estadoEfectivo === 'ACTIVA'"
                    type="button"
                    class="text-sm font-semibold text-rojizo transition hover:text-coral"
                    @click.stop="pedirRevertir(fila)"
                  >
                    Revertir
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!filas.length" class="p-8 text-center text-cafe/50">
          No hay sanciones que coincidan con los filtros.
        </div>

        <div v-else class="flex items-center justify-between px-5 py-4">
          <span class="text-sm text-cafe/60">
            Página {{ totalPaginas ? pagina + 1 : 1 }} de {{ totalPaginas || 1 }}
          </span>
          <div class="flex gap-2">
            <button
              type="button"
              :disabled="pagina === 0"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-40"
              @click="paginaAnterior"
            >
              Anterior
            </button>
            <button
              type="button"
              :disabled="pagina >= totalPaginas - 1 || totalPaginas === 0"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-40"
              @click="paginaSiguiente"
            >
              Siguiente
            </button>
          </div>
        </div>
      </section>

      <aside class="lg:sticky lg:top-24 lg:h-fit lg:self-start">
        <div class="rounded-3xl bg-blanco p-5 shadow-xl shadow-rosa/40">
          <template v-if="seleccionada">
            <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-coral">
              Detalle
            </p>
            <h2 class="mt-1 font-display text-lg font-extrabold text-granate">
              @{{ seleccionada.nombreUsuario }}
            </h2>
            <p class="text-sm text-cafe/55">{{ seleccionada.correo }}</p>

            <dl class="mt-4 space-y-2 text-sm">
              <div class="flex justify-between gap-3">
                <dt class="text-cafe/55">Tipo</dt>
                <dd class="text-right font-medium text-cafe">{{ nombreTipo(seleccionada.tipo) }}</dd>
              </div>
              <div class="flex justify-between gap-3">
                <dt class="text-cafe/55">Estado</dt>
                <dd class="text-right">
                  <span
                    class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
                    :class="claseEstado(seleccionada.estadoEfectivo)"
                  >
                    {{ nombreEstado(seleccionada.estadoEfectivo) }}
                  </span>
                </dd>
              </div>
              <div class="flex justify-between gap-3">
                <dt class="text-cafe/55">Inicio</dt>
                <dd class="text-right text-cafe">{{ formatearFecha(seleccionada.fechaInicio) }}</dd>
              </div>
              <div class="flex justify-between gap-3">
                <dt class="text-cafe/55">Fin</dt>
                <dd class="text-right text-cafe">
                  {{ seleccionada.fechaFin ? formatearFecha(seleccionada.fechaFin) : 'Permanente' }}
                </dd>
              </div>
              <div class="flex justify-between gap-3">
                <dt class="text-cafe/55">Aplicó</dt>
                <dd class="text-right text-cafe">{{ seleccionada.adminAplico || '—' }}</dd>
              </div>
              <div v-if="seleccionada.adminRevertio" class="flex justify-between gap-3">
                <dt class="text-cafe/55">Revirtió</dt>
                <dd class="text-right text-cafe">{{ seleccionada.adminRevertio }}</dd>
              </div>
            </dl>

            <div class="mt-4 rounded-xl border border-rosa/50 bg-rosa/10 px-3 py-2.5 text-sm">
              <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">Motivo</p>
              <p class="mt-1 whitespace-normal break-words text-cafe">{{ seleccionada.motivo }}</p>
            </div>

            <button
              v-if="seleccionada.estadoEfectivo === 'ACTIVA'"
              type="button"
              class="mt-4 inline-flex min-h-10 w-full items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
              @click="pedirRevertir(seleccionada)"
            >
              Revertir sanción
            </button>

            <div class="mt-5 border-t border-rosa/40 pt-4">
              <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">
                Historial de la cuenta
              </p>
              <p v-if="cargandoHistorial" class="mt-2 text-sm text-cafe/55">Cargando…</p>
              <ul v-else-if="historial.length" class="mt-2 space-y-2">
                <li
                  v-for="item in historial"
                  :key="item.idSancion"
                  class="rounded-lg border border-rosa/40 px-3 py-2 text-xs text-cafe/75"
                >
                  <p class="font-semibold text-cafe">{{ nombreTipo(item.tipo) }}</p>
                  <p>{{ formatearFecha(item.fechaInicio) }} · {{ nombreEstado(item.estadoEfectivo) }}</p>
                </li>
              </ul>
              <p v-else class="mt-2 text-sm text-cafe/55">Sin otras sanciones.</p>
            </div>
          </template>

          <p v-else class="text-sm text-cafe/55">
            Seleccioná una sanción para ver el detalle y el historial de la cuenta.
          </p>
        </div>
      </aside>
    </div>

    <Teleport to="body">
      <div
        v-if="porRevertir"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-granate/50 p-4 backdrop-blur-sm"
        role="dialog"
        aria-modal="true"
        @click.self="porRevertir = null"
      >
        <div class="w-full max-w-md rounded-3xl bg-blanco p-6 shadow-2xl">
          <h2 class="font-display text-xl font-extrabold text-granate">Revertir sanción</h2>
          <p class="mt-2 text-sm text-cafe/70">
            @{{ porRevertir.nombreUsuario }} podrá volver a iniciar sesión. Se conserva todo el
            historial y la sanción queda registrada como revertida.
          </p>
          <label for="observacion-reversion" class="mt-4 block text-sm font-semibold text-cafe">
            Observación (opcional)
          </label>
          <textarea
            id="observacion-reversion"
            v-model="observacion"
            rows="2"
            maxlength="500"
            class="mt-1 block w-full resize-y rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          ></textarea>
          <p v-if="errorRevertir" class="mt-2 text-sm text-rojizo">{{ errorRevertir }}</p>
          <div class="mt-4 flex justify-end gap-2">
            <button
              type="button"
              class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-cafe transition hover:bg-rosa/20"
              @click="porRevertir = null"
            >
              Cancelar
            </button>
            <button
              type="button"
              :disabled="revirtiendo"
              class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="confirmarRevertir"
            >
              {{ revirtiendo ? 'Revirtiendo…' : 'Revertir' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </main>
</template>
