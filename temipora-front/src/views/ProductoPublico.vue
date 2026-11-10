<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatearPrecio } from '../servicios/moneda'
import {
  agregarProductoFavorito,
  cargarIdsProductosFavoritos,
  cargarProductoPublico,
  eliminarProductoFavorito,
  etiquetaDisponibilidad,
} from '../servicios/productos'
import VisorImagenes from '../components/VisorImagenes.vue'

const route = useRoute()
const router = useRouter()

const idProducto = computed(() => Number(route.params.id))

const producto = ref(null)
const cargando = ref(true)
const error = ref('')
const visorAbierto = ref(false)
const visorIndice = ref(0)
const indiceImagen = ref(0)
const revelado = ref(false)

const esFavorito = ref(false)
const actualizandoFavorito = ref(false)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const esSensible = computed(() => !!producto.value?.contenidoSensible)
const requiereAdvertencia = computed(() => esSensible.value && !revelado.value)
const tiposSensibles = computed(() => {
  const tipos = producto.value?.contenidosSensibles || []
  return tipos.map((t) => ETIQUETAS_SENSIBLE[t] || t)
})
const imagenes = computed(() => {
  if (!producto.value) return []
  return producto.value.imagenPortada ? [producto.value.imagenPortada] : []
})
const imagenActiva = computed(() => imagenes.value[indiceImagen.value] || null)
const precioMostrado = computed(() =>
  formatearPrecio(producto.value?.precioDesde, producto.value?.moneda || 'ARS'),
)
const esEntregaFisica = computed(
  () => producto.value?.tipoEntrega === 'FISICA',
)
const disponibilidad = computed(() => etiquetaDisponibilidad(producto.value))
const inicialArtista = computed(
  () => (producto.value?.nombreArtista || '?').charAt(0).toUpperCase(),
)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    producto.value = await cargarProductoPublico(idProducto.value)
    indiceImagen.value = 0
    await cargarFavorito()
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Este producto no está disponible.'
      : (e.response?.data?.message
        || 'No se pudo cargar el producto.')
  } finally {
    cargando.value = false
  }
}

async function cargarFavorito() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const ids = await cargarIdsProductosFavoritos()
    esFavorito.value = ids.includes(idProducto.value)
  } catch {
    esFavorito.value = false
  }
}

async function alternarFavorito() {
  if (actualizandoFavorito.value) return
  if (!localStorage.getItem('token')) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  actualizandoFavorito.value = true
  try {
    if (esFavorito.value) {
      await eliminarProductoFavorito(idProducto.value)
      esFavorito.value = false
    } else {
      await agregarProductoFavorito(idProducto.value)
      esFavorito.value = true
    }
  } catch {
    // Se mantiene el estado anterior si la llamada falla.
  } finally {
    actualizandoFavorito.value = false
  }
}

function abrirVisor(indice) {
  visorIndice.value = indice
  visorAbierto.value = true
}

watch(idProducto, cargar)
onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8">
    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando...</p>

    <div v-else-if="error" class="mx-auto max-w-xl rounded-2xl border border-rosa/60 bg-blanco px-6 py-12 text-center">
      <p class="font-display text-lg font-bold text-granate">{{ error }}</p>
      <router-link
        to="/productos"
        class="mt-4 inline-block text-sm font-medium text-coral transition hover:text-rojizo"
      >
        ← Volver al catálogo
      </router-link>
    </div>

    <template v-else-if="producto">
      <router-link
        to="/productos"
        class="text-sm text-granate transition hover:text-coral"
      >
        ← Volver a productos
      </router-link>

      <div class="mt-6 grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,22rem)] lg:items-start">
        <div class="space-y-8">
          <div class="overflow-hidden rounded-[22px] border border-borde/60 bg-superficie">
            <button
              type="button"
              class="relative block aspect-[4/3] w-full overflow-hidden bg-rosa/20"
              :aria-label="`Ampliar imagen de ${producto.titulo}`"
              @click="imagenes.length && abrirVisor(indiceImagen)"
            >
              <img
                v-if="imagenActiva"
                :src="imagenActiva"
                :alt="producto.titulo"
                class="h-full w-full object-cover transition duration-500 hover:scale-[1.02]"
                :class="requiereAdvertencia ? 'scale-105 blur-[24px]' : ''"
              />
              <span
                v-else
                class="flex h-full items-center justify-center text-sm text-cafe/45"
              >
                Sin imagen
              </span>
            </button>

            <div
              v-if="requiereAdvertencia"
              class="flex flex-col items-center gap-3 border-t border-rosa/50 bg-crema/40 px-4 py-6 text-center"
            >
              <p class="text-sm font-semibold text-granate">
                Este producto tiene contenido sensible
              </p>
              <p v-if="tiposSensibles.length" class="text-xs text-cafe/65">
                {{ tiposSensibles.join(' · ') }}
              </p>
              <button
                type="button"
                class="rounded-lg bg-coral px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
                @click="revelado = true"
              >
                Ver de todos modos
              </button>
            </div>
          </div>

          <section class="rounded-[22px] border border-borde/60 bg-superficie p-6">
            <h2 class="font-display text-lg font-bold text-granate">Descripción</h2>
            <p class="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-cafe/75">
              {{ producto.descripcion || 'Sin descripción.' }}
            </p>

            <div v-if="producto.contenidoSensible" class="mt-4 flex flex-wrap gap-2">
              <span
                v-for="tipo in tiposSensibles"
                :key="tipo"
                class="rounded-full bg-granate/10 px-3 py-1 text-xs font-medium text-granate"
              >
                {{ tipo }}
              </span>
            </div>
          </section>

          <section class="rounded-[22px] border border-borde/60 bg-superficie p-6">
            <h2 class="font-display text-lg font-bold text-granate">Entrega</h2>
            <p v-if="esEntregaFisica" class="mt-3 text-sm leading-relaxed text-cafe/75">
              Es una pieza física: Temipora no gestiona envíos. El envío y la
              entrega se coordinan directamente con el artista.
            </p>
            <p v-else class="mt-3 text-sm leading-relaxed text-cafe/75">
              Al acreditarse el pago vas a poder descargar el archivo original
              de la pieza.
            </p>
          </section>
        </div>

        <aside class="lg:sticky lg:top-24 lg:h-fit lg:self-start">
          <div class="space-y-4">
            <section class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-[0_10px_28px_rgba(74,29,46,0.08)]">
              <div class="flex flex-wrap items-start justify-between gap-2">
                <span
                  class="rounded-full px-2.5 py-0.5 text-[11px] font-semibold"
                  :class="producto.disponible
                    ? 'bg-green-500/10 text-green-700'
                    : 'bg-cafe/10 text-cafe/60'"
                >
                  {{ producto.disponible ? 'Disponible' : 'Sin stock' }}
                </span>
                <span
                  v-if="producto.esPiezaUnica"
                  class="rounded-full bg-mantequilla/70 px-2.5 py-0.5 text-[11px] font-bold text-granate"
                >
                  Pieza única
                </span>
              </div>

              <h1 class="mt-3 font-display text-2xl font-bold leading-tight text-granate">
                {{ producto.titulo }}
              </h1>
              <p class="mt-2 text-sm text-cafe/60">
                {{ producto.nombreTipoProducto }}
                <template v-if="producto.nombreMedio"> · {{ producto.nombreMedio }}</template>
              </p>

              <router-link
                :to="`/perfil/${producto.nombreArtista}`"
                class="mt-3 flex w-fit items-center gap-2"
              >
                <img
                  v-if="producto.urlFotoArtista"
                  :src="producto.urlFotoArtista"
                  alt=""
                  class="h-7 w-7 rounded-full object-cover ring-1 ring-borde/50"
                />
                <span
                  v-else
                  class="flex h-7 w-7 items-center justify-center rounded-full bg-rosa/40 text-[10px] font-bold text-granate"
                >
                  {{ inicialArtista }}
                </span>
                <span class="text-sm font-medium text-cafe/70 transition hover:text-coral">
                  @{{ producto.nombreArtista }}
                </span>
              </router-link>

              <p class="mt-5 font-display text-3xl font-extrabold text-coral">
                {{ precioMostrado }}
                <span class="text-xs font-semibold text-cafe/45">desde</span>
              </p>

              <p class="mt-2 text-xs text-cafe/50">
                {{ disponibilidad }}
              </p>

              <button
                type="button"
                disabled
                class="mt-5 flex w-full cursor-not-allowed items-center justify-center rounded-lg bg-cafe/15 px-4 py-2.5 text-sm font-semibold text-cafe/50"
                title="La compra todavía no está habilitada"
              >
                Comprar (próximamente)
              </button>
              <p class="mt-2 text-center text-[11px] leading-relaxed text-cafe/45">
                La compra directa todavía no está habilitada en la plataforma.
              </p>

              <button
                type="button"
                class="mt-3 flex w-full items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-semibold transition disabled:cursor-wait disabled:opacity-60"
                :class="esFavorito
                  ? 'border-coral bg-coral/10 text-granate hover:bg-coral/20'
                  : 'border-rosa text-cafe hover:bg-rosa/20'"
                :disabled="actualizandoFavorito"
                :aria-pressed="esFavorito"
                @click="alternarFavorito"
              >
                <svg
                  class="h-4 w-4"
                  viewBox="0 0 24 24"
                  :fill="esFavorito ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  stroke-width="1.8"
                  aria-hidden="true"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
                </svg>
                {{ esFavorito ? 'En tus favoritos' : 'Agregar a favoritos' }}
              </button>
            </section>
          </div>
        </aside>
      </div>

      <VisorImagenes
        :abierto="visorAbierto"
        :imagenes="imagenes"
        :indice="visorIndice"
        etiqueta="Imagen del producto"
        @cerrar="visorAbierto = false"
        @actualizar:indice="indiceImagen = $event"
      />
    </template>
  </div>
</template>
