<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'

const route = useRoute()
const router = useRouter()

const servicio = ref(null)
const cargando = ref(true)
const error = ref('')

const mostrarSensible = ref(false)
const indiceImagen = ref(0)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const esSensible = computed(() => !!servicio.value?.contenidoSensible)
const requiereAdvertencia = computed(() => !!servicio.value?.requiereAdvertencia)
const tiposSensibles = computed(() => {
  const tipos = servicio.value?.contenidosSensibles || []
  return tipos.map((t) => ETIQUETAS_SENSIBLE[t] || t)
})
const textoSensible = computed(() => tiposSensibles.value.join(' · ') || 'Contenido sensible')
const esDuenio = computed(() => {
  const actual = localStorage.getItem('nombreUsuario') || ''
  return actual.toLowerCase() === (servicio.value?.nombreArtista || '').toLowerCase()
})

const precioMostrado = computed(() =>
  formatearPrecio(
    servicio.value?.precioBaseMostrado ?? servicio.value?.precioBase,
    servicio.value?.monedaMostrada ?? servicio.value?.moneda,
  ),
)
const precioConvertido = computed(
  () =>
    !!servicio.value?.monedaMostrada &&
    servicio.value.monedaMostrada !== servicio.value.moneda,
)

const galeria = computed(() => {
  const s = servicio.value
  if (!s) return []
  const portada = s.imagenPortada ? [{ url: s.imagenPortada, portada: true }] : []
  const ejemplos = (s.imagenes || [])
    .filter((url) => url && url !== s.imagenPortada)
    .map((url) => ({ url, portada: false }))
  return [...portada, ...ejemplos]
})

const imagenActiva = computed(() => galeria.value[indiceImagen.value] || null)
const activaEsPortada = computed(() => !!imagenActiva.value?.portada)
const mostrarBlur = computed(
  () => requiereAdvertencia.value && activaEsPortada.value && !mostrarSensible.value,
)

function textoEntrega(s) {
  return `${s.tiempoEntregaValor} ${(s.tiempoEntregaUnidad || '').toLowerCase()}`
}

function seleccionarImagen(i) {
  indiceImagen.value = i
}

function irAlArtista() {
  router.push({ name: 'perfil', params: { nombreUsuario: servicio.value.nombreArtista } })
}

function irAServicios() {
  router.push({ name: 'servicios' })
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/servicios/${route.params.id}`)
    servicio.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el servicio.'
  } finally {
    cargando.value = false
  }
}

function manejarTecla(event) {
  if (!servicio.value || event.key !== 'Escape') return
  if (mostrarSensible.value) mostrarSensible.value = false
}

onMounted(() => {
  cargar()
  window.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => window.removeEventListener('keydown', manejarTecla))
</script>

<template>
  <div class="mx-auto px-4 sm:px-8 py-8">
    <button type="button" class="text-sm text-granate hover:text-coral transition" @click="irAServicios">
      ← Volver a servicios
    </button>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando servicio...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else-if="servicio">
      <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_22rem] gap-8 mt-4">
        <!-- Galería -->
        <div>
          <div class="relative rounded-2xl overflow-hidden bg-rosa/20 aspect-video">
            <img
              v-if="imagenActiva"
              :src="imagenActiva.url"
              :alt="servicio.nombre"
              class="w-full h-full object-cover transition duration-200"
              :class="mostrarBlur ? 'blur-[18px] scale-105' : ''"
            />
            <div
              v-if="mostrarBlur"
              class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-granate/30 text-center px-6"
            >
              <svg class="h-10 w-10 text-white drop-shadow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
              </svg>
              <p class="text-sm font-medium text-white drop-shadow">Contenido sensible</p>
              <span class="rounded-full bg-blanco/90 px-3 py-0.5 text-xs text-cafe">{{ textoSensible }}</span>
              <button
                type="button"
                class="mt-1 rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
                @click="mostrarSensible = true"
              >
                Ver de todos modos
              </button>
            </div>
            <div
              v-if="requiereAdvertencia && activaEsPortada && mostrarSensible"
              class="absolute left-3 top-3 rounded-full bg-granate/70 px-3 py-1 text-xs text-inverso-contraste"
            >
              Mostrando contenido sensible
              <button type="button" class="ml-2 underline hover:text-white" @click="mostrarSensible = false">
                Ocultar
              </button>
            </div>
          </div>

          <div v-if="galeria.length > 1" class="mt-3 grid grid-cols-5 sm:grid-cols-8 gap-2">
            <button
              v-for="(img, i) in galeria"
              :key="i"
              type="button"
              class="relative aspect-square rounded-lg overflow-hidden bg-rosa/20 border-2 transition"
              :class="indiceImagen === i ? 'border-coral' : 'border-transparent hover:border-rosa'"
              :aria-label="img.portada ? 'Ver portada' : `Ver imagen de ejemplo ${i}`"
              @click="seleccionarImagen(i)"
            >
              <img :src="img.url" alt="" class="w-full h-full object-cover" />
              <span
                v-if="img.portada"
                class="absolute bottom-0 inset-x-0 bg-granate/60 text-white text-[10px] px-1 py-0.5 text-center"
              >
                Portada
              </span>
            </button>
          </div>

          <div class="mt-6">
            <h2 class="text-xl text-granate">{{ servicio.nombre }}</h2>
            <button
              type="button"
              class="mt-1 text-sm text-cafe/60 hover:text-coral transition"
              @click="irAlArtista"
            >
              Por @{{ servicio.nombreArtista }}
            </button>
            <p class="mt-4 text-sm text-cafe leading-relaxed whitespace-pre-wrap">{{ servicio.descripcion }}</p>
          </div>
        </div>

        <!-- Datos del servicio -->
        <aside class="lg:sticky lg:top-16 h-fit rounded-2xl border border-rosa/50 bg-blanco p-6">
          <p class="text-2xl text-granate">{{ precioMostrado }}</p>
          <p class="mt-1 text-xs text-cafe/50">
            Precio base
            <span v-if="precioConvertido"> · equivalente</span>
          </p>
          <p v-if="precioConvertido" class="mt-1 text-xs text-cafe/50">
            El artista lo cargó como {{ formatearPrecio(servicio.precioBase, servicio.moneda) }}
          </p>

          <dl class="mt-6 space-y-4 text-sm border-t border-rosa/60 pt-6">
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Tipo</dt>
              <dd class="text-cafe">{{ servicio.nombreTipoServicio }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Medio</dt>
              <dd class="text-cafe">{{ servicio.nombreMedio }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Entrega estimada</dt>
              <dd class="text-cafe">{{ textoEntrega(servicio) }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Cupos disponibles</dt>
              <dd class="text-cafe">{{ servicio.cupos }}</dd>
            </div>
            <div v-if="esSensible" class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Contenido sensible</dt>
              <dd class="text-coral text-right">{{ textoSensible }}</dd>
            </div>
          </dl>

          <div class="mt-6 space-y-3 border-t border-rosa/60 pt-6">
            <button
              type="button"
              class="w-full rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
              @click="irAlArtista"
            >
              Ver perfil del artista
            </button>
            <router-link
              v-if="esDuenio"
              :to="`/mis-servicios/${servicio.idServicio}/editar`"
              class="block w-full rounded-md border border-rosa px-4 py-2 text-sm text-granate text-center hover:bg-rosa/20 transition"
            >
              Editar servicio
            </router-link>
          </div>

          <p class="mt-6 text-xs text-cafe/50 leading-relaxed border-t border-rosa/60 pt-4">
            Para contratar este servicio, contactá al artista desde su perfil. Próximamente: pedidos y presupuestos desde la plataforma.
          </p>
        </aside>
      </div>
    </template>
  </div>
</template>
