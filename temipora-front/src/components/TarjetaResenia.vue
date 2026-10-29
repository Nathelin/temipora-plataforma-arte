<script setup>
import { computed, ref } from 'vue'
import { formatearFechaHora } from '../servicios/pedidos'
import ImagenGaleria from './ImagenGaleria.vue'
import ValoracionEstrellas from './ValoracionEstrellas.vue'

const props = defineProps({
  resenia: { type: Object, required: true },
  titulo: { type: String, default: 'Reseña' },
  puedeEditar: { type: Boolean, default: false },
  puedeEliminar: { type: Boolean, default: false },
  fechaLimiteEdicion: { type: String, default: null },
})

const emit = defineEmits(['editar', 'eliminar'])

const verOriginal = ref(false)

const versionOriginal = computed(() => {
  const historial = props.resenia?.historial || []
  if (historial.length < 2) return null
  return historial[0]
})

const autor = computed(() => (
  props.resenia?.autor?.nombreDisplay
  || props.resenia?.autor?.nombreUsuario
  || ''
))

function claseOpcion(opcion) {
  return opcion.polaridad === 'NEGATIVA'
    ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
    : 'border-rosa bg-blanco text-cafe'
}
</script>

<template>
  <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
          {{ titulo }}
        </p>
        <div class="mt-2 flex flex-wrap items-center gap-2">
          <ValoracionEstrellas
            :model-value="resenia.calificacionGeneral"
            readonly
            tamano="sm"
            etiqueta="Experiencia general"
          />
          <span class="text-sm font-semibold text-granate">
            {{ resenia.calificacionGeneral }}/5
          </span>
          <span
            v-if="resenia.editada"
            class="rounded-full border border-durazno bg-durazno/40 px-2 py-0.5 text-[11px] font-semibold text-granate"
          >
            Editada
          </span>
        </div>
        <p class="mt-1 text-xs text-cafe/50">
          <span v-if="autor">{{ autor }} · </span>
          {{ formatearFechaHora(resenia.fechaVersion || resenia.fechaCreacion) }}
        </p>
      </div>
    </div>

    <p
      v-if="resenia.descripcion"
      class="mt-4 whitespace-pre-line text-sm leading-relaxed text-cafe/80"
    >
      {{ resenia.descripcion }}
    </p>

    <dl v-if="resenia.valores?.length" class="mt-4 grid gap-2 sm:grid-cols-2">
      <div
        v-for="valor in resenia.valores"
        :key="valor.idCriterio"
        class="flex items-center justify-between gap-3 rounded-lg bg-rosa/10 px-3 py-2"
      >
        <dt class="text-xs text-cafe/70">{{ valor.nombreCriterio }}</dt>
        <dd class="flex items-center gap-1.5">
          <ValoracionEstrellas
            :model-value="valor.valor"
            readonly
            tamano="sm"
            :etiqueta="valor.nombreCriterio"
          />
          <span class="text-xs font-semibold text-granate">{{ valor.valor }}</span>
        </dd>
      </div>
    </dl>

    <div v-if="resenia.opciones?.length" class="mt-4 flex flex-wrap gap-2">
      <span
        v-for="opcion in resenia.opciones"
        :key="opcion.idOpcion"
        class="rounded-full border px-3 py-1 text-xs font-medium"
        :class="claseOpcion(opcion)"
      >
        {{ opcion.etiqueta }}
      </span>
    </div>

    <div
      v-if="resenia.imagenes?.length"
      class="mt-4 grid grid-cols-3 gap-2 sm:grid-cols-4"
    >
      <div
        v-for="imagen in resenia.imagenes"
        :key="imagen.idArchivo"
        class="aspect-square overflow-hidden rounded-lg border border-rosa/50 bg-rosa/10"
      >
        <ImagenGaleria :url="imagen.urlPreview" alt="" class="h-full w-full" />
      </div>
    </div>

    <div v-if="versionOriginal" class="mt-4">
      <button
        type="button"
        class="text-xs font-semibold text-coral transition hover:text-rojizo"
        :aria-expanded="verOriginal"
        @click="verOriginal = !verOriginal"
      >
        {{ verOriginal ? 'Ocultar versión original' : 'Ver versión original' }}
      </button>

      <div
        v-if="verOriginal"
        class="mt-3 rounded-xl border border-cafe/10 bg-cafe/5 p-4"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
          Versión original · {{ formatearFechaHora(versionOriginal.fechaVersion) }}
        </p>
        <div class="mt-2 flex items-center gap-2">
          <ValoracionEstrellas
            :model-value="versionOriginal.calificacionGeneral"
            readonly
            tamano="sm"
            etiqueta="Calificación original"
          />
          <span class="text-xs font-semibold text-cafe/70">
            {{ versionOriginal.calificacionGeneral }}/5
          </span>
        </div>
        <p
          v-if="versionOriginal.descripcion"
          class="mt-2 whitespace-pre-line text-sm leading-relaxed text-cafe/70"
        >
          {{ versionOriginal.descripcion }}
        </p>
        <dl v-if="versionOriginal.valores?.length" class="mt-3 grid gap-1.5 sm:grid-cols-2">
          <div
            v-for="valor in versionOriginal.valores"
            :key="valor.idCriterio"
            class="flex items-center justify-between gap-2 text-xs text-cafe/70"
          >
            <dt>{{ valor.nombreCriterio }}</dt>
            <dd class="font-semibold text-cafe">{{ valor.valor }}</dd>
          </div>
        </dl>
        <div v-if="versionOriginal.opciones?.length" class="mt-3 flex flex-wrap gap-1.5">
          <span
            v-for="opcion in versionOriginal.opciones"
            :key="opcion.idOpcion"
            class="rounded-full bg-blanco px-2.5 py-0.5 text-[11px] text-cafe/70"
          >
            {{ opcion.etiqueta }}
          </span>
        </div>
        <div
          v-if="versionOriginal.imagenes?.length"
          class="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-4"
        >
          <div
            v-for="imagen in versionOriginal.imagenes"
            :key="imagen.idArchivo"
            class="aspect-square overflow-hidden rounded-lg border border-rosa/50 bg-blanco"
          >
            <ImagenGaleria :url="imagen.urlPreview" alt="" class="h-full w-full" />
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="puedeEditar || puedeEliminar"
      class="mt-5 flex flex-wrap items-center gap-3 border-t border-rosa/40 pt-4"
    >
      <button
        v-if="puedeEditar"
        type="button"
        class="rounded-md border border-coral/40 px-4 py-2 text-sm font-semibold text-coral transition hover:bg-coral/10"
        @click="emit('editar')"
      >
        Editar reseña
      </button>
      <button
        v-if="puedeEliminar"
        type="button"
        class="rounded-md border border-rojizo/40 px-4 py-2 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10"
        @click="emit('eliminar')"
      >
        Eliminar reseña
      </button>
      <p v-if="puedeEditar && fechaLimiteEdicion" class="text-xs text-cafe/50">
        Podés editarla una sola vez hasta el {{ formatearFechaHora(fechaLimiteEdicion) }}.
      </p>
    </div>
  </div>
</template>
