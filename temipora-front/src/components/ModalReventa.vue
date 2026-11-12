<script setup>
import { computed, ref, watch } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { calcularLiquidacionProducto } from '../servicios/liquidacion'
import { editarPrecioReventa, publicarReventa } from '../servicios/reventas'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  pieza: { type: Object, default: null },
  publicacion: { type: Object, default: null },
  comisionPorcentaje: { type: Number, default: null },
  arancelPorcentaje: { type: Number, default: null },
})

const emit = defineEmits(['cerrar', 'guardado'])

const precio = ref('')
const guardando = ref(false)
const error = ref('')

const esEdicion = computed(() => !!props.publicacion)
const titulo = computed(() => props.pieza?.titulo || props.publicacion?.titulo || '')
const variante = computed(() => {
  if (props.pieza) return props.pieza.nombreVariante || ''
  return props.publicacion?.tituloOriginal
    || props.publicacion?.nombreVariante
    || ''
})
const imagen = computed(() => props.pieza?.imagenUrl || props.publicacion?.imagenUrl || '')
const moneda = computed(() => props.pieza?.moneda || props.publicacion?.moneda || 'ARS')
const nombreArtista = computed(
  () => props.pieza?.nombreArtistaOriginal || props.publicacion?.nombreArtistaOriginal || '',
)
const tope = computed(() => {
  const valor = esEdicion.value
    ? (props.publicacion?.precioTope ?? props.publicacion?.precio)
    : props.pieza?.precioTope
  return valor != null ? Number(valor) : null
})
const precioInicial = computed(() => {
  if (esEdicion.value) return props.publicacion?.precio
  return props.pieza?.ultimoPrecio
})

const liquidacion = computed(() => calcularLiquidacionProducto({
  precio: Number(precio.value),
  comisionPorcentaje: props.comisionPorcentaje,
  arancelPorcentaje: props.arancelPorcentaje,
}))

function formatear(valor) {
  return valor != null ? formatearPrecio(valor, moneda.value) : '—'
}

function cerrar() {
  if (guardando.value) return
  emit('cerrar')
}

function alEntrar() {
  precio.value = precioInicial.value != null ? String(precioInicial.value) : ''
  error.value = ''
}

watch(() => props.abierto, (abierto) => {
  if (abierto) alEntrar()
})

async function guardar() {
  const valor = Number(precio.value)
  if (!Number.isFinite(valor) || valor <= 0) {
    error.value = 'Ingresá un precio mayor a cero.'
    return
  }
  if (tope.value != null && valor > tope.value) {
    error.value = `El precio máximo para esta pieza es ${formatear(tope.value)} (precio original de publicación).`
    return
  }
  guardando.value = true
  error.value = ''
  try {
    if (esEdicion.value) {
      await editarPrecioReventa(props.publicacion.idPublicacionReventa, valor)
    } else {
      await publicarReventa(props.pieza.idCompraItem, valor)
    }
    emit('guardado')
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo guardar la publicación de reventa.'
  } finally {
    guardando.value = false
  }
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-modal-reventa"
  >
    <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrar"></div>

    <div class="relative w-full max-w-2xl overflow-hidden rounded-2xl border border-rosa bg-blanco shadow-xl shadow-granate/20">
      <div class="border-b border-rosa/40 px-6 py-4">
        <h2 id="titulo-modal-reventa" class="font-display text-lg font-bold text-granate">
          {{ esEdicion ? 'Editar precio de reventa' : 'Poner en reventa' }}
        </h2>
        <p class="mt-1 text-sm text-cafe/60">
          {{ esEdicion
            ? 'Podés cambiar el precio mientras la publicación esté activa.'
            : 'La pieza, la licencia y la autoría ya están registradas: solo elegís el precio.' }}
        </p>
      </div>

      <div class="max-h-[70vh] overflow-y-auto px-6 py-5">
        <div class="flex flex-col gap-4 sm:flex-row">
          <img
            v-if="imagen"
            :src="imagen"
            :alt="titulo"
            class="h-24 w-24 shrink-0 rounded-xl border border-rosa/50 object-cover"
          />
          <div class="min-w-0">
            <h3 class="font-display text-base font-bold text-granate">{{ titulo }}</h3>
            <p v-if="variante" class="text-sm text-cafe/65">{{ variante }}</p>
            <p v-if="nombreArtista" class="mt-1 text-xs text-cafe/50">
              Obra original de @{{ nombreArtista }}
            </p>
            <p class="mt-3 text-sm text-cafe/70">
              Precio máximo: <strong class="text-granate">{{ formatear(tope) }}</strong>
              <span class="block text-xs text-cafe/45">Es el precio original de publicación de la pieza.</span>
            </p>
          </div>
        </div>

        <label class="mt-5 block text-sm font-medium text-granate">
          Precio de reventa
          <div class="mt-1 flex items-center rounded-lg bg-superficie shadow-sm ring-1 ring-inset ring-rosa focus-within:ring-2 focus-within:ring-coral">
            <span class="pl-3 text-sm text-cafe/60">{{ moneda }}</span>
            <input
              v-model="precio"
              type="number"
              min="0.01"
              step="0.01"
              :max="tope ?? undefined"
              class="w-full border-0 bg-transparent px-2 py-2.5 text-sm text-cafe focus:outline-none"
              placeholder="0.00"
              @keydown.enter="guardar"
            />
          </div>
        </label>

        <div
          v-if="liquidacion && comisionPorcentaje != null"
          class="mt-4 rounded-xl border border-rosa/50 bg-rosa/10 px-4 py-3 text-sm"
        >
          <p class="flex items-center justify-between gap-3 text-cafe/75">
            <span>Comisión Temipora ({{ comisionPorcentaje }}%)</span>
            <span class="font-medium text-granate">−{{ formatear(liquidacion.comision) }}</span>
          </p>
          <template v-if="arancelPorcentaje != null">
            <p class="mt-1 flex items-center justify-between gap-3 text-cafe/75">
              <span>Arancel estimado de MercadoPago ({{ arancelPorcentaje }}%)</span>
              <span class="font-medium text-granate">−{{ formatear(liquidacion.arancel) }}</span>
            </p>
            <p class="mt-2 flex items-center justify-between gap-3 border-t border-rosa/50 pt-2 font-semibold text-granate">
              <span>Recibís aproximadamente</span>
              <span class="text-base text-coral">{{ formatear(liquidacion.recibis) }}</span>
            </p>
          </template>
          <p v-else class="mt-2 text-xs leading-relaxed text-cafe/55">
            El arancel de MercadoPago se descuenta aparte. El monto final se
            informa al momento del pago.
          </p>
        </div>
        <p
          v-else-if="Number(precio) > 0"
          class="mt-4 rounded-xl border border-rosa/50 bg-rosa/10 px-4 py-3 text-xs leading-relaxed text-cafe/60"
        >
          No pudimos calcular la liquidación en este momento. El monto final se
          informa al momento del pago.
        </p>

        <p class="mt-4 rounded-xl bg-superficie px-4 py-3 text-xs leading-relaxed text-cafe/65">
          El comprador paga por MercadoPago y el dinero se acredita en tu cuenta.
          Podés archivar la publicación cuando quieras: la pieza vuelve a
          «Puedo revender» y el historial queda guardado.
        </p>

        <p v-if="error" class="mt-4 text-sm text-rojizo" role="alert">{{ error }}</p>
      </div>

      <div class="flex flex-col-reverse gap-3 border-t border-rosa/40 px-6 py-4 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
          :disabled="guardando"
          @click="cerrar"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="rounded-md bg-coral px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
          :disabled="guardando"
          @click="guardar"
        >
          {{ guardando ? 'Guardando...' : (esEdicion ? 'Guardar precio' : 'Publicar reventa') }}
        </button>
      </div>
    </div>
  </div>
</template>
