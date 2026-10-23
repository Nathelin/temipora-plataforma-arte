<script setup>
import { ref } from 'vue'
import api from '../servicios/api'
import {
  acceptImagenes,
  validarImagen,
  procesarImagen,
  LIMITES_IMAGEN,
} from '../servicios/archivos'

const props = defineProps({
  preguntas: { type: Array, default: () => [] },
  modelValue: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue', 'imagen-subida', 'imagen-eliminada'])

const subiendo = ref({})
const erroresSubida = ref({})

function valores(pregunta) {
  const actuales = props.modelValue?.[pregunta.idPregunta]
  return Array.isArray(actuales) ? actuales : []
}

function guardarValores(pregunta, valoresNuevos) {
  emit('update:modelValue', {
    ...(props.modelValue || {}),
    [pregunta.idPregunta]: valoresNuevos.filter(
      (valor) => typeof valor === 'string' && valor.trim() !== '',
    ),
  })
}

function primerValor(pregunta) {
  return valores(pregunta)[0] || ''
}

function texto(pregunta) {
  return primerValor(pregunta)
}

function actualizarTexto(pregunta, event) {
  const valor = event.target.value
  guardarValores(pregunta, valor.trim() ? [valor] : [])
}

function actualizarOpcion(pregunta, opcion) {
  guardarValores(pregunta, [opcion])
}

function tieneOpcion(pregunta, opcion) {
  return valores(pregunta).includes(opcion)
}

function alternarOpcion(pregunta, opcion, marcada) {
  const seleccionadas = new Set(valores(pregunta))
  if (marcada) seleccionadas.add(opcion)
  else seleccionadas.delete(opcion)
  guardarValores(pregunta, [...seleccionadas])
}

function quitarImagen(pregunta, url) {
  guardarValores(
    pregunta,
    valores(pregunta).filter((valor) => valor !== url),
  )
  emit('imagen-eliminada', url)
}

async function subirImagenes(pregunta, event) {
  const archivos = [...(event.target.files || [])]
  event.target.value = ''
  erroresSubida.value = { ...erroresSubida.value, [pregunta.idPregunta]: '' }

  const actuales = valores(pregunta)
  const disponibles = Math.max(0, 10 - actuales.length)
  if (archivos.length > disponibles) {
    erroresSubida.value = {
      ...erroresSubida.value,
      [pregunta.idPregunta]: `Podés subir hasta 10 imágenes. Te quedan ${disponibles} lugar(es).`,
    }
    return
  }

  subiendo.value = { ...subiendo.value, [pregunta.idPregunta]: true }
  const nuevas = []

  try {
    for (const archivo of archivos) {
      const problema = validarImagen(archivo)
      if (problema) {
        erroresSubida.value = {
          ...erroresSubida.value,
          [pregunta.idPregunta]: problema,
        }
        continue
      }

      try {
        const optimizada = await procesarImagen(archivo, LIMITES_IMAGEN.obra)
        const fd = new FormData()
        fd.append('archivo', optimizada)
        const res = await api.post('/upload', fd)
        const url = res.data?.url
        if (!url) throw new Error('No se recibió la URL de la imagen.')
        nuevas.push(url)
        emit('imagen-subida', url)
      } catch (e) {
        erroresSubida.value = {
          ...erroresSubida.value,
          [pregunta.idPregunta]:
            e.response?.data?.message || e.message || 'No se pudo subir la imagen.',
        }
      }
    }
  } finally {
    if (nuevas.length) {
      guardarValores(pregunta, [...valores(pregunta), ...nuevas])
    }
    subiendo.value = { ...subiendo.value, [pregunta.idPregunta]: false }
  }
}
</script>

<template>
  <div class="space-y-5">
    <p
      v-if="!preguntas.length"
      class="rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center text-sm text-cafe/50"
    >
      Este servicio no tiene preguntas adicionales.
    </p>

    <div v-for="p in preguntas" :key="p.idPregunta" class="rounded-xl border border-rosa/50 bg-blanco p-4">
      <template v-if="p.codigoTipo === 'ENCABEZADO'">
        <h3 class="text-base font-medium text-granate">{{ p.enunciado }}</h3>
        <p v-if="p.ayuda" class="mt-1 text-sm text-cafe/60">{{ p.ayuda }}</p>
      </template>

      <template v-else>
        <label class="block text-sm text-cafe mb-1">
          {{ p.enunciado }}<span v-if="p.obligatoria" class="text-rojizo"> *</span>
        </label>
        <p v-if="p.ayuda" class="text-xs text-cafe/60 mb-2">{{ p.ayuda }}</p>

        <textarea
          v-if="p.codigoTipo === 'TEXTO'"
          rows="3"
          placeholder="Escribí tu respuesta..."
          :value="texto(p)"
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
          @input="actualizarTexto(p, $event)"
        ></textarea>

        <div v-else-if="p.codigoTipo === 'CASILLAS'" class="space-y-2">
          <label v-for="(o, oi) in p.opciones" :key="oi" class="flex items-center gap-2 text-sm text-cafe">
            <input
              type="checkbox"
              class="accent-coral"
              :checked="tieneOpcion(p, o)"
              @change="alternarOpcion(p, o, $event.target.checked)"
            />
            <span>{{ o }}</span>
          </label>
        </div>

        <div v-else-if="p.codigoTipo === 'OPCIONES'" class="space-y-2">
          <label v-for="(o, oi) in p.opciones" :key="oi" class="flex items-center gap-2 text-sm pb-1 pt-1 text-cafe">
            <input
              type="radio"
              :name="`opciones-${p.idPregunta}`"
              class="accent-coral"
              :checked="primerValor(p) === o"
              @change="actualizarOpcion(p, o)"
            />
            <span>{{ o }}</span>
          </label>
        </div>

        <div v-else-if="p.codigoTipo === 'SUBIR_IMAGEN'">
          <div v-if="valores(p).length" class="mb-3 grid grid-cols-3 gap-2 sm:grid-cols-5">
            <div
              v-for="url in valores(p)"
              :key="url"
              class="group relative aspect-square overflow-hidden rounded-lg bg-rosa/20"
            >
              <img :src="url" alt="" class="h-full w-full object-cover" />
              <button
                type="button"
                class="absolute right-1 top-1 rounded-full bg-granate/80 px-1.5 text-xs text-white opacity-0 transition group-hover:opacity-100"
                aria-label="Quitar imagen"
                @click="quitarImagen(p, url)"
              >
                ×
              </button>
            </div>
          </div>

          <label
            class="flex cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed border-rosa px-4 py-6 text-center text-sm text-cafe/60 transition hover:border-coral"
            :class="subiendo[p.idPregunta] ? 'opacity-60' : ''"
          >
            <svg class="h-6 w-6 mb-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 7.5m0 0L7.5 12M12 7.5V21" />
            </svg>
            <span>{{ subiendo[p.idPregunta] ? 'Subiendo...' : 'Subir imagen' }}</span>
            <span class="mt-1 text-xs text-cafe/40">JPG, PNG o WEBP · máximo 5 MB · hasta 10 imágenes</span>
            <input
              type="file"
              class="sr-only"
              multiple
              :accept="acceptImagenes"
              :disabled="subiendo[p.idPregunta] || valores(p).length >= 10"
              @change="subirImagenes(p, $event)"
            />
          </label>

          <p v-if="erroresSubida[p.idPregunta]" class="mt-2 text-xs text-rojizo">
            {{ erroresSubida[p.idPregunta] }}
          </p>
        </div>

        <p v-else class="text-xs text-cafe/50">Tipo de campo no soportado: {{ p.codigoTipo }}</p>
      </template>
    </div>
  </div>
</template>
