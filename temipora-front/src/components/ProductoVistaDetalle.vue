<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFechaHora } from '../servicios/fechas'
import {
  estadoProductoDe,
  estadoVarianteDe,
  etiquetaStock,
} from '../servicios/productos'
import VisorImagenes from './VisorImagenes.vue'

const props = defineProps({
  producto: { type: Object, required: true },
  imagenes: { type: Array, default: () => [] },
  variantesPagina: { type: Array, default: () => [] },
  paginaVariantes: { type: Number, default: 1 },
  totalPaginasVariantes: { type: Number, default: 1 },
  totalVariantes: { type: Number, default: 0 },
  ledger: { type: Object, default: null },
  paginaLedger: { type: Number, default: 1 },
  totalPaginasLedger: { type: Number, default: 1 },
  totalLedger: { type: Number, default: 0 },
  modo: { type: String, default: 'publico' },
  disponibilidad: { type: String, default: '' },
  precioDesde: { type: String, default: '—' },
})

const emit = defineEmits([
  'cambiar-pagina-variantes',
  'cambiar-pagina-ledger',
  'agregar-carrito',
])

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const galeria = ref(null)
const indiceImagen = ref(0)
const visorAbierto = ref(false)
const visorIndice = ref(0)
const revelado = ref(false)
const mostrarInfoLedger = ref(false)
const abrirInfoArriba = ref(false)
const contenedorInfoLedger = ref(null)
const tooltipLedger = ref(null)

const esSensible = computed(() => !!props.producto?.contenidoSensible)
const requiereAdvertencia = computed(() => esSensible.value && !revelado.value)
const tiposSensibles = computed(() =>
  (props.producto?.contenidosSensibles || []).map(
    (tipo) => ETIQUETAS_SENSIBLE[tipo] || tipo,
  ),
)
const imagenActiva = computed(() => props.imagenes[indiceImagen.value] || null)
const esPrevia = computed(() => props.modo === 'previa')
const esEntregaFisica = computed(
  () => (props.producto?.tipoEntrega || props.producto?.tipoEntregaMedio) === 'FISICA',
)
const licencia = computed(() => props.producto?.licencia || null)
const resumenDerechos = computed(
  () => (licencia.value?.derechos || []).map((derecho) => derecho.nombre).join(' · '),
)
const inicialArtista = computed(
  () => (props.producto?.nombreArtista || '?').charAt(0).toUpperCase(),
)
const ledgerPagina = computed(() => props.ledger?.variantes || [])
const tieneAlgunLedger = computed(() => props.totalLedger > 0)

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

function seleccionarImagen(indice) {
  if (!props.imagenes.length) return
  const destino = Math.min(
    Math.max(Number(indice) || 0, 0),
    props.imagenes.length - 1,
  )
  indiceImagen.value = destino
  if (visorAbierto.value) visorIndice.value = destino
}

function moverImagen(paso) {
  if (props.imagenes.length < 2) return
  const total = props.imagenes.length
  seleccionarImagen((indiceImagen.value + paso + total) % total)
}

function imagenDeVariante(variante) {
  const url = variante.imagenUrl || props.producto?.imagenPortada || props.imagenes[0]
  if (!url) return
  const posicion = props.imagenes.indexOf(url)
  if (posicion >= 0) seleccionarImagen(posicion)
}

function abrirVisor(indice = indiceImagen.value) {
  if (!props.imagenes.length || requiereAdvertencia.value) return
  const destino = Math.min(
    Math.max(Number(indice) || 0, 0),
    props.imagenes.length - 1,
  )
  visorIndice.value = destino
  visorAbierto.value = true
}

function irAGaleria() {
  const el = galeria.value
  if (!el) return
  const top = el.getBoundingClientRect().top + window.scrollY - 88
  window.scrollTo({ top, behavior: 'smooth' })
}

function seleccionarVariante(variante) {
  imagenDeVariante(variante)
  irAGaleria()
}

function varianteDisponible(variante) {
  const stock = variante?.stockDisponible
  return variante?.estado === 'DISPONIBLE'
    && (stock === null || stock === undefined || Number(stock) > 0)
}

function agregarVariante(variante) {
  if (!varianteDisponible(variante)) return
  emit('agregar-carrito', variante)
}

async function alternarInfoLedger() {
  if (mostrarInfoLedger.value) {
    mostrarInfoLedger.value = false
    return
  }
  mostrarInfoLedger.value = true
  abrirInfoArriba.value = false
  await nextTick()
  const contenedor = contenedorInfoLedger.value
  const tooltip = tooltipLedger.value
  if (!contenedor || !tooltip) return
  const espacioAbajo = window.innerHeight - contenedor.getBoundingClientRect().bottom
  abrirInfoArriba.value = espacioAbajo < tooltip.offsetHeight + 16
}

watch(
  () => props.producto,
  () => {
    indiceImagen.value = 0
    visorIndice.value = 0
    visorAbierto.value = false
    revelado.value = false
  },
)
</script>

<template>
  <div class="grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(22rem,30rem)]">
    <div class="min-w-0 space-y-6">
      <section
        ref="galeria"
        class="overflow-hidden rounded-[22px] border border-borde/60 bg-superficie shadow-[0_10px_28px_rgba(74,29,46,0.08)]"
      >
        <div class="relative aspect-[3/2] w-full overflow-hidden bg-rosa/20">
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
            <p v-if="tiposSensibles.length" class="text-xs text-white/85">
              {{ tiposSensibles.join(' · ') }}
            </p>
            <button
              type="button"
              class="rounded-full bg-white/95 px-4 py-1.5 text-xs font-bold text-granate transition hover:bg-white"
              @click="revelado = true"
            >
              Ver de todos modos
            </button>
          </div>

          <template v-if="imagenes.length > 1 && !requiereAdvertencia">
            <button
              type="button"
              class="absolute left-3 top-1/2 z-10 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-superficie/95 text-lg text-granate shadow-sm ring-1 ring-borde/60 transition hover:bg-white"
              aria-label="Imagen anterior"
              @click.stop="moverImagen(-1)"
            >
              ←
            </button>
            <button
              type="button"
              class="absolute right-3 top-1/2 z-10 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-superficie/95 text-lg text-granate shadow-sm ring-1 ring-borde/60 transition hover:bg-white"
              aria-label="Imagen siguiente"
              @click.stop="moverImagen(1)"
            >
              →
            </button>
          </template>

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
          {{ producto.descripcion || 'Sin descripción.' }}
        </p>
        <div v-if="producto.contenidoSensible" class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="tipo in tiposSensibles"
            :key="tipo"
            class="rounded-full bg-granate/10 px-3 py-1 text-xs font-semibold text-granate"
          >
            {{ tipo }}
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

        <p v-if="!variantesPagina.length" class="mt-4 text-sm text-cafe/60">
          Sin variantes publicadas.
        </p>

        <div class="mt-4 divide-y divide-rosa/40">
          <article
            v-for="variante in variantesPagina"
            :key="variante.idVariante"
            role="button"
            tabindex="0"
            :aria-label="`Ver ${variante.nombre} en la galería`"
            class="group flex cursor-pointer flex-col gap-4 rounded-xl px-3 py-4 transition hover:bg-rosa/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50 sm:flex-row sm:items-center"
            @click="seleccionarVariante(variante)"
            @keydown.enter="seleccionarVariante(variante)"
            @keydown.space.prevent="seleccionarVariante(variante)"
          >
            <div class="h-20 w-20 shrink-0 overflow-hidden rounded-xl border border-rosa/50 bg-rosa/10">
              <img
                v-if="variante.imagenUrl"
                :src="variante.imagenUrl"
                :alt="variante.nombre"
                class="h-full w-full object-cover transition duration-300 group-hover:scale-105"
              />
            </div>

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

            <div class="flex shrink-0 flex-col items-end gap-2">
              <p class="font-display text-lg font-extrabold text-coral">
                {{ formatearPrecio(variante.precio, producto.moneda) }}
              </p>
              <button
                v-if="!esPrevia"
                type="button"
                class="rounded-lg bg-coral px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="!varianteDisponible(variante)"
                @click.stop="agregarVariante(variante)"
              >
                {{ varianteDisponible(variante) ? 'Agregar' : 'Sin stock' }}
              </button>
            </div>
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
            @click="$emit('cambiar-pagina-variantes', -1)"
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
            @click="$emit('cambiar-pagina-variantes', 1)"
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
        class="rounded-[22px] border p-6 shadow-sm"
        :class="esEntregaFisica
          ? 'border-coral/30 bg-rosa/15'
          : 'border-borde/60 bg-superficie'"
      >
        <h2 class="font-display text-lg font-bold text-granate">Entrega</h2>
        <template v-if="esEntregaFisica">
          <p class="mt-2 text-sm font-semibold text-granate">
            Temipora no gestiona envíos.
          </p>
          <p class="mt-1 text-sm leading-relaxed text-cafe/75">
            El envío y la entrega de esta pieza tradicional se coordinan
            directamente entre artista y comprador.
          </p>
        </template>
        <p v-else class="mt-2 text-sm leading-relaxed text-cafe/75">
          Al acreditarse el pago vas a poder descargar el archivo original de
          la pieza.
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
            <template v-if="esPrevia">
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
            </template>
            <template v-else>
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
            </template>
          </div>

          <h1 class="mt-3 font-display text-2xl font-bold leading-tight text-granate">
            {{ producto.titulo || 'Sin título' }}
          </h1>
          <p class="mt-2 text-sm text-cafe/60">
            {{ producto.nombreTipoProducto || 'Sin tipo' }}
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
            {{ precioDesde }}
            <span class="text-xs font-semibold text-cafe/45">desde</span>
          </p>

          <p class="mt-2 text-xs text-cafe/50">
            {{ producto.cantidadVariantes }} variante(s) ·
            {{ producto.cantidadVariantesDisponibles }} disponible(s)
          </p>
          <p v-if="!esPrevia && disponibilidad" class="mt-1 text-xs text-cafe/50">
            {{ disponibilidad }}
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

          <div class="mt-5 space-y-3">
            <slot name="acciones" />
          </div>
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
            <div ref="contenedorInfoLedger" class="relative flex items-center gap-2">
              <button
                type="button"
                class="rounded-full border border-rosa/60 bg-blanco px-2.5 py-1 text-[11px] font-semibold text-granate transition hover:bg-rosa/20"
                @click="alternarInfoLedger"
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
                ref="tooltipLedger"
                class="absolute right-0 z-30 w-72 rounded-xl border border-rosa/60 bg-blanco p-4 text-xs leading-relaxed text-cafe/70 shadow-xl"
                :class="abrirInfoArriba ? 'bottom-full mb-2' : 'top-full mt-2'"
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
              @click="$emit('cambiar-pagina-ledger', -1)"
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
              @click="$emit('cambiar-pagina-ledger', 1)"
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
