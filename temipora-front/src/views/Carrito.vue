<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCarrito } from '../composables/useCarrito'
import { useNotificaciones } from '../composables/useNotificaciones'
import { formatearPrecio } from '../servicios/moneda'

const router = useRouter()
const { exito, error: toastError } = useNotificaciones()
const {
  cargando,
  error,
  grupos,
  total,
  puedeCheckout,
  avisos,
  cargar,
  actualizar,
  quitar,
  vaciar,
  confirmarCheckout,
} = useCarrito()

const avisosUnicos = computed(() => [...new Set(avisos.value.filter(Boolean))])
const cantidadGrupos = computed(() => grupos.value.length)

function totalGrupo(grupo) {
  return formatearPrecio(grupo.total ?? grupo.subtotal, grupo.moneda || 'ARS')
}

async function cambiarCantidad(item, paso) {
  if (item.esUnica) return
  const nueva = Number(item.cantidad || 0) + paso
  if (nueva < 1) return
  try {
    await actualizar(item.idCarritoItem, nueva)
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo actualizar la cantidad.')
  }
}

async function eliminar(item) {
  try {
    await quitar(item.idCarritoItem)
    exito('Producto quitado del carrito.')
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo quitar el producto.')
  }
}

async function limpiar() {
  try {
    await vaciar()
    exito('Carrito vaciado.')
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo vaciar el carrito.')
  }
}

async function continuar() {
  if (!puedeCheckout.value) return
  const clave = globalThis.crypto?.randomUUID
    ? globalThis.crypto.randomUUID()
    : `compra-${Date.now()}-${Math.random()}`
  try {
    const compra = await confirmarCheckout(clave)
    router.push({ name: 'compra-detalle', params: { id: compra.idCompra } })
  } catch (e) {
    toastError(e.response?.data?.message || 'No se pudo iniciar la compra.')
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
          Tu carrito
        </h1>
        <p v-if="cantidadGrupos > 1" class="mt-1 text-sm text-cafe/60">
          Se procesará un pago por artista. Podés pagar cada grupo por separado.
        </p>
      </div>
      <button
        v-if="grupos.length"
        type="button"
        class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-cafe transition hover:bg-rosa/20"
        @click="limpiar"
      >
        Vaciar carrito
      </button>
    </div>

    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando...</p>

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

    <div v-else class="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6">
        <div v-if="avisosUnicos.length" class="rounded-xl border border-coral/25 bg-rosa/20 px-4 py-3 text-sm text-granate">
          <p class="font-semibold">Antes de continuar:</p>
          <ul class="mt-1 list-inside list-disc space-y-1 text-xs">
            <li v-for="aviso in avisosUnicos" :key="aviso">{{ aviso }}</li>
          </ul>
        </div>

        <section
          v-for="grupo in grupos"
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
                  :to="{ name: 'producto-publico', params: { id: item.idProducto } }"
                  class="font-display text-base font-bold text-granate transition hover:text-coral"
                >
                  {{ item.titulo }}
                </router-link>
                <p class="mt-1 text-sm text-cafe/60">{{ item.nombreVariante }}</p>
                <p v-if="!item.disponible" class="mt-1 text-xs text-rojizo">
                  {{ item.motivoNoDisponible }}
                </p>
                <div class="mt-2 flex flex-wrap items-center gap-3">
                  <div
                    v-if="item.cantidadEditable"
                    class="inline-flex items-center rounded-lg border border-rosa/70"
                  >
                    <button
                      type="button"
                      class="px-2.5 py-1 text-sm text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                      :disabled="item.cantidad <= 1"
                      @click="cambiarCantidad(item, -1)"
                    >
                      −
                    </button>
                    <span class="min-w-8 text-center text-sm font-semibold text-cafe">
                      {{ item.cantidad }}
                    </span>
                    <button
                      type="button"
                      class="px-2.5 py-1 text-sm text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                      @click="cambiarCantidad(item, 1)"
                    >
                      +
                    </button>
                  </div>
                  <p v-else class="text-xs text-cafe/55">
                    {{ item.cantidad }} unidad
                    <span v-if="item.motivoCantidadFija" class="text-cafe/40">
                      · {{ item.motivoCantidadFija }}
                    </span>
                  </p>
                  <button
                    type="button"
                    class="text-xs font-medium text-cafe/55 transition hover:text-rojizo"
                    @click="eliminar(item)"
                  >
                    Quitar
                  </button>
                </div>
              </div>

              <div class="shrink-0 text-right">
                <p class="font-display text-lg font-extrabold text-coral">
                  {{ formatearPrecio(item.precioCobro, item.monedaCobro || 'ARS') }}
                </p>
                <p v-if="item.precioCobro" class="text-[11px] text-cafe/45">
                  {{ item.cantidad }} ×
                  {{ formatearPrecio(item.precioCobro, item.monedaCobro || 'ARS') }}
                </p>
              </div>
            </article>
          </div>

          <div class="mt-3 flex items-center justify-between border-t border-rosa/40 pt-3">
            <span class="text-xs text-cafe/55">Subtotal del artista</span>
            <span class="font-display text-base font-bold text-granate">
              {{ totalGrupo(grupo) }}
            </span>
          </div>
        </section>
      </div>

      <aside class="lg:sticky lg:top-24 lg:h-fit">
        <section class="rounded-[22px] border border-borde/60 bg-superficie p-6 shadow-[0_10px_28px_rgba(74,29,46,0.08)]">
          <h2 class="font-display text-lg font-bold text-granate">Resumen</h2>
          <div class="mt-4 space-y-2 text-sm">
            <div class="flex items-center justify-between text-cafe/65">
              <span>Productos</span>
              <span>{{ grupos.reduce((total, grupo) => total + grupo.items.length, 0) }}</span>
            </div>
            <div class="flex items-center justify-between border-t border-rosa/40 pt-3 text-base font-bold text-granate">
              <span>Total</span>
              <span class="font-display text-xl text-coral">
                {{ formatearPrecio(total, 'ARS') }}
              </span>
            </div>
          </div>

          <button
            type="button"
            class="mt-5 flex w-full items-center justify-center rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!puedeCheckout"
            @click="continuar"
          >
            Continuar con el pago
          </button>
          <p v-if="!puedeCheckout" class="mt-2 text-center text-[11px] leading-relaxed text-cafe/50">
            Revisá los avisos antes de continuar.
          </p>
          <p class="mt-3 text-center text-[11px] leading-relaxed text-cafe/45">
            Cada artista cobra directo en su cuenta de MercadoPago.
          </p>
        </section>
      </aside>
    </div>
  </div>
</template>
