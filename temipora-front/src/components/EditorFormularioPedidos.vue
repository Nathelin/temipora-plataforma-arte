<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  tipos: { type: Array, default: () => [] },
})

const preguntas = defineModel({ type: Array, default: () => [] })

const arrastrando = ref(null)

const modal = ref({
  abierto: false,
  editando: false,
  indice: null,
  idTipoCampo: '',
  enunciado: '',
  ayuda: '',
  obligatoria: false,
  opciones: [],
})
const errorModal = ref('')

function siguienteUid() {
  return preguntas.value.reduce((max, p) => Math.max(max, Number(p.uid) || 0), 0) + 1
}

function codigoDeTipo(id) {
  return props.tipos.find((t) => t.idTipoCampo === Number(id))?.codigo || ''
}

const codigoModal = computed(() => codigoDeTipo(modal.value.idTipoCampo))
const requiereOpciones = computed(() => codigoModal.value === 'CASILLAS' || codigoModal.value === 'OPCIONES')
const esEncabezado = computed(() => codigoModal.value === 'ENCABEZADO')

function nombreTipo(codigo) {
  return props.tipos.find((t) => t.codigo === codigo)?.nombre || codigo
}

function abrirNueva() {
  modal.value = {
    abierto: true,
    editando: false,
    indice: null,
    idTipoCampo: '',
    enunciado: '',
    ayuda: '',
    obligatoria: false,
    opciones: [''],
  }
  errorModal.value = ''
}

function editarPregunta(indice) {
  const p = preguntas.value[indice]
  modal.value = {
    abierto: true,
    editando: true,
    indice,
    idTipoCampo: p.idTipoCampo,
    enunciado: p.enunciado,
    ayuda: p.ayuda,
    obligatoria: p.obligatoria,
    opciones: p.opciones.length ? [...p.opciones] : [''],
  }
  errorModal.value = ''
}

function cerrarModal() {
  modal.value.abierto = false
}

function confirmarPregunta() {
  errorModal.value = ''
  const enunciado = modal.value.enunciado.trim()
  if (!modal.value.idTipoCampo) {
    errorModal.value = 'Elegí un tipo de campo.'
    return
  }
  if (!enunciado) {
    errorModal.value = 'Completá el enunciado.'
    return
  }

  const codigo = codigoModal.value
  let opciones = []
  if (codigo === 'CASILLAS' || codigo === 'OPCIONES') {
    opciones = modal.value.opciones.map((o) => o.trim()).filter(Boolean)
    if (!opciones.length) {
      errorModal.value = 'Agregá al menos una opción.'
      return
    }
  }

  const pregunta = {
    uid: modal.value.editando && modal.value.indice !== null
      ? preguntas.value[modal.value.indice].uid
      : siguienteUid(),
    idTipoCampo: Number(modal.value.idTipoCampo),
    codigoTipo: codigo,
    enunciado,
    ayuda: modal.value.ayuda.trim(),
    obligatoria: codigo === 'ENCABEZADO' ? false : modal.value.obligatoria,
    opciones,
  }

  if (modal.value.editando && modal.value.indice !== null) {
    preguntas.value[modal.value.indice] = pregunta
  } else {
    preguntas.value.push(pregunta)
  }
  modal.value.abierto = false
}

function eliminarPregunta(indice) {
  preguntas.value.splice(indice, 1)
}

function onDragStart(indice) {
  arrastrando.value = indice
}

function onDragOver(indice) {
  if (arrastrando.value === null || arrastrando.value === indice) return
  const movida = preguntas.value.splice(arrastrando.value, 1)[0]
  preguntas.value.splice(indice, 0, movida)
  arrastrando.value = indice
}

function onDragEnd() {
  arrastrando.value = null
}
</script>

<template>
  <div class="space-y-4">
    <div v-if="preguntas.length" class="space-y-3">
      <div
        v-for="(p, i) in preguntas"
        :key="p.uid"
        draggable="true"
        class="group flex items-start gap-3 rounded-xl border border-rosa/50 bg-rosa/5 p-4 transition"
        :class="arrastrando === i ? 'opacity-60 ring-2 ring-coral' : 'hover:bg-rosa/10'"
        @dragstart="onDragStart(i)"
        @dragover.prevent="onDragOver(i)"
        @dragend="onDragEnd"
      >
        <span class="mt-0.5 cursor-grab text-cafe/40 group-hover:text-cafe/70" aria-hidden="true" title="Arrastrar para reordenar">
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
            <circle cx="9" cy="6" r="1.6" /><circle cx="15" cy="6" r="1.6" />
            <circle cx="9" cy="12" r="1.6" /><circle cx="15" cy="12" r="1.6" />
            <circle cx="9" cy="18" r="1.6" /><circle cx="15" cy="18" r="1.6" />
          </svg>
        </span>

        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-full bg-durazno/50 px-2.5 py-0.5 text-xs text-cafe">{{ nombreTipo(p.codigoTipo) }}</span>
            <span v-if="p.obligatoria" class="rounded-full bg-rojizo/10 px-2.5 py-0.5 text-xs text-rojizo">Obligatoria</span>
          </div>
          <p class="mt-1.5 text-cafe" :class="p.codigoTipo === 'ENCABEZADO' ? 'font-medium text-granate' : ''">
            {{ p.enunciado }}
          </p>
          <p v-if="p.ayuda" class="mt-0.5 text-xs text-cafe/60">{{ p.ayuda }}</p>
          <div v-if="p.opciones.length" class="mt-2 flex flex-wrap gap-1.5">
            <span
              v-for="(o, oi) in p.opciones"
              :key="oi"
              class="rounded-md bg-blanco px-2 py-0.5 text-xs text-cafe ring-1 ring-rosa/60"
            >
              {{ o }}
            </span>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-1">
          <button
            type="button"
            class="rounded-md p-1.5 text-cafe/60 hover:bg-rosa/20 hover:text-cafe transition"
            aria-label="Editar pregunta"
            @click="editarPregunta(i)"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L10.582 16.07a4.5 4.5 0 0 1-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 0 1 1.13-1.897l8.932-8.931ZM19.5 7.125 16.862 4.487" />
            </svg>
          </button>
          <button
            type="button"
            class="rounded-md p-1.5 text-cafe/60 hover:bg-rojizo/10 hover:text-rojizo transition"
            aria-label="Eliminar pregunta"
            @click="eliminarPregunta(i)"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673A2.25 2.25 0 0 1 15.916 21.75H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <p v-else class="rounded-xl border border-dashed border-rosa/60 px-4 py-8 text-center text-sm text-cafe/50">
      Todavía no agregaste preguntas.
    </p>

    <button
      type="button"
      class="inline-flex items-center gap-2 rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20"
      @click="abrirNueva"
    >
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
      </svg>
      Agregar pregunta
    </button>

    <div
      v-if="modal.abierto"
      class="fixed inset-0 z-[70] flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="w-full max-w-lg bg-blanco rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg text-granate mb-4">
          {{ modal.editando ? 'Modificar pregunta' : 'Agregar pregunta' }}
        </h3>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2">Tipo de campo <span class="text-rojizo">*</span></label>
            <select
              v-model="modal.idTipoCampo"
              class="block w-full rounded-lg border-0 py-2.5 px-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            >
              <option value="" disabled>Seleccioná un tipo</option>
              <option v-for="t in tipos" :key="t.idTipoCampo" :value="t.idTipoCampo">{{ t.nombre }}</option>
            </select>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">
              {{ esEncabezado ? 'Título' : 'Pregunta' }} <span class="text-rojizo">*</span>
            </label>
            <textarea
              v-model="modal.enunciado"
              rows="2"
              maxlength="250"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2">Ayuda (opcional)</label>
            <input
              v-model="modal.ayuda"
              type="text"
              maxlength="250"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
          </div>

          <div v-if="requiereOpciones">
            <label class="block text-sm text-cafe mb-2">Opciones <span class="text-rojizo">*</span></label>
            <div class="space-y-2">
              <div v-for="(op, oi) in modal.opciones" :key="oi" class="flex items-center gap-2">
                <input
                  v-model="modal.opciones[oi]"
                  type="text"
                  maxlength="150"
                  class="pl-4 pr-4 block w-full rounded-lg border-0 py-2 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
                />
                <button
                  type="button"
                  class="rounded-md p-1.5 text-cafe/60 hover:bg-rojizo/10 hover:text-rojizo transition"
                  aria-label="Quitar opción"
                  @click="modal.opciones.splice(oi, 1)"
                >
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
            <button
              type="button"
              class="mt-2 text-sm text-coral hover:text-rojizo transition"
              @click="modal.opciones.push('')"
            >
              + Agregar opción
            </button>
          </div>

          <label v-if="!esEncabezado" class="flex items-center gap-2 text-sm text-cafe">
            <input v-model="modal.obligatoria" type="checkbox" class="accent-coral" />
            <span>Respuesta obligatoria</span>
          </label>

          <p v-if="errorModal" class="text-rojizo text-sm">{{ errorModal }}</p>
        </div>

        <div class="flex items-center justify-end gap-3 mt-6">
          <button
            type="button"
            class="px-4 py-2 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/20 transition"
            @click="cerrarModal"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
            @click="confirmarPregunta"
          >
            {{ modal.editando ? 'Guardar cambios' : 'Agregar' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
