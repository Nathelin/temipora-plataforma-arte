<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'

const route = useRoute()
const router = useRouter()

const solicitud = ref(null)
const cargando = ref(true)
const errorCarga = ref('')

const ESTADOS = {
  PENDIENTE: { texto: 'Pendiente', clase: 'border-mantequilla bg-mantequilla/40 text-granate' },
  EN_LISTA_ESPERA: { texto: 'En lista de espera', clase: 'border-durazno bg-durazno/50 text-granate' },
  PRESUPUESTADA: { texto: 'Presupuestada', clase: 'border-coral/30 bg-coral/10 text-coral' },
  PAGADA: { texto: 'Pagada', clase: 'border-rosa bg-rosa/35 text-granate' },
  FINALIZADA: { texto: 'Finalizada', clase: 'border-rosa bg-rosa/35 text-granate' },
  RECHAZADA: { texto: 'Rechazada', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
  CANCELADA: { texto: 'Cancelada', clase: 'border-rojizo/25 bg-rojizo/10 text-rojizo' },
}

const idActual = Number(localStorage.getItem('idUsuario'))
const rolActual = localStorage.getItem('rol')

const esArtistaDeLaSolicitud = computed(() => {
  const idArtista = solicitud.value?.artista?.idUsuario
  if (idArtista && idActual) return idArtista === idActual
  return rolActual === 'ARTISTA'
})

const contraparte = computed(() =>
  esArtistaDeLaSolicitud.value ? solicitud.value?.cliente : solicitud.value?.artista,
)
const tituloContraparte = computed(() =>
  esArtistaDeLaSolicitud.value ? 'Cliente' : 'Artista',
)
const nombreContraparte = computed(
  () => contraparte.value?.nombreDisplay || contraparte.value?.nombreUsuario || 'No disponible',
)
const usuarioContraparte = computed(() => contraparte.value?.nombreUsuario || '')
const fotoContraparte = computed(
  () => contraparte.value?.urlFotoPerfilMiniatura || contraparte.value?.urlFotoPerfil || '',
)
const inicialContraparte = computed(() => (nombreContraparte.value || '?').charAt(0).toUpperCase())
const estadoInfo = computed(
  () => ESTADOS[solicitud.value?.estado] || { texto: solicitud.value?.estado || 'Sin estado', clase: 'border-rosa bg-rosa/20 text-cafe' },
)

function formatearFecha(valor) {
  if (!valor) return ''
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(fecha)
}

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

function volver() {
  router.push({ name: 'notificaciones' })
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
      ← Volver a notificaciones
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
          Ir a notificaciones
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
            {{ formatearFecha(solicitud.fechaCreacion) }}
          </p>
        </div>
        <span
          class="inline-flex self-start rounded-full border px-3 py-1.5 text-xs font-semibold"
          :class="estadoInfo.clase"
        >
          {{ estadoInfo.texto }}
        </span>
      </header>

      <div class="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div class="space-y-6">
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

            <div class="mt-4 space-y-4">
              <article
                v-for="(item, indice) in solicitud.items"
                :key="item.idItemSolicitud"
                class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
              >
                <header class="flex flex-wrap items-start justify-between gap-3 border-b border-rosa/40 pb-4">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                      Dibujo {{ indice + 1 }}
                    </p>
                    <h3 class="mt-1 text-lg font-semibold text-granate">{{ item.nombre }}</h3>
                  </div>
                  <span
                    v-if="solicitud.prioridadDefinidaPorCliente && item.ordenPrioridad"
                    class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
                  >
                    Prioridad {{ item.ordenPrioridad }}
                  </span>
                </header>

                <div class="mt-4 space-y-5">
                  <div
                    v-for="grupo in gruposDeRespuestas(item)"
                    :key="grupo.clave"
                    class="border-b border-rosa/30 pb-5 last:border-b-0 last:pb-0"
                  >
                    <p class="text-sm font-semibold text-cafe">{{ grupo.enunciado }}</p>

                    <div v-if="grupo.codigoTipo === 'SUBIR_IMAGEN'" class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                      <a
                        v-for="(url, urlIndice) in grupo.valores"
                        :key="`${url}-${urlIndice}`"
                        :href="url"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="group relative block aspect-square overflow-hidden rounded-xl bg-rosa/20 ring-1 ring-rosa/40"
                      >
                        <img
                          :src="url"
                          :alt="`Imagen de referencia ${urlIndice + 1} de ${item.nombre}`"
                          class="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                          loading="lazy"
                        />
                      </a>
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

                  <p v-if="!gruposDeRespuestas(item).length" class="text-sm text-cafe/55">
                    Este dibujo no tiene respuestas registradas.
                  </p>
                </div>
              </article>
            </div>
          </section>
        </div>

        <aside class="space-y-4">
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
            <p class="mt-2 text-xs leading-relaxed text-cafe/60">
              Rechazar y presupuestar estarán disponibles próximamente.
            </p>
            <div class="mt-4 grid gap-2">
              <button
                type="button"
                disabled
                class="inline-flex min-h-10 cursor-not-allowed items-center justify-center rounded-full border border-rosa bg-rosa/15 px-4 text-sm font-semibold text-cafe/40"
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
  </div>
</template>
