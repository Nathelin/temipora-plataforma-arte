<script setup>
import { onMounted, ref } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { estadoCompraDe, listarCompras } from '../servicios/compras'

const compras = ref([])
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const pagina = ref(0)
const hayMas = ref(false)

async function cargar({ append = false } = {}) {
  const siguiente = append ? pagina.value + 1 : 0
  if (append) cargandoMas.value = true
  else cargando.value = true
  error.value = ''
  try {
    const data = await listarCompras({ pagina: siguiente, tamanio: 12 })
    const contenido = data.contenido || []
    compras.value = append ? [...compras.value, ...contenido] : contenido
    pagina.value = data.pagina ?? siguiente
    hayMas.value = Boolean(data.hayMas)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus compras.'
  } finally {
    cargando.value = false
    cargandoMas.value = false
  }
}

function cargarMas() {
  if (cargandoMas.value || !hayMas.value) return
  cargar({ append: true })
}

function fecha(valor) {
  if (!valor) return '—'
  try {
    return new Intl.DateTimeFormat('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(valor))
  } catch {
    return valor
  }
}

onMounted(() => cargar())
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="font-display text-2xl font-bold text-granate">Mis compras</h1>
        <p class="mt-1 text-sm text-cafe/60">
          Tus compras de productos de la tienda.
        </p>
      </div>
      <router-link
        to="/productos"
        class="text-sm font-medium text-coral transition hover:text-rojizo"
      >
        Ver productos →
      </router-link>
    </div>

    <p v-if="cargando" class="py-16 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error && !compras.length" class="mt-8 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
      {{ error }}
    </p>

    <div
      v-else-if="!compras.length"
      class="mt-10 rounded-[22px] border border-rosa/60 bg-superficie px-6 py-14 text-center"
    >
      <p class="font-display text-lg font-bold text-granate">
        Todavía no hiciste compras de productos
      </p>
      <p class="mt-2 text-sm text-cafe/60">
        Cuando compres una pieza o licencia, va a aparecer acá.
      </p>
      <router-link
        to="/productos"
        class="mt-5 inline-flex rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-rojizo"
      >
        Explorar productos
      </router-link>
    </div>

    <p
      v-if="error && compras.length"
      class="mt-6 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo"
    >
      {{ error }}
    </p>

    <div v-if="!cargando && compras.length" class="mt-8 space-y-3">
      <router-link
        v-for="compra in compras"
        :key="compra.idCompra"
        :to="{ name: 'compra-detalle', params: { id: compra.idCompra } }"
        class="flex flex-col gap-4 rounded-[18px] border border-borde/60 bg-superficie p-5 transition hover:border-coral/40 hover:shadow-[0_10px_28px_rgba(74,29,46,0.08)] sm:flex-row sm:items-center sm:justify-between"
      >
        <div class="flex flex-wrap items-center gap-3">
          <span
            class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
            :class="estadoCompraDe(compra.estado).clase"
          >
            {{ estadoCompraDe(compra.estado).texto }}
          </span>
          <div>
            <p class="font-display text-base font-bold text-granate">
              Compra #{{ compra.idCompra }}
            </p>
            <p class="mt-0.5 text-xs text-cafe/55">
              {{ fecha(compra.fechaCreacion) }} ·
              {{ compra.cantidadItems }} ítem(s) ·
              {{ compra.cantidadGrupos }} artista(s)
            </p>
          </div>
        </div>

        <div class="flex items-center justify-between gap-4 sm:justify-end">
          <p class="font-display text-lg font-extrabold text-coral">
            {{ formatearPrecio(compra.total, compra.moneda || 'ARS') }}
          </p>
          <span class="rounded-lg border border-rosa px-3 py-1.5 text-xs font-semibold text-granate">
            {{ compra.puedePagar ? 'Pagar' : 'Ver detalle' }}
          </span>
        </div>
      </router-link>

      <div v-if="hayMas" class="flex justify-center pt-3">
        <button
          type="button"
          class="inline-flex items-center justify-center rounded-lg border border-rosa/60 px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="cargandoMas"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
      </div>
    </div>
  </div>
</template>
