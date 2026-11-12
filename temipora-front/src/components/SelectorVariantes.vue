<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { acceptImagenes, validarImagen } from '../servicios/archivos'
import { formatearPrecio } from '../servicios/moneda'
import { subirImagenProducto } from '../servicios/productos'
import { calcularLiquidacionProducto } from '../servicios/liquidacion'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
  esAdoptable: { type: Boolean, default: false },
  imagenPortada: { type: String, default: '' },
  imagenesGaleria: { type: Array, default: () => [] },
  erroresVariantes: { type: Object, default: () => ({}) },
  comisionPorcentaje: { type: Number, default: null },
  arancelPorcentaje: { type: Number, default: null },
})

const emit = defineEmits(['update:modelValue', 'agregar-imagen', 'ver-calculo'])

const MODOS_STOCK = [
  { valor: 'ILIMITADO', texto: 'Ilimitado' },
  { valor: 'LIMITADA', texto: 'Cantidad limitada' },
  { valor: 'UNICA', texto: 'Pieza única' },
]

const MAX_VARIANTES = 50
const cantidadAdoptables = ref(3)
const precioMasivo = ref('')
const subiendo = ref(null)
const error = ref('')
const confirmacion = ref(null)
const galeriaAbierta = ref(false)
const indiceGaleria = ref(null)
const bulkAbierto = ref(false)
const ayudaAbierta = ref(false)

const variantes = computed(() => props.modelValue)
const esPresentacionUnica = computed(() => variantes.value.length === 1)
const recibisDe = (variante) => {
  if (props.comisionPorcentaje == null || props.arancelPorcentaje == null) return null
  return calcularLiquidacionProducto({
    precio: variante?.precio,
    comisionPorcentaje: props.comisionPorcentaje,
    arancelPorcentaje: props.arancelPorcentaje,
  })
}
const indiceConLiquidacion = computed(
  () => variantes.value.findIndex((variante) => recibisDe(variante)),
)
const hayMasImagenes = computed(() => props.imagenesGaleria.length > 3)
const imagenesAsignables = computed(
  () => Math.max(0, props.imagenesGaleria.length - 1),
)
const textoBotonAdoptables = computed(() => {
  const total = Math.max(1, Math.min(50, Number(cantidadAdoptables.value) || 1))
  return `Ajustar a ${total} adoptable${total === 1 ? '' : 's'}`
})
const simboloMoneda = computed(() => {
  const ejemplo = formatearPrecio(0, props.moneda)
  return ejemplo.replace(/[\d.,\s]/g, '') || props.moneda
})

function esIlimitada(variante) {
  return variante?.stockDisponible === null || variante?.stockDisponible === undefined
}

function modoStock(variante) {
  if (esIlimitada(variante)) return 'ILIMITADO'
  return Number(variante.stockDisponible) === 1 ? 'UNICA' : 'LIMITADA'
}

function actualizar(indice, campo, valor) {
  confirmacion.value = null
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
  confirmacion.value = null
  if (props.modelValue.length >= MAX_VARIANTES) {
    error.value = `Un producto puede tener como máximo ${MAX_VARIANTES} variantes.`
    return
  }
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

function aplicarPrecioATodas() {
  const precio = Number(precioMasivo.value)
  if (!Number.isFinite(precio) || precio <= 0) {
    error.value = 'Ingresá un precio mayor a cero para aplicar a todas.'
    return
  }
  error.value = ''
  confirmacion.value = null
  bulkAbierto.value = false
  emit('update:modelValue', props.modelValue.map((variante) => ({
    ...variante,
    precio: precioMasivo.value,
  })))
}

function asignarImagenesEnOrden() {
  const disponibles = props.imagenesGaleria.slice(1)
  if (!disponibles.length) {
    error.value = 'No hay imágenes de galería además de la portada.'
    return
  }
  confirmacion.value = null
  let conLedger = 0
  const copia = props.modelValue.map((variante, indice) => {
    if (variante.tieneLedger) {
      conLedger++
      return variante
    }
    const imagen = disponibles[indice]
    if (!imagen) return variante
    return {
      ...variante,
      urlImagen: imagen.url,
      sha256: imagen.sha256,
      claveArchivoOriginal: imagen.claveArchivoOriginal || '',
    }
  })
  error.value = conLedger
    ? 'Las piezas únicas ya registradas conservan su imagen.'
    : ''
  bulkAbierto.value = false
  emit('update:modelValue', copia)
}

function tieneDatos(variante) {
  const precio = String(variante.precio ?? '').trim()
  const nombre = String(variante.nombre || '').trim()
  const nombrePorDefecto = /^(Única|Variante \d+|Adoptable \d+)$/i.test(nombre)
  const stock = variante.stockDisponible
  const stockConDatos = !(stock !== null && stock !== undefined && Number(stock) === 1)
  return Boolean(
    precio
    || variante.urlImagen
    || variante.sha256
    || String(variante.descripcion || '').trim()
    || (nombre && !nombrePorDefecto)
    || stockConDatos
    || (variante.estado && variante.estado !== 'DISPONIBLE')
    || variante.numeroEdicion,
  )
}

function crearVariosAdoptables() {
  confirmacion.value = null
  const objetivo = Math.max(1, Math.min(MAX_VARIANTES, Number(cantidadAdoptables.value) || 1))
  const soloPresentacionUnica =
    props.modelValue.length === 1 && props.modelValue[0].nombre === 'Única'
  const base = soloPresentacionUnica
    ? [{ ...props.modelValue[0], nombre: 'Adoptable 1', stockDisponible: 1 }]
    : [...props.modelValue]

  if (soloPresentacionUnica && objetivo === 1) return

  if (base.length < objetivo) {
    const nuevas = Array.from({ length: objetivo - base.length }, (_, i) =>
      nuevaVariante({
        nombre: `Adoptable ${base.length + i + 1}`,
        stockDisponible: 1,
      }),
    )
    emit('update:modelValue', [...base, ...nuevas])
    return
  }

  if (base.length === objetivo) return

  const aBorrar = base.slice(objetivo)
  if (aBorrar.some(tieneDatos)) {
    confirmacion.value = {
      objetivo,
      nombres: aBorrar.map((variante, i) =>
        variante.nombre || `Variante ${objetivo + i + 1}`,
      ),
    }
    return
  }

  emit('update:modelValue', base.slice(0, objetivo))
}

function confirmarAjuste() {
  const pendiente = confirmacion.value
  if (!pendiente) return
  emit('update:modelValue', props.modelValue.slice(0, pendiente.objetivo))
  confirmacion.value = null
}

function cancelarAjuste() {
  confirmacion.value = null
}

function quitarVariante(indice) {
  confirmacion.value = null
  if (props.modelValue.length <= 1) return
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== indice))
}

function moverVariante(indice, paso) {
  confirmacion.value = null
  const destino = indice + paso
  if (destino < 0 || destino >= props.modelValue.length) return
  const copia = [...props.modelValue]
  const [movida] = copia.splice(indice, 1)
  copia.splice(destino, 0, movida)
  emit('update:modelValue', copia)
}

function cambiarModoStock(indice, modo) {
  confirmacion.value = null
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

function imagenElegida(variante) {
  if (variante?.urlImagen) return variante.urlImagen
  return esPresentacionUnica.value ? props.imagenPortada : ''
}

function imagenActual(variante) {
  return imagenElegida(variante)
}

function imagenSeleccionada(variante, imagen) {
  return imagenElegida(variante) === imagen.url
}

function imagenesVisibles(variante) {
  const primeras = props.imagenesGaleria.slice(0, 3)
  if (!variante?.urlImagen) return primeras
  if (primeras.some((imagen) => imagen.url === variante.urlImagen)) return primeras
  const seleccionada = props.imagenesGaleria.find(
    (imagen) => imagen.url === variante.urlImagen,
  )
  return seleccionada ? [...primeras, seleccionada] : primeras
}

function abrirGaleria(indice) {
  indiceGaleria.value = indice
  galeriaAbierta.value = true
}

function cerrarGaleria() {
  galeriaAbierta.value = false
  indiceGaleria.value = null
}

function imagenSeleccionadaEnGaleria(imagen) {
  if (indiceGaleria.value === null) return false
  return imagenSeleccionada(props.modelValue[indiceGaleria.value], imagen)
}

function seleccionarDesdeGaleria(imagen) {
  if (indiceGaleria.value === null) return
  seleccionarImagen(indiceGaleria.value, imagen)
  cerrarGaleria()
}

function seleccionarImagen(indice, imagen) {
  confirmacion.value = null
  if (!imagen?.url || !imagen?.sha256) return
  const varianteActual = props.modelValue[indice]
  if (varianteActual?.tieneLedger
      && varianteActual.sha256 !== imagen.sha256) {
    error.value = 'La imagen de una pieza única ya registrada no se puede cambiar.'
    return
  }
  const copia = props.modelValue.map((variante, i) =>
    i === indice
      ? {
          ...variante,
          urlImagen: imagen.url,
          sha256: imagen.sha256,
          claveArchivoOriginal: imagen.claveArchivoOriginal || '',
        }
      : variante,
  )
  error.value = ''
  emit('update:modelValue', copia)
}

async function subirImagen(event, indice) {
  const archivo = event.target.files?.[0] || null
  event.target.value = ''
  if (!archivo) return
  if (props.modelValue[indice]?.tieneLedger) {
    error.value = 'La imagen de una pieza única ya registrada no se puede cambiar.'
    return
  }

  const problema = validarImagen(archivo)
  if (problema) {
    error.value = problema
    return
  }

  subiendo.value = indice
  error.value = ''
  try {
    const data = await subirImagenProducto(archivo)
    const imagen = {
      url: data.url,
      sha256: data.sha256,
      claveArchivoOriginal: data.claveOriginal,
    }
    emit('agregar-imagen', imagen)
    const copia = props.modelValue.map((variante, i) =>
      i === indice
        ? {
            ...variante,
            urlImagen: imagen.url,
            sha256: imagen.sha256,
            claveArchivoOriginal: imagen.claveArchivoOriginal,
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

function toggleBulk() {
  bulkAbierto.value = !bulkAbierto.value
}

function onDocumentClick(event) {
  if (!event.target.closest('.bulk-acciones')) {
    bulkAbierto.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
})
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
            : 'Cada variante se vende por separado y puede tener su propio precio, stock e imagen. Máximo 50.' }}
        </p>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-2">
        <button
          type="button"
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-rosa/60 bg-blanco text-sm font-semibold text-cafe/55 transition hover:bg-rosa/20 hover:text-granate"
          aria-label="Información sobre las variantes"
          aria-haspopup="dialog"
          :aria-expanded="ayudaAbierta"
          title="¿Qué son las variantes?"
          @click="ayudaAbierta = true"
        >
          i
        </button>
        <button
          type="button"
          class="shrink-0 rounded-lg border border-coral/40 bg-blanco px-4 py-2 text-sm font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="variantes.length >= MAX_VARIANTES"
          @click="agregarVariante"
        >
          Agregar variante
        </button>
      </div>
    </div>

    <div
      v-if="esAdoptable"
      class="flex flex-col gap-3 rounded-2xl border border-rosa/60 bg-crema/50 p-4 sm:flex-row sm:items-end sm:justify-between"
    >
      <div>
        <p class="text-sm font-semibold text-granate">¿Es una plancha de adoptables?</p>
        <p class="mt-1 text-xs leading-relaxed text-cafe/55">
          Definí el total de piezas únicas de la plancha. Cada adoptable queda con
          stock 1 y usa una imagen de la galería.
        </p>
        <p class="mt-1 text-[11px] font-medium text-cafe/45">
          Tenés {{ variantes.length }} presentación{{ variantes.length === 1 ? '' : 'es' }}.
        </p>
      </div>
      <div class="flex items-end gap-2">
        <div class="w-28">
          <label class="mb-1.5 block text-xs font-medium text-cafe/70">
            Total
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
          {{ textoBotonAdoptables }}
        </button>
      </div>
    </div>

    <div
      v-if="confirmacion"
      class="rounded-2xl border border-rojizo/30 bg-rojizo/5 p-4"
    >
      <p class="text-sm font-semibold text-granate">
        Vas a quitar {{ confirmacion.nombres.length }} variante(s) con datos
      </p>
      <p class="mt-1 text-xs leading-relaxed text-cafe/60">
        Para dejar {{ confirmacion.objetivo }} adoptables se eliminarán:
      </p>
      <ul class="mt-2 list-inside list-disc text-xs text-cafe/70">
        <li v-for="(nombre, indice) in confirmacion.nombres" :key="indice">
          {{ nombre }}
        </li>
      </ul>
      <div class="mt-3 flex flex-wrap gap-2">
        <button
          type="button"
          class="rounded-lg bg-rojizo px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-granate"
          @click="confirmarAjuste"
        >
          Sí, quitar y ajustar
        </button>
        <button
          type="button"
          class="rounded-lg border border-rosa/60 bg-blanco px-4 py-2 text-sm font-medium text-granate transition hover:bg-rosa/20"
          @click="cancelarAjuste"
        >
          Cancelar
        </button>
      </div>
    </div>

    <p v-if="error" class="rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
      {{ error }}
    </p>

    <div
      v-if="!esPresentacionUnica"
      class="rounded-2xl border border-rosa/60 bg-crema/50 p-4"
    >
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p class="text-sm font-semibold text-granate">Acciones para todas</p>
          <p class="mt-1 text-xs leading-relaxed text-cafe/55">
            Aplicá un mismo precio o asigná las imágenes de la galería en orden.
          </p>
        </div>
        <div class="bulk-acciones relative">
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg border px-4 py-2 text-sm font-semibold text-granate shadow-sm transition"
            :class="bulkAbierto
              ? 'border-coral/50 bg-rosa/60'
              : 'border-coral/30 bg-rosa/40 hover:bg-rosa/60'"
            :aria-expanded="bulkAbierto"
            @click.stop="toggleBulk"
          >
            Abrir acciones
            <svg
              class="h-3 w-3 transition-transform"
              :class="bulkAbierto ? 'rotate-180' : ''"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
            </svg>
          </button>

          <div
            v-if="bulkAbierto"
            class="absolute right-0 top-full z-20 mt-2 w-[390px] max-w-[calc(100vw-48px)] rounded-2xl border border-rosa/60 bg-blanco p-2.5 shadow-xl"
          >
            <div class="flex flex-col gap-3 rounded-xl p-2.5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p class="text-[11px] font-semibold text-granate">Mismo precio</p>
                <p class="mt-0.5 text-[10px] leading-relaxed text-cafe/55">
                  Aplica un precio único a las {{ variantes.length }} variantes.
                </p>
              </div>
              <div class="flex items-center gap-2">
                <div class="relative w-32">
                  <span class="pointer-events-none absolute inset-y-0 left-2 flex items-center text-[9px] font-semibold text-cafe/45">
                    {{ simboloMoneda }}
                  </span>
                  <input
                    v-model="precioMasivo"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="0"
                    class="w-full rounded-lg border border-rosa/60 bg-blanco py-1.5 pl-7 pr-10 text-xs text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  />
                  <span class="pointer-events-none absolute inset-y-0 right-2 flex items-center text-[9px] font-semibold text-cafe/45">
                    {{ moneda }}
                  </span>
                </div>
                <button
                  type="button"
                  class="rounded-lg bg-coral px-3 py-1.5 text-[10px] font-semibold text-white transition hover:bg-rojizo"
                  @click="aplicarPrecioATodas"
                >
                  Aplicar
                </button>
              </div>
            </div>

            <div class="flex flex-col gap-3 border-t border-rosa/40 p-2.5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p class="text-[11px] font-semibold text-granate">Asignar imágenes en orden</p>
                <p class="mt-0.5 text-[10px] leading-relaxed text-cafe/55">
                  Saltea la portada y usa las {{ imagenesAsignables }} imágenes de la galería.
                </p>
              </div>
              <button
                type="button"
                class="shrink-0 rounded-lg border border-coral/40 bg-blanco px-3 py-1.5 text-[10px] font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
                :disabled="imagenesAsignables === 0"
                @click="asignarImagenesEnOrden"
              >
                Asignar
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Presentación única -->
    <article
      v-if="esPresentacionUnica"
      class="rounded-2xl border border-rosa/60 bg-blanco p-4 shadow-sm"
    >
      <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_16rem]">
        <div class="space-y-4">
          <div class="grid gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-medium text-cafe/70">
                Precio <span class="text-rojizo">*</span>
              </label>
              <div class="relative">
                <span class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-xs font-semibold text-cafe/45">
                  {{ simboloMoneda }}
                </span>
                <input
                  :value="variantes[0].precio"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0"
                  class="w-full rounded-lg border border-rosa/60 bg-blanco py-2 pl-8 pr-14 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  @input="actualizar(0, 'precio', $event.target.value)"
                />
                <span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs font-semibold text-cafe/45">
                  {{ moneda }}
                </span>
              </div>
              <p v-if="erroresVariantes[0]?.precio" class="mt-1 text-xs font-medium text-rojizo">
                {{ erroresVariantes[0].precio }}
              </p>
              <p
                v-if="recibisDe(variantes[0])"
                class="mt-1 text-[11px] text-cafe/50"
              >
                Recibís ≈ {{ formatearPrecio(recibisDe(variantes[0]).recibis, moneda) }}
                <button
                  type="button"
                  class="ml-1 font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
                  @click="emit('ver-calculo')"
                >
                  ¿Cómo se calcula?
                </button>
              </p>
            </div>

            <div>
              <p class="mb-1 text-xs font-medium text-cafe/70">Stock</p>
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
              <div v-if="modoStock(variantes[0]) === 'LIMITADA'" class="mt-1.5">
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
                class="mt-1.5 text-[11px] leading-relaxed text-cafe/50"
              >
                Se vende una sola unidad. Si habilitás la reventa, puede transferirse.
              </p>
              <p v-if="erroresVariantes[0]?.stock" class="mt-1 text-xs font-medium text-rojizo">
                {{ erroresVariantes[0].stock }}
              </p>
            </div>
          </div>

          <p class="rounded-xl bg-crema/60 px-3 py-2 text-xs leading-relaxed text-cafe/60">
            Elegí qué imagen de la galería representa esta presentación. En productos
            digitales es el archivo que recibe el comprador. Si no elegís ninguna,
            se usa la portada de la publicación.
          </p>
          <p v-if="erroresVariantes[0]?.imagen" class="text-xs font-medium text-rojizo">
            {{ erroresVariantes[0].imagen }}
          </p>
        </div>

        <div>
          <p class="mb-1.5 text-xs font-medium text-cafe/70">Imagen de la presentación</p>
          <div class="overflow-hidden rounded-xl border border-rosa/60 bg-rosa/10">
            <img
              v-if="imagenActual(variantes[0])"
              :src="imagenActual(variantes[0])"
              alt="Imagen de la presentación"
              class="aspect-square w-full object-cover"
            />
            <div
              v-else
              class="flex aspect-square flex-col items-center justify-center gap-2 px-4 text-center text-xs text-cafe/45"
            >
              <span class="text-2xl" aria-hidden="true">＋</span>
              Subí una imagen en el paso 1
            </div>
          </div>

          <div v-if="imagenesGaleria.length" class="mt-1.5">
            <div class="mb-1 flex items-center justify-between gap-2">
              <p class="text-[11px] text-cafe/50">Elegí de la galería:</p>
              <button
                v-if="hayMasImagenes"
                type="button"
                class="text-[11px] font-medium text-coral transition hover:text-rojizo"
                @click="abrirGaleria(0)"
              >
                Ver todas ({{ imagenesGaleria.length }})
              </button>
            </div>
            <div class="flex gap-1.5 overflow-x-auto pb-1">
              <button
                v-for="imagen in imagenesVisibles(variantes[0])"
                :key="imagen.url"
                type="button"
                class="h-14 w-14 shrink-0 overflow-hidden rounded-lg border-2 transition"
                :class="imagenSeleccionada(variantes[0], imagen)
                  ? 'border-coral ring-2 ring-coral/30'
                  : 'border-rosa/60 hover:border-coral/50'"
                :aria-label="`Usar imagen ${imagen.url}`"
                @click="seleccionarImagen(0, imagen)"
              >
                <img :src="imagen.url" alt="" class="h-full w-full object-cover" />
              </button>
            </div>
          </div>
          <p v-else class="mt-1.5 text-[11px] leading-relaxed text-cafe/50">
            No hay imágenes en la galería. Subí una acá o volvé al paso 1.
          </p>

          <label
            class="mt-1.5 flex cursor-pointer items-center justify-center rounded-lg border border-coral/40 bg-blanco px-3 py-2 text-xs font-medium text-granate transition hover:bg-rosa/20"
            :class="subiendo === 0 ? 'pointer-events-none opacity-50' : ''"
          >
            {{ subiendo === 0
              ? 'Subiendo...'
              : (imagenActual(variantes[0]) ? 'Cambiar imagen' : 'Subir nueva imagen') }}
            <input
              type="file"
              class="sr-only"
              :accept="acceptImagenes"
              @change="subirImagen($event, 0)"
            />
          </label>
        </div>
      </div>
    </article>

    <!-- Variantes múltiples -->
    <article
      v-for="(variante, indice) in (esPresentacionUnica ? [] : variantes)"
      :key="variante.idVariante || `nueva-${indice}`"
      class="rounded-2xl border border-rosa/60 bg-blanco p-4 shadow-sm"
    >
      <header class="mb-3 flex flex-wrap items-center justify-between gap-3">
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

      <div class="grid gap-3 lg:grid-cols-[minmax(0,1fr)_16rem]">
        <div class="flex flex-col gap-3">
          <div class="grid gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-xs font-medium text-cafe/70">
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
              <p v-if="erroresVariantes[indice]?.nombre" class="mt-1 text-xs font-medium text-rojizo">
                {{ erroresVariantes[indice].nombre }}
              </p>
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-cafe/70">
                Precio <span class="text-rojizo">*</span>
              </label>
              <div class="relative">
                <span class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-xs font-semibold text-cafe/45">
                  {{ simboloMoneda }}
                </span>
                <input
                  :value="variante.precio"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0"
                  class="w-full rounded-lg border border-rosa/60 bg-blanco py-2 pl-8 pr-14 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
                  @input="actualizar(indice, 'precio', $event.target.value)"
                />
                <span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs font-semibold text-cafe/45">
                  {{ moneda }}
                </span>
              </div>
              <p v-if="erroresVariantes[indice]?.precio" class="mt-1 text-xs font-medium text-rojizo">
                {{ erroresVariantes[indice].precio }}
              </p>
              <p
                v-if="recibisDe(variante)"
                class="mt-1 text-[11px] text-cafe/50"
              >
                Recibís ≈ {{ formatearPrecio(recibisDe(variante).recibis, moneda) }}
                <button
                  v-if="indice === indiceConLiquidacion"
                  type="button"
                  class="ml-1 font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
                  @click="emit('ver-calculo')"
                >
                  ¿Cómo se calcula?
                </button>
              </p>
            </div>
          </div>

          <div>
            <p class="mb-1 text-xs font-medium text-cafe/70">Stock</p>
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
              class="mt-1.5 w-28 rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              @input="onStockInput(indice, $event)"
            />
            <p v-if="erroresVariantes[indice]?.stock" class="mt-1 text-xs font-medium text-rojizo">
              {{ erroresVariantes[indice].stock }}
            </p>
          </div>

          <div class="flex min-h-[120px] flex-1 flex-col">
            <label class="mb-1 block text-xs font-medium text-cafe/70">
              Descripción de la variante (opcional)
            </label>
            <textarea
              :value="variante.descripcion"
              rows="3"
              maxlength="500"
              class="min-h-[110px] w-full flex-1 resize-none rounded-lg border border-rosa/60 bg-blanco px-3 py-2 text-sm text-cafe focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
              @input="actualizar(indice, 'descripcion', $event.target.value)"
            ></textarea>
          </div>
        </div>

        <div>
          <p class="mb-1.5 text-xs font-medium text-cafe/70">
            Imagen de la variante <span class="text-rojizo">*</span>
          </p>
          <div class="overflow-hidden rounded-xl border border-rosa/60 bg-rosa/10">
            <img
              v-if="imagenActual(variante)"
              :src="imagenActual(variante)"
              :alt="`Imagen de ${variante.nombre}`"
              class="aspect-square w-full object-cover"
            />
            <div
              v-else
              class="flex aspect-square flex-col items-center justify-center gap-2 px-4 text-center text-xs text-cafe/45"
            >
              <span class="text-2xl" aria-hidden="true">＋</span>
              Elegí una imagen de la galería
            </div>
          </div>

          <div v-if="imagenesGaleria.length" class="mt-1.5">
            <div class="mb-1 flex items-center justify-between gap-2">
              <p class="text-[11px] text-cafe/50">Elegí de la galería:</p>
              <button
                v-if="hayMasImagenes"
                type="button"
                class="text-[11px] font-medium text-coral transition hover:text-rojizo"
                @click="abrirGaleria(indice)"
              >
                Ver todas ({{ imagenesGaleria.length }})
              </button>
            </div>
            <div class="flex gap-1.5 overflow-x-auto pb-1">
              <button
                v-for="imagen in imagenesVisibles(variante)"
                :key="imagen.url"
                type="button"
                class="h-14 w-14 shrink-0 overflow-hidden rounded-lg border-2 transition"
                :class="imagenSeleccionada(variante, imagen)
                  ? 'border-coral ring-2 ring-coral/30'
                  : 'border-rosa/60 hover:border-coral/50'"
                :aria-label="`Usar imagen ${imagen.url}`"
                @click="seleccionarImagen(indice, imagen)"
              >
                <img :src="imagen.url" alt="" class="h-full w-full object-cover" />
              </button>
            </div>
          </div>
          <p v-else class="mt-1.5 text-[11px] leading-relaxed text-cafe/50">
            No hay imágenes en la galería. Subí una acá o volvé al paso 1.
          </p>

          <label
            class="mt-1.5 flex cursor-pointer items-center justify-center rounded-lg border border-coral/40 bg-blanco px-3 py-2 text-xs font-medium text-granate transition hover:bg-rosa/20"
            :class="subiendo === indice ? 'pointer-events-none opacity-50' : ''"
          >
            {{ subiendo === indice ? 'Subiendo...' : (variante.urlImagen ? 'Cambiar imagen' : 'Subir nueva imagen') }}
            <input
              type="file"
              class="sr-only"
              :accept="acceptImagenes"
              @change="subirImagen($event, indice)"
            />
          </label>
          <p v-if="erroresVariantes[indice]?.imagen" class="mt-1 text-xs font-medium text-rojizo">
            {{ erroresVariantes[indice].imagen }}
          </p>
        </div>
      </div>
    </article>

    <button
      v-if="variantes.length"
      type="button"
      class="flex w-full items-center justify-center rounded-lg border border-coral/40 bg-blanco px-4 py-2 text-sm font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-40"
      :disabled="variantes.length >= MAX_VARIANTES"
      @click="agregarVariante"
    >
      Agregar variante
    </button>

    <Teleport to="body">
      <div
        v-if="galeriaAbierta"
        class="fixed inset-0 z-[90] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-label="Galería de imágenes"
      >
        <div class="absolute inset-0 bg-granate/50" aria-hidden="true" @click="cerrarGaleria"></div>
        <div class="relative flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-blanco shadow-xl">
          <header class="flex items-center justify-between gap-3 border-b border-rosa/60 px-5 py-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Galería
              </p>
              <p class="text-sm font-semibold text-granate">
                {{ imagenesGaleria.length }} imágenes
              </p>
            </div>
            <button
              type="button"
              class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20"
              @click="cerrarGaleria"
            >
              Cerrar
            </button>
          </header>
          <div class="grid grid-cols-3 gap-3 overflow-y-auto p-4 sm:grid-cols-4">
            <button
              v-for="imagen in imagenesGaleria"
              :key="imagen.url"
              type="button"
              class="overflow-hidden rounded-xl border-2 transition"
              :class="imagenSeleccionadaEnGaleria(imagen)
                ? 'border-coral ring-2 ring-coral/30'
                : 'border-rosa/60 hover:border-coral/50'"
              :aria-label="`Usar imagen ${imagen.url}`"
              @click="seleccionarDesdeGaleria(imagen)"
            >
              <img :src="imagen.url" alt="" class="aspect-square w-full object-cover" />
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="ayudaAbierta"
        class="fixed inset-0 z-[95] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="variantes-ayuda-titulo"
      >
        <div
          class="absolute inset-0 bg-granate/50"
          aria-hidden="true"
          @click="ayudaAbierta = false"
        ></div>
        <div class="relative flex max-h-[90vh] w-full max-w-xl flex-col overflow-hidden rounded-2xl bg-blanco shadow-xl">
          <header class="flex items-start justify-between gap-3 border-b border-rosa/60 px-5 py-4">
            <div>
              <p class="text-[10px] font-semibold uppercase tracking-[0.14em] text-cafe/45">
                Ayuda
              </p>
              <h3 id="variantes-ayuda-titulo" class="mt-0.5 font-display text-lg font-bold text-granate">
                ¿Qué son las variantes?
              </h3>
            </div>
            <button
              type="button"
              class="rounded-lg border border-rosa/60 px-3 py-1.5 text-xs font-medium text-granate transition hover:bg-rosa/20"
              @click="ayudaAbierta = false"
            >
              Cerrar
            </button>
          </header>

          <div class="overflow-y-auto px-5 py-4">
            <p class="text-sm leading-relaxed text-cafe/70">
              Si es tu primera vez vendiendo, no te preocupes: no siempre necesitás
              variantes. Una variante es una versión vendible de la misma publicación.
              Cada una tiene su propio precio, stock e imagen, y se vende por separado.
            </p>

            <div class="mt-4 space-y-3">
              <div class="rounded-xl border border-rosa/60 bg-crema/30 p-3">
                <p class="text-sm font-semibold text-granate">Una pieza única</p>
                <p class="mt-1 text-xs leading-relaxed text-cafe/60">
                  Si vendés un solo cuadro, una escultura o una pieza irrepetible,
                  no necesitás crear variantes. Dejá la presentación única y usá
                  la portada de la publicación.
                </p>
              </div>

              <div class="rounded-xl border border-rosa/60 bg-crema/30 p-3">
                <p class="text-sm font-semibold text-granate">Varios adoptables</p>
                <p class="mt-1 text-xs leading-relaxed text-cafe/60">
                  Si ofrecés una plancha de adoptables, cada adoptable puede ser
                  una variante. Así cada uno tiene su nombre, precio, stock e imagen
                  propios y se vende por separado. Una plancha de 5 adoptables puede
                  tener 5 variantes.
                </p>
              </div>

              <div class="rounded-xl border border-rosa/60 bg-crema/30 p-3">
                <p class="text-sm font-semibold text-granate">
                  El mismo producto en varias versiones
                </p>
                <p class="mt-1 text-xs leading-relaxed text-cafe/60">
                  Si vendés el mismo diseño en distintos tamaños, colores o formatos,
                  creá una variante por opción. El comprador elige la que quiere y
                  el stock se controla por separado.
                </p>
              </div>
            </div>

            <p class="mt-4 rounded-xl bg-mantequilla/20 px-3 py-2.5 text-xs font-medium leading-relaxed text-granate">
              En resumen: una pieza única es una sola presentación; varios adoptables
              o versiones son una variante por opción. Podés empezar con una y agregar
              más cuando quieras.
            </p>

            <p class="mt-3 text-[11px] leading-relaxed text-cafe/50">
              Máximo 50 variantes por publicación. La portada es la imagen general;
              cada variante puede usar otra imagen de la galería.
            </p>
          </div>

          <footer class="flex justify-end border-t border-rosa/60 px-5 py-3">
            <button
              type="button"
              class="rounded-lg bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
              @click="ayudaAbierta = false"
            >
              Entendido
            </button>
          </footer>
        </div>
      </div>
    </Teleport>
  </div>
</template>
