<script setup>
import { computed, ref } from 'vue'
import { acceptImagenes, validarImagen } from '../servicios/archivos'
import { formatearPrecio } from '../servicios/moneda'
import { subirImagenProducto } from '../servicios/productos'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
  esAdoptable: { type: Boolean, default: false },
  imagenPortada: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const MODOS_STOCK = [
  { valor: 'ILIMITADO', texto: 'Ilimitado' },
  { valor: 'LIMITADA', texto: 'Cantidad limitada' },
  { valor: 'UNICA', texto: 'Pieza única' },
]

const cantidadAdoptables = ref(3)
const subiendo = ref(null)
const error = ref('')

const variantes = computed(() => props.modelValue)
const esPresentacionUnica = computed(() => variantes.value.length === 1)

function esIlimitada(variante) {
  return variante?.stockDisponible === null || variante?.stockDisponible === undefined
}

function modoStock(variante) {
  if (esIlimitada(variante)) return 'ILIMITADO'
  return Number(variante.stockDisponible) === 1 ? 'UNICA' : 'LIMITADA'
}

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
    stockDisponible: props.esAdoptable ? 1 : null,
    estado: 'DISPONIBLE',
    urlImagen: '',
    sha256: '',
    claveArchivoOriginal: '',
    ...overrides,
  }
}

function agregarVariante() {
  const total = props.modelValue.length
  const iniciales = props.modelValue.map((variante, i) =>
    i === 0 && variante.nombre === 'Única'
      ? { ...variante, nombre: props.esAdoptable ? 'Adoptable 1' : 'Variante 1' }
      : variante,
  )
  emit('update:modelValue', [
    ...iniciales,
    nuevaVariante({
      nombre: props.esAdoptable ? `Adoptable ${total + 1}` : `Variante ${total + 1}`,
    }),
  ])
}

function crearVariosAdoptables() {
  const cantidad = Math.max(1, Math.min(50, Number(cantidadAdoptables.value) || 1))
  const soloPresentacionUnica =
    props.modelValue.length === 1 && props.modelValue[0].nombre === 'Única'
  const iniciales = soloPresentacionUnica
    ? [{ ...props.modelValue[0], nombre: 'Adoptable 1', stockDisponible: 1 }]
    : [...props.modelValue]
  const cantidadNuevas = soloPresentacionUnica
    ? Math.max(0, cantidad - 1)
    : cantidad
  const base = iniciales.length
  const nuevas = Array.from({ length: cantidadNuevas }, (_, i) =>
    nuevaVariante({
      nombre: `Adoptable ${base + i + 1}`,
      stockDisponible: 1,
    }),
  )
  emit('update:modelValue', [...iniciales, ...nuevas])
}

function quitarVariante(indice) {
  if (props.modelValue.length <= 1) return
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

function cambiarModoStock(indice, modo) {
  const variante = props.modelValue[indice]
  if (modo === 'ILIMITADO') {
    actualizar(indice, 'stockDisponible', null)
    return
  }
  if (modo === 'UNICA') {
    actualizar(indice, 'stockDisponible', 1)
    return
  }
  const actual = Number(variante.stockDisponible)
  if (!esIlimitada(variante) && actual !== 1) return
  actualizar(indice, 'stockDisponible', 2)
}

function onStockInput(indice, event) {
  const valor = Number(event.target.value)
  actualizar(indice, 'stockDisponible', Number.isFinite(valor) && valor >= 0 ? valor : 0)
}

function precioVisible(variante) {
  const numero = Number(variante.precio)
  return Number.isFinite(numero) && numero > 0
    ? formatearPrecio(numero, props.moneda)
    : ''
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
    <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h3 class="font-display text-lg font-bold text-granate">
          {{ esPresentacionUnica ? 'Precio y stock' : 'Variantes' }}
        </h3>
        <p class="mt-1 text-sm text-cafe/60">
          {{ esPresentacionUnica
            ? 'Esta publicación tiene una sola presentación.'
            : 'Cada variante se vende por separado y puede tener su propio precio, stock e imagen.' }}
        </p>
      </div>
      <button
        type="button"
        class="shrink-0 rounded-lg border border-coral/40 bg-blanco px-4 py-2 text-sm font-medium text-granate transition hover:bg-rosa/20"
        @click="agregarVariante"
      >
        {{ esPresentacionUnica ? 'Agregar otra presentación' : 'Agregar variante' }}
      </button>
    </div>

    <div
      v-if="esAdoptable"
      class="flex flex-col gap-3 rounded-2xl border border-rosa/60 bg-crema/50 p-4 sm:flex-row sm:items-end sm:justify-between"
    >
      <div>
        <p class="text-sm font-semibold text-granate">¿Es una plancha de adoptables?</p>
        <p class="mt-1 text-xs leading-relaxed text-cafe/55">
          Crea varias piezas únicas de una vez. Cada adoptable queda con stock 1
          y podés renombrarlo o subirle su imagen después.
        </p>
      </div>
      <div class="flex items-end gap-2">
        <div class="w-28">
          <label class="mb-1.5 block text-xs font-medium text-cafe/70">
            Cantidad
          </label>
          <input
            v-model.number="cantidadAdoptables"
            type="number"
            min="1"
            max="50"
            class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
          />
        </div>
        <button
          type="button"
          class="rounded-lg bg-coral px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-rojizo"
          @click="crearVariosAdoptables"
        >
          Crear varios adoptables
        </button>
      </div>
    </div>

    <p v-if="error" class="rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
      {{ error }}
    </p>

    <!-- Presentación única -->
    <article
      v-if="esPresentacionUnica"
      class="rounded-2xl border border-rosa/60 bg-blanco p-5 shadow-sm"
    >
      <div class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_14rem]">
        <div class="space-y-5">
          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Precio <span class="text-rojizo">*</span>
              </label>
              <div class="relative">
                <input
                  :value="variantes[0].precio"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0"
                  class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 pr-14 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  @input="actualizar(0, 'precio', $event.target.value)"
                />
                <span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs font-semibold text-cafe/45">
                  {{ moneda }}
                </span>
              </div>
              <p class="mt-1 text-[11px] text-cafe/45">{{ precioVisible(variantes[0]) }}</p>
            </div>

            <div>
              <p class="mb-1.5 text-xs font-medium text-cafe/70">Stock</p>
              <div class="inline-flex w-full rounded-lg border border-rosa/60 bg-crema/40 p-0.5">
                <button
                  v-for="modo in MODOS_STOCK"
                  :key="modo.valor"
                  type="button"
                  class="flex-1 rounded-md px-2 py-1.5 text-[11px] font-medium transition"
                  :class="modoStock(variantes[0]) === modo.valor
                    ? 'bg-coral text-white shadow-sm'
                    : 'text-cafe/60 hover:bg-rosa/20 hover:text-granate'"
                  :aria-pressed="modoStock(variantes[0]) === modo.valor"
                  @click="cambiarModoStock(0, modo.valor)"
                >
                  {{ modo.texto }}
                </button>
              </div>
              <div v-if="modoStock(variantes[0]) === 'LIMITADA'" class="mt-2">
                <input
                  :value="variantes[0].stockDisponible"
                  type="number"
                  min="0"
                  step="1"
                  class="w-28 rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  @input="onStockInput(0, $event)"
                />
              </div>
              <p
                v-if="modoStock(variantes[0]) === 'UNICA'"
                class="mt-2 text-[11px] leading-relaxed text-cafe/50"
              >
                Se vende una sola unidad. Si habilitás la reventa, puede transferirse.
              </p>
            </div>
          </div>

          <p class="rounded-xl bg-crema/60 px-3 py-2 text-xs leading-relaxed text-cafe/60">
            La imagen de la presentación será la portada de la publicación.
            Si querés una imagen distinta, agregá una segunda presentación.
          </p>
        </div>

        <div>
          <p class="mb-2 text-xs font-medium text-cafe/70">Imagen de portada</p>
          <div class="overflow-hidden rounded-xl border border-rosa/60 bg-rosa/10">
            <img
              v-if="imagenPortada"
              :src="imagenPortada"
              alt="Portada de la publicación"
              class="aspect-square w-full object-cover"
            />
            <div
              v-else
              class="flex aspect-square flex-col items-center justify-center gap-2 px-4 text-center text-xs text-cafe/45"
            >
              <span class="text-2xl" aria-hidden="true">＋</span>
              Subí la portada en el paso de imágenes
            </div>
          </div>
        </div>
      </div>
    </article>

    <!-- Variantes múltiples -->
    <article
      v-for="(variante, indice) in (esPresentacionUnica ? [] : variantes)"
      :key="variante.idVariante || `nueva-${indice}`"
      class="rounded-2xl border border-rosa/60 bg-blanco p-5 shadow-sm"
    >
      <header class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h3 class="font-display text-base font-bold text-granate">
          Variante {{ indice + 1 }}
        </h3>
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
                :placeholder="esAdoptable ? 'Ej. Adoptable 1' : 'Ej. Tamaño A4'"
                class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                @input="actualizar(indice, 'nombre', $event.target.value)"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-cafe/70">
                Precio <span class="text-rojizo">*</span>
              </label>
              <div class="relative">
                <input
                  :value="variante.precio"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0"
                  class="w-full rounded-lg border border-rosa/60 bg-blanco px-3 py-2 pr-14 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  @input="actualizar(indice, 'precio', $event.target.value)"
                />
                <span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs font-semibold text-cafe/45">
                  {{ moneda }}
                </span>
              </div>
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

          <div>
            <p class="mb-1.5 text-xs font-medium text-cafe/70">Stock</p>
            <div class="inline-flex w-full max-w-md rounded-lg border border-rosa/60 bg-crema/40 p-0.5">
              <button
                v-for="modo in MODOS_STOCK"
                :key="modo.valor"
                type="button"
                class="flex-1 rounded-md px-2 py-1.5 text-[11px] font-medium transition"
                :class="modoStock(variante) === modo.valor
                  ? 'bg-coral text-white shadow-sm'
                  : 'text-cafe/60 hover:bg-rosa/20 hover:text-granate'"
                :aria-pressed="modoStock(variante) === modo.valor"
                @click="cambiarModoStock(indice, modo.valor)"
              >
                {{ modo.texto }}
              </button>
            </div>
            <input
              v-if="modoStock(variante) === 'LIMITADA'"
              :value="variante.stockDisponible"
              type="number"
              min="0"
              step="1"
              class="mt-2 w-28 rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              @input="onStockInput(indice, $event)"
            />
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
        </div>
      </div>
    </article>
  </div>
</template>
