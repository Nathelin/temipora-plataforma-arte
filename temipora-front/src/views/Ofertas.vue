<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import { useSesion } from '../composables/useSesion'
import { useAccesoEnlace } from '../composables/useAccesoEnlace'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const { usuarioActual, estaAutenticado } = useSesion()
const { consumirSiCorresponde, avisoAcceso, errorAcceso } = useAccesoEnlace()

const ofertas = ref([])
const campana = ref(null)
const cargando = ref(true)
const error = ref('')
const autenticado = ref(estaAutenticado())
const cuentaActual = ref(usuarioActual())
const accesoNombre = ref(avisoAcceso())
const mensajeAcceso = ref(errorAcceso())

const token = computed(() => route.params.token || null)
const esCampana = computed(() => !!token.value)
const ofertaDestacada = computed(() => String(route.query.oferta || ''))

const cuentaOferta = computed(() => campana.value?.nombreUsuario || '')

const mismaCuenta = computed(() => {
  if (!cuentaOferta.value) return true
  return !!cuentaActual.value
    && cuentaOferta.value.toLowerCase() === cuentaActual.value.toLowerCase()
})

const vigente = computed(() => {
  if (esCampana.value) return !!campana.value?.vigente
  return ofertas.value.some((oferta) => oferta.vigente)
})

const vencimiento = computed(() => {
  const fechas = ofertas.value
    .map((oferta) => oferta.fechaVencimiento)
    .filter(Boolean)
    .map((fecha) => new Date(fecha))
  if (!fechas.length) return null
  return new Date(Math.max(...fechas.map((fecha) => fecha.getTime())))
})

function descuento(oferta) {
  const valor = Number(oferta?.porcentajeDescuento)
  return Number.isFinite(valor) && valor > 0 ? valor : 0
}

function precioDesde(oferta) {
  if (oferta?.precioBase == null) return ''
  return formatearPrecio(oferta.precioBase, oferta.moneda || 'ARS')
}

function precioConDescuento(oferta) {
  if (oferta?.precioConDescuentoEstimado == null) return ''
  return formatearPrecio(oferta.precioConDescuentoEstimado, oferta.moneda || 'ARS')
}

function fechaVencimiento(oferta) {
  if (!oferta?.fechaVencimiento) return ''
  return new Date(oferta.fechaVencimiento).toLocaleDateString('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function inicial(oferta) {
  const nombre = oferta?.nombreArtista || oferta?.nombreServicio || '?'
  return String(nombre).charAt(0).toUpperCase()
}

function cerrarSesionAcceso() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  router.push({ name: 'login' })
}

function irALogin() {
  const query = { ...route.query }
  delete query.acceso
  const redirect = router.resolve({
    name: route.name,
    params: route.params,
    query,
  }).href
  router.push({ name: 'login', query: { redirect } })
}

function solicitar(oferta) {
  if (!autenticado.value) {
    irALogin()
    return
  }
  if (!mismaCuenta.value || !oferta?.vigente) return
  router.push({
    name: 'solicitud-servicio',
    params: { id: oferta.idServicio },
    query: { oferta: oferta.idOferta },
  })
}

function verServicio(oferta) {
  router.push({
    name: 'servicio-detalle',
    params: { id: oferta.idServicio },
    query: oferta.idOferta ? { oferta: oferta.idOferta } : {},
  })
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    if (esCampana.value) {
      const res = await api.get(`/ofertas/campana/${token.value}`)
      campana.value = res.data || null
      ofertas.value = res.data?.ofertas || []
    } else {
      const res = await api.get('/ofertas')
      ofertas.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    error.value =
      e.response?.data?.message ||
      'No pudimos cargar las ofertas. Probá de nuevo en un rato.'
  } finally {
    cargando.value = false
  }
}

onMounted(async () => {
  const acceso = await consumirSiCorresponde()
  if (acceso.habiaEnlace) {
    return
  }
  await cargar()
})
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8 sm:py-10">
    <PageHeader
      eyebrow="Ofertas"
      titulo="Propuestas para retomar"
      descripcion="Elegimos artistas que pueden interesarte y les aplicamos un descuento de reactivación para que vuelvas a crear. Lo financia Temipora y se aplica sobre el presupuesto final."
    >
      <template #acciones>
        <router-link
          to="/servicios"
          class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
        >
          Seguir explorando →
        </router-link>
      </template>
    </PageHeader>

    <div
      v-if="accesoNombre"
      class="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-coral/25 bg-coral/5 px-5 py-4"
    >
      <p class="text-sm leading-relaxed text-granate">
        Entraste con el enlace de <strong>@{{ accesoNombre }}</strong>.
      </p>
      <button
        type="button"
        class="inline-flex min-h-10 items-center rounded-full border border-coral/40 px-5 text-sm font-bold text-coral transition hover:bg-coral/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
        @click="cerrarSesionAcceso"
      >
        Cerrar sesión
      </button>
    </div>

    <p
      v-if="mensajeAcceso"
      class="mt-6 rounded-2xl border border-rojizo/20 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
    >
      {{ mensajeAcceso }}
    </p>

    <div
      v-if="cuentaOferta && !mismaCuenta"
      class="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-coral/25 bg-coral/5 px-5 py-4"
    >
      <p class="text-sm leading-relaxed text-granate">
        Estas ofertas son para <strong>@{{ cuentaOferta }}</strong>.
        <span v-if="cuentaActual">Estás con <strong>@{{ cuentaActual }}</strong>.</span>
        Iniciá sesión con la cuenta correcta para solicitar.
      </p>
      <button
        type="button"
        class="inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
        @click="irALogin"
      >
        {{ autenticado ? 'Iniciar sesión con otra cuenta' : 'Iniciar sesión' }}
      </button>
    </div>

    <div v-if="cargando" class="flex flex-col items-center justify-center py-20 text-cafe/60">
      <span class="h-8 w-8 animate-spin rounded-full border-2 border-borde border-t-coral" aria-hidden="true"></span>
      <p class="mt-4 text-sm">Cargando ofertas...</p>
    </div>

    <p v-else-if="error" class="mt-10 rounded-3xl border border-rojizo/20 bg-rojizo/5 px-6 py-12 text-center text-rojizo">
      {{ error }}
    </p>

    <template v-else>
      <div
        v-if="autenticado && vencimiento"
        class="mt-6 inline-flex items-center gap-2 rounded-full bg-mantequilla px-4 py-2 text-xs font-bold text-tinta"
      >
        <span aria-hidden="true">✦</span>
        Ofertas válidas hasta el {{ vencimiento.toLocaleDateString('es-AR', { day: '2-digit', month: 'long' }) }}
      </div>

      <div
        v-if="!autenticado && !cuentaOferta"
        class="mt-8 flex flex-wrap items-center justify-between gap-4 rounded-[24px] border border-coral/25 bg-coral/5 px-5 py-4"
      >
        <p class="text-sm leading-relaxed text-granate">
          Iniciá sesión para aprovechar el descuento y enviar tu solicitud.
        </p>
        <button
          type="button"
          class="inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          @click="irALogin"
        >
          Iniciar sesión
        </button>
      </div>

      <div
        v-if="esCampana && !vigente"
        class="mt-10 rounded-[28px] border border-dashed border-borde bg-superficie px-6 py-14 text-center"
      >
        <h2 class="font-display text-2xl font-extrabold text-granate">Esta campaña ya venció</h2>
        <p class="mx-auto mt-2 max-w-lg text-sm leading-relaxed text-cafe/70">
          Las ofertas tenían un plazo limitado. Podés seguir explorando servicios y artistas disponibles.
        </p>
        <router-link
          to="/servicios"
          class="mt-5 inline-flex min-h-11 items-center rounded-full bg-coral px-6 text-sm font-bold text-white transition hover:bg-rojizo"
        >
          Ver servicios
        </router-link>
      </div>

      <div
        v-else-if="!ofertas.length"
        class="mt-10 rounded-[28px] border border-dashed border-borde bg-superficie px-6 py-14 text-center"
      >
        <h2 class="font-display text-2xl font-extrabold text-granate">No tenés ofertas activas</h2>
        <p class="mx-auto mt-2 max-w-lg text-sm leading-relaxed text-cafe/70">
          Cuando pase un tiempo sin compras o sin entrar, vamos a prepararte propuestas con descuento.
        </p>
        <router-link
          to="/servicios"
          class="mt-5 inline-flex min-h-11 items-center rounded-full bg-coral px-6 text-sm font-bold text-white transition hover:bg-rojizo"
        >
          Explorar artistas
        </router-link>
      </div>

      <div v-else class="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="oferta in ofertas"
          :key="oferta.idOferta"
          class="group flex h-full flex-col overflow-hidden rounded-[22px] border border-borde/60 bg-superficie shadow-[0_10px_28px_rgba(74,29,46,0.08)] transition duration-300 hover:-translate-y-1 hover:border-coral/40"
          :class="{ 'ring-2 ring-coral/60': String(oferta.idOferta) === ofertaDestacada }"
        >
          <div class="relative aspect-video overflow-hidden bg-rosa/30">
            <img
              v-if="oferta.imagen"
              :src="oferta.imagen"
              :alt="`Trabajo de ${oferta.nombreArtista || oferta.nombreServicio}`"
              class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
              loading="lazy"
            />
            <div
              v-else
              class="flex h-full w-full items-center justify-center font-display text-5xl font-extrabold text-granate/25"
            >
              {{ inicial(oferta) }}
            </div>
            <span
              v-if="descuento(oferta)"
              class="absolute left-3 top-3 rounded-full bg-coral px-2.5 py-1 text-[11px] font-extrabold text-white shadow-sm"
            >
              -{{ descuento(oferta) }}%
            </span>
            <span
              v-if="!oferta.vigente"
              class="absolute right-3 top-3 rounded-full bg-granate/80 px-2.5 py-1 text-[11px] font-bold text-blanco"
            >
              Vencida
            </span>
          </div>

          <div class="flex flex-1 flex-col p-4">
            <h3 class="line-clamp-1 font-display text-base font-bold leading-snug text-granate">
              {{ oferta.nombreServicio }}
            </h3>

            <div class="mt-2 flex items-center gap-2">
              <img
                v-if="oferta.urlFotoArtista"
                :src="oferta.urlFotoArtista"
                alt=""
                class="h-6 w-6 shrink-0 rounded-full object-cover ring-1 ring-borde/50"
              />
              <span
                v-else
                class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-rosa/40 text-[11px] font-bold text-granate ring-1 ring-borde/50"
                aria-hidden="true"
              >
                {{ inicial(oferta) }}
              </span>
              <span class="line-clamp-1 text-xs font-semibold text-cafe/60">@{{ oferta.nombreArtista }}</span>
            </div>

            <div class="mt-3 flex flex-wrap items-baseline gap-x-2 gap-y-1 border-t border-borde/40 pt-3">
              <template v-if="precioConDescuento(oferta)">
                <span class="font-display text-lg font-extrabold leading-none text-coral">
                  {{ precioConDescuento(oferta) }}
                </span>
                <span
                  class="text-[10px] font-semibold text-cafe/60 line-through"
                  :title="`Precio base: ${precioDesde(oferta)}`"
                >
                  {{ precioDesde(oferta) }}
                </span>
              </template>
              <span
                v-else-if="precioDesde(oferta)"
                class="font-display text-lg font-extrabold leading-none text-coral"
              >
                Desde {{ precioDesde(oferta) }}
              </span>
              <span v-if="oferta.fechaVencimiento" class="ml-auto text-[10px] font-medium text-cafe/55">
                Hasta el {{ fechaVencimiento(oferta) }}
              </span>
            </div>

            <p v-if="oferta.motivo" class="mt-1 line-clamp-1 text-[11px] text-cafe/55">
              {{ oferta.motivo }}
            </p>

            <div class="mt-auto flex flex-wrap gap-2 pt-3">
              <button
                type="button"
                class="inline-flex min-h-9 flex-1 items-center justify-center rounded-full bg-coral px-3 text-xs font-bold text-white transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="!oferta.vigente || (autenticado && !mismaCuenta)"
                @click="solicitar(oferta)"
              >
                {{ autenticado ? 'Solicitar con descuento' : 'Iniciar sesión' }}
              </button>
              <button
                type="button"
                class="inline-flex min-h-9 items-center justify-center rounded-full border border-coral/40 px-3 text-xs font-bold text-coral transition hover:bg-coral/5"
                @click="verServicio(oferta)"
              >
                Ver servicio
              </button>
            </div>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>
