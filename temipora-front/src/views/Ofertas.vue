<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'

const route = useRoute()
const router = useRouter()

const ofertas = ref([])
const campana = ref(null)
const cargando = ref(true)
const error = ref('')
const autenticado = ref(!!localStorage.getItem('token'))

const token = computed(() => route.params.token || null)
const esCampana = computed(() => !!token.value)

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

function irALogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function solicitar(oferta) {
  if (!autenticado.value) {
    irALogin()
    return
  }
  if (!oferta?.vigente) return
  router.push({
    name: 'solicitud-servicio',
    params: { id: oferta.idServicio },
    query: { oferta: oferta.idOferta },
  })
}

function verServicio(oferta) {
  router.push({ name: 'servicio-detalle', params: { id: oferta.idServicio } })
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

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-10 sm:px-8 sm:py-14">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <span class="inline-block -rotate-2 rounded-full bg-mantequilla px-4 py-1.5 text-sm font-bold text-tinta shadow-sm">
          Ofertas
        </span>
        <h1 class="mt-5 font-display text-4xl font-extrabold tracking-[-0.03em] text-granate sm:text-5xl">
          Propuestas para retomar
        </h1>
        <p class="mt-3 max-w-2xl text-lg leading-relaxed text-cafe/75">
          Elegimos artistas que pueden interesarte y les aplicamos un descuento para que vuelvas a crear.
          El descuento lo financia Temipora y se aplica sobre el presupuesto final.
        </p>
      </div>
      <router-link
        to="/servicios"
        class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
      >
        Seguir explorando →
      </router-link>
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
        v-if="!autenticado"
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
          class="flex h-full flex-col overflow-hidden rounded-[28px] border border-borde/60 bg-superficie shadow-[0_12px_30px_rgba(74,29,46,0.08)]"
        >
          <div class="relative aspect-[4/3] overflow-hidden bg-rosa/30">
            <img
              v-if="oferta.imagen"
              :src="oferta.imagen"
              :alt="`Trabajo de ${oferta.nombreArtista || oferta.nombreServicio}`"
              class="h-full w-full object-cover"
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
              class="absolute left-3 top-3 rounded-full bg-coral px-3 py-1 text-xs font-extrabold text-white shadow-sm"
            >
              -{{ descuento(oferta) }}%
            </span>
            <span
              v-if="!oferta.vigente"
              class="absolute right-3 top-3 rounded-full bg-granate/80 px-3 py-1 text-xs font-bold text-blanco"
            >
              Vencida
            </span>
          </div>

          <div class="flex flex-1 flex-col gap-3 p-5">
            <div>
              <p class="font-display text-xl font-extrabold leading-tight text-granate">
                {{ oferta.nombreServicio }}
              </p>
              <p class="mt-1 text-sm text-cafe/60">@{{ oferta.nombreArtista }}</p>
            </div>

            <p v-if="oferta.descripcionServicio" class="line-clamp-2 text-sm leading-relaxed text-cafe/70">
              {{ oferta.descripcionServicio }}
            </p>

            <div class="flex flex-wrap items-center gap-2">
              <span
                v-if="descuento(oferta)"
                class="rounded-full bg-mantequilla px-2.5 py-1 text-xs font-bold text-tinta"
              >
                {{ descuento(oferta) }}% en el presupuesto final
              </span>
              <span
                v-if="oferta.motivo"
                class="rounded-full bg-rosa/30 px-2.5 py-1 text-xs font-semibold text-granate"
              >
                {{ oferta.motivo }}
              </span>
            </div>

            <div class="mt-auto space-y-3">
              <div class="flex flex-wrap items-center justify-between gap-2 text-sm">
                <span v-if="precioDesde(oferta)" class="font-bold text-granate">
                  Desde {{ precioDesde(oferta) }}
                </span>
                <span v-if="oferta.fechaVencimiento" class="text-xs text-cafe/55">
                  Hasta el {{ fechaVencimiento(oferta) }}
                </span>
              </div>

              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  class="inline-flex min-h-11 flex-1 items-center justify-center rounded-full bg-coral px-4 text-sm font-bold text-white transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="!oferta.vigente"
                  @click="solicitar(oferta)"
                >
                  {{ autenticado ? 'Solicitar con descuento' : 'Iniciar sesión' }}
                </button>
                <button
                  type="button"
                  class="inline-flex min-h-11 items-center justify-center rounded-full border border-coral/40 px-4 text-sm font-bold text-coral transition hover:bg-coral/5"
                  @click="verServicio(oferta)"
                >
                  Ver servicio
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>
