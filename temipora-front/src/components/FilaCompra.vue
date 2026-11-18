<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { estadoCompraDe } from '../servicios/compras'

const props = defineProps({
  compra: { type: Object, required: true },
  descargando: { type: Number, default: null },
})

defineEmits(['descargar'])

const detalle = computed(() => ({
  name: 'compra-detalle',
  params: { id: props.compra.idCompra },
}))
const estado = computed(() => estadoCompraDe(props.compra.estado))
const previews = computed(() => props.compra.itemsPreview || [])
const totalProductos = computed(() => Number(
  props.compra.cantidadProductos || props.compra.cantidadItems || 0,
))
const extra = computed(() => Math.max(0, totalProductos.value - previews.value.length))
const fecha = computed(() => {
  const valor = props.compra.fechaCreacion
  if (!valor) return '—'
  try {
    return new Intl.DateTimeFormat('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(valor))
  } catch {
    return valor
  }
})
</script>

<template>
  <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 transition hover:border-coral/40 hover:shadow-[0_10px_28px_rgba(74,29,46,0.08)]">
    <router-link :to="detalle" class="block">
      <div class="flex items-start gap-3">
        <div class="flex shrink-0 -space-x-2">
          <template v-if="previews.length">
            <template v-for="(preview, index) in previews" :key="preview.idCompraItem">
              <img
                v-if="preview.imagenUrl"
                :src="preview.imagenUrl"
                alt=""
                class="h-12 w-12 rounded-lg border-2 border-blanco object-cover shadow-sm"
                :style="{ zIndex: previews.length - index }"
              />
              <span
                v-else
                class="flex h-12 w-12 items-center justify-center rounded-lg border-2 border-blanco bg-rosa/30 text-granate shadow-sm"
                :style="{ zIndex: previews.length - index }"
                aria-hidden="true"
              >
                <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007Z" />
                </svg>
              </span>
            </template>
            <span
              v-if="extra > 0"
              class="flex h-12 min-w-12 items-center justify-center rounded-lg border-2 border-blanco bg-granate px-1.5 text-[11px] font-bold text-white shadow-sm"
            >
              +{{ extra }}
            </span>
          </template>
          <span
            v-else
            class="flex h-12 w-12 items-center justify-center rounded-lg bg-rosa/30 text-granate"
            aria-hidden="true"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007Z" />
            </svg>
          </span>
        </div>

        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <p class="font-display text-base font-bold text-granate">
              Compra #{{ compra.idCompra }}
            </p>
            <span
              class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
              :class="estado.clase"
            >
              {{ estado.texto }}
            </span>
          </div>
          <p class="mt-0.5 text-xs text-cafe/55">
            {{ fecha }} · {{ totalProductos }} producto(s) ·
            {{ compra.cantidadGrupos }} artista(s)
          </p>
        </div>
      </div>
    </router-link>

    <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
      <p class="font-display text-lg font-extrabold text-coral">
        {{ formatearPrecio(compra.total, compra.moneda || 'ARS') }}
      </p>
      <div class="flex flex-wrap items-center gap-2">
        <button
          v-if="compra.puedeDescargar"
          type="button"
          class="inline-flex min-h-8 items-center gap-1 rounded-full border border-rosa bg-blanco px-3 text-xs font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
          :disabled="descargando === compra.idCompra"
          @click="$emit('descargar')"
        >
          {{ descargando === compra.idCompra ? 'Preparando...' : 'Descargar' }}
        </button>
        <router-link
          :to="detalle"
          class="inline-flex min-h-8 items-center gap-1 rounded-full px-3 text-xs font-semibold transition"
          :class="compra.puedePagar
            ? 'bg-coral text-white hover:bg-rojizo'
            : 'border border-rosa text-granate hover:bg-rosa/20'"
        >
          {{ compra.puedePagar ? 'Pagar' : 'Ver detalle' }}
        </router-link>
      </div>
    </div>
  </div>
</template>
