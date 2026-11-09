<script setup>
import { computed, ref } from 'vue'
import { acceptImagenes, validarImagen } from '../servicios/archivos'
import { formatearPrecio } from '../servicios/moneda'
import {
  esIlimitada,
  esPiezaUnica,
  etiquetaStock,
  estadoVarianteDe,
  subirImagenProducto,
} from '../servicios/productos'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
  permiteReventa: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const cantidadPlancha = ref(3)
const subiendo = ref(null)
const error = ref('')

const variantes = computed(() => props.modelValue)

function actualizar(indice, campo, valor) {
  const copia = props.modelValue.map((variante, i) =>
    i === indice ? { ...variante, [campo]: valor } : variante,
  )
  emit('update:modelValue', copia)
}

function nuevaVariante(overrides = {}) {
  return {
    nombre: 'Variante',
    descripcion: '',
    precio: '',
    stockDisponible: 1,
    estado: 'DISPONIBLE',
    urlImagen: '',
    sha256: '',
    claveArchivoOriginal: '',
    ...overrides,
  }
}

function agregarVariante() {
  const total = props.modelValue.length
  emit('update:modelValue', [
    ...props.modelValue,
    nuevaVariante({
      nombre: total === 0 ? 'Única' : `Variante ${total + 1}`,
    }),
  ])
}

function generarPlancha() {
  const cantidad = Math.max(1, Math.min(50, Number(cantidadPlancha.value) || 1))
  const base = props.modelValue.length
  const nuevas = Array.from({ length: cantidad }, (_, i) =>
    nuevaVariante({
      nombre: `Adoptable ${base + i + 1}`,
      stockDisponible: 1,
    }),
  )
  emit('update:modelValue', [...props.modelValue, ...nuevas])
}

function quitarVariante(indice) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== indice))
}

function moverVariante(indice, paso) {
  const destino = indice + paso
  if (destino < 0 || destino >= props.modelValue.length) return
  const copia = [...props.modelValue]
  const [movida] = copia.splice(indice, 1)
  copia.splice(destino, 0, movida)
  emit('update:modelValue', copia)
}

function alternarIlimitado(indice) {
  const variante = props.modelValue[indice]
  actualizar(indice, 'stockDisponible', esIlimitada(variante) ? 1 : null)
}

function onStockInput(indice, event) {
  const valor = event.target.value
  actualizar(indice, 'stockDisponible', valor === '' ? 1 : Number(valor))
}

function precioVisible(variante) {
  const numero = Number(variante.precio)
  return Number.isFinite(numero) && numero > 0
    ? formatearPrecio(numero, props.moneda)
    : 'Sin precio'
}

async function subirImagen(event, indice) {
  const archivo = event.target.files?.[0] || null
  event.target.value = ''
  if (!archivo) return

  const problema = validarImagen(archivo)
  if (problema) {
    error.value = problema
    return
  }

  subiendo.value = indice
  error.value = ''
  try {
    const data = await subirImagenProducto(archivo)
    const copia = props.modelValue.map((variante, i) =>
      i === indice
        ? {
            ...variante,
            urlImagen: data.url,
            sha256: data.sha256,
            claveArchivoOriginal: data.claveOriginal,
          }
        : variante,
    )
    emit('update:modelValue', copia)
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo subir la imagen de la variante.'
  } finally {
    subiendo.value = null
  }
}
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-col gap-3 rounded-2xl border border-rosa/60 bg-crema/50 p-4 sm:flex-row sm:items-end sm:justify-between">
      <div class="flex flex-1 flex-col gap-3 sm:flex-row sm:items-end">
        <div class="w-full sm:w-40">
          <label class="mb-1.5 block text-xs font-medium text-cafe/70">
            Plancha de adoptables
          </label>
          <input
            v-model.number="cantidadPlancha"
            type="number"
            min="1"
            max="50"
            class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
          />
        </div>
        <button
          type="button"
          class="rounded-lg border border-coral/40 bg-blanco px-4 py-2 text-sm font-medium text-granate transition hover:bg-rosa/20"
          @click="generarPlancha"
        >
          Generar plancha
        </button>
        <button
          type="button"
          class="rounded-lg bg-coral px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
          @click="agregarVariante"
        >
          Agregar variante
        </button>
      </div>
      <p class="max-w-sm text-xs leading-relaxed text-cafe/55">
        Cada producto tiene una o más variantes. Una plancha genera una variante
        con stock 1 por cada adoptable.
      </p>
    </div>

    <p
      v-if="permiteReventa"
      class="rounded-xl border border-coral/20 bg-rosa/20 px-4 py-3 text-xs leading-relaxed text-granate"
    >
      La reventa se habilita solo para variantes con stock 1. Las variantes
      ilimitadas o con stock mayor a 1 se venden como licencia y no se revenden.
    </p>

    <p v-if="error" class="rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
      {{ error }}
    </p>

    <div
      v-if="!variantes.length"
      class="rounded-2xl border border-dashed border-rosa/70 px-4 py-10 text-center"
    >
      <p class="text-sm font-medium text-granate">Todavía no hay variantes.</p>
      <p class="mt-1 text-sm text-cafe/50">
        Agregá una variante única o generá una plancha de adoptables.
      </p>
    </div>

    <article
      v-for="(variante, indice) in variantes"
      :key="variante.idVariante || `nueva-${indice}`"
      class="rounded-2xl border border-rosa/60 bg-blanco p-5 shadow-sm"
    >
      <header class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-display text-base font-bold text-granate">
            Variante {{ indice + 1 }}
          </h3>
          <span
            class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
            :class="estadoVarianteDe(variante.estado).clase"
          >
            {{ estadoVarianteDe(variante.estado).texto }}
          </span>
          <span
            class="rounded-full bg-crema px-2.5 py-0.5 text-[11px] font-semibold text-cafe/65"
          >
            {{ etiquetaStock(variante) }}
          </span>
          <span
            v-if="esPiezaUnica(variante)"
            class="rounded-full bg-mantequilla/60 px-2.5 py-0.5 text-[11px] font-bold text-granate"
          >
            Pieza única
          </span>
        </div>

        <div class="flex items-center gap-1">
          <button
            type="button"
            class="flex h-8 w-8 items-center justify-center rounded-lg border border-rosa/60 text-cafe/60 transition hover:bg-rosa/20 disabled:opacity-30"
            :disabled="indice === 0"
            aria-label="Mover variante hacia arriba"
            @click="moverVariante(indice, -1)"
          >
            ↑
          </button>
          <button
            type="button"
            class="flex h-8 w-8 items-center justify-center rounded-lg border border-rosa/60 text-cafe/60 transition hover:bg-rosa/20 disabled:opacity-30"
            :disabled="indice === variantes.length - 1"
            aria-label="Mover variante hacia abajo"
            @click="moverVariante(indice, 1)"
          >
            ↓
          </button>
          <button
            type="button"
            class="rounded-lg px-2.5 py-1.5 text-xs font-medium text-rojizo/80 transition hover:bg-rojizo/5 hover:text-rojizo"
            @click="quitarVariante(indice)"
          >
            Quitar
          </button>
        </div>
      </header>

      <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_16rem]">
        <div class="space-y-4">
          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Nombre <span class="text-rojizo">*</span>
              </label>
              <input
                :value="variante.nombre"
                type="text"
                maxlength="100"
                placeholder="Ej. Adoptable 1"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                @input="actualizar(indice, 'nombre', $event.target.value)"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Precio ({{ moneda }})
              </label>
              <input
                :value="variante.precio"
                type="number"
                min="0"
                step="0.01"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                @input="actualizar(indice, 'precio', $event.target.value)"
              />
              <p class="mt-1 text-[11px] text-cafe/45">{{ precioVisible(variante) }}</p>
            </div>
          </div>

          <div>
            <label class="mb-1.5 block text-xs font-medium text-cafe/70">
              Descripción de la variante (opcional)
            </label>
            <textarea
              :value="variante.descripcion"
              rows="2"
              maxlength="500"
              class="w-full resize-none rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              @input="actualizar(indice, 'descripcion', $event.target.value)"
            ></textarea>
          </div>

          <div class="flex flex-wrap items-end gap-5">
            <label class="inline-flex items-center gap-2 text-sm text-cafe">
              <input
                type="checkbox"
                :checked="esIlimitada(variante)"
                class="h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
                @change="alternarIlimitado(indice)"
              />
              Stock ilimitado
            </label>

            <div v-if="!esIlimitada(variante)" class="w-32">
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Stock disponible
              </label>
              <input
                :value="variante.stockDisponible"
                type="number"
                min="0"
                step="1"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                @input="onStockInput(indice, $event)"
              />
            </div>

            <div class="w-40">
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Estado
              </label>
              <select
                :value="variante.estado"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                @change="actualizar(indice, 'estado', $event.target.value)"
              >
                <option value="DISPONIBLE">Disponible</option>
                <option value="PAUSADO">Pausado</option>
                <option value="AGOTADO">Agotado</option>
                <option value="ARCHIVADO">Archivado</option>
              </select>
            </div>
          </div>
        </div>

        <div>
          <p class="mb-2 text-xs font-medium text-cafe/70">
            Imagen de la variante <span class="text-rojizo">*</span>
          </p>
          <div class="overflow-hidden rounded-xl border border-rosa/60 bg-rosa/10">
            <img
              v-if="variante.urlImagen"
              :src="variante.urlImagen"
              :alt="`Imagen de ${variante.nombre}`"
              class="aspect-square w-full object-cover"
            />
            <div
              v-else
              class="flex aspect-square flex-col items-center justify-center gap-2 px-4 text-center text-xs text-cafe/45"
            >
              <span class="text-2xl" aria-hidden="true">＋</span>
              Sin imagen
            </div>
          </div>
          <label
            class="mt-2 flex cursor-pointer items-center justify-center rounded-lg border border-coral/40 bg-blanco px-3 py-2 text-xs font-medium text-granate transition hover:bg-rosa/20"
            :class="subiendo === indice ? 'pointer-events-none opacity-50' : ''"
          >
            {{ subiendo === indice ? 'Subiendo...' : (variante.urlImagen ? 'Cambiar imagen' : 'Subir imagen') }}
            <input
              type="file"
              class="sr-only"
              :accept="acceptImagenes"
              @change="subirImagen($event, indice)"
            />
          </label>
          <p v-if="variante.sha256" class="mt-2 break-all text-[10px] leading-relaxed text-cafe/35">
            SHA-256: {{ variante.sha256 }}
          </p>
        </div>
      </div>
    </article>
  </div>
</template>
