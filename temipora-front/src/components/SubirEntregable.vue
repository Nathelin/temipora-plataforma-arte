<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import {
  MAX_ARCHIVOS_ENTREGA,
  MAX_BYTES_TOTAL_ENTREGA,
  crearEntrega,
  validarArchivoEntrega,
} from '../servicios/entregas'
import ModalConfirmacion from './ModalConfirmacion.vue'

const props = defineProps({
  idPedido: { type: Number, required: true },
  hito: { type: Object, required: true },
  dibujos: { type: Array, default: () => [] },
})

const emit = defineEmits(['entregado'])

const descripcion = ref('')
const seleccionados = ref([])
const error = ref('')
const subiendo = ref(false)
const confirmacionAbierta = ref(false)

const mensajeConfirmacion = computed(() => {
  const cantidad = seleccionados.value.length
  const archivos = cantidad === 1 ? '1 archivo' : `${cantidad} archivos`
  if (props.hito.tipo === 'ENTREGA') {
    return `Vas a entregar ${archivos} como versión final. El cliente verá la preview protegida y el original se libera al pagar la última cuota.`
  }
  return `Vas a entregar ${archivos}. El cliente podrá aprobar el lote o pedir una revisión.`
})

function etiquetaDibujo(dibujo) {
  const numero = (dibujo.orden ?? 0) + 1
  return `Dibujo ${numero} — ${dibujo.nombre || 'Sin nombre'}`
}

function peso(bytes) {
  return `${(Number(bytes || 0) / 1024 / 1024).toFixed(1)} MB`
}

function agregar(event) {
  error.value = ''
  const archivos = Array.from(event.target.files || [])
  event.target.value = ''
  if (!archivos.length) return

  if (seleccionados.value.length + archivos.length > MAX_ARCHIVOS_ENTREGA) {
    error.value = `Podés subir hasta ${MAX_ARCHIVOS_ENTREGA} archivos por entrega.`
    return
  }

  const totalActual = seleccionados.value
    .reduce((total, archivo) => total + archivo.file.size, 0)
  const totalNuevo = archivos
    .reduce((total, archivo) => total + archivo.size, 0)
  if (totalActual + totalNuevo > MAX_BYTES_TOTAL_ENTREGA) {
    error.value = 'La entrega no puede superar los 100 MB en total.'
    return
  }

  for (const archivo of archivos) {
    const mensaje = validarArchivoEntrega(archivo)
    if (mensaje) {
      error.value = `${archivo.name}: ${mensaje}`
      continue
    }
    seleccionados.value.push({
      file: archivo,
      idItemSolicitud: '',
      previewUrl: URL.createObjectURL(archivo),
    })
  }
}

function quitar(indice) {
  const [archivo] = seleccionados.value.splice(indice, 1)
  if (archivo?.previewUrl) URL.revokeObjectURL(archivo.previewUrl)
}

function limpiar() {
  seleccionados.value.forEach((archivo) => {
    if (archivo.previewUrl) URL.revokeObjectURL(archivo.previewUrl)
  })
  seleccionados.value = []
}

function enviar() {
  if (subiendo.value) return
  error.value = ''
  if (!seleccionados.value.length) {
    error.value = 'Seleccioná al menos un archivo.'
    return
  }
  confirmacionAbierta.value = true
}

async function confirmarEnvio() {
  confirmacionAbierta.value = false
  subiendo.value = true
  error.value = ''
  try {
    const res = await crearEntrega(
      props.idPedido,
      props.hito.idHitoPedido,
      descripcion.value,
      seleccionados.value,
    )
    emit('entregado', res.data)
    descripcion.value = ''
    limpiar()
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo subir la entrega.'
  } finally {
    subiendo.value = false
  }
}

onBeforeUnmount(limpiar)
</script>

<template>
  <div class="mt-4 rounded-xl border border-rosa/50 bg-rosa/5 p-4">
    <h4 class="text-sm font-semibold text-granate">
      {{ hito.tipo === 'ENTREGA' ? 'Subir entrega final' : 'Subir entregable' }}
    </h4>
    <p class="mt-1 text-xs leading-relaxed text-cafe/55">
      <template v-if="hito.tipo === 'ENTREGA'">
        El cliente verá el archivo recién cuando pague la última cuota.
      </template>
      <template v-else>
        Podés subir varios archivos y asociarlos a cada dibujo. El cliente aprueba el lote.
      </template>
    </p>

    <label :for="`archivos-${hito.idHitoPedido}`" class="mt-3 block text-xs text-cafe/60">
      Archivos <span class="text-rojizo">*</span>
      <span class="text-cafe/40">(hasta {{ MAX_ARCHIVOS_ENTREGA }}, 25 MB cada uno)</span>
    </label>
    <input
      :id="`archivos-${hito.idHitoPedido}`"
      type="file"
      accept=".jpg,.jpeg,.png,.webp"
      multiple
      class="mt-1 block w-full text-sm text-cafe file:mr-3 file:rounded-full file:border-0 file:bg-coral/10 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-coral hover:file:bg-coral/20"
      @change="agregar"
    />

    <ul v-if="seleccionados.length" class="mt-3 space-y-2">
      <li
        v-for="(archivo, indice) in seleccionados"
        :key="`${archivo.file.name}-${indice}`"
        class="grid gap-2 rounded-xl border border-rosa/40 bg-blanco p-2 sm:grid-cols-[4rem_minmax(0,1fr)_minmax(0,14rem)_auto] sm:items-center"
      >
        <img
          :src="archivo.previewUrl"
          :alt="archivo.file.name"
          class="h-16 w-16 rounded-lg border border-rosa/40 object-cover"
        />
        <div class="min-w-0">
          <p class="truncate text-xs font-semibold text-cafe">{{ archivo.file.name }}</p>
          <p class="text-[11px] text-cafe/50">{{ peso(archivo.file.size) }}</p>
        </div>
        <select
          v-model="archivo.idItemSolicitud"
          class="block w-full rounded-lg border-0 bg-blanco px-2 py-1.5 text-xs text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option value="">Sin dibujo específico</option>
          <option
            v-for="dibujo in dibujos"
            :key="dibujo.idItemSolicitud"
            :value="dibujo.idItemSolicitud"
          >
            {{ etiquetaDibujo(dibujo) }}
          </option>
        </select>
        <button
          type="button"
          class="inline-flex min-h-8 items-center justify-center rounded-full border border-rojizo/30 px-3 text-xs font-semibold text-rojizo transition hover:bg-rojizo/10"
          @click="quitar(indice)"
        >
          Quitar
        </button>
      </li>
    </ul>

    <label :for="`descripcion-entrega-${hito.idHitoPedido}`" class="mt-3 block text-xs text-cafe/60">
      Descripción <span class="text-cafe/40">(opcional)</span>
    </label>
    <textarea
      :id="`descripcion-entrega-${hito.idHitoPedido}`"
      v-model="descripcion"
      rows="2"
      maxlength="500"
      placeholder="Ej. Bocetos de los tres dibujos, listos para revisión."
      class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
    ></textarea>

    <p
      v-if="error"
      class="mt-3 rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-xs text-rojizo"
      role="alert"
    >
      {{ error }}
    </p>

    <button
      type="button"
      :disabled="subiendo || !seleccionados.length"
      class="mt-3 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
      @click="enviar"
    >
      {{ subiendo ? 'Subiendo...' : 'Entregar' }}
    </button>

    <ModalConfirmacion
      :abierto="confirmacionAbierta"
      titulo="¿Confirmás la entrega?"
      :mensaje="mensajeConfirmacion"
      texto-confirmar="Sí, entregar"
      @confirmar="confirmarEnvio"
      @cerrar="confirmacionAbierta = false"
    />
  </div>
</template>
