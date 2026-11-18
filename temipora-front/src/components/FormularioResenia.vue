<script setup>
import { computed, ref, watch } from 'vue'
import ValoracionEstrellas from './ValoracionEstrellas.vue'

const props = defineProps({
  criterios: { type: Array, default: () => [] },
  resenia: { type: Object, default: null },
  guardando: { type: Boolean, default: false },
  titulo: { type: String, default: 'Dejá tu reseña' },
  descripcionAyuda: { type: String, default: '' },
  textoGuardar: { type: String, default: 'Publicar reseña' },
  textoCancelar: { type: String, default: 'Cancelar' },
  anonimoForzado: { type: Boolean, default: false },
  permiteAnonimato: { type: Boolean, default: true },
  etiquetaAnonima: { type: String, default: 'Cliente verificado' },
  placeholderDescripcion: {
    type: String,
    default: 'Contá cómo fue el trabajo, la comunicación y la entrega',
  },
})

const emit = defineEmits(['guardar', 'cancelar'])

const descripcion = ref('')
const calificacion = ref(0)
const valores = ref({})
const opcionesSeleccionadas = ref([])
const mostrarNombre = ref(true)
const errorForm = ref('')

const claseCampo = 'block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral'

const polaridadEsperada = computed(() => {
  if (!calificacion.value) return 'POSITIVA'
  return calificacion.value >= 4 ? 'POSITIVA' : 'NEGATIVA'
})

function estadoInicial() {
  descripcion.value = props.resenia?.descripcion || ''
  calificacion.value = props.resenia?.calificacionGeneral || 0
  const mapa = {}
  for (const valor of props.resenia?.valores || []) {
    mapa[valor.idCriterio] = valor.valor
  }
  valores.value = mapa
  opcionesSeleccionadas.value = (props.resenia?.opciones || [])
    .map((opcion) => opcion.idOpcion)
  mostrarNombre.value = !props.permiteAnonimato
    ? true
    : (props.anonimoForzado ? false : (props.resenia?.mostrarNombre ?? true))
  errorForm.value = ''
}

watch(() => props.resenia, estadoInicial, { immediate: true })

function opcionesActivas(criterio) {
  return (criterio.opciones || []).filter((opcion) => opcion.activo)
}

function opcionesVisibles(criterio) {
  return opcionesActivas(criterio).filter(
    (opcion) => opcion.polaridad === polaridadEsperada.value,
  )
}

watch(calificacion, () => {
  const visibles = new Set()
  for (const criterio of props.criterios) {
    if (criterio.escala !== 'OPCIONES_MULTIPLES') continue
    for (const opcion of opcionesVisibles(criterio)) {
      visibles.add(opcion.idOpcionCriterio)
    }
  }
  opcionesSeleccionadas.value = opcionesSeleccionadas.value
    .filter((id) => visibles.has(id))
})

function fijarValor(idCriterio, valor) {
  valores.value = { ...valores.value, [idCriterio]: valor }
}

function alternarOpcion(idOpcion) {
  opcionesSeleccionadas.value = opcionesSeleccionadas.value.includes(idOpcion)
    ? opcionesSeleccionadas.value.filter((id) => id !== idOpcion)
    : [...opcionesSeleccionadas.value, idOpcion]
}

function tieneOpcionElegida(criterio) {
  return opcionesSeleccionadas.value.some((id) => (
    opcionesVisibles(criterio).some((opcion) => opcion.idOpcionCriterio === id)
  ))
}

function validar() {
  if (!calificacion.value) return 'Elegí una calificación de 1 a 5 estrellas.'

  for (const criterio of props.criterios) {
    if (!criterio.obligatorio) continue
    if (criterio.escala === 'ESTRELLAS' && !valores.value[criterio.idCriterioResenia]) {
      return `Completá el criterio "${criterio.nombre}".`
    }
    if (criterio.escala === 'OPCIONES_MULTIPLES'
      && opcionesVisibles(criterio).length
      && !tieneOpcionElegida(criterio)) {
      return `Elegí al menos una opción en "${criterio.nombre}".`
    }
  }

  return ''
}

function guardar() {
  errorForm.value = validar()
  if (errorForm.value) return

  emit('guardar', {
    descripcion: descripcion.value.trim() || null,
    calificacionGeneral: calificacion.value,
    mostrarNombre: props.anonimoForzado ? false : mostrarNombre.value,
    valores: props.criterios
      .filter((criterio) => criterio.escala === 'ESTRELLAS'
        && valores.value[criterio.idCriterioResenia])
      .map((criterio) => ({
        idCriterio: criterio.idCriterioResenia,
        valor: valores.value[criterio.idCriterioResenia],
      })),
    opciones: [...opcionesSeleccionadas.value],
  })
}
</script>

<template>
  <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
    <h3 class="text-lg font-semibold text-granate">{{ titulo }}</h3>
    <p v-if="descripcionAyuda" class="mt-1 text-sm leading-relaxed text-cafe/60">
      {{ descripcionAyuda }}
    </p>

    <div class="mt-5 space-y-5">
      <div>
        <p class="mb-2 text-sm text-cafe">
          Experiencia general <span class="text-rojizo">*</span>
        </p>
        <ValoracionEstrellas
          v-model="calificacion"
          etiqueta="Experiencia general"
        />
      </div>

      <div
        v-for="criterio in criterios"
        :key="criterio.idCriterioResenia"
        class="rounded-xl border border-rosa/40 bg-rosa/5 p-4"
      >
        <div class="flex flex-wrap items-baseline justify-between gap-2">
          <p class="text-sm font-medium text-granate">
            {{ criterio.nombre }}
            <span v-if="criterio.obligatorio" class="text-rojizo">*</span>
          </p>
          <span class="rounded-full bg-blanco px-2 py-0.5 text-[11px] font-semibold text-cafe/50">
            {{ criterio.escala === 'ESTRELLAS' ? '1 a 5 estrellas' : 'Opcional' }}
          </span>
        </div>
        <p v-if="criterio.descripcion" class="mt-1 text-xs leading-relaxed text-cafe/60">
          {{ criterio.descripcion }}
        </p>

        <div v-if="criterio.escala === 'ESTRELLAS'" class="mt-3">
          <ValoracionEstrellas
            :model-value="valores[criterio.idCriterioResenia] || 0"
            :etiqueta="criterio.nombre"
            tamano="sm"
            @update:model-value="fijarValor(criterio.idCriterioResenia, $event)"
          />
        </div>

        <div v-else class="mt-3">
          <p class="mb-2 text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">
            {{ polaridadEsperada === 'POSITIVA' ? 'Lo que destacó' : 'Lo que no funcionó' }}
          </p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="opcion in opcionesVisibles(criterio)"
              :key="opcion.idOpcionCriterio"
              type="button"
              class="rounded-full border px-3 py-1.5 text-sm transition"
              :class="opcionesSeleccionadas.includes(opcion.idOpcionCriterio)
                ? (polaridadEsperada === 'NEGATIVA'
                  ? 'border-rojizo bg-rojizo text-white'
                  : 'border-coral bg-coral text-white')
                : 'border-rosa bg-blanco text-cafe hover:bg-rosa/20'"
              :aria-pressed="opcionesSeleccionadas.includes(opcion.idOpcionCriterio)"
              @click="alternarOpcion(opcion.idOpcionCriterio)"
            >
              {{ opcion.etiqueta }}
            </button>
            <p v-if="!opcionesVisibles(criterio).length" class="text-xs text-cafe/50">
              Este criterio no tiene opciones para esta calificación.
            </p>
          </div>
        </div>
      </div>

      <div>
        <label for="resenia-descripcion" class="mb-2 block text-sm text-cafe">
          Descripción <span class="text-cafe/40">(opcional)</span>
        </label>
        <textarea
          id="resenia-descripcion"
          v-model="descripcion"
          rows="4"
          maxlength="1000"
          :class="[claseCampo, 'resize-none']"
          :placeholder="placeholderDescripcion"
        ></textarea>
        <p class="mt-1 text-right text-xs text-cafe/50">{{ descripcion.length }}/1000</p>
      </div>
    </div>

    <label
      v-if="permiteAnonimato"
      class="mt-5 flex items-start gap-3 rounded-xl border border-rosa/50 bg-rosa/5 p-4 text-sm text-cafe"
    >
      <input
        v-model="mostrarNombre"
        type="checkbox"
        :disabled="anonimoForzado"
        class="mt-0.5 h-4 w-4 shrink-0 rounded border-rosa text-coral focus:ring-coral disabled:opacity-50"
      />
      <span>
        Mostrar mi nombre junto a la reseña
        <span v-if="anonimoForzado" class="mt-1 block text-xs leading-relaxed text-cafe/55">
          Este contenido es sensible: la reseña se publica como
          “{{ etiquetaAnonima }}” y no se muestra tu identidad.
        </span>
        <span v-else class="mt-1 block text-xs leading-relaxed text-cafe/55">
          Si lo desactivás, se publica como “{{ etiquetaAnonima }}” y no se
          muestra tu identidad.
        </span>
      </span>
    </label>

    <p v-if="errorForm" role="alert" class="mt-4 text-sm text-rojizo">{{ errorForm }}</p>

    <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
      <button
        v-if="resenia"
        type="button"
        class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
        :disabled="guardando"
        @click="emit('cancelar')"
      >
        {{ textoCancelar }}
      </button>
      <button
        type="button"
        :disabled="guardando"
        class="rounded-md bg-coral px-4 py-2 text-sm text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
        @click="guardar"
      >
        {{ guardando ? 'Guardando...' : textoGuardar }}
      </button>
    </div>
  </div>
</template>
