<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  contraparteDe,
  datosContraparte,
  estadoDe,
  formatearFechaSolicitud,
} from '../servicios/solicitudes'
import { estadoServicioDe } from '../servicios/servicios'
import VisorImagenes from '../components/VisorImagenes.vue'
import ModalRechazo from '../components/ModalRechazo.vue'

const route = useRoute()
const router = useRouter()

const solicitud = ref(null)
const cargando = ref(true)
const errorCarga = ref('')

const idActual = Number(localStorage.getItem('idUsuario'))

const esArtistaDeLaSolicitud = computed(() => {
  const idArtista = solicitud.value?.artista?.idUsuario
  if (!idArtista || !idActual) return false
  return idArtista === idActual
})

const contraparte = computed(() =>
  contraparteDe(solicitud.value, esArtistaDeLaSolicitud.value),
)
const datosPersona = computed(() => datosContraparte(contraparte.value))
const tituloContraparte = computed(() =>
  esArtistaDeLaSolicitud.value ? 'Cliente' : 'Artista',
)
const nombreContraparte = computed(() => datosPersona.value?.nombre || 'No disponible')
const usuarioContraparte = computed(() => datosPersona.value?.usuario || '')
const fotoContraparte = computed(() => datosPersona.value?.foto || '')
const inicialContraparte = computed(() => datosPersona.value?.inicial || '?')
const estadoInfo = computed(() => estadoDe(solicitud.value?.estado))

const avisoServicio = computed(() => {
  const estado = solicitud.value?.estadoServicio
  if (!estado || estado === 'PUBLICADO') return null
  return estado === 'ARCHIVADO'
    ? 'Este servicio fue dado de baja definitivamente. El historial de tu solicitud sigue disponible.'
    : 'Este servicio está pausado: no recibe solicitudes nuevas, pero esta solicitud sigue vigente.'
})
const avisoServicioClase = computed(() => (
  solicitud.value?.estadoServicio === 'ARCHIVADO'
    ? 'border-rojizo/25 bg-rojizo/5 text-rojizo'
    : 'border-durazno bg-durazno/30 text-granate'
))
const estadoServicioInfo = computed(() => estadoServicioDe(solicitud.value?.estadoServicio))

const modalRechazoAbierto = ref(false)
const rechazando = ref(false)
const errorRechazo = ref('')
const avisoExito = ref('')

const puedeRechazar = computed(
  () => esArtistaDeLaSolicitud.value
    && ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.value?.estado),
)
const textoMotivoRechazo = computed(() => {
  if (solicitud.value?.motivoRechazo) return solicitud.value.motivoRechazo
  return esArtistaDeLaSolicitud.value
    ? 'No especificaste un motivo.'
    : 'El artista decidió no mencionar el motivo.'
})

function gruposDeRespuestas(item) {
  const respuestas = [...(item?.respuestas || [])].sort((a, b) => {
    const orden = (a.orden ?? 0) - (b.orden ?? 0)
    return orden !== 0 ? orden : (a.suborden ?? 0) - (b.suborden ?? 0)
  })
  const grupos = new Map()

  for (const respuesta of respuestas) {
    const valor = respuesta.valor
    if (valor === null || valor === undefined || String(valor).trim() === '') continue

    const clave = respuesta.idPregunta != null
      ? `pregunta-${respuesta.idPregunta}`
      : `respuesta-${respuesta.orden}-${respuesta.enunciado}`
    if (!grupos.has(clave)) {
      grupos.set(clave, {
        clave,
        enunciado: respuesta.enunciado,
        codigoTipo: respuesta.codigoTipo,
        valores: [],
      })
    }
    grupos.get(clave).valores.push(String(valor))
  }

  return [...grupos.values()]
}

const dibujosAbiertos = ref(new Set())
const visor = ref({ abierto: false, imagenes: [], indice: 0, etiqueta: '', descripcion: '' })

function resumenDeGrupos(grupos) {
  if (!grupos.length) return 'Este dibujo no tiene respuestas registradas.'
  const primero = grupos[0]
  if (primero.codigoTipo === 'SUBIR_IMAGEN') {
    const cantidad = primero.valores.length
    return `${primero.enunciado}: ${cantidad} ${cantidad === 1 ? 'imagen adjunta' : 'imágenes adjuntas'}`
  }
  const texto = primero.valores.join(' ').replace(/\s+/g, ' ').trim()
  return `${primero.enunciado}: ${texto}`
}

const dibujos = computed(() =>
  (solicitud.value?.items || []).map((item, posicion) => {
    const grupos = gruposDeRespuestas(item)
    return { ...item, posicion, grupos, resumen: resumenDeGrupos(grupos) }
  }),
)

function alternarDibujo(idItem) {
  const siguiente = new Set(dibujosAbiertos.value)
  if (siguiente.has(idItem)) {
    siguiente.delete(idItem)
  } else {
    siguiente.add(idItem)
  }
  dibujosAbiertos.value = siguiente
}

function abrirVisor(dibujo, grupo, posicionImagen) {
  visor.value = {
    abierto: true,
    imagenes: grupo.valores,
    indice: posicionImagen,
    etiqueta: `Dibujo ${dibujo.posicion + 1}`,
    descripcion: grupo.enunciado,
  }
}

function cerrarVisor() {
  visor.value = { ...visor.value, abierto: false }
}

function abrirModalRechazo() {
  errorRechazo.value = ''
  modalRechazoAbierto.value = true
}

function cerrarModalRechazo() {
  if (!rechazando.value) modalRechazoAbierto.value = false
}

async function confirmarRechazo(motivo) {
  if (rechazando.value) return
  rechazando.value = true
  errorRechazo.value = ''
  try {
    const res = await api.post(
      `/solicitudes-servicio/${solicitud.value.idSolicitud}/rechazar`,
      { motivo: motivo || null },
    )
    solicitud.value = res.data
    modalRechazoAbierto.value = false
    avisoExito.value = 'La solicitud fue rechazada y el cliente recibió el aviso.'
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorRechazo.value = e.response?.data?.message || 'No se pudo rechazar la solicitud.'
  } finally {
    rechazando.value = false
  }
}

function volver() {
  router.push({ name: 'solicitudes' })
}

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const res = await api.get(`/solicitudes-servicio/${route.params.idSolicitud}`)
    solicitud.value = res.data
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (e.response?.status === 403 || e.response?.status === 404) {
      errorCarga.value = 'No podés ver esta solicitud o ya no existe.'
    } else {
      errorCarga.value = e.response?.data?.message || 'No se pudo cargar la solicitud.'
    }
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a solicitudes
    </button>

    <div v-if="cargando" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center text-sm text-cafe/60">
      Cargando solicitud...
    </div>

    <div v-else-if="errorCarga" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center">
      <p class="text-sm text-cafe/70">{{ errorCarga }}</p>
      <div class="mt-4 flex flex-wrap justify-center gap-2">
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargar"
        >
          Reintentar
        </button>
        <button
          type="button"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="volver"
        >
          Ir a mis solicitudes
        </button>
      </div>
    </div>

    <template v-else-if="solicitud">
      <header class="mt-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Solicitud de servicio</p>
          <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
            Solicitud #{{ solicitud.idSolicitud }}
          </h1>
          <p class="mt-2 text-sm text-cafe/65">
            {{ solicitud.nombreServicio || 'Servicio' }}
            <span aria-hidden="true"> · </span>
            {{ formatearFechaSolicitud(solicitud.fechaCreacion) }}
          </p>
        </div>
        <span
          class="inline-flex self-start rounded-full border px-3 py-1.5 text-xs font-semibold"
          :class="estadoInfo.clase"
        >
          {{ estadoInfo.texto }}
        </span>
      </header>

      <p
        v-if="avisoServicio"
        class="mt-4 rounded-xl border px-4 py-3 text-sm leading-relaxed"
        :class="avisoServicioClase"
        role="status"
      >
        {{ avisoServicio }}
        <span
          v-if="solicitud.estadoServicio === 'ARCHIVADO'"
          class="ml-1 inline-flex rounded-full border px-2 py-0.5 text-[11px] font-semibold"
          :class="estadoServicioInfo.clase"
        >
          {{ estadoServicioInfo.texto }}
        </span>
      </p>

      <div class="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div class="space-y-6">
          <section
            v-if="solicitud.estado === 'RECHAZADA'"
            class="rounded-2xl border border-rojizo/25 bg-rojizo/5 p-5 sm:p-6"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-rojizo">
              Solicitud rechazada
            </p>
            <p class="mt-2 text-sm leading-relaxed text-cafe/80">{{ textoMotivoRechazo }}</p>
            <p v-if="solicitud.fechaRechazo" class="mt-2 text-xs text-cafe/50">
              {{ formatearFechaSolicitud(solicitud.fechaRechazo) }}
            </p>
          </section>

          <section class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
              {{ tituloContraparte }}
            </p>

            <div v-if="contraparte" class="mt-4 flex flex-col gap-4 sm:flex-row sm:items-center">
              <img
                v-if="fotoContraparte"
                :src="fotoContraparte"
                alt=""
                class="h-16 w-16 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
              />
              <span
                v-else
                class="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-rosa text-xl font-bold text-granate shadow-sm ring-1 ring-white/70"
                aria-hidden="true"
              >
                {{ inicialContraparte }}
              </span>

              <div class="min-w-0">
                <p class="text-lg font-semibold text-granate">{{ nombreContraparte }}</p>
                <p v-if="usuarioContraparte" class="text-sm text-cafe/55">@{{ usuarioContraparte }}</p>
                <p v-if="contraparte.nombrePais" class="mt-1 text-xs text-cafe/50">{{ contraparte.nombrePais }}</p>
              </div>
            </div>

            <p v-else class="mt-3 text-sm text-cafe/60">
              Los datos públicos de esta persona no están disponibles.
            </p>

            <p v-if="contraparte?.biografia" class="mt-4 border-t border-rosa/40 pt-4 text-sm leading-relaxed text-cafe/70">
              {{ contraparte.biografia }}
            </p>
          </section>

          <section>
            <div class="flex flex-wrap items-end justify-between gap-2">
              <div>
                <h2 class="text-xl font-semibold text-granate">Dibujos solicitados</h2>
                <p class="mt-1 text-sm text-cafe/60">
                  {{ solicitud.cantidadDibujos }} {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }} en esta solicitud.
                </p>
              </div>
              <span
                v-if="solicitud.prioridadDefinidaPorCliente"
                class="rounded-full bg-durazno/50 px-3 py-1 text-xs font-semibold text-granate"
              >
                Prioridad definida
              </span>
            </div>

            <div class="mt-4 space-y-3">
              <article
                v-for="dibujo in dibujos"
                :key="dibujo.idItemSolicitud"
                class="overflow-hidden rounded-2xl border border-rosa/50 bg-blanco"
              >
                <h3>
                  <button
                    type="button"
                    class="flex w-full items-start justify-between gap-4 px-5 py-4 text-left transition hover:bg-rosa/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/50 sm:px-6"
                    :aria-expanded="dibujosAbiertos.has(dibujo.idItemSolicitud)"
                    :aria-controls="`panel-dibujo-${dibujo.idItemSolicitud}`"
                    @click="alternarDibujo(dibujo.idItemSolicitud)"
                  >
                    <span class="min-w-0">
                      <span class="flex flex-wrap items-center gap-2">
                        <span class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                          Dibujo {{ dibujo.posicion + 1 }}
                        </span>
                        <span
                          v-if="solicitud.prioridadDefinidaPorCliente && dibujo.ordenPrioridad"
                          class="rounded-full bg-coral/10 px-2.5 py-0.5 text-[11px] font-semibold text-coral"
                        >
                          Prioridad {{ dibujo.ordenPrioridad }}
                        </span>
                      </span>
                      <span class="mt-1 block text-lg font-semibold text-granate">{{ dibujo.nombre }}</span>
                      <span
                        v-if="!dibujosAbiertos.has(dibujo.idItemSolicitud)"
                        class="mt-1.5 line-clamp-2 block text-sm leading-relaxed text-cafe/60"
                      >
                        {{ dibujo.resumen }}
                      </span>
                    </span>

                    <svg
                      class="mt-1 h-5 w-5 shrink-0 text-cafe/50 transition-transform duration-300"
                      :class="dibujosAbiertos.has(dibujo.idItemSolicitud) ? 'rotate-180' : ''"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      aria-hidden="true"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
                    </svg>
                  </button>
                </h3>

                <div
                  :id="`panel-dibujo-${dibujo.idItemSolicitud}`"
                  class="grid transition-[grid-template-rows] duration-300 ease-out"
                  :class="dibujosAbiertos.has(dibujo.idItemSolicitud) ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
                  :inert="!dibujosAbiertos.has(dibujo.idItemSolicitud)"
                >
                  <div class="overflow-hidden">
                    <div class="space-y-5 border-t border-rosa/40 px-5 py-5 sm:px-6">
                      <div
                        v-for="grupo in dibujo.grupos"
                        :key="grupo.clave"
                        class="border-b border-rosa/30 pb-5 last:border-b-0 last:pb-0"
                      >
                        <p class="text-sm font-semibold text-cafe">{{ grupo.enunciado }}</p>

                        <div v-if="grupo.codigoTipo === 'SUBIR_IMAGEN'" class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                          <button
                            v-for="(url, urlIndice) in grupo.valores"
                            :key="`${url}-${urlIndice}`"
                            type="button"
                            class="group relative block aspect-square overflow-hidden rounded-xl bg-rosa/20 ring-1 ring-rosa/40 transition hover:ring-2 hover:ring-coral focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral"
                            :aria-label="`Abrir ${dibujo.nombre}: imagen ${urlIndice + 1} de ${grupo.valores.length}`"
                            @click="abrirVisor(dibujo, grupo, urlIndice)"
                          >
                            <img
                              :src="url"
                              :alt="`Imagen de referencia ${urlIndice + 1} de ${dibujo.nombre}`"
                              class="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                              loading="lazy"
                            />
                            <span
                              class="absolute bottom-2 right-2 rounded-full bg-granate/75 px-2 py-0.5 text-[11px] font-semibold text-white"
                            >
                              {{ urlIndice + 1 }}/{{ grupo.valores.length }}
                            </span>
                          </button>
                        </div>

                        <p
                          v-else-if="grupo.codigoTipo === 'TEXTO'"
                          class="mt-2 whitespace-pre-line text-sm leading-relaxed text-cafe/75"
                        >
                          {{ grupo.valores.join('\n') }}
                        </p>

                        <div v-else class="mt-2 flex flex-wrap gap-2">
                          <span
                            v-for="(valor, valorIndice) in grupo.valores"
                            :key="`${valor}-${valorIndice}`"
                            class="rounded-full bg-rosa/20 px-3 py-1 text-sm text-cafe"
                          >
                            {{ valor }}
                          </span>
                        </div>
                      </div>

                      <p v-if="!dibujo.grupos.length" class="text-sm text-cafe/55">
                        Este dibujo no tiene respuestas registradas.
                      </p>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </div>

        <aside class="space-y-4 lg:sticky lg:top-16 lg:h-fit lg:self-start">
          <section class="rounded-2xl border border-rosa/50 bg-blanco p-5">
            <h2 class="text-base font-semibold text-granate">Resumen</h2>
            <dl class="mt-4 space-y-3 text-sm">
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Servicio</dt>
                <dd class="text-right font-medium text-cafe">{{ solicitud.nombreServicio || '—' }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Cantidad</dt>
                <dd class="text-right font-medium text-cafe">{{ solicitud.cantidadDibujos }} dibujo(s)</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Precio base por dibujo</dt>
                <dd class="text-right font-medium text-cafe">
                  {{ solicitud.precioBase != null ? formatearPrecio(solicitud.precioBase, solicitud.moneda) : 'A definir' }}
                </dd>
              </div>
              <div v-if="solicitud.terminosVersion" class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Términos aceptados</dt>
                <dd class="text-right font-medium text-cafe">Versión {{ solicitud.terminosVersion }}</dd>
              </div>
              <div v-if="solicitud.posicionListaEspera" class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Lista de espera</dt>
                <dd class="text-right font-medium text-cafe">Posición {{ solicitud.posicionListaEspera }}</dd>
              </div>
            </dl>
          </section>

          <section class="rounded-2xl border border-rosa/50 bg-blanco p-5">
            <h2 class="text-base font-semibold text-granate">Acciones</h2>

            <p
              v-if="avisoExito"
              role="status"
              class="mt-3 rounded-xl border border-green-600/20 bg-green-500/10 px-3 py-2 text-xs leading-relaxed text-green-700"
            >
              {{ avisoExito }}
            </p>

            <p v-if="puedeRechazar" class="mt-2 text-xs leading-relaxed text-cafe/60">
              Podés rechazar la solicitud y, si querés, explicarle el motivo al cliente.
            </p>
            <p
              v-else-if="esArtistaDeLaSolicitud && solicitud.estado === 'RECHAZADA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              Rechazaste esta solicitud.
            </p>
            <p v-else-if="esArtistaDeLaSolicitud" class="mt-2 text-xs leading-relaxed text-cafe/60">
              Esta solicitud ya no se puede rechazar.
            </p>
            <p v-else class="mt-2 text-xs leading-relaxed text-cafe/60">
              Cuando el artista envíe el presupuesto, vas a poder pagarlo desde acá.
            </p>

            <div v-if="esArtistaDeLaSolicitud" class="mt-4 grid gap-2">
              <button
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-4 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:cursor-not-allowed disabled:border-rosa disabled:text-cafe/40 disabled:hover:bg-transparent"
                :disabled="!puedeRechazar"
                @click="abrirModalRechazo"
              >
                Rechazar
              </button>
              <button
                type="button"
                disabled
                class="inline-flex min-h-10 cursor-not-allowed items-center justify-center rounded-full bg-coral/40 px-4 text-sm font-semibold text-white/70"
              >
                Presupuestar
              </button>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <VisorImagenes
      :abierto="visor.abierto"
      :imagenes="visor.imagenes"
      :indice="visor.indice"
      :etiqueta="visor.etiqueta"
      :descripcion="visor.descripcion"
      @cerrar="cerrarVisor"
    />

    <ModalRechazo
      :abierto="modalRechazoAbierto"
      :cargando="rechazando"
      :error="errorRechazo"
      @cerrar="cerrarModalRechazo"
      @confirmar="confirmarRechazo"
    />
  </div>
</template>
