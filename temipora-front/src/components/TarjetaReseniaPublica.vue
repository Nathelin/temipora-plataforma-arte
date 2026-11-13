<script setup>
import { computed, onMounted, ref } from 'vue'
import { formatearFechaHora } from '../servicios/pedidos'
import ValoracionEstrellas from './ValoracionEstrellas.vue'
import ModalReporteResenia from './ModalReporteResenia.vue'
import { useMisReportesResenia } from '../composables/useMisReportesResenia'

const props = defineProps({
  resenia: { type: Object, required: true },
  mostrarServicio: { type: Boolean, default: false },
})

const modalAbierto = ref(false)
const idUsuario = Number(localStorage.getItem('idUsuario'))
const sesionIniciada = Boolean(localStorage.getItem('token'))
const { asegurarCarga, marcarReportada, estaReportada } = useMisReportesResenia()

const autor = computed(() => (
  props.resenia.autor?.nombreDisplay
  || props.resenia.autor?.nombreUsuario
  || ''
))
const foto = computed(() => (
  props.resenia.autor?.urlFotoPerfilMiniatura
  || props.resenia.autor?.urlFotoPerfil
  || ''
))
const inicial = computed(() => (
  autor.value || props.resenia.autorMostrado || 'C'
).charAt(0).toUpperCase())

const esPropia = computed(() => (
  sesionIniciada
  && Number(props.resenia.autor?.idUsuario) === idUsuario
))
const reportada = computed(() => estaReportada(props.resenia.idResenia))
const puedeReportar = computed(() => sesionIniciada && !esPropia.value)

onMounted(() => {
  if (puedeReportar.value) asegurarCarga()
})

function alReportar(idResenia) {
  marcarReportada(idResenia)
}

function claseOpcion(opcion) {
  return opcion.polaridad === 'NEGATIVA'
    ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
    : 'border-rosa bg-blanco text-cafe'
}
</script>

<template>
  <article class="rounded-2xl border border-rosa/50 bg-blanco p-5">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div class="flex min-w-0 items-center gap-3">
        <img
          v-if="foto && !resenia.anonima"
          :src="foto"
          alt=""
          class="h-9 w-9 rounded-full border border-rosa/50 object-cover"
        />
        <span
          v-else
          class="flex h-9 w-9 items-center justify-center rounded-full bg-rosa/40 text-sm font-bold text-granate"
        >
          {{ inicial }}
        </span>
        <div class="min-w-0">
          <p class="truncate text-sm font-semibold text-granate">
            {{ resenia.anonima
              ? (resenia.autorMostrado || 'Cliente verificado')
              : (autor || resenia.autorMostrado) }}
          </p>
          <p class="text-xs text-cafe/50">
            {{ formatearFechaHora(resenia.fechaVersion) }}
            <span v-if="mostrarServicio && (resenia.nombreProducto || resenia.nombreServicio)">
              · {{ resenia.nombreProducto || resenia.nombreServicio }}
            </span>
          </p>
          <p v-if="resenia.nombreVariante" class="text-xs text-cafe/45">
            Variante: {{ resenia.nombreVariante }}
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <ValoracionEstrellas
          :model-value="resenia.calificacionGeneral"
          readonly
          tamano="sm"
          etiqueta="Calificación"
        />
        <span class="text-sm font-semibold text-granate">
          {{ resenia.calificacionGeneral }}/5
        </span>
        <span
          v-if="resenia.compraVerificada"
          class="rounded-full border border-green-700/25 bg-green-500/10 px-2 py-0.5 text-[11px] font-semibold text-green-700"
        >
          Compra verificada
        </span>
        <span
          v-if="resenia.editada"
          class="rounded-full border border-durazno bg-durazno/40 px-2 py-0.5 text-[11px] font-semibold text-granate"
        >
          Editada
        </span>
        <button
          v-if="puedeReportar && !reportada"
          type="button"
          class="inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-semibold text-cafe/45 transition hover:bg-rojizo/5 hover:text-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50"
          :aria-label="`Reportar reseña de ${resenia.nombreProducto || resenia.nombreServicio || 'esta pieza'}`"
          @click="modalAbierto = true"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M5 21V4m0 0h11l-2.5 4L16 12H5" />
          </svg>
          Reportar
        </button>
        <span
          v-else-if="puedeReportar && reportada"
          class="text-[11px] font-semibold text-cafe/40"
        >
          Reportada
        </span>
      </div>
    </div>

    <p
      v-if="resenia.descripcion"
      class="mt-3 whitespace-pre-line text-sm leading-relaxed text-cafe/80"
    >
      {{ resenia.descripcion }}
    </p>

    <p
      v-if="resenia.cantidadVariantesCompradas > 1"
      class="mt-3 rounded-lg bg-rosa/10 px-3 py-2 text-xs leading-relaxed text-cafe/70"
    >
      Compró {{ resenia.cantidadVariantesCompradas }} variantes:
      <strong class="text-cafe/85">{{ resenia.variantesCompradas }}</strong>.
    </p>

    <dl v-if="resenia.valores?.length" class="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
      <div
        v-for="valor in resenia.valores"
        :key="valor.idCriterio"
        class="rounded-lg bg-rosa/10 px-3 py-2 text-xs"
      >
        <dt class="text-cafe/65">{{ valor.nombreCriterio }}</dt>
        <dd class="mt-1 flex items-center gap-1 font-semibold text-granate">
          <span class="text-coral" aria-hidden="true">★</span>
          {{ valor.valor }}
        </dd>
      </div>
    </dl>

    <div v-if="resenia.opciones?.length" class="mt-3 flex flex-wrap gap-1.5">
      <span
        v-for="opcion in resenia.opciones"
        :key="opcion.idOpcion"
        class="rounded-full border px-2.5 py-0.5 text-[11px] font-medium"
        :class="claseOpcion(opcion)"
      >
        {{ opcion.etiqueta }}
      </span>
    </div>

    <ModalReporteResenia
      :abierto="modalAbierto"
      :resenia="resenia"
      @cerrar="modalAbierto = false"
      @reportado="alReportar"
    />
  </article>
</template>
