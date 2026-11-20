<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCarrito } from '../composables/useCarrito'
import { useNotificaciones } from '../composables/useNotificaciones'
import { formatearPrecio } from '../servicios/moneda'
import { pagarGrupo } from '../servicios/compras'

const route = useRoute()
const router = useRouter()
const { exito, error: toastError } = useNotificaciones()
const {
  cargando,
  error,
  grupos,
  total,
  avisos,
  cargar,
  quitar,
  vaciar,
  confirmarCheckout,
} = useCarrito()

const artistaVaciado = ref('')
const carritoVaciado = ref(false)
const procesandoPago = ref(false)
const grupoPagando = ref(null)

const idArtistaSeleccionado = computed(() => {
  const valor = route.query.artista
  return valor ? Number(valor) : null
})
const grupoSeleccionado = computed(() =>
  grupos.value.find(
    (grupo) => Number(grupo.idArtista) === idArtistaSeleccionado.value,
  ) || null,
)
const cantidadGrupos = computed(() => grupos.value.length)
const mostrarIndice = computed(
  () => cantidadGrupos.value > 1 && !grupoSeleccionado.value,
)
const gruposVisibles = computed(() =>
  grupoSeleccionado.value ? [grupoSeleccionado.value] : grupos.value,
)
const grupoActivo = computed(
  () => grupoSeleccionado.value
    || (cantidadGrupos.value === 1 ? grupos.value[0] : null),
)
const puedePagarActivo = computed(() => {
  const grupo = grupoActivo.value
  if (!grupo) return false
  return Boolean(grupo.puedeCobrar)
    && (grupo.items || []).every((item) => item.disponible)
})
const totalVisible = computed(() =>
  mostrarIndice.value
    ? Number(total.value || 0)
    : gruposVisibles.value.reduce(
      (suma, grupo) => suma + Number(grupo.total ?? grupo.subtotal ?? 0),
      0,
    ),
)
const avisosVisibles = computed(() => {
  if (mostrarIndice.value) {
    return [...new Set(avisos.value.filter(Boolean))]
  }
  const grupo = grupoActivo.value
  if (!grupo) return [...new Set(avisos.value.filter(Boolean))]
  const propios = (grupo.items || [])
    .filter((item) => !item.disponible)
    .map((item) => item.motivoNoDisponible)
  if (!grupo.puedeCobrar && grupo.motivoNoCobro) {
    propios.push(grupo.motivoNoCobro)
  }
  return [...new Set(propios.filter(Boolean))]
})

function totalGrupo(grupo) {
  return formatearPrecio(grupo.total ?? grupo.subtotal, grupo.moneda || 'ARS')
}

function imagenesGrupo(grupo) {
  return (grupo.items || [])
    .map((item) => item.imagenUrl)
    .filter(Boolean)
    .slice(0, 4)
}

function restantesGrupo(grupo) {
  return Math.max(0, (grupo.items || []).length - 4)
}

function verCarrito(grupo) {
  router.push({ name: 'carrito', query: { artista: grupo.idArtista } })
}

function volverAlIndice() {
  carritoVaciado.value = false
  router.push({ name: 'carrito' })
}

async function eliminar(item) {
  try {
    await quitar(item.idCarritoItem)
    exito('Producto quitado del carrito.')
    if (grupoSeleccionado.value && (grupoSeleccionado.value.items || []).length === 0) {
      volverAlIndice()
    }
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo quitar el producto.')
  }
}

async function limpiar() {
  const grupo = grupoActivo.value
  const soloEsteGrupo = Boolean(grupo) && cantidadGrupos.value > 1
  try {
    if (soloEsteGrupo) {
      const nombre = grupo.nombreArtista
      await vaciar(grupo.idArtista)
      if (grupos.value.length) {
        artistaVaciado.value = nombre
        carritoVaciado.value = true
      }
      exito(`Carrito de @${nombre} vaciado.`)
    } else {
      await vaciar()
      exito('Carrito vaciado.')
    }
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo vaciar el carrito.')
  }
}

async function continuar() {
  if (!puedePagarActivo.value || !grupoActivo.value || procesandoPago.value) return
  const grupo = grupoActivo.value
  const clave = globalThis.crypto?.randomUUID
    ? globalThis.crypto.randomUUID()
    : `compra-${Date.now()}-${Math.random()}`
  grupoPagando.value = grupo
  procesandoPago.value = true
  let compra = null
  let redirigiendo = false
  try {
    compra = await confirmarCheckout(clave, grupo.idArtista)
    const grupoCompra = (compra.grupos || []).find(
      (item) => Number(item.idArtista) === Number(grupo.idArtista),
    ) || (compra.grupos || [])[0]
    if (!grupoCompra) {
      router.push({ name: 'compra-detalle', params: { id: compra.idCompra } })
      return
    }
    const pago = await pagarGrupo(compra.idCompra, grupoCompra.idCompraGrupo)
    if (pago.instruccion && /^https?:\/\//.test(pago.instruccion)) {
      redirigiendo = true
      window.location.href = pago.instruccion
      return
    }
    router.push({ name: 'compra-detalle', params: { id: compra.idCompra } })
  } catch (e) {
    if (compra?.idCompra) {
      toastError(e.response?.data?.message
        || 'No se pudo iniciar el pago. Reintentalo desde el detalle de la compra.')
      router.push({ name: 'compra-detalle', params: { id: compra.idCompra } })
    } else {
      toastError(e.response?.data?.message || 'No se pudo iniciar la compra.')
    }
  } finally {
    if (!redirigiendo) {
      procesandoPago.value = false
      grupoPagando.value = null
    }
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-xl px-4 py-8 sm:px-8 sm:py-10">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <router-link
          to="/productos"
          class="text-sm text-granate transition hover:text-coral"
        >
          ← Seguir explorando productos
        </router-link>
        <h1 class="mt-3 font-display text-2xl font-bold text-granate">
          {{ mostrarIndice
            ? 'Tus carritos'
            : (grupoActivo
              ? `Carrito de @${grupoActivo.nombreArtista}`
              : 'Tu carrito') }}
        </h1>
        <p v-if="mostrarIndice" class="mt-1 text-sm text-cafe/60">
          Tenés productos de {{ cantidadGrupos }} vendedores. Cada tienda se paga por separado.
        </p>
        <p v-else-if="cantidadGrupos > 1" class="mt-1 text-sm text-cafe/60">
          Este pago corresponde solo a este vendedor. Los demás quedan en tu carrito.
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <button
          v-if="!mostrarIndice && cantidadGrupos > 1"
          type="button"
          class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20"
          @click="volverAlIndice"
        >
          ← Volver a los carritos
        </button>
        <button
          v-if="grupos.length"
          type="button"
          class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-cafe transition hover:bg-rosa/20"
          @click="limpiar"
        >
          {{ !mostrarIndice && cantidadGrupos > 1
            ? 'Vaciar este carrito'
            : 'Vaciar carrito' }}
        </button>
      </div>
    </div>

    <CargandoPagina v-if="cargando" tipo="lista" mensaje="Cargando carrito…" :tarjetas="4" />

    <div
      v-else-if="error && !grupos.length"
      class="mt-6 rounded-2xl border border-rojizo/30 bg-rojizo/5 px-5 py-4 text-sm text-rojizo"
    >
      {{ error }}
    </div>

    <div
      v-else-if="!grupos.length"
      class="mt-10 rounded-[22px] border border-rosa/60 bg-superficie px-6 py-14 text-center"
    >
      <p class="font-display text-lg font-bold text-granate">
        Tu carrito está vacío
      </p>
      <p class="mt-2 text-sm text-cafe/60">
        Explorá el catálogo y agregá una variante para empezar.
      </p>
      <router-link
        to="/productos"
        class="mt-5 inline-flex rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-rojizo"
      >
        Ver productos
      </router-link>
    </div>

    <div
      v-else-if="carritoVaciado"
      class="mt-10 rounded-[22px] border border-rosa/60 bg-superficie px-6 py-14 text-center"
    >
      <p class="font-display text-lg font-bold text-granate">
        Vaciaste el carrito de @{{ artistaVaciado }}
      </p>
      <p class="mt-2 text-sm text-cafe/60">
        {{ grupos.length === 1
          ? 'Te queda 1 carrito con productos.'
          : `Te quedan ${grupos.length} carritos con productos.` }}
      </p>
      <div class="mt-5 flex flex-wrap justify-center gap-3">
        <router-link
          to="/productos"
          class="inline-flex rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Ver productos
        </router-link>
        <button
          type="button"
          class="inline-flex rounded-lg border border-rosa px-4 py-2.5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="volverAlIndice"
        >
          Volver a mis carritos
        </button>
      </div>
    </div>

    <div v-else class="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6">
        <div
          v-if="avisosVisibles.length"
          class="rounded-xl border border-coral/25 bg-rosa/20 px-4 py-3 text-sm text-granate"
        >
          <p class="font-semibold">Antes de continuar:</p>
          <ul class="mt-1 list-inside list-disc space-y-1 text-xs">
            <li v-for="aviso in avisosVisibles" :key="aviso">{{ aviso }}</li>
          </ul>
        </div>

        <div v-if="mostrarIndice" class="grid gap-4 sm:grid-cols-2">
          <article
            v-for="grupo in grupos"
            :key="grupo.idArtista"
            class="flex h-full flex-col rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm transition hover:border-coral/40 hover:shadow-[0_10px_28px_rgba(74,29,46,0.08)]"
          >
            <div class="flex items-center gap-3">
              <img
                v-if="grupo.urlFotoArtista"
                :src="grupo.urlFotoArtista"
                alt=""
                class="h-10 w-10 rounded-full object-cover ring-1 ring-borde/50"
              />
              <div class="min-w-0">
                <p class="truncate font-display text-base font-bold text-granate">
                  @{{ grupo.nombreArtista }}
                </p>
                <p class="text-xs text-cafe/55">
                  <span
                    v-if="grupo.esReventa"
                    class="mr-1 rounded-full bg-coral/15 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-[0.08em] text-coral"
                  >
                    Reventa
                  </span>
                  {{ grupo.items.length }} producto(s)
                </p>
              </div>
            </div>

            <div class="mt-4 flex gap-2">
              <div
                v-for="(url, indice) in imagenesGrupo(grupo)"
                :key="`${url}-${indice}`"
                class="h-16 flex-1 overflow-hidden rounded-xl border border-rosa/50 bg-rosa/10"
              >
                <img :src="url" alt="" class="h-full w-full object-cover" />
              </div>
              <div
                v-if="restantesGrupo(grupo)"
                class="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl border border-rosa/50 bg-rosa/20 text-xs font-bold text-granate"
              >
                +{{ restantesGrupo(grupo) }}
              </div>
            </div>

            <p
              v-if="!grupo.puedeCobrar"
              class="mt-3 rounded-lg bg-rojizo/10 px-3 py-2 text-[11px] leading-relaxed text-rojizo"
            >
              {{ grupo.motivoNoCobro }}
            </p>

            <div class="mt-auto flex items-center justify-between gap-3 pt-5">
              <p class="font-display text-lg font-bold text-granate">
                {{ totalGrupo(grupo) }}
              </p>
              <button
                type="button"
                class="rounded-lg bg-coral px-3 py-2 text-xs font-semibold text-white transition hover:bg-rojizo"
                @click="verCarrito(grupo)"
              >
                Ver carrito
              </button>
            </div>
          </article>
        </div>

        <template v-else>
          <section
            v-for="grupo in gruposVisibles"
            :key="grupo.idArtista"
            class="rounded-[22px] border border-borde/60 bg-superficie p-5 shadow-sm"
          >
            <div class="flex flex-wrap items-center justify-between gap-2">
              <router-link
                :to="`/perfil/${grupo.nombreArtista}`"
                class="flex items-center gap-2"
              >
                <img
                  v-if="grupo.urlFotoArtista"
                  :src="grupo.urlFotoArtista"
                  alt=""
                  class="h-8 w-8 rounded-full object-cover ring-1 ring-borde/50"
                />
                <span class="text-sm font-semibold text-cafe/75 transition hover:text-coral">
                  @{{ grupo.nombreArtista }}
                </span>
              </router-link>
              <span
                v-if="grupo.esReventa"
                class="rounded-full bg-coral/15 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-[0.08em] text-coral"
              >
                Reventa
              </span>
              <span
                v-if="!grupo.puedeCobrar"
                class="rounded-full bg-rojizo/10 px-2.5 py-0.5 text-[11px] font-semibold text-rojizo"
              >
                {{ grupo.motivoNoCobro }}
              </span>
            </div>

            <div class="mt-4 divide-y divide-rosa/40">
              <article
                v-for="item in grupo.items"
                :key="item.idCarritoItem"
                class="flex flex-col gap-4 py-4 sm:flex-row sm:items-center"
              >
                <div class="h-20 w-20 shrink-0 overflow-hidden rounded-xl border border-rosa/50 bg-rosa/10">
                  <img
                    v-if="item.imagenUrl"
                    :src="item.imagenUrl"
                    :alt="item.titulo"
                    class="h-full w-full object-cover"
                  />
                </div>

                <div class="min-w-0 flex-1">
                  <router-link
                    :to="item.esReventa
                      ? { name: 'reventa-publica', params: { id: item.idPublicacionReventa } }
                      : { name: 'producto-publico', params: { id: item.idProducto } }"
                    class="font-display text-base font-bold text-granate transition hover:text-coral"
                  >
                    {{ item.titulo }}
                  </router-link>
                  <p class="mt-1 text-sm text-cafe/60">{{ item.nombreVariante }}</p>
                  <p v-if="item.esReventa && item.nombreArtistaOriginal" class="mt-0.5 text-[11px] text-cafe/45">
                    Reventa · Obra original de @{{ item.nombreArtistaOriginal }}
                  </p>
                  <p v-if="!item.disponible" class="mt-1 text-xs text-rojizo">
                    {{ item.motivoNoDisponible }}
                  </p>
                  <p class="mt-2 text-xs text-cafe/55">
                    {{ item.cantidad }} unidad
                    <span v-if="item.motivoCantidadFija" class="text-cafe/40">
                      · {{ item.motivoCantidadFija }}
                    </span>
                  </p>
                </div>

                <div class="flex shrink-0 items-center gap-4">
                  <p class="font-display text-lg font-extrabold text-coral">
                    {{ formatearPrecio(item.precioCobro, item.monedaCobro || 'ARS') }}
                  </p>
                  <button
                    type="button"
                    class="inline-flex items-center gap-1.5 rounded-lg border border-rojizo/40 bg-rojizo/5 px-3 py-1.5 text-xs font-bold text-rojizo transition hover:bg-rojizo/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rojizo/40"
                    @click="eliminar(item)"
                  >
                    <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4 7h16M9 7V4h6v3m-8 0 1 13h8l1-13M10 11v5m4-5v5" />
                    </svg>
                    Quitar
                  </button>
                </div>
              </article>
            </div>

            <div class="mt-3 flex items-center justify-between border-t border-rosa/40 pt-3">
              <span class="text-xs text-cafe/55">Subtotal del vendedor</span>
              <span class="font-display text-base font-bold text-granate">
                {{ totalGrupo(grupo) }}
              </span>
            </div>
          </section>
        </template>
      </div>

      <aside class="lg:sticky lg:top-24 lg:h-fit">
        <section class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-[0_10px_28px_rgba(74,29,46,0.08)]">
          <template v-if="mostrarIndice">
            <h2 class="font-display text-lg font-bold text-granate">
              Resumen del carrito
            </h2>
            <div class="mt-4 space-y-2 text-sm">
              <div class="flex items-center justify-between text-cafe/65">
                <span>Artistas</span>
                <span>{{ cantidadGrupos }}</span>
              </div>
              <div class="flex items-center justify-between text-cafe/65">
                <span>Productos</span>
                <span>{{ grupos.reduce((total, grupo) => total + grupo.items.length, 0) }}</span>
              </div>
              <div class="flex items-center justify-between border-t border-rosa/40 pt-3 text-base font-bold text-granate">
                <span>Total</span>
                <span class="font-display text-xl text-coral">
                  {{ formatearPrecio(totalVisible, 'ARS') }}
                </span>
              </div>
            </div>
            <p class="mt-4 rounded-xl bg-crema/70 px-4 py-3 text-xs leading-relaxed text-cafe/65">
              Los pagos se realizan por separado a cada artista. Elegí uno de
              los carritos para ver el detalle y pagar.
            </p>
          </template>

          <template v-else-if="grupoActivo">
            <h2 class="font-display text-lg font-bold text-granate">
              Resumen de @{{ grupoActivo.nombreArtista }}
            </h2>
            <div class="mt-4 space-y-2 text-sm">
              <div class="flex items-center justify-between text-cafe/65">
                <span>Productos</span>
                <span>{{ grupoActivo.items.length }}</span>
              </div>
              <div class="flex items-center justify-between border-t border-rosa/40 pt-3 text-base font-bold text-granate">
                <span>Total</span>
                <span class="font-display text-xl text-coral">
                  {{ formatearPrecio(totalVisible, 'ARS') }}
                </span>
              </div>
            </div>

            <button
              type="button"
              class="mt-5 flex w-full items-center justify-center rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="!puedePagarActivo || procesandoPago"
              @click="continuar"
            >
              {{ procesandoPago
                ? 'Redirigiendo a MercadoPago...'
                : 'Pagar a @' + grupoActivo.nombreArtista }}
            </button>
            <p v-if="!puedePagarActivo" class="mt-2 text-center text-[11px] leading-relaxed text-cafe/50">
              Revisá los avisos antes de continuar.
            </p>
            <p v-if="cantidadGrupos > 1" class="mt-3 text-center text-[11px] leading-relaxed text-cafe/45">
              Los productos de los otros artistas quedan en tu carrito.
            </p>
            <p v-else class="mt-3 text-center text-[11px] leading-relaxed text-cafe/45">
              Cada artista cobra directo en su cuenta de MercadoPago.
            </p>
          </template>
        </section>
      </aside>
    </div>
    <div
      v-if="procesandoPago"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-granate/40 p-4 backdrop-blur-sm"
      role="status"
      aria-live="polite"
    >
      <div class="w-full max-w-sm rounded-2xl border border-rosa bg-blanco p-6 text-center shadow-2xl">
        <div
          class="mx-auto h-9 w-9 animate-spin rounded-full border-[3px] border-rosa border-t-coral"
          aria-hidden="true"
        ></div>
        <p class="mt-4 font-display text-base font-bold text-granate">
          Te estamos redirigiendo a MercadoPago...
        </p>
        <p class="mt-1 text-sm leading-relaxed text-cafe/65">
          No cierres esta ventana.
          <template v-if="grupoPagando">
            Estamos preparando el pago de @{{ grupoPagando.nombreArtista }}
            ({{ totalGrupo(grupoPagando) }}).
          </template>
        </p>
      </div>
    </div>
  </div>
</template>
