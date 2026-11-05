<script setup>
import { ref, computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { formatearPromedio } from '../servicios/resenias'

const props = defineProps({
  servicio: { type: Object, required: true },
  mostrarFavorito: { type: Boolean, default: false },
  esFavorito: { type: Boolean, default: false },
  actualizandoFavorito: { type: Boolean, default: false },
})

const emit = defineEmits(['alternar-favorito'])

const revelado = ref(false)

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
const inicialArtista = computed(() => (props.servicio.nombreArtista || '?').charAt(0).toUpperCase())

const precioMostrado = computed(() =>
  formatearPrecio(
    props.servicio.precioBaseMostrado ?? props.servicio.precioBase,
    props.servicio.monedaMostrada ?? props.servicio.moneda,
  ),
)
const precioConvertido = computed(
  () => !!props.servicio.monedaMostrada && props.servicio.monedaMostrada !== props.servicio.moneda,
)
const descuentoOferta = computed(() => {
  const valor = Number(props.servicio.descuentoOfertaPorcentaje)
  return Number.isFinite(valor) && valor > 0 ? valor : 0
})
const precioConDescuentoMostrado = computed(() =>
  formatearPrecio(
    props.servicio.precioConDescuentoEstimado,
    props.servicio.monedaMostrada ?? props.servicio.moneda,
  ),
)

const destino = computed(() => ({
  name: 'servicio-detalle',
  params: { id: props.servicio.idServicio },
  query: props.servicio.idOferta ? { oferta: props.servicio.idOferta } : {},
}))

function textoEntrega(s) {
  if (s.tiempoEntregaValor == null || !s.tiempoEntregaUnidad) return 'A convenir'
  return `${s.tiempoEntregaValor} ${s.tiempoEntregaUnidad.toLowerCase()}`
}
</script>

<template>
  <article class="group relative flex h-full flex-col overflow-hidden rounded-[22px] border border-borde/60 bg-superficie shadow-[0_10px_28px_rgba(74,29,46,0.08)] transition duration-300 hover:-translate-y-1 hover:border-coral/40 hover:shadow-[0_20px_44px_var(--sombra-acento)] focus-within:ring-2 focus-within:ring-coral/50">
    <router-link :to="destino" class="absolute inset-0 z-10 focus:outline-none">
      <span class="sr-only">Ver {{ servicio.nombre }}</span>
    </router-link>

    <div class="relative aspect-video w-full overflow-hidden bg-rosa/20">
      <img
        v-if="servicio.imagenPortada"
        :src="servicio.imagenPortada"
        :alt="servicio.nombre"
        class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
        :class="requiereAdvertencia && !revelado ? 'scale-105 blur-[18px]' : ''"
      />
      <span
        v-if="requiereAdvertencia && !revelado"
        class="pointer-events-none absolute inset-0 flex items-center justify-center gap-2 bg-granate/30 text-sm font-medium text-white drop-shadow"
        :title="`Contenido sensible: ${textoSensible}`"
      >
        <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
        </svg>
        Contenido sensible
      </span>

      <span
        v-if="esSensible"
        class="absolute right-3 top-3 rounded-full bg-granate/80 px-2.5 py-1 text-[11px] font-semibold text-inverso-contraste backdrop-blur-sm"
      >
        Sensible
      </span>
      <span
        v-if="descuentoOferta"
        class="absolute bottom-3 left-3 rounded-full bg-coral px-2.5 py-1 text-[11px] font-extrabold text-white shadow-sm"
      >
        Descuento de reactivación -{{ descuentoOferta }}%
      </span>

      <button
        v-if="mostrarFavorito"
        type="button"
        class="absolute left-3 top-3 z-20 flex h-8 w-8 items-center justify-center rounded-full border shadow-sm backdrop-blur-sm transition hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema disabled:cursor-wait disabled:opacity-60"
        :class="esFavorito
          ? 'border-coral bg-coral text-white hover:bg-rojizo'
          : 'border-white/70 bg-superficie/95 text-coral hover:bg-coral hover:text-white'"
        :disabled="actualizandoFavorito"
        :aria-pressed="esFavorito"
        :aria-label="esFavorito
          ? `Quitar ${servicio.nombre} de tus favoritos`
          : `Agregar ${servicio.nombre} a tus favoritos`"
        :title="esFavorito ? 'Quitar de favoritos' : 'Agregar a favoritos'"
        @click.stop="emit('alternar-favorito', servicio)"
      >
        <svg
          class="h-3.5 w-3.5"
          viewBox="0 0 24 24"
          :fill="esFavorito ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="1.8"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
        </svg>
      </button>
    </div>

    <div class="flex flex-1 flex-col p-4">
      <h3 class="line-clamp-1 font-display text-base font-bold leading-snug text-granate">
        {{ servicio.nombre }}
      </h3>

      <div class="mt-2 flex items-center gap-2">
        <img
          v-if="servicio.urlFotoArtista"
          :src="servicio.urlFotoArtista"
          alt=""
          class="h-6 w-6 shrink-0 rounded-full object-cover ring-1 ring-borde/50"
        />
        <span
          v-else
          class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-rosa/40 text-[11px] font-bold text-granate ring-1 ring-borde/50"
          aria-hidden="true"
        >
          {{ inicialArtista }}
        </span>
        <router-link
          :to="{ name: 'perfil', params: { nombreUsuario: servicio.nombreArtista } }"
          class="relative z-20 line-clamp-1 text-xs font-semibold text-cafe/60 transition hover:text-coral"
          @click.stop
        >
          @{{ servicio.nombreArtista }}
        </router-link>
      </div>

      <div class="mt-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-1.5 border-t border-borde/40 pt-3">
        <p class="font-display text-lg font-extrabold leading-none text-coral">
          <span class="mr-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-cafe/45">Desde</span>
          <span :title="descuentoOferta ? 'Precio estimado con tu descuento sobre el presupuesto final' : undefined">
            {{ descuentoOferta ? precioConDescuentoMostrado : precioMostrado }}
          </span>
          <span
            v-if="descuentoOferta"
            class="ml-1 text-[10px] font-semibold text-cafe/60 line-through"
            :title="`Precio base: ${precioMostrado}`"
          >
            {{ precioMostrado }}
          </span>
          <span v-if="precioConvertido" class="text-xs text-cafe/40" :title="`Precio original: ${formatearPrecio(servicio.precioBase, servicio.moneda)}`">≈</span>
        </p>

        <div class="flex items-center gap-1.5 text-[11px] font-semibold">
          <span
            v-if="servicio.cantidadResenias > 0"
            class="inline-flex items-center gap-1 rounded-full bg-mantequilla/50 px-2 py-0.5 text-granate"
            :title="`${servicio.cantidadResenias} reseña(s)`"
          >
            ★ {{ formatearPromedio(servicio.calificacionPromedio) }}
            <span class="font-normal text-cafe/55">({{ servicio.cantidadResenias }})</span>
          </span>
          <span
            v-else
            class="rounded-full bg-rosa/20 px-2 py-0.5 text-cafe/55"
            title="Todavía no tiene reseñas"
          >
            Nuevo
          </span>
          <span class="rounded-full bg-crema px-2 py-0.5 text-cafe/60" :title="`Entrega estimada: ${textoEntrega(servicio)}`">
            Entrega estimada: {{ textoEntrega(servicio) }}
          </span>
        </div>
      </div>

      <div
        v-if="requiereAdvertencia"
        class="mt-2 flex items-center justify-between gap-2 text-[11px] text-cafe/50"
      >
        <span>{{ revelado ? 'Mostrando contenido sensible' : 'Oculto por contenido sensible' }}</span>
        <button
          type="button"
          class="relative z-20 font-semibold text-coral underline decoration-coral/40 underline-offset-2 transition hover:text-rojizo"
          @click.stop.prevent="revelado = !revelado"
        >
          {{ revelado ? 'Ocultar' : 'Ver de todos modos' }}
        </button>
      </div>
    </div>
  </article>
</template>
