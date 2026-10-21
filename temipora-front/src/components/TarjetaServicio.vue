<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatearPrecio } from '../servicios/moneda'

const props = defineProps({
  servicio: { type: Object, required: true },
})

const router = useRouter()
const revelado = ref(false)
const mostrarInfoTipo = ref(false)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const esSensible = computed(() => !!props.servicio.contenidoSensible)
const requiereAdvertencia = computed(() => !!props.servicio.requiereAdvertencia)
const tiposSensibles = computed(() => {
  const tipos = props.servicio.contenidosSensibles || []
  return tipos.map((t) => ETIQUETAS_SENSIBLE[t] || t).join(' · ')
})
const textoSensible = computed(() => tiposSensibles.value || 'Contenido sensible')

const precioMostrado = computed(() =>
  formatearPrecio(
    props.servicio.precioBaseMostrado ?? props.servicio.precioBase,
    props.servicio.monedaMostrada ?? props.servicio.moneda,
  ),
)
const precioConvertido = computed(
  () => !!props.servicio.monedaMostrada && props.servicio.monedaMostrada !== props.servicio.moneda,
)

function textoEntrega(s) {
  return `${s.tiempoEntregaValor} ${(s.tiempoEntregaUnidad || '').toLowerCase()}`
}

function irAlServicio() {
  router.push({ name: 'servicio-detalle', params: { id: props.servicio.idServicio } })
}

function irAlArtista() {
  router.push({ name: 'perfil', params: { nombreUsuario: props.servicio.nombreArtista } })
}
</script>

<template>
  <article class="group flex h-full flex-col overflow-hidden rounded-[26px] border border-borde/60 bg-superficie shadow-[0_12px_32px_rgba(74,29,46,0.08)] transition duration-300 hover:-translate-y-1.5 hover:border-coral/40 hover:shadow-[0_22px_48px_rgba(194,24,91,0.16)]">
    <button type="button" class="relative h-48 w-full overflow-hidden bg-rosa/20 text-left" @click="irAlServicio">
      <img
        v-if="servicio.imagenPortada"
        :src="servicio.imagenPortada"
        :alt="servicio.nombre"
        class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
        :class="requiereAdvertencia && !revelado ? 'blur-[18px] scale-105' : ''"
      />
      <span
        v-if="requiereAdvertencia && !revelado"
        class="absolute inset-0 flex items-center justify-center gap-2 bg-granate/30 text-sm font-medium text-white drop-shadow"
        :title="`Contenido sensible: ${textoSensible}`"
      >
        <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
        </svg>
        Contenido sensible
      </span>

      <span
        v-if="esSensible"
        class="absolute right-4 top-4 rounded-full bg-granate/80 px-3 py-1 text-xs font-semibold text-inverso-contraste backdrop-blur-sm"
      >
        Sensible
      </span>
    </button>

    <div class="flex flex-1 flex-col p-5">
      <div class="flex items-start justify-between gap-3">
        <h3 class="font-display text-xl font-bold leading-snug text-granate">
          <button type="button" class="text-left transition hover:text-coral" @click="irAlServicio">
            {{ servicio.nombre }}
          </button>
        </h3>
      </div>

      <button
        type="button"
        class="mt-1.5 w-fit text-left text-xs font-semibold text-cafe/60 transition hover:text-coral"
        @click="irAlArtista"
      >
        Por @{{ servicio.nombreArtista }}
      </button>

      <div class="mt-3 flex flex-wrap items-center gap-1.5">
        <span
          v-if="servicio.nombreTipoServicio"
          class="rounded-full bg-rosa/30 px-2.5 py-1 text-[11px] font-semibold text-cafe"
        >
          {{ servicio.nombreTipoServicio }}
        </span>
        <button
          v-if="servicio.descripcionTipoServicio"
          type="button"
          class="flex h-5 w-5 items-center justify-center rounded-full text-cafe/40 transition hover:bg-rosa/20 hover:text-cafe"
          :aria-label="`Qué incluye ${servicio.nombreTipoServicio}`"
          :aria-expanded="mostrarInfoTipo"
          :aria-controls="`info-tipo-${servicio.idServicio}`"
          :title="`Qué incluye ${servicio.nombreTipoServicio}`"
          @click="mostrarInfoTipo = !mostrarInfoTipo"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" />
          </svg>
        </button>
        <span v-if="servicio.nombreMedio" class="rounded-full bg-durazno/50 px-2.5 py-1 text-[11px] font-semibold text-cafe">
          {{ servicio.nombreMedio }}
        </span>
      </div>

      <p
        v-if="mostrarInfoTipo && servicio.descripcionTipoServicio"
        :id="`info-tipo-${servicio.idServicio}`"
        class="mt-2 rounded-lg bg-rosa/15 px-3 py-2 text-xs leading-relaxed text-cafe/70"
      >
        {{ servicio.descripcionTipoServicio }}
      </p>

      <p class="mt-3 line-clamp-2 text-sm leading-relaxed text-cafe/75">{{ servicio.descripcion }}</p>

      <div class="mt-4 flex items-end justify-between gap-3">
        <div>
          <p class="text-[11px] uppercase tracking-[0.12em] text-cafe/45">Desde</p>
          <p class="font-display text-xl font-extrabold text-coral">
            {{ precioMostrado }}
            <span v-if="precioConvertido" class="text-sm text-cafe/40" :title="`Precio original: ${formatearPrecio(servicio.precioBase, servicio.moneda)}`">≈</span>
          </p>
        </div>
        <div class="flex flex-wrap justify-end gap-1.5 text-[11px] font-medium text-cafe/60">
          <span class="rounded-full bg-crema px-2.5 py-1">{{ textoEntrega(servicio) }}</span>
          <span class="rounded-full bg-crema px-2.5 py-1">{{ servicio.cupos }} cupos</span>
        </div>
      </div>

      <div
        v-if="requiereAdvertencia && !revelado"
        class="mt-4 flex items-center justify-between gap-3 border-t border-borde/40 pt-4"
      >
        <span class="text-xs text-cafe/50">Oculto por contenido sensible</span>
        <button type="button" class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo" @click="revelado = true">
          Ver de todos modos
        </button>
      </div>

      <div v-else-if="requiereAdvertencia" class="mt-4 flex items-center justify-between gap-3 border-t border-borde/40 pt-4">
        <span class="text-xs text-cafe/50">Mostrando contenido sensible</span>
        <button type="button" class="text-sm font-semibold text-cafe/60 transition hover:text-rojizo" @click="revelado = false">
          Ocultar
        </button>
      </div>

      <div class="mt-auto flex justify-end border-t border-borde/40 pt-4">
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-full bg-rosa/35 px-4 py-2 text-sm font-bold text-coral transition hover:bg-coral hover:text-white"
          @click="irAlServicio"
        >
          Ver servicio
          <span aria-hidden="true">→</span>
        </button>
      </div>
    </div>
  </article>
</template>
