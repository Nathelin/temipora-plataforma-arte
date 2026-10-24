<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { estadoServicioDe } from '../servicios/servicios'
import { datosContraparte, estadoDe, formatearFechaSolicitud } from '../servicios/solicitudes'
import { hayContenidoTerminos } from '../servicios/terminos'

const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const servicios = ref([])
const solicitudes = ref([])
const terminos = ref(null)
const formulario = ref(null)
const terminosConsultados = ref(false)

const ICONOS = {
  servicios: 'M2.25 7.125C2.25 6.504 2.754 6 3.375 6h6c.621 0 1.125.504 1.125 1.125v3.75c0 .621-.504 1.125-1.125 1.125h-6a1.125 1.125 0 0 1-1.125-1.125v-3.75ZM14.25 8.625c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-8.25ZM3.75 16.125c0-.621.504-1.125 1.125-1.125h5.25c.621 0 1.125.504 1.125 1.125v2.25c0 .621-.504 1.125-1.125 1.125h-5.25a1.125 1.125 0 0 1-1.125-1.125v-2.25Z',
  solicitudes: 'M2.25 13.5h3.86a2.25 2.25 0 0 1 2.012 1.244l.256.512a2.25 2.25 0 0 0 2.013 1.244h3.218a2.25 2.25 0 0 0 2.013-1.244l.256-.512a2.25 2.25 0 0 1 2.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 0 0-2.15-1.588H6.911a2.25 2.25 0 0 0-2.15 1.588L2.35 13.177a2.25 2.25 0 0 0-.1.661Z',
  presupuesto: 'M12 6v12m-3-2.818.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z',
  cupos: 'M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75c0 .621-.504 1.125-1.125 1.125h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z',
  terminos: 'M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z',
  formulario: 'M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25Z',
}

const perfilPublico = computed(() =>
  nombreUsuario
    ? { name: 'perfil', params: { nombreUsuario } }
    : { name: 'inicio' },
)

const serviciosPublicados = computed(() =>
  servicios.value.filter((servicio) => servicio.estado === 'PUBLICADO'),
)
const serviciosPausados = computed(() =>
  servicios.value.filter((servicio) => servicio.estado === 'PAUSADO'),
)
const solicitudesPorResponder = computed(() =>
  solicitudes.value.filter((solicitud) =>
    ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.estado)),
)
const solicitudesPresupuestadas = computed(() =>
  solicitudes.value.filter((solicitud) => solicitud.estado === 'PRESUPUESTADA'),
)
const cuposLibres = computed(() =>
  serviciosPublicados.value.reduce(
    (total, servicio) => total + (servicio.cuposDisponibles ?? servicio.cupos ?? 0),
    0,
  ),
)
const terminosListos = computed(() => hayContenidoTerminos({ form: terminos.value || {} }))
const usaFormularioSistema = computed(() => formulario.value?.esSistema === true)

const resumenTexto = computed(() => {
  if (!servicios.value.length) {
    return 'Empezá creando tu primer servicio para recibir solicitudes.'
  }
  const publicados = serviciosPublicados.value.length
  const partes = [
    `${publicados} ${publicados === 1 ? 'servicio publicado' : 'servicios publicados'}`,
  ]
  if (solicitudesPorResponder.value.length) {
    partes.push(
      `${solicitudesPorResponder.value.length} ${solicitudesPorResponder.value.length === 1 ? 'solicitud por responder' : 'solicitudes por responder'}`,
    )
  }
  return `Tenés ${partes.join(' y ')}.`
})

const metricas = computed(() => [
  {
    etiqueta: 'Servicios publicados',
    valor: serviciosPublicados.value.length,
    detalle: serviciosPausados.value.length
      ? `${serviciosPausados.value.length} pausado(s)`
      : 'Ninguno pausado',
    to: '/panel/servicios',
    icono: ICONOS.servicios,
  },
  {
    etiqueta: 'Por responder',
    valor: solicitudesPorResponder.value.length,
    detalle: 'Pendientes o en lista de espera',
    to: '/panel/solicitudes',
    icono: ICONOS.solicitudes,
  },
  {
    etiqueta: 'Presupuestadas',
    valor: solicitudesPresupuestadas.value.length,
    detalle: 'Pedidos en curso',
    to: '/panel/solicitudes',
    icono: ICONOS.presupuesto,
  },
  {
    etiqueta: 'Cupos libres',
    valor: cuposLibres.value,
    detalle: 'En tus servicios publicados',
    to: '/panel/servicios',
    icono: ICONOS.cupos,
  },
])

const atenciones = computed(() => {
  const items = []

  if (terminosConsultados.value && !terminosListos.value) {
    items.push({
      clave: 'terminos',
      titulo: 'Completá tus términos y condiciones',
      detalle: 'Los necesitás para publicar servicios y recibir solicitudes.',
      to: '/panel/terminos',
      icono: ICONOS.terminos,
      tono: 'alerta',
    })
  }

  const pendientes = solicitudesPorResponder.value.length
  if (pendientes) {
    items.push({
      clave: 'solicitudes',
      titulo: pendientes === 1
        ? 'Una solicitud espera tu respuesta'
        : `${pendientes} solicitudes esperan tu respuesta`,
      detalle: 'Revisalas para presupuestar o rechazar.',
      to: '/panel/solicitudes',
      icono: ICONOS.solicitudes,
      tono: 'alerta',
    })
  }

  const pausados = serviciosPausados.value.length
  if (pausados) {
    items.push({
      clave: 'pausados',
      titulo: pausados === 1
        ? 'Tenés un servicio pausado'
        : `Tenés ${pausados} servicios pausados`,
      detalle: 'Mientras estén pausados no reciben solicitudes nuevas.',
      to: '/panel/servicios',
      icono: ICONOS.servicios,
      tono: 'info',
    })
  }

  if (usaFormularioSistema.value) {
    items.push({
      clave: 'formulario',
      titulo: 'Estás usando el formulario del sistema',
      detalle: 'Personalizalo para pedir exactamente lo que necesitás.',
      to: '/panel/formulario-pedidos',
      icono: ICONOS.formulario,
      tono: 'info',
    })
  }

  if (!servicios.value.length) {
    items.push({
      clave: 'primer-servicio',
      titulo: 'Creá tu primer servicio',
      detalle: 'Definí precio, tiempos y cupos para empezar a recibir pedidos.',
      to: '/panel/servicios/crear',
      icono: ICONOS.servicios,
      tono: 'info',
    })
  }

  return items
})

const solicitudesRecientes = computed(() =>
  solicitudes.value
    .filter((solicitud) =>
      ['PENDIENTE', 'EN_LISTA_ESPERA', 'PRESUPUESTADA', 'PAGADA'].includes(solicitud.estado))
    .slice(0, 4),
)

const serviciosDestacados = computed(() =>
  [...servicios.value]
    .sort((a, b) => {
      if (a.estado !== b.estado) return a.estado === 'PUBLICADO' ? -1 : 1
      return (b.solicitudesActivas || 0) - (a.solicitudesActivas || 0)
    })
    .slice(0, 4),
)

function clienteDe(solicitud) {
  return datosContraparte(solicitud?.cliente)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [serviciosRes, solicitudesRes, perfilRes] = await Promise.all([
      api.get('/mis-servicios'),
      api.get('/solicitudes-servicio/recibidas'),
      nombreUsuario ? api.get(`/perfil/${nombreUsuario}`).catch(() => null) : Promise.resolve(null),
    ])

    servicios.value = serviciosRes.data || []
    solicitudes.value = solicitudesRes.data || []

    const idUsuario = perfilRes?.data?.idUsuario
    if (idUsuario) {
      const [terminosRes, formularioRes] = await Promise.all([
        api.get(`/usuarios/${idUsuario}/terminos`).catch(() => null),
        api.get(`/usuarios/${idUsuario}/formulario-pedidos`).catch(() => null),
      ])
      terminos.value = terminosRes?.data || null
      formulario.value = formularioRes?.data || null
      terminosConsultados.value = true
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.data?.message || 'No se pudo cargar el resumen.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div>
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tu espacio de trabajo</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          {{ nombreUsuario ? `Hola, @${nombreUsuario}` : 'Mi panel' }}
        </h1>
        <p class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
          {{ resumenTexto }}
        </p>
      </div>

      <div class="flex flex-wrap gap-2">
        <router-link
          to="/panel/servicios/crear"
          class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
        >
          Crear servicio
        </router-link>
        <router-link
          :to="perfilPublico"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        >
          Ver mi perfil público
        </router-link>
      </div>
    </header>

    <div v-if="cargando" class="mt-8 space-y-6" role="status" aria-live="polite">
      <span class="sr-only">Cargando resumen...</span>
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 4" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
      </div>
      <div class="h-40 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <div class="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <router-link
          v-for="metrica in metricas"
          :key="metrica.etiqueta"
          :to="metrica.to"
          class="group rounded-2xl border border-rosa/60 bg-blanco p-5 transition hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)]"
        >
          <div class="flex items-start justify-between gap-3">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">{{ metrica.etiqueta }}</p>
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-rosa/25 text-coral">
              <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" :d="metrica.icono" />
              </svg>
            </span>
          </div>
          <p class="mt-3 font-display text-3xl font-extrabold text-granate">{{ metrica.valor }}</p>
          <p class="mt-1 text-xs text-cafe/55">{{ metrica.detalle }}</p>
        </router-link>
      </div>

      <section class="mt-8 rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">Requiere tu atención</h2>
          <span
            v-if="atenciones.length"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
          >
            {{ atenciones.length }}
          </span>
        </div>

        <ul v-if="atenciones.length" class="mt-4 space-y-2">
          <li v-for="atencion in atenciones" :key="atencion.clave">
            <router-link
              :to="atencion.to"
              class="group flex items-start gap-3 rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
            >
              <span
                class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border"
                :class="atencion.tono === 'alerta'
                  ? 'border-coral/25 bg-coral/10 text-coral'
                  : 'border-mantequilla bg-mantequilla/40 text-tinta'"
              >
                <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="atencion.icono" />
                </svg>
              </span>
              <span class="min-w-0 flex-1">
                <span class="block text-sm font-semibold text-granate">{{ atencion.titulo }}</span>
                <span class="mt-0.5 block text-xs leading-relaxed text-cafe/60">{{ atencion.detalle }}</span>
              </span>
              <svg class="mt-2 h-4 w-4 shrink-0 text-cafe/35 transition group-hover:translate-x-0.5 group-hover:text-coral" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="m9 5 7 7-7 7" />
              </svg>
            </router-link>
          </li>
        </ul>

        <p v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-6 text-center text-sm text-cafe/60">
          Todo al día. No hay nada pendiente por ahora.
        </p>
      </section>

      <div class="mt-8 grid gap-6 lg:grid-cols-2">
        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-xl font-bold text-granate">Solicitudes recientes</h2>
            <router-link
              to="/panel/solicitudes"
              class="text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Ver todas
            </router-link>
          </div>

          <ul v-if="solicitudesRecientes.length" class="mt-4 space-y-2">
            <li v-for="solicitud in solicitudesRecientes" :key="solicitud.idSolicitud">
              <router-link
                :to="{ name: 'solicitud-detalle', params: { idSolicitud: solicitud.idSolicitud } }"
                class="group flex items-center gap-3 rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
              >
                <img
                  v-if="clienteDe(solicitud)?.foto"
                  :src="clienteDe(solicitud).foto"
                  alt=""
                  class="h-9 w-9 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
                />
                <span
                  v-else
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate"
                  aria-hidden="true"
                >
                  {{ clienteDe(solicitud)?.inicial || '?' }}
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block truncate text-sm font-semibold text-granate">
                    {{ clienteDe(solicitud)?.nombre || 'Usuario no disponible' }}
                  </span>
                  <span class="block truncate text-xs text-cafe/60">
                    {{ solicitud.nombreServicio || 'Servicio' }} · {{ solicitud.cantidadDibujos }}
                    {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }}
                  </span>
                </span>
                <span class="shrink-0 text-right">
                  <span
                    class="block whitespace-nowrap rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                    :class="estadoDe(solicitud.estado).clase"
                  >
                    {{ estadoDe(solicitud.estado).texto }}
                  </span>
                  <span class="mt-1 block text-[11px] text-cafe/45">
                    {{ formatearFechaSolicitud(solicitud.fechaCreacion, { corta: true }) }}
                  </span>
                </span>
              </router-link>
            </li>
          </ul>

          <div v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center">
            <p class="text-sm text-cafe/60">Todavía no recibiste solicitudes.</p>
            <router-link
              to="/panel/servicios"
              class="mt-2 inline-block text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Revisá tus servicios
            </router-link>
          </div>
        </section>

        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-display text-xl font-bold text-granate">Tus servicios</h2>
            <router-link
              to="/panel/servicios"
              class="text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Ver todos
            </router-link>
          </div>

          <ul v-if="serviciosDestacados.length" class="mt-4 space-y-2">
            <li v-for="servicio in serviciosDestacados" :key="servicio.idServicio">
              <router-link
                :to="`/panel/servicios/${servicio.idServicio}/solicitudes`"
                class="group flex items-center gap-3 rounded-xl border border-rosa/40 p-3 transition hover:border-coral/40 hover:bg-rosa/10"
              >
                <img
                  v-if="servicio.imagenPortada"
                  :src="servicio.imagenPortada"
                  alt=""
                  class="h-10 w-10 shrink-0 rounded-lg object-cover ring-1 ring-rosa/50"
                />
                <span
                  v-else
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-rosa/30 text-granate"
                  aria-hidden="true"
                >
                  <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                    <path stroke-linecap="round" stroke-linejoin="round" :d="ICONOS.servicios" />
                  </svg>
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block truncate text-sm font-semibold text-granate">{{ servicio.nombre }}</span>
                  <span class="block text-xs text-cafe/60">
                    {{ servicio.cuposDisponibles ?? servicio.cupos ?? 0 }} cupos libres ·
                    {{ servicio.solicitudesActivas || 0 }} solicitudes activas
                  </span>
                </span>
                <span
                  class="shrink-0 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                  :class="estadoServicioDe(servicio.estado).clase"
                >
                  {{ estadoServicioDe(servicio.estado).texto }}
                </span>
              </router-link>
            </li>
          </ul>

          <div v-else class="mt-4 rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center">
            <p class="text-sm text-cafe/60">Todavía no creaste servicios.</p>
            <router-link
              to="/panel/servicios/crear"
              class="mt-2 inline-block text-sm font-semibold text-coral transition hover:text-rojizo"
            >
              Crear mi primer servicio
            </router-link>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>
