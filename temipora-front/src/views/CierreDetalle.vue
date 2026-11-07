<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TarjetaServicioAlternativa from '../components/TarjetaServicioAlternativa.vue'
import {
  detalleCierrePedido,
  detalleCierreSolicitud,
  elegirAlternativa,
} from '../servicios/sustituciones'
import { formatearPrecio } from '../servicios/moneda'
import { useNotificaciones } from '../composables/useNotificaciones'
import { useAccesoEnlace } from '../composables/useAccesoEnlace'

const route = useRoute()
const router = useRouter()
const { error: notificarError } = useNotificaciones()
const { consumirSiCorresponde, avisoAcceso, errorAcceso } = useAccesoEnlace()

const cargando = ref(true)
const error = ref('')
const detalle = ref(null)
const eligiendo = ref(null)
const accesoNombre = ref(avisoAcceso())
const mensajeAcceso = ref(errorAcceso())
const requiereLogin = ref(false)

const esPedido = computed(() => route.name === 'pedido-resultado')
const idSolicitud = computed(() => Number(route.params.idSolicitud))
const idPedido = computed(() => Number(route.params.idPedido))
const opciones = computed(() => detalle.value?.opciones || [])
const servicio = computed(() => detalle.value?.servicio || null)
const listado = computed(() =>
  esPedido.value ? { name: 'pedidos' } : { name: 'solicitudes' },
)
const registro = computed(() =>
  esPedido.value
    ? { name: 'pedido-detalle', params: { idPedido: idPedido.value } }
    : { name: 'solicitud-detalle', params: { idSolicitud: idSolicitud.value } },
)
const etiquetaRegistro = computed(() =>
  esPedido.value ? 'Ver pedido' : 'Ver solicitud',
)
const precio = computed(() => {
  const s = servicio.value
  if (!s) return ''
  return formatearPrecio(
    s.precioBaseMostrado ?? s.precioBase,
    s.monedaMostrada ?? s.moneda,
  )
})
const fechaTexto = computed(() => {
  const valor = detalle.value?.fecha
  if (!valor) return ''
  try {
    const fecha = new Date(valor)
    return `${fecha.toLocaleDateString('es-AR')} · ${fecha.toLocaleTimeString('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
    })}`
  } catch {
    return ''
  }
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    detalle.value = esPedido.value
      ? await detalleCierrePedido(idPedido.value)
      : await detalleCierreSolicitud(idSolicitud.value)
  } catch (e) {
    if (e.response?.status === 401) {
      requiereLogin.value = true
      error.value = 'Iniciá sesión para ver el resultado de este cierre.'
    } else {
      error.value =
        e.response?.data?.message ||
        'No pudimos cargar el resultado de este cierre.'
    }
  } finally {
    cargando.value = false
  }
}

function irALogin() {
  router.push({
    name: 'login',
    query: { redirect: route.fullPath },
  })
}

function irASolicitar(id) {
  router.push({
    name: 'solicitud-servicio',
    params: { id },
    query: detalle.value?.idSustitucion
      ? { sustitucion: detalle.value.idSustitucion }
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
    if (detalle.value?.idSustitucion) {
      await elegirAlternativa(detalle.value.idSustitucion, opcion.idServicio)
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

function cerrarSesionAcceso() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
  router.push({ name: 'login' })
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
  <div class="mx-auto max-w-screen-xl px-4 py-10 sm:px-8 sm:py-14">
    <router-link
      :to="listado"
      class="text-sm font-semibold text-coral transition hover:text-rojizo"
    >
      ← Volver a mis {{ esPedido ? 'pedidos' : 'solicitudes' }}
    </router-link>

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

    <div v-if="cargando" class="mt-10 space-y-6">
      <div class="h-40 animate-pulse rounded-[28px] bg-rosa/25"></div>
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
        <div v-for="marcador in 3" :key="marcador" class="h-80 animate-pulse rounded-[26px] bg-rosa/20"></div>
      </div>
    </div>

    <div
      v-else-if="error"
      class="mt-10 rounded-3xl border border-rojizo/30 bg-superficie px-6 py-12 text-center"
    >
      <p class="text-sm text-rojizo">{{ error }}</p>
      <button
        v-if="requiereLogin"
        type="button"
        class="mt-4 inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white transition hover:bg-rojizo"
        @click="irALogin"
      >
        Iniciar sesión
      </button>
      <button
        v-else
        type="button"
        class="mt-4 inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <header class="mt-4 max-w-3xl">
        <span class="inline-block -rotate-2 rounded-full bg-mantequilla px-4 py-1.5 text-sm font-bold text-tinta shadow-sm">
          El proceso terminó
        </span>
        <h1 class="mt-4 font-display text-4xl font-extrabold tracking-[-0.03em] text-granate sm:text-5xl">
          {{ detalle.titulo }}
        </h1>
        <p class="mt-3 text-lg leading-relaxed text-cafe/75">
          {{ detalle.detalle }}
        </p>
      </header>

      <section
        v-if="servicio"
        class="mt-8 flex flex-col gap-4 rounded-[28px] border border-rosa/40 bg-superficie p-5 shadow-[0_10px_28px_rgba(74,29,46,0.08)] sm:flex-row sm:items-center sm:p-6"
      >
        <img
          v-if="servicio.imagenPortada"
          :src="servicio.imagenPortada"
          :alt="servicio.nombre"
          class="h-24 w-full shrink-0 rounded-2xl object-cover sm:h-20 sm:w-28"
        />
        <div class="min-w-0 flex-1">
          <p class="text-[11px] font-bold uppercase tracking-[0.12em] text-cafe/55">
            {{ esPedido ? 'Pedido' : 'Solicitud' }} #{{ esPedido ? idPedido : idSolicitud }}
          </p>
          <p class="mt-1 font-display text-lg font-bold text-granate">
            {{ servicio.nombre }}
            <span v-if="precio" class="ml-1 text-sm font-semibold text-cafe/60">· {{ precio }}</span>
          </p>
          <p v-if="servicio.nombreArtista" class="text-xs font-semibold text-cafe/60">
            @{{ servicio.nombreArtista }}
          </p>
          <p v-if="fechaTexto" class="mt-1 text-xs text-cafe/55">{{ fechaTexto }}</p>
        </div>
        <div class="flex shrink-0 flex-col gap-2 sm:w-48">
          <router-link
            :to="registro"
            class="inline-flex min-h-11 items-center justify-center rounded-full bg-coral px-4 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          >
            {{ etiquetaRegistro }}
          </router-link>
          <router-link
            to="/servicios"
            class="inline-flex min-h-11 items-center justify-center rounded-full border border-borde/60 px-4 text-sm font-semibold text-cafe/75 transition hover:bg-rosa/20"
          >
            Explorar servicios
          </router-link>
        </div>
      </section>

      <section v-if="opciones.length" class="mt-12">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="text-xs font-bold uppercase tracking-[0.18em] text-coral">
              Para no perder el impulso
            </p>
            <h2 class="mt-2 font-display text-3xl font-extrabold text-granate sm:text-4xl">
              Otros artistas con cupo
            </h2>
            <p class="mt-1 text-sm text-cafe/60">
              Parecidos a lo que buscabas, con lugar para empezar ahora.
            </p>
          </div>
        </div>

        <div class="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
          <TarjetaServicioAlternativa
            v-for="opcion in opciones"
            :key="opcion.idServicio"
            :opcion="opcion"
            :eligiendo="eligiendo === opcion.idServicio"
            @solicitar="solicitar"
          />
        </div>
      </section>

      <section
        v-else
        class="mt-12 rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center"
      >
        <h2 class="font-display text-2xl font-extrabold text-granate">
          {{ detalle.procesoHabilitado ? 'No encontramos alternativas' : 'Las sugerencias están pausadas' }}
        </h2>
        <p class="mx-auto mt-2 max-w-lg text-sm leading-relaxed text-cafe/70">
          {{ detalle.procesoHabilitado
            ? 'No hay artistas con cupo y características parecidas en este momento. Podés explorar el catálogo completo.'
            : 'El equipo desactivó las sugerencias automáticas por ahora. Podés explorar el catálogo completo.' }}
        </p>
        <router-link
          to="/servicios"
          class="mt-5 inline-flex min-h-11 items-center rounded-full bg-coral px-6 text-sm font-bold text-white transition hover:bg-rojizo"
        >
          Explorar servicios
        </router-link>
      </section>
    </template>
  </div>
</template>
