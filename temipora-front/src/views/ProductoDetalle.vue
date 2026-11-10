<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFechaHora } from '../servicios/fechas'
import {
  estadoProductoDe,
  estadoVarianteDe,
  etiquetaStock,
} from '../servicios/productos'
import VisorImagenes from '../components/VisorImagenes.vue'

const route = useRoute()
const idProducto = computed(() => Number(route.params.id))

const producto = ref(null)
const ledger = ref(null)
const cargando = ref(true)
const error = ref('')
const visorAbierto = ref(false)
const visorIndice = ref(0)
const indiceImagen = ref(0)
const revelado = ref(false)
const mostrarInfoLedger = ref(false)
const POR_PAGINA = 4
const paginaVariantes = ref(1)
const paginaLedger = ref(1)

const imagenes = computed(() => {
  const deGaleria = (producto.value?.imagenes || []).map((imagen) => imagen.url)
  const deVariantes = (producto.value?.variantes || [])
    .map((variante) => variante.imagenUrl)
    .filter(Boolean)
  return [...new Set([...deGaleria, ...deVariantes])]
})
const imagenActiva = computed(() => imagenes.value[indiceImagen.value] || null)

const variantes = computed(() => producto.value?.variantes || [])
const variantesPagina = ref([])
const totalPaginasVariantes = ref(1)
const totalVariantes = ref(0)
const esSensible = computed(() => !!producto.value?.contenidoSensible)
const requiereAdvertencia = computed(() => esSensible.value && !revelado.value)
const precioDesde = computed(() => {
  if (!producto.value) return '—'
  return formatearPrecio(producto.value.precioDesde, producto.value.moneda || 'ARS')
})
const licencia = computed(() => producto.value?.licencia || null)
const resumenDerechos = computed(
  () => (licencia.value?.derechos || []).map((derecho) => derecho.nombre).join(' · '),
)
const esEntregaFisica = computed(
  () => producto.value?.tipoEntregaMedio === 'FISICA',
)
const entregables = computed(() => producto.value?.entregables || [])
const ledgerPagina = computed(() => ledger.value?.variantes || [])
const totalPaginasLedger = computed(
  () => Math.max(1, Number(ledger.value?.totalPaginas) || 1),
)
const totalLedger = computed(() => Number(ledger.value?.totalElementos) || 0)
const tieneAlgunLedger = computed(() => totalLedger.value > 0)

function hashCorto(hash) {
  if (!hash) return '—'
  return `${hash.slice(0, 10)}…${hash.slice(-6)}`
}

function etiquetaEvento(tipo) {
  const etiquetas = {
    GENESIS: 'Origen',
    VENTA: 'Venta',
    TRANSFERENCIA: 'Transferencia',
    REVENTA: 'Reventa',
    ANULACION: 'Anulación',
  }
  return etiquetas[tipo] || tipo
}

function imagenDeVariante(variante) {
  const url = variante.imagenUrl || producto.value?.imagenPortada || imagenes.value[0]
  if (!url) return
  const posicion = imagenes.value.indexOf(url)
  if (posicion >= 0) seleccionarImagen(posicion)
}

function seleccionarImagen(indice) {
  if (!imagenes.value.length) return
  const destino = Math.min(
    Math.max(Number(indice) || 0, 0),
    imagenes.value.length - 1,
  )
  indiceImagen.value = destino
  if (visorAbierto.value) visorIndice.value = destino
}

function moverImagen(paso) {
  if (imagenes.value.length < 2) return
  const total = imagenes.value.length
  seleccionarImagen((indiceImagen.value + paso + total) % total)
}

function abrirVisor(indice = indiceImagen.value) {
  if (!imagenes.value.length || requiereAdvertencia.value) return
  const destino = Math.min(
    Math.max(Number(indice) || 0, 0),
    imagenes.value.length - 1,
  )
  visorIndice.value = destino
  visorAbierto.value = true
}

function aplicarVariantes(data) {
  variantesPagina.value = data?.contenido || []
  paginaVariantes.value = data?.pagina || 1
  totalPaginasVariantes.value = Math.max(1, Number(data?.totalPaginas) || 1)
  totalVariantes.value = Number(data?.totalElementos) || 0
}

function aplicarLedger(data) {
  ledger.value = data || null
  paginaLedger.value = data?.pagina || 1
}

async function cambiarPaginaVariantes(paso) {
  const destino = Math.min(
    Math.max(paginaVariantes.value + paso, 1),
    totalPaginasVariantes.value,
  )
  if (destino === paginaVariantes.value) return
  try {
    const res = await api.get(`/mis-productos/${idProducto.value}/variantes`, {
      params: { pagina: destino, tamano: POR_PAGINA },
    })
    aplicarVariantes(res.data)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudieron cargar las variantes.'
  }
}

async function cambiarPaginaLedger(paso) {
  const destino = Math.min(
    Math.max(paginaLedger.value + paso, 1),
    totalPaginasLedger.value,
  )
  if (destino === paginaLedger.value) return
  try {
    const res = await api.get(`/mis-productos/${idProducto.value}/ledger`, {
      params: { pagina: destino, tamano: POR_PAGINA },
    })
    aplicarLedger(res.data)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar el historial de autenticidad.'
  }
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [productoRes, variantesRes, ledgerRes] = await Promise.all([
      api.get(`/mis-productos/${idProducto.value}`),
      api.get(`/mis-productos/${idProducto.value}/variantes`, {
        params: { pagina: 1, tamano: POR_PAGINA },
      }),
      api.get(`/mis-productos/${idProducto.value}/ledger`, {
        params: { pagina: 1, tamano: POR_PAGINA },
      }),
    ])
    producto.value = productoRes.data
    aplicarVariantes(variantesRes.data)
    aplicarLedger(ledgerRes.data)
    indiceImagen.value = 0
    visorIndice.value = 0
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la vista previa del producto.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8">
    <router-link
      to="/panel/productos"
      class="text-sm text-granate transition hover:text-coral"
    >
      ← Volver a mis productos
    </router-link>

    <div class="mt-4 rounded-xl border border-coral/25 bg-rosa/20 px-4 py-3 text-sm text-granate">
      <span class="font-semibold">Vista previa del artista.</span>
      Esta pantalla todavía no es pública; te muestra cómo quedará la publicación
      y el estado de su registro de autenticidad.
    </div>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <template v-else-if="producto">
      <div class="mt-6 grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(22rem,30rem)]">
        <div class="min-w-0 space-y-6">
          <section class="overflow-hidden rounded-[22px] border border-borde/60 bg-superficie shadow-[0_10px_28px_rgba(74,29,46,0.08)]">
            <div class="relative aspect-[4/3] w-full overflow-hidden bg-rosa/20">
              <button
                v-if="imagenActiva"
                type="button"
                class="block h-full w-full"
                aria-label="Ampliar imagen del producto"
                @click="abrirVisor()"
              >
                <img
                  :src="imagenActiva"
                  :alt="producto.titulo"
                  class="h-full w-full object-cover transition duration-500"
                  :class="requiereAdvertencia ? 'scale-105 blur-[18px]' : ''"
                />
              </button>
              <div
                v-else
                class="flex h-full items-center justify-center text-sm text-cafe/45"
              >
                Sin imágenes
              </div>

              <div
                v-if="requiereAdvertencia"
                class="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-granate/35 px-6 text-center text-white"
              >
                <span class="text-sm font-semibold">Contenido sensible</span>
                <button
                  type="button"
                  class="rounded-full bg-white/95 px-4 py-1.5 text-xs font-bold text-granate transition hover:bg-white"
                  @click="revelado = true"
                >
                  Ver de todos modos
                </button>
              </div>

              <button
                v-if="imagenes.length > 1 && !requiereAdvertencia"
                type="button"
                class="absolute left-3 top-1/2 z-10 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-superficie/95 text-lg text-granate shadow-sm ring-1 ring-borde/60 transition hover:bg-white"
                aria-label="Imagen anterior"
                @click.stop="moverImagen(-1)"
              >
                ←
              </button>
              <button
                v-if="imagenes.length > 1 && !requiereAdvertencia"
                type="button"
                class="absolute right-3 top-1/2 z-10 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-superficie/95 text-lg text-granate shadow-sm ring-1 ring-borde/60 transition hover:bg-white"
                aria-label="Imagen siguiente"
                @click.stop="moverImagen(1)"
              >
                →
              </button>

              <span
                v-if="imagenes.length > 1"
                class="absolute bottom-3 left-3 rounded-full bg-granate/85 px-2.5 py-1 text-[11px] font-semibold text-white"
              >
                {{ indiceImagen + 1 }} / {{ imagenes.length }}
              </span>

              <button
                v-if="imagenes.length && !requiereAdvertencia"
                type="button"
                class="absolute bottom-3 right-3 rounded-full bg-superficie/95 px-3 py-1.5 text-xs font-semibold text-granate shadow-sm ring-1 ring-borde/60 transition hover:bg-white"
                @click.stop="abrirVisor()"
              >
                Ampliar
              </button>
            </div>

            <div v-if="imagenes.length > 1" class="flex gap-2 overflow-x-auto px-4 py-3">
              <button
                v-for="(url, indice) in imagenes"
                :key="`${url}-${indice}`"
                type="button"
                class="h-16 w-16 shrink-0 overflow-hidden rounded-lg border-2 transition"
                :class="indice === indiceImagen ? 'border-coral' : 'border-rosa/50 hover:border-coral/50'"
                :aria-label="`Ver imagen ${indice + 1}`"
                @click="seleccionarImagen(indice)"
              >
                <img :src="url" alt="" class="h-full w-full object-cover" />
              </button>
            </div>
          </section>

          <section class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-sm">
            <h2 class="font-display text-lg font-bold text-granate">Descripción</h2>
            <p class="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-cafe/75">
              {{ producto.descripcion }}
            </p>
            <div class="mt-4 flex flex-wrap gap-2">
              <span
                v-for="contenido in producto.contenidosSensibles || []"
                :key="contenido"
                class="rounded-full bg-granate/10 px-3 py-1 text-xs font-semibold text-granate"
              >
                {{ contenido }}
              </span>
            </div>
          </section>

          <section class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-sm">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h2 class="font-display text-lg font-bold text-granate">
                {{ totalVariantes === 1 ? 'Presentación' : 'Variantes' }}
                ({{ totalVariantes }})
              </h2>
              <span class="text-xs text-cafe/45">
                Cada variante es un ítem vendible.
              </span>
            </div>

            <div class="mt-4 divide-y divide-rosa/40">
              <article
                v-for="variante in variantesPagina"
                :key="variante.idVariante"
                class="flex flex-col gap-4 py-4 sm:flex-row sm:items-center"
              >
                <button
                  type="button"
                  class="h-20 w-20 shrink-0 overflow-hidden rounded-xl border border-rosa/50 bg-rosa/10"
                  @click="imagenDeVariante(variante)"
                >
                  <img
                    v-if="variante.imagenUrl"
                    :src="variante.imagenUrl"
                    :alt="variante.nombre"
                    class="h-full w-full object-cover"
                  />
                </button>

                <div class="min-w-0 flex-1">
                  <div class="flex flex-wrap items-center gap-2">
                    <h3 class="font-display text-base font-bold text-granate">
                      {{ variante.nombre }}
                    </h3>
                    <span
                      class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                      :class="estadoVarianteDe(variante.estado).clase"
                    >
                      {{ estadoVarianteDe(variante.estado).texto }}
                    </span>
                    <span
                      v-if="variante.tieneLedger"
                      class="rounded-full bg-mantequilla/60 px-2.5 py-0.5 text-[11px] font-bold text-granate"
                    >
                      Origen registrado
                    </span>
                  </div>
                  <p v-if="variante.descripcion" class="mt-1 text-sm text-cafe/65">
                    {{ variante.descripcion }}
                  </p>
                  <p class="mt-2 text-sm text-cafe/60">
                    {{ etiquetaStock(variante) }}
                    <span v-if="variante.nombrePropietario" class="text-cafe/45">
                      · Propietario actual: @{{ variante.nombrePropietario }}
                    </span>
                  </p>
                </div>

                <p class="shrink-0 font-display text-lg font-extrabold text-coral">
                  {{ formatearPrecio(variante.precio, producto.moneda) }}
                </p>
              </article>
            </div>

            <div
              v-if="totalPaginasVariantes > 1"
              class="mt-4 flex items-center justify-between gap-3 border-t border-rosa/40 pt-3"
            >
              <button
                type="button"
                class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                :disabled="paginaVariantes === 1"
                @click="cambiarPaginaVariantes(-1)"
              >
                ← Anterior
              </button>
              <span class="text-xs text-cafe/55">
                Página {{ paginaVariantes }} de {{ totalPaginasVariantes }}
              </span>
              <button
                type="button"
                class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                :disabled="paginaVariantes === totalPaginasVariantes"
                @click="cambiarPaginaVariantes(1)"
              >
                Siguiente →
              </button>
            </div>
          </section>

          <section
            v-if="licencia"
            class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-sm"
          >
            <h2 class="font-display text-lg font-bold text-granate">
              Licencia de uso
            </h2>
            <p class="mt-1 text-[11px] text-cafe/45">
              Versión {{ licencia.version }}
            </p>
            <p class="mt-3 text-sm leading-relaxed text-cafe/75">
              <span class="font-semibold text-granate">Usos permitidos:</span>
              {{ resumenDerechos || 'Sin usos definidos' }}
            </p>
            <div class="mt-3 grid gap-2 sm:grid-cols-2">
              <p class="rounded-xl bg-crema/60 px-3 py-2 text-xs text-cafe/70">
                Modificaciones:
                <span class="font-semibold text-granate">
                  {{ licencia.permiteModificaciones ? 'permitidas' : 'no permitidas' }}
                </span>
              </p>
              <p class="rounded-xl bg-crema/60 px-3 py-2 text-xs text-cafe/70">
                Crédito:
                <span class="font-semibold text-granate">
                  {{ licencia.requiereCredito ? 'requerido' : 'no requerido' }}
                </span>
              </p>
            </div>
            <p
              v-if="licencia.permiteReventa"
              class="mt-3 rounded-xl border border-coral/20 bg-rosa/20 px-3 py-2 text-xs leading-relaxed text-granate"
            >
              Reventa permitida para piezas únicas
              <span v-if="Number(licencia.porcentajeRegalia) > 0">
                con regalía del {{ licencia.porcentajeRegalia }}%.
              </span>
              <span v-else>sin regalía.</span>
            </p>
            <p
              v-if="licencia.textoAdicional"
              class="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-cafe/75"
            >
              {{ licencia.textoAdicional }}
            </p>
          </section>

          <section
            v-if="esEntregaFisica"
            class="rounded-[22px] border border-coral/30 bg-rosa/15 p-6 shadow-sm"
          >
            <h2 class="font-display text-lg font-bold text-granate">Entrega</h2>
            <p class="mt-2 text-sm font-semibold text-granate">
              Temipora no gestiona envíos.
            </p>
            <p class="mt-1 text-sm leading-relaxed text-cafe/75">
              El envío y la entrega de esta pieza tradicional se coordinan
              directamente entre artista y comprador.
            </p>
          </section>

          <section
            v-if="(producto.etiquetas || []).length"
            class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-sm"
          >
            <h2 class="font-display text-lg font-bold text-granate">Etiquetas</h2>
            <div class="mt-3 flex flex-wrap gap-2">
              <span
                v-for="etiqueta in producto.etiquetas"
                :key="etiqueta.idEtiqueta"
                class="rounded-full bg-durazno/40 px-3 py-1 text-xs text-cafe"
              >
                {{ etiqueta.nombre }}
              </span>
            </div>
          </section>
        </div>

        <aside class="lg:sticky lg:top-24 lg:h-fit lg:self-start">
          <div class="space-y-4">
            <section class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-[0_10px_28px_rgba(74,29,46,0.08)]">
              <div class="flex flex-wrap items-start justify-between gap-2">
                <span
                  class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
                  :class="estadoProductoDe(producto.estado).clase"
                >
                  {{ estadoProductoDe(producto.estado).texto }}
                </span>
                <span
                  v-if="producto.disponible"
                  class="rounded-full bg-green-500/10 px-2.5 py-0.5 text-[11px] font-semibold text-green-700"
                >
                  Disponible para vender
                </span>
                <span
                  v-else-if="producto.estado !== 'BORRADOR'"
                  class="rounded-full bg-cafe/10 px-2.5 py-0.5 text-[11px] font-semibold text-cafe/60"
                >
                  Sin stock
                </span>
                <span
                  v-else
                  class="rounded-full bg-cafe/10 px-2.5 py-0.5 text-[11px] font-semibold text-cafe/60"
                >
                  Todavía no publicado
                </span>
              </div>

              <h1 class="mt-3 font-display text-2xl font-bold leading-tight text-granate">
                {{ producto.titulo || 'Sin título' }}
              </h1>
              <p class="mt-2 text-sm text-cafe/60">
                {{ producto.nombreTipoProducto || 'Sin tipo' }}
                · {{ producto.nombreMedio || 'Sin medio' }}
              </p>

              <div class="mt-3 flex items-center gap-2">
                <img
                  v-if="producto.urlFotoArtista"
                  :src="producto.urlFotoArtista"
                  alt=""
                  class="h-7 w-7 rounded-full object-cover ring-1 ring-borde/50"
                />
                <span class="text-sm font-medium text-cafe/70">
                  @{{ producto.nombreArtista }}
                </span>
              </div>

              <p class="mt-5 font-display text-3xl font-extrabold text-coral">
                {{ precioDesde }}
                <span class="text-xs font-semibold text-cafe/45">desde</span>
              </p>

              <p class="mt-2 text-xs text-cafe/50">
                {{ producto.cantidadVariantes }} variante(s) ·
                {{ producto.cantidadVariantesDisponibles }} disponible(s)
              </p>

              <div
                v-if="esEntregaFisica"
                class="mt-3 rounded-xl border border-coral/20 bg-rosa/20 px-3 py-2 text-xs leading-relaxed text-granate"
              >
                <p class="font-semibold">Temipora no gestiona envíos.</p>
                <p class="mt-1">
                  La entrega se coordina directamente entre artista y comprador.
                </p>
              </div>

              <div
                v-if="licencia"
                class="mt-4 rounded-xl border border-coral/20 bg-rosa/20 px-3 py-2 text-xs leading-relaxed text-granate"
              >
                <p class="font-semibold">Licencia</p>
                <p class="mt-1">{{ resumenDerechos || 'Sin usos definidos' }}</p>
                <p v-if="licencia.permiteReventa" class="mt-1">
                  Reventa de piezas únicas
                  <span v-if="Number(licencia.porcentajeRegalia) > 0">
                    con regalía del {{ licencia.porcentajeRegalia }}%.
                  </span>
                  <span v-else>sin regalía.</span>
                </p>
              </div>

              <router-link
                :to="`/panel/productos/${producto.idProducto}/editar`"
                class="mt-5 flex w-full items-center justify-center rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo"
              >
                {{ producto.estado === 'BORRADOR' ? 'Continuar borrador' : 'Editar producto' }}
              </router-link>
            </section>

            <section
              v-if="!esEntregaFisica && entregables.length"
              class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm"
            >
              <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Entrega
              </p>
              <h2 class="mt-1 font-display text-base font-bold text-granate">
                Qué recibe el comprador
              </h2>
              <p class="mt-2 text-xs leading-relaxed text-cafe/60">
                Archivos originales, tal como los subiste. Se habilitan al
                acreditarse el pago.
              </p>
              <ul class="mt-3 space-y-2">
                <li
                  v-for="entregable in entregables"
                  :key="entregable.idEntregable"
                  class="flex items-start gap-2.5 rounded-xl border border-rosa/50 bg-crema/40 px-3 py-2"
                >
                  <span
                    class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-coral/10 text-coral"
                    aria-hidden="true"
                  >
                    <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M14 3v5h5M7 3h7l6 6v12H7z" />
                    </svg>
                  </span>
                  <span class="min-w-0">
                    <span class="block break-words text-sm font-semibold text-granate">
                      {{ entregable.nombre }}
                    </span>
                    <span class="block text-xs text-cafe/55">
                      {{ entregable.alcance === 'COMUN'
                        ? 'Para todos los compradores'
                        : `Variante: ${entregable.nombreVariante}` }}
                    </span>
                  </span>
                </li>
              </ul>
            </section>

            <section class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                    Propiedad y procedencia
                  </p>
                  <h2 class="mt-1 font-display text-base font-bold text-granate">
                    Historial de autenticidad
                  </h2>
                </div>
                <div class="relative flex items-center gap-2">
                  <button
                    type="button"
                    class="rounded-full border border-rosa/60 bg-blanco px-2.5 py-1 text-[11px] font-semibold text-granate transition hover:bg-rosa/20"
                    @click="mostrarInfoLedger = !mostrarInfoLedger"
                  >
                    ¿Qué es?
                  </button>
                  <span
                    v-if="tieneAlgunLedger"
                    class="rounded-full px-2.5 py-0.5 text-[11px] font-bold"
                    :class="ledger?.integridad
                      ? 'bg-green-500/10 text-green-700'
                      : 'bg-rojizo/10 text-rojizo'"
                  >
                    {{ ledger?.integridad ? 'Verificado' : 'Revisar historial' }}
                  </span>

                  <div
                    v-if="mostrarInfoLedger"
                    class="absolute right-0 top-9 z-30 w-72 rounded-xl border border-rosa/60 bg-blanco p-4 text-xs leading-relaxed text-cafe/70 shadow-xl"
                  >
                    <p class="font-semibold text-granate">Historial de autenticidad</p>
                    <p class="mt-1">
                      Es un registro digital de la pieza. Guarda su origen, quién la
                      creó y cada cambio de propiedad. Cada registro se encadena con
                      el anterior: si alguien intenta modificarlo, la verificación lo
                      detecta.
                    </p>
                    <p class="mt-2">
                      No es una criptomoneda ni una wallet, y no reemplaza tus
                      derechos de autor.
                    </p>
                    <button
                      type="button"
                      class="mt-3 text-[11px] font-semibold text-coral transition hover:text-rojizo"
                      @click="mostrarInfoLedger = false"
                    >
                      Entendido
                    </button>
                  </div>
                </div>
              </div>

              <p v-if="ledger?.mensaje" class="mt-3 text-xs leading-relaxed text-cafe/60">
                {{ ledger.mensaje }}
              </p>

              <div v-if="!tieneAlgunLedger" class="mt-3 rounded-xl bg-crema/70 px-3 py-3 text-xs leading-relaxed text-cafe/60">
                Las variantes ilimitadas o con stock mayor a 1 se venden como
                licencia y no tienen registro de propiedad individual.
              </div>

              <div v-else class="mt-4 space-y-4">
                <article
                  v-for="varianteLedger in ledgerPagina"
                  :key="varianteLedger.idVariante"
                  class="rounded-xl border border-rosa/50 bg-crema/40 p-3"
                >
                  <div class="flex items-center justify-between gap-2">
                    <p class="text-sm font-semibold text-granate">
                      {{ varianteLedger.nombre }}
                    </p>
                    <span
                      class="rounded-full px-2 py-0.5 text-[10px] font-bold"
                      :class="varianteLedger.integridad
                        ? 'bg-green-500/10 text-green-700'
                        : 'bg-rojizo/10 text-rojizo'"
                    >
                      {{ varianteLedger.integridad ? 'Verificado' : 'Con diferencias' }}
                    </span>
                  </div>
                  <p class="mt-1 text-[11px] text-cafe/55">
                    {{ varianteLedger.mensaje }}
                  </p>

                  <ol class="mt-3 space-y-2">
                    <li
                      v-for="evento in varianteLedger.eventos"
                      :key="evento.idEventoLedger"
                      class="rounded-lg bg-blanco px-3 py-2 ring-1 ring-rosa/40"
                    >
                      <div class="flex items-center justify-between gap-2">
                        <p class="text-[11px] font-bold uppercase tracking-wide text-coral">
                          #{{ evento.secuencia }} · {{ etiquetaEvento(evento.tipo) }}
                        </p>
                        <span
                          class="text-[10px] font-semibold"
                          :class="evento.verificado ? 'text-green-700' : 'text-rojizo'"
                        >
                          {{ evento.verificado ? '✓ verificado' : '✗ no coincide' }}
                        </span>
                      </div>
                      <p class="mt-1 text-[11px] text-cafe/60">
                        {{ formatearFechaHora(evento.ocurridoEn) }}
                      </p>
                      <p class="mt-1 text-[11px] text-cafe/55">
                        @{{ evento.nombreOrigen || '—' }} → @{{ evento.nombreDestino || '—' }}
                      </p>
                      <p class="mt-1 break-all font-mono text-[10px] text-cafe/35" :title="evento.hashEvento">
                        {{ hashCorto(evento.hashEvento) }}
                      </p>
                    </li>
                  </ol>
                </article>
              </div>

              <div
                v-if="tieneAlgunLedger && totalPaginasLedger > 1"
                class="mt-4 flex items-center justify-between gap-3 border-t border-rosa/40 pt-3"
              >
                <button
                  type="button"
                  class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                  :disabled="paginaLedger === 1"
                  @click="cambiarPaginaLedger(-1)"
                >
                  ← Anterior
                </button>
                <span class="text-xs text-cafe/55">
                  Página {{ paginaLedger }} de {{ totalPaginasLedger }}
                </span>
                <button
                  type="button"
                  class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                  :disabled="paginaLedger === totalPaginasLedger"
                  @click="cambiarPaginaLedger(1)"
                >
                  Siguiente →
                </button>
              </div>
            </section>

            <section
              v-if="producto.contenidoSensible"
              class="rounded-[22px] border border-mantequilla/70 bg-mantequilla/30 p-4 text-xs leading-relaxed text-granate"
            >
              Esta publicación está marcada como contenido sensible.
              Los usuarios verán una advertencia antes de abrirla.
            </section>
          </div>
        </aside>
      </div>
    </template>

    <VisorImagenes
      :abierto="visorAbierto"
      :imagenes="imagenes"
      :indice="visorIndice"
      :etiqueta="producto?.titulo || 'Producto'"
      @cerrar="visorAbierto = false"
      @actualizar:indice="seleccionarImagen"
    />
  </div>
</template>
