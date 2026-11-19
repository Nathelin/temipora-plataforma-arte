<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import TarjetaServicioAlternativa from '../components/TarjetaServicioAlternativa.vue'
import {
  buscarAlternativas,
  datosMotivo,
  elegirAlternativa,
} from '../servicios/sustituciones'
import { formatearPrecio } from '../servicios/moneda'
import { useNotificaciones } from '../composables/useNotificaciones'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()

const cargando = ref(true)
const error = ref('')
const sustitucion = ref(null)
const origen = ref(null)
const eligiendo = ref(null)
const favoritos = ref(new Set())
const actualizandoFavorito = ref(new Set())

const idServicio = computed(() => Number(route.params.id))
const opciones = computed(() => sustitucion.value?.opciones || [])
const motivo = computed(() =>
  sustitucion.value?.motivo
    ? datosMotivo(sustitucion.value.motivo)
    : { titulo: 'Tiene cupo disponible', detalle: 'Podés solicitar el servicio directamente.' },
)
const tieneCupo = computed(() => !sustitucion.value?.motivo)
const puedeEsperar = computed(() => !!sustitucion.value?.puedeQuedarEnCola)
const esSoloEspera = computed(() => sustitucion.value?.motivo === 'SOLO_ESPERA')

const nombreOrigen = computed(() => origen.value?.nombre || 'este servicio')
const artistaOrigen = computed(() => origen.value?.nombreArtista || '')
const precioOrigen = computed(() => {
  const s = origen.value
  if (!s) return ''
  return formatearPrecio(
    s.precioBaseMostrado ?? s.precioBase,
    s.monedaMostrada ?? s.moneda,
  )
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [alternativas, publica] = await Promise.all([
      buscarAlternativas(
        idServicio.value,
        typeof route.query.disparador === 'string'
          ? route.query.disparador
          : 'DETALLE',
      ),
      api
        .get(`/servicios/${idServicio.value}/solicitud-publica`)
        .catch(() => null),
    ])
    sustitucion.value = alternativas
    origen.value = publica?.data?.servicio || null
  } catch (e) {
    error.value =
      e.response?.data?.message ||
      'No pudimos buscar artistas con cupo para este servicio.'
  } finally {
    cargando.value = false
  }
}

function irASolicitar(id) {
  router.push({
    name: 'solicitud-servicio',
    params: { id },
    query: sustitucion.value?.idSustitucion
      ? { sustitucion: sustitucion.value.idSustitucion }
      : {},
  })
}

async function solicitar(opcion) {
  if (!opcion?.disponible || eligiendo.value) return
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login' })
    return
  }
  eligiendo.value = opcion.idServicio
  try {
    if (sustitucion.value?.idSustitucion) {
      await elegirAlternativa(
        sustitucion.value.idSustitucion,
        opcion.idServicio,
      )
    }
    irASolicitar(opcion.idServicio)
  } catch (e) {
    if (e.response?.status === 409) {
      notificarError(
        'Esa alternativa dejó de tener cupo. Actualizamos las opciones.',
      )
      await cargar()
    } else {
      notificarError(
        e.response?.data?.message || 'No se pudo abrir la solicitud.',
      )
    }
  } finally {
    eligiendo.value = null
  }
}

async function solicitarEnCola() {
  if (eligiendo.value) return
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login' })
    return
  }
  eligiendo.value = idServicio.value
  try {
    irASolicitar(idServicio.value)
  } finally {
    eligiendo.value = null
  }
}

async function cargarFavoritos() {
  if (!localStorage.getItem('token')) return
  try {
    const res = await api.get('/favoritos/servicios/ids')
    favoritos.value = new Set(res.data || [])
  } catch {
    favoritos.value = new Set()
  }
}

async function alternarFavorito(servicio) {
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const id = servicio.idServicio
  if (actualizandoFavorito.value.has(id)) return

  actualizandoFavorito.value = new Set(actualizandoFavorito.value).add(id)
  try {
    if (favoritos.value.has(id)) {
      await api.delete(`/favoritos/servicios/${id}`)
      const siguientes = new Set(favoritos.value)
      siguientes.delete(id)
      favoritos.value = siguientes
      exito(`Quitaste "${servicio.nombre}" de tus favoritos.`)
    } else {
      await api.post(`/favoritos/servicios/${id}`)
      favoritos.value = new Set(favoritos.value).add(id)
      exito(`Agregaste "${servicio.nombre}" a tus favoritos.`)
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    notificarError(
      e.response?.data?.message || 'No se pudieron actualizar tus favoritos.',
    )
  } finally {
    const siguientes = new Set(actualizandoFavorito.value)
    siguientes.delete(id)
    actualizandoFavorito.value = siguientes
  }
}

onMounted(async () => {
  void cargarFavoritos()
  await cargar()
})
</script>

<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-10 sm:px-8 sm:py-14">
    <router-link
      :to="{ name: 'servicio-detalle', params: { id: idServicio } }"
      class="text-sm font-semibold text-coral transition hover:text-rojizo"
    >
      ← Volver al servicio
    </router-link>

    <PageHeader
      class="mt-4"
      eyebrow="Alternativas con cupo"
      titulo="Artistas con cupo para este pedido"
    >
      <template v-if="origen" #descripcion>
        Mismo estilo y rango de precio que «{{ nombreOrigen }}»
        <span v-if="artistaOrigen">de @{{ artistaOrigen }}</span>, con lugar para
        empezar ahora.
      </template>
    </PageHeader>

    <div v-if="cargando" class="mt-10 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
      <div v-for="marcador in 6" :key="marcador" class="h-96 animate-pulse rounded-[26px] bg-rosa/25"></div>
    </div>

    <div
      v-else-if="error"
      class="mt-10 rounded-3xl border border-rojizo/30 bg-superficie px-6 py-12 text-center"
    >
      <p class="text-sm text-rojizo">{{ error }}</p>
      <button
        type="button"
        class="mt-4 inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <section
        v-if="origen"
        class="mt-8 flex flex-col gap-4 rounded-[28px] border border-rosa/40 bg-superficie p-5 shadow-[0_10px_28px_rgba(74,29,46,0.08)] sm:flex-row sm:items-center sm:p-6"
      >
        <img
          v-if="origen.imagenPortada"
          :src="origen.imagenPortada"
          :alt="origen.nombre"
          class="h-24 w-full shrink-0 rounded-2xl object-cover sm:h-20 sm:w-28"
        />
        <div class="min-w-0 flex-1">
          <p class="text-[11px] font-bold uppercase tracking-[0.12em] text-cafe/55">
            El servicio que querías
          </p>
          <p class="mt-1 font-display text-lg font-bold text-granate">
            {{ nombreOrigen }}
            <span v-if="precioOrigen" class="ml-1 text-sm font-semibold text-cafe/60">· {{ precioOrigen }}</span>
          </p>
          <p v-if="artistaOrigen" class="text-xs font-semibold text-cafe/60">
            @{{ artistaOrigen }}
          </p>
          <p class="mt-2 text-sm leading-relaxed text-cafe/80">
            <strong class="font-semibold text-granate">{{ motivo.titulo }}.</strong>
            {{ motivo.detalle }}
          </p>
        </div>
        <div class="flex shrink-0 flex-col gap-2 sm:w-56">
          <button
            v-if="puedeEsperar"
            type="button"
            class="inline-flex min-h-11 items-center justify-center rounded-full border-2 border-coral/50 px-4 text-sm font-bold text-granate transition hover:border-coral hover:bg-coral hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 disabled:opacity-60"
            :disabled="eligiendo === idServicio"
            @click="solicitarEnCola"
          >
            {{ esSoloEspera ? 'Lista de espera' : 'Lista de espera' }}
          </button>
          <router-link
            :to="{ name: 'servicio-detalle', params: { id: idServicio } }"
            class="inline-flex min-h-11 items-center justify-center rounded-full border border-borde/60 px-4 text-sm font-semibold text-cafe/75 transition hover:bg-rosa/20"
          >
            Ver el servicio original
          </router-link>
        </div>
      </section>

      <section v-if="tieneCupo" class="mt-8 rounded-3xl border border-borde/60 bg-superficie px-6 py-12 text-center">
        <p class="text-sm text-cafe/70">
          Este servicio tiene cupo disponible ahora mismo.
        </p>
        <button
          type="button"
          class="mt-4 inline-flex min-h-11 items-center rounded-full bg-coral px-6 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
          @click="irASolicitar(idServicio)"
        >
          Solicitar servicio
        </button>
      </section>

      <template v-else>
        <div class="mt-10 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 class="font-display text-2xl font-extrabold text-granate sm:text-3xl">
              {{ opciones.length }}
              {{ opciones.length === 1 ? 'artista disponible' : 'artistas disponibles' }}
            </h2>
            <p class="mt-1 max-w-2xl text-sm leading-relaxed text-cafe/60">
              Los ordenamos de mayor a menor afinidad con «{{ nombreOrigen }}».
              Cada tarjeta indica qué la acerca a lo que buscabas.
            </p>
          </div>
        </div>

        <div
          v-if="opciones.length"
          class="mt-5 rounded-[24px] border border-rosa/40 bg-durazno/30 px-5 py-4"
        >
          <p class="text-[11px] font-bold uppercase tracking-wide text-cafe/60">
            Cómo elegimos estas propuestas
          </p>
          <ul class="mt-2 grid gap-x-6 gap-y-1 text-sm text-cafe/80 sm:grid-cols-2">
            <li>• Estilos y etiquetas en común.</li>
            <li>• Mismo tipo de servicio y medio.</li>
            <li>• Precio dentro del rango del servicio original.</li>
            <li>• Tiempo de entrega y reputación del artista.</li>
          </ul>
          <p class="mt-2 text-xs leading-relaxed text-cafe/55">
            Solo mostramos artistas con cupo disponible ahora. El orden no es una
            valoración del artista, sino qué tan parecida es la propuesta a la que
            querías.
          </p>
        </div>

        <div v-if="opciones.length" class="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
          <TarjetaServicioAlternativa
            v-for="opcion in opciones"
            :key="opcion.idServicio"
            :opcion="opcion"
            :eligiendo="eligiendo === opcion.idServicio"
            :es-favorito="favoritos.has(opcion.idServicio)"
            :actualizando-favorito="actualizandoFavorito.has(opcion.idServicio)"
            @solicitar="solicitar"
            @alternar-favorito="alternarFavorito"
          />
        </div>

        <div v-else class="mt-6 rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center">
          <p class="text-sm text-cafe/70">
            No encontramos artistas con cupo parecidos a este servicio.
          </p>
          <div class="mt-5 flex flex-wrap justify-center gap-3">
            <router-link
              to="/servicios"
              class="inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
            >
              Explorar servicios
            </router-link>
            <button
              v-if="puedeEsperar"
              type="button"
              class="inline-flex min-h-10 items-center rounded-full border-2 border-coral/50 px-5 text-sm font-bold text-granate transition hover:border-coral hover:bg-coral hover:text-white"
              @click="solicitarEnCola"
            >
              {{ esSoloEspera ? 'Lista de espera' : 'Lista de espera' }}
            </button>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>
