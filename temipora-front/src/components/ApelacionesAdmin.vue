<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearFechaHora } from '../servicios/fechas'
import AdminPageHeader from './admin/AdminPageHeader.vue'

const estado = ref('PENDIENTE')
const pagina = ref(0)
const tamanio = 10
const apelaciones = ref([])
const cargando = ref(true)
const error = ref('')
const aviso = ref('')
const seleccionada = ref(null)
const respuesta = ref('')
const resolviendo = ref(false)
const errorResolucion = ref('')

const totalElementos = computed(() => apelaciones.value.length)
const totalPaginas = computed(
  () => Math.max(1, Math.ceil(totalElementos.value / tamanio)),
)
const visibles = computed(() => {
  const desde = pagina.value * tamanio
  return apelaciones.value.slice(desde, desde + tamanio)
})

const nombresTipo = {
  SUSPENSION: 'Suspensión temporal',
  INHABILITACION: 'Inhabilitación permanente',
}

const nombresAlcance = {
  ACCESO: 'Acceso completo',
  VENTAS: 'Ventas',
  ENTREGAS: 'Solo entregas',
}

const nombresEstado = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
}

function formatearFecha(valor) {
  if (!valor) return '—'
  return formatearFechaHora(valor) || '—'
}

function nombreTipo(valor) {
  return nombresTipo[valor] || valor || '—'
}

function nombreAlcance(valor) {
  return nombresAlcance[valor] || valor || '—'
}

function nombreEstado(valor) {
  return nombresEstado[valor] || valor || '—'
}

function claseEstado(valor) {
  if (valor === 'PENDIENTE') return 'bg-durazno/40 text-cafe'
  if (valor === 'ACEPTADA') return 'bg-rosa/40 text-granate'
  return 'bg-rojizo/10 text-rojizo'
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/apelaciones', {
      params: { estado: estado.value },
    })
    apelaciones.value = Array.isArray(res.data) ? res.data : []
    if (pagina.value > totalPaginas.value - 1) {
      pagina.value = Math.max(0, totalPaginas.value - 1)
    }
    if (!apelaciones.value.some(
      (item) => item.idApelacion === seleccionada.value?.idApelacion,
    )) {
      seleccionada.value = visibles.value[0] || null
      respuesta.value = ''
      errorResolucion.value = ''
    }
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudieron cargar las apelaciones.'
    apelaciones.value = []
    seleccionada.value = null
  } finally {
    cargando.value = false
  }
}

function cambiarEstado() {
  pagina.value = 0
  seleccionada.value = null
  aviso.value = ''
  cargar()
}

function seleccionar(apelacion) {
  seleccionada.value = apelacion
  respuesta.value = ''
  errorResolucion.value = ''
  aviso.value = ''
}

function paginaAnterior() {
  if (pagina.value > 0) pagina.value -= 1
}

function paginaSiguiente() {
  if (pagina.value < totalPaginas.value - 1) pagina.value += 1
}

async function resolver(aceptada) {
  if (!seleccionada.value) return
  resolviendo.value = true
  errorResolucion.value = ''
  aviso.value = ''
  try {
    await api.post(
      `/admin/apelaciones/${seleccionada.value.idApelacion}/resolver`,
      {
        aceptada,
        respuesta: respuesta.value.trim() || null,
      },
    )
    await cargar()
    aviso.value = aceptada
      ? 'Apelación aceptada. La sanción quedó revertida y el catálogo restaurado.'
      : 'Apelación rechazada.'
  } catch (e) {
    errorResolucion.value = e.response?.data?.message
      || 'No se pudo resolver la apelación.'
  } finally {
    resolviendo.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <main class="mx-auto w-full px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Comunidad"
      titulo="Apelaciones de sanciones"
      descripcion="Revisá el motivo de la sanción, el pedido que la originó y el descargo del artista antes de resolver."
    >
      <template #acciones>
        <span class="text-sm tabular-nums text-cafe/60">
          {{ totalElementos }} apelación(es)
        </span>
      </template>
    </AdminPageHeader>

    <AdminTabs />

    <div class="mb-6 grid grid-cols-1 gap-3 md:grid-cols-[minmax(0,1fr)_auto]">
      <select
        v-model="estado"
        aria-label="Filtrar por estado"
        class="rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        @change="cambiarEstado"
      >
        <option value="PENDIENTE">Pendientes</option>
        <option value="ACEPTADA">Aceptadas</option>
        <option value="RECHAZADA">Rechazadas</option>
      </select>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Actualizar
      </button>
    </div>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando apelaciones…</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <p
        v-if="aviso"
        class="mb-4 rounded-xl border border-coral/30 bg-coral/10 px-4 py-3 text-sm text-granate"
        role="status"
      >
        {{ aviso }}
      </p>

      <div
        v-if="!apelaciones.length"
        class="rounded-3xl bg-blanco p-8 text-center text-cafe/50 shadow-xl shadow-rosa/40"
      >
        No hay apelaciones en este estado.
      </div>

      <div v-else class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_26rem]">
        <section class="overflow-hidden rounded-3xl bg-blanco shadow-xl shadow-rosa/40">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[44rem] text-left text-sm">
              <thead class="bg-rosa/40 text-granate">
                <tr>
                  <th class="px-5 py-3 font-normal">Artista</th>
                  <th class="px-5 py-3 font-normal">Sanción</th>
                  <th class="px-5 py-3 font-normal">Pedido</th>
                  <th class="px-5 py-3 font-normal">Recibida</th>
                  <th class="px-5 py-3 font-normal">Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="apelacion in visibles"
                  :key="apelacion.idApelacion"
                  class="cursor-pointer border-t border-rosa/50 transition hover:bg-rosa/20"
                  :class="seleccionada?.idApelacion === apelacion.idApelacion ? 'bg-rosa/30' : ''"
                  @click="seleccionar(apelacion)"
                >
                  <td class="px-5 py-3">
                    <p class="font-semibold text-granate">@{{ apelacion.nombreUsuario }}</p>
                    <p class="text-xs text-cafe/55">Apelación #{{ apelacion.idApelacion }}</p>
                  </td>
                  <td class="px-5 py-3">
                    <p class="text-cafe">{{ nombreTipo(apelacion.tipoSancion) }}</p>
                    <p class="text-xs text-cafe/50">{{ nombreAlcance(apelacion.alcanceSancion) }}</p>
                  </td>
                  <td class="px-5 py-3 text-cafe/80">
                    {{ apelacion.idPedido ? `#${apelacion.idPedido}` : '—' }}
                  </td>
                  <td class="px-5 py-3 text-cafe/80">{{ formatearFecha(apelacion.creadaEn) }}</td>
                  <td class="px-5 py-3">
                    <span
                      class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
                      :class="claseEstado(apelacion.estado)"
                    >
                      {{ nombreEstado(apelacion.estado) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="flex items-center justify-between px-5 py-4">
            <span class="text-sm text-cafe/60">
              Página {{ pagina + 1 }} de {{ totalPaginas }}
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
                :disabled="pagina >= totalPaginas - 1"
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
              <p class="text-sm text-cafe/55">
                Apelación #{{ seleccionada.idApelacion }} · sanción #{{ seleccionada.idSancion }}
              </p>

              <dl class="mt-4 space-y-2 text-sm">
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Estado</dt>
                  <dd class="text-right">
                    <span
                      class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
                      :class="claseEstado(seleccionada.estado)"
                    >
                      {{ nombreEstado(seleccionada.estado) }}
                    </span>
                  </dd>
                </div>
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Tipo</dt>
                  <dd class="text-right text-cafe">{{ nombreTipo(seleccionada.tipoSancion) }}</dd>
                </div>
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Alcance</dt>
                  <dd class="text-right text-cafe">{{ nombreAlcance(seleccionada.alcanceSancion) }}</dd>
                </div>
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Sanción vigente</dt>
                  <dd class="text-right text-cafe">{{ seleccionada.sancionVigente ? 'Sí' : 'No' }}</dd>
                </div>
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Inicio</dt>
                  <dd class="text-right text-cafe">{{ formatearFecha(seleccionada.fechaInicioSancion) }}</dd>
                </div>
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Fin</dt>
                  <dd class="text-right text-cafe">
                    {{ seleccionada.fechaFinSancion ? formatearFecha(seleccionada.fechaFinSancion) : 'Permanente' }}
                  </dd>
                </div>
                <div v-if="seleccionada.idPedido" class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Pedido</dt>
                  <dd class="text-right">
                    <router-link
                      :to="`/admin/pedidos/${seleccionada.idPedido}`"
                      class="font-semibold text-coral hover:underline"
                    >
                      #{{ seleccionada.idPedido }}
                    </router-link>
                    <span v-if="seleccionada.idHitoPedido" class="text-cafe/55">
                      · hito #{{ seleccionada.idHitoPedido }}
                    </span>
                  </dd>
                </div>
                <div v-if="seleccionada.fechaObjetivo" class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Plazo incumplido</dt>
                  <dd class="text-right text-cafe">{{ formatearFecha(seleccionada.fechaObjetivo) }}</dd>
                </div>
                <div class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Recibida</dt>
                  <dd class="text-right text-cafe">{{ formatearFecha(seleccionada.creadaEn) }}</dd>
                </div>
                <div v-if="seleccionada.resueltaEn" class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Resuelta</dt>
                  <dd class="text-right text-cafe">{{ formatearFecha(seleccionada.resueltaEn) }}</dd>
                </div>
                <div v-if="seleccionada.adminResolvio" class="flex justify-between gap-3">
                  <dt class="text-cafe/55">Resolvió</dt>
                  <dd class="text-right text-cafe">{{ seleccionada.adminResolvio }}</dd>
                </div>
              </dl>

              <div class="mt-4 rounded-xl border border-rosa/50 bg-rosa/10 px-3 py-2.5 text-sm">
                <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">Motivo de la sanción</p>
                <p class="mt-1 whitespace-normal break-words text-cafe">{{ seleccionada.motivoSancion }}</p>
              </div>

              <div class="mt-3 rounded-xl border border-rosa/50 px-3 py-2.5 text-sm">
                <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">Mensaje del artista</p>
                <p class="mt-1 whitespace-pre-line text-cafe">{{ seleccionada.mensaje }}</p>
              </div>

              <div v-if="seleccionada.estado === 'PENDIENTE'" class="mt-4">
                <label for="respuesta-apelacion" class="block text-sm font-semibold text-cafe">
                  Respuesta al artista (opcional)
                </label>
                <textarea
                  id="respuesta-apelacion"
                  v-model="respuesta"
                  rows="3"
                  maxlength="1000"
                  class="mt-1 block w-full resize-y rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
                ></textarea>
                <p v-if="errorResolucion" class="mt-2 text-sm text-rojizo">{{ errorResolucion }}</p>
                <div class="mt-3 flex flex-wrap gap-2">
                  <button
                    type="button"
                    :disabled="resolviendo"
                    class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
                    @click="resolver(true)"
                  >
                    {{ resolviendo ? 'Resolviendo…' : 'Aceptar y revertir' }}
                  </button>
                  <button
                    type="button"
                    :disabled="resolviendo"
                    class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-50"
                    @click="resolver(false)"
                  >
                    Rechazar
                  </button>
                </div>
              </div>

              <div v-else class="mt-4 rounded-xl bg-rosa/10 px-3 py-2.5 text-sm text-cafe/80">
                <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">Resolución</p>
                <p class="mt-1">{{ seleccionada.respuesta || 'Sin respuesta escrita.' }}</p>
              </div>
            </template>

            <p v-else class="text-sm text-cafe/55">
              Seleccioná una apelación para ver el detalle y resolverla.
            </p>
          </div>
        </aside>
      </div>
    </template>
  </main>
</template>
