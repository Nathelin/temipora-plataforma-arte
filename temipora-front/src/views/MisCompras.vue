<script setup>
import { onMounted, ref } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { estadoCompraDe, listarCompras } from '../servicios/compras'

const compras = ref([])
const cargando = ref(true)
const error = ref('')
const pagina = ref(0)
const totalPaginas = ref(0)
const hayMas = ref(false)

async function cargar(nuevaPagina = pagina.value) {
  cargando.value = true
  error.value = ''
  try {
    const data = await listarCompras({ pagina: nuevaPagina, tamanio: 12 })
    compras.value = data.contenido || []
    pagina.value = data.pagina ?? nuevaPagina
    totalPaginas.value = data.totalPaginas || 0
    hayMas.value = Boolean(data.hayMas)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus compras.'
  } finally {
    cargando.value = false
  }
}

function irA(nuevaPagina) {
  if (nuevaPagina < 0 || nuevaPagina >= totalPaginas.value) return
  cargar(nuevaPagina)
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

onMounted(() => cargar(0))
</script>

<template>
  <div class="mx-auto w-full max-w-screen-lg px-4 py-8 sm:px-8 sm:py-10">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="font-display text-2xl font-bold text-granate">Mis compras</h1>
        <p class="mt-1 text-sm text-cafe/60">
          Compras de productos de la tienda. Los pedidos de servicios están en
          “Mis pedidos”.
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
    <p v-else-if="error" class="mt-8 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
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

    <div v-else class="mt-8 space-y-3">
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

      <div v-if="totalPaginas > 1" class="flex items-center justify-between pt-3">
        <button
          type="button"
          class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="pagina === 0"
          @click="irA(pagina - 1)"
        >
          ← Anterior
        </button>
        <span class="text-xs text-cafe/55">
          Página {{ pagina + 1 }} de {{ totalPaginas }}
        </span>
        <button
          type="button"
          class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="!hayMas"
          @click="irA(pagina + 1)"
        >
          Siguiente →
        </button>
      </div>
    </div>
  </div>
</template>
