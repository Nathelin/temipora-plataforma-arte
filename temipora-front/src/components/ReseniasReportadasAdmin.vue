<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import ModalConfirmacion from './ModalConfirmacion.vue'
import ValoracionEstrellas from './ValoracionEstrellas.vue'
import {
  cargarReseniasOcultas,
  cargarReseniasReportadas,
  descartarReportesResenia,
  etiquetaEstadoReporte,
  etiquetaMotivoReporte,
  ocultarResenia,
  restaurarResenia,
} from '../servicios/resenias'

const pestania = ref('pendientes')
const contenido = ref([])
const pagina = ref(0)
const totalPaginas = ref(0)
const totalElementos = ref(0)
const hayMas = ref(false)
const cargando = ref(true)
const error = ref('')
const accion = ref(null)
const motivo = ref('')
const procesando = ref(false)
const errorAccion = ref('')
const mensaje = ref('')

const esPendientes = computed(() => pestania.value === 'pendientes')

const tituloModal = computed(() => {
  if (!accion.value) return ''
  if (accion.value.tipo === 'ocultar') return '¿Ocultar esta reseña?'
  if (accion.value.tipo === 'descartar') return '¿Descartar los reportes?'
  return '¿Restaurar esta reseña?'
})

const textoConfirmar = computed(() => {
  if (!accion.value) return 'Confirmar'
  if (accion.value.tipo === 'ocultar') return 'Ocultar reseña'
  if (accion.value.tipo === 'descartar') return 'Descartar reportes'
  return 'Restaurar reseña'
})

const mensajeModal = computed(() => {
  if (!accion.value) return ''
  if (accion.value.tipo === 'ocultar') {
    return 'La reseña dejará de verse en el servicio, el perfil y los promedios. '
      + 'Los reportes pendientes se marcan como resueltos y el autor recibe un aviso con el motivo.'
  }
  if (accion.value.tipo === 'descartar') {
    return 'La reseña seguirá publicada. Los reportes pendientes se marcan como descartados '
      + 'y no se notifica al autor.'
  }
  return 'La reseña volverá a mostrarse públicamente y se limpia el motivo de moderación. '
    + 'Los reportes resueltos quedan en el historial.'
})

async function cargar(nuevaPagina = 0) {
  cargando.value = true
  error.value = ''
  errorAccion.value = ''
  try {
    const params = { pagina: nuevaPagina, tamanio: 10 }
    const res = esPendientes.value
      ? await cargarReseniasReportadas(params)
      : await cargarReseniasOcultas(params)
    const data = res.data || {}
    contenido.value = data.contenido || []
    pagina.value = data.pagina || 0
    totalPaginas.value = data.totalPaginas || 0
    totalElementos.value = data.totalElementos || 0
    hayMas.value = Boolean(data.hayMas)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudieron cargar las reseñas para moderar.'
  } finally {
    cargando.value = false
  }
}

watch(pestania, () => {
  mensaje.value = ''
  totalElementos.value = 0
  cargar(0)
})

onMounted(() => cargar())

function abrirAccion(tipo, resenia) {
  accion.value = { tipo, resenia }
  motivo.value = ''
  errorAccion.value = ''
  mensaje.value = ''
}

function cerrarAccion() {
  if (procesando.value) return
  accion.value = null
}

async function confirmarAccion() {
  if (!accion.value || procesando.value) return
  procesando.value = true
  errorAccion.value = ''
  try {
    const { tipo, resenia } = accion.value
    if (tipo === 'ocultar') {
      await ocultarResenia(resenia.idResenia, {
        motivo: motivo.value.trim() || null,
      })
      mensaje.value = 'La reseña quedó oculta y los reportes pendientes se resolvieron.'
    } else if (tipo === 'descartar') {
      await descartarReportesResenia(resenia.idResenia)
      mensaje.value = 'Los reportes pendientes se descartaron. La reseña sigue publicada.'
    } else {
      await restaurarResenia(resenia.idResenia)
      mensaje.value = 'La reseña volvió a publicarse.'
    }

    const paginaActual = pagina.value
    accion.value = null
    await cargar(paginaActual)
    if (!contenido.value.length && paginaActual > 0) {
      await cargar(paginaActual - 1)
    }
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudo completar la acción.'
  } finally {
    procesando.value = false
  }
}

function formatearFecha(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return '—'
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}

function etiquetaEstadoResenia(estado) {
  if (estado === 'OCULTA') return 'Oculta'
  if (estado === 'ELIMINADA') return 'Eliminada'
  return 'Publicada'
}

function claseEstadoResenia(estado) {
  if (estado === 'OCULTA') return 'border-rojizo/30 bg-rojizo/10 text-rojizo'
  if (estado === 'ELIMINADA') return 'border-cafe/20 bg-cafe/5 text-cafe/60'
  return 'border-green-700/25 bg-green-500/10 text-green-700'
}

function claseReporte(estado) {
  if (estado === 'PENDIENTE') return 'border-durazno bg-durazno/30 text-granate'
  if (estado === 'RESUELTO') return 'border-green-700/25 bg-green-500/10 text-green-700'
  return 'border-cafe/15 bg-cafe/5 text-cafe/60'
}

function claseOpcion(opcion) {
  return opcion.polaridad === 'NEGATIVA'
    ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
    : 'border-rosa bg-blanco text-cafe'
}
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <div class="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 class="text-xl font-normal text-granate">Moderación de reseñas</h1>
        <p class="mt-1 max-w-2xl text-sm leading-relaxed text-cafe/60">
          Revisá los reportes de la comunidad y decidí si ocultás la reseña o descartás el reporte.
          Al ocultarla deja de aparecer en el servicio, el perfil y los promedios.
        </p>
      </div>
      <router-link
        :to="{ name: 'admin-resenias-historial' }"
        class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
      >
        Ver historial de reportes →
      </router-link>
    </div>

    <div class="mb-4 flex flex-wrap gap-2" role="tablist" aria-label="Estado de las reseñas">
      <button
        type="button"
        role="tab"
        :aria-selected="esPendientes"
        class="inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition"
        :class="esPendientes
          ? 'border-coral bg-coral/10 font-semibold text-granate'
          : 'border-rosa bg-blanco text-cafe/60 hover:bg-rosa/20'"
        @click="pestania = 'pendientes'"
      >
        Reportadas
        <span
          v-if="esPendientes && totalElementos"
          class="rounded-full bg-coral/20 px-2 py-0.5 text-[11px] font-bold text-granate"
        >
          {{ totalElementos }}
        </span>
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="!esPendientes"
        class="inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition"
        :class="!esPendientes
          ? 'border-coral bg-coral/10 font-semibold text-granate'
          : 'border-rosa bg-blanco text-cafe/60 hover:bg-rosa/20'"
        @click="pestania = 'ocultas'"
      >
        Ocultas
        <span
          v-if="!esPendientes && totalElementos"
          class="rounded-full bg-coral/20 px-2 py-0.5 text-[11px] font-bold text-granate"
        >
          {{ totalElementos }}
        </span>
      </button>
    </div>

    <p v-if="mensaje" class="mb-4 rounded-lg bg-green-500/10 px-3 py-2 text-sm text-green-700">
      {{ mensaje }}
    </p>

    <p v-if="cargando" class="py-10 text-center text-sm text-cafe/55">Cargando reseñas...</p>
    <p v-else-if="error" class="py-10 text-center text-sm text-rojizo" role="alert">{{ error }}</p>
    <p v-else-if="!contenido.length" class="rounded-2xl border border-dashed border-rosa/60 py-10 text-center text-sm text-cafe/55">
      {{ esPendientes
        ? 'No hay reseñas reportadas pendientes de revisión.'
        : 'No hay reseñas ocultas actualmente.' }}
    </p>

    <div v-else class="space-y-4">
      <article
        v-for="resenia in contenido"
        :key="resenia.idResenia"
        class="rounded-3xl border border-rosa/50 bg-blanco p-5 shadow-sm sm:p-6"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                :class="claseEstadoResenia(resenia.estado)"
              >
                {{ etiquetaEstadoResenia(resenia.estado) }}
              </span>
              <p class="text-sm font-semibold text-granate">
                {{ resenia.nombreServicio || 'Servicio' }}
              </p>
            </div>
            <p class="mt-1 text-xs text-cafe/60">
              Pedido #{{ resenia.idPedido }} ·
              {{ resenia.autor?.nombreUsuario ? `@${resenia.autor.nombreUsuario}` : 'Autor desconocido' }}
              <span v-if="!resenia.mostrarNombre">· publicada como anónima</span>
              · {{ formatearFecha(resenia.fechaVersion || resenia.fechaCreacion) }}
            </p>
          </div>

          <div class="flex items-center gap-2">
            <ValoracionEstrellas
              :model-value="resenia.calificacionGeneral || 0"
              readonly
              tamano="sm"
              etiqueta="Calificación"
            />
            <span class="text-sm font-semibold text-granate">
              {{ resenia.calificacionGeneral ?? '—' }}/5
            </span>
          </div>
        </div>

        <p
          v-if="resenia.descripcion"
          class="mt-3 whitespace-pre-line text-sm leading-relaxed text-cafe/80"
        >
          {{ resenia.descripcion }}
        </p>
        <p v-else class="mt-3 text-sm italic text-cafe/45">
          La reseña no tiene descripción.
        </p>

        <dl v-if="resenia.valores?.length" class="mt-3 grid gap-1.5 sm:grid-cols-2">
          <div
            v-for="valor in resenia.valores"
            :key="valor.idCriterio"
            class="flex items-center justify-between gap-2 rounded-lg bg-rosa/10 px-3 py-1.5 text-xs"
          >
            <dt class="text-cafe/65">{{ valor.nombreCriterio }}</dt>
            <dd class="font-semibold text-granate">{{ valor.valor }}</dd>
          </div>
        </dl>

        <div v-if="resenia.opciones?.length" class="mt-3 flex flex-wrap gap-1.5">
          <span
            v-for="opcion in resenia.opciones"
            :key="opcion.idOpcion"
            class="rounded-full border px-2.5 py-0.5 text-[11px] font-medium"
            :class="claseOpcion(opcion)"
          >
            {{ opcion.etiqueta }}
          </span>
        </div>

        <p
          v-if="resenia.estado === 'OCULTA' && (resenia.moderadaEn || resenia.motivoModeracion)"
          class="mt-3 rounded-lg bg-rojizo/5 px-3 py-2 text-xs leading-relaxed text-cafe/70"
        >
          Ocultada {{ formatearFecha(resenia.moderadaEn) }}.
          Motivo: {{ resenia.motivoModeracion || 'No especificado' }}.
        </p>

        <div class="mt-4 border-t border-rosa/40 pt-4">
          <div class="mb-2 flex flex-wrap items-center justify-between gap-2">
            <p class="text-xs font-semibold uppercase tracking-wide text-cafe/55">
              Reportes ({{ resenia.reportesTotales }})
            </p>
            <span
              v-if="resenia.reportesPendientes"
              class="rounded-full bg-durazno/50 px-2 py-0.5 text-[11px] font-semibold text-granate"
            >
              {{ resenia.reportesPendientes }} pendientes
            </span>
          </div>

          <p v-if="!resenia.reportes?.length" class="text-xs text-cafe/50">
            Sin reportes registrados.
          </p>
          <ul v-else class="space-y-2">
            <li
              v-for="reporte in resenia.reportes"
              :key="reporte.idReporte"
              class="rounded-xl bg-rosa/5 px-3 py-2"
            >
              <div class="flex flex-wrap items-center justify-between gap-2 text-[11px]">
                <span class="font-semibold text-granate">
                  {{ reporte.etiquetaMotivo || etiquetaMotivoReporte(reporte.motivo) }}
                </span>
                <span class="rounded-full border px-2 py-0.5" :class="claseReporte(reporte.estado)">
                  {{ etiquetaEstadoReporte(reporte.estado) }}
                </span>
              </div>
              <p v-if="reporte.comentario" class="mt-1 text-xs leading-relaxed text-cafe/75">
                {{ reporte.comentario }}
              </p>
              <p class="mt-1 text-[11px] text-cafe/50">
                {{ reporte.reportante?.nombreUsuario ? `@${reporte.reportante.nombreUsuario}` : 'Usuario' }}
                · {{ formatearFecha(reporte.fechaCreacion) }}
                <span v-if="reporte.fechaResolucion">
                  · {{ reporte.accion === 'DESCARTAR' ? 'descartado' : 'resuelto' }}
                  {{ formatearFecha(reporte.fechaResolucion) }}
                  <span v-if="reporte.resueltoPor?.nombreUsuario">
                    por @{{ reporte.resueltoPor.nombreUsuario }}
                  </span>
                </span>
              </p>
            </li>
          </ul>
        </div>

        <div class="mt-4 flex flex-wrap gap-2">
          <template v-if="esPendientes">
            <button
              type="button"
              class="rounded-md bg-rojizo px-4 py-2 text-sm text-white shadow-sm transition hover:bg-granate"
              @click="abrirAccion('ocultar', resenia)"
            >
              Ocultar reseña
            </button>
            <button
              type="button"
              class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
              @click="abrirAccion('descartar', resenia)"
            >
              Descartar reportes
            </button>
          </template>
          <button
            v-else
            type="button"
            class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
            @click="abrirAccion('restaurar', resenia)"
          >
            Restaurar reseña
          </button>
        </div>
      </article>
    </div>

    <div
      v-if="!cargando && !error && totalPaginas > 1"
      class="mt-6 flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
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

    <ModalConfirmacion
      :abierto="Boolean(accion)"
      :titulo="tituloModal"
      :mensaje="mensajeModal"
      :texto-confirmar="textoConfirmar"
      :destructivo="accion?.tipo !== 'restaurar'"
      @confirmar="confirmarAccion"
      @cerrar="cerrarAccion"
    >
      <div v-if="accion?.tipo === 'ocultar'" class="mt-4">
        <label for="motivo-ocultar" class="mb-2 block text-sm text-cafe">
          Motivo visible para el autor <span class="text-cafe/40">(opcional)</span>
        </label>
        <textarea
          id="motivo-ocultar"
          v-model="motivo"
          rows="3"
          maxlength="500"
          class="block w-full resize-none rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          placeholder="Ej.: contiene insultos o datos personales."
        ></textarea>
        <p class="mt-1 text-xs text-cafe/50">
          Si lo dejás vacío se registra “No especificado”.
        </p>
      </div>

      <p v-if="errorAccion" class="mt-3 text-sm text-rojizo" role="alert">
        {{ errorAccion }}
      </p>
    </ModalConfirmacion>
  </div>
</template>
