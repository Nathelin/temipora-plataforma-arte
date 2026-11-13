<script setup>
import { ref, computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'
import { etiquetaDisponibilidad } from '../servicios/productos'
import { formatearPromedio } from '../servicios/resenias'
import ValoracionEstrellas from './ValoracionEstrellas.vue'

const props = defineProps({
  producto: { type: Object, required: true },
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

const esSensible = computed(() => !!props.producto.contenidoSensible)
const requiereAdvertencia = computed(
  () => esSensible.value && !revelado.value,
)
const tiposSensibles = computed(() => {
  const tipos = props.producto.contenidosSensibles || []
  return tipos.map((t) => ETIQUETAS_SENSIBLE[t] || t).join(' · ')
})
const inicialArtista = computed(
  () => (props.producto.nombreArtista || '?').charAt(0).toUpperCase(),
)
const precioMostrado = computed(
  () => formatearPrecio(props.producto.precioDesde, props.producto.moneda || 'ARS'),
)
const sinStock = computed(() => props.producto.disponible === false)
const esReventa = computed(() => !!props.producto.esReventa)
const disponibilidad = computed(
  () => (esReventa.value ? 'En reventa' : etiquetaDisponibilidad(props.producto)),
)
const tieneResenias = computed(
  () => Number(props.producto.cantidadResenias || 0) > 0,
)
const estrellasPromedio = computed(
  () => Math.round(Number(props.producto.calificacionPromedio || 0)),
)

const destino = computed(() => (
  esReventa.value
    ? { name: 'reventa-publica', params: { id: props.producto.idPublicacionReventa } }
    : { name: 'producto-publico', params: { id: props.producto.idProducto } }
))
</script>

<template>
  <article class="group relative flex h-full flex-col overflow-hidden rounded-[22px] border border-borde/60 bg-superficie shadow-[0_10px_28px_rgba(74,29,46,0.08)] transition duration-300 hover:-translate-y-1 hover:border-coral/40 hover:shadow-[0_20px_44px_var(--sombra-acento)] focus-within:ring-2 focus-within:ring-coral/50">
    <router-link :to="destino" class="absolute inset-0 z-10 focus:outline-none">
      <span class="sr-only">Ver {{ producto.titulo }}</span>
    </router-link>

    <div class="relative aspect-[4/3] w-full overflow-hidden bg-rosa/20">
      <img
        v-if="producto.imagenPortada"
        :src="producto.imagenPortada"
        :alt="producto.titulo"
        class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.04]"
        :class="requiereAdvertencia ? 'scale-105 blur-[18px]' : ''"
      />
      <span
        v-if="requiereAdvertencia"
        class="pointer-events-none absolute inset-0 flex items-center justify-center gap-2 bg-granate/30 text-sm font-medium text-white drop-shadow"
        :title="`Contenido sensible: ${tiposSensibles || 'sin detallar'}`"
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
        v-if="producto.esPiezaUnica && !sinStock"
        class="absolute bottom-3 left-3 rounded-full bg-mantequilla px-2.5 py-1 text-[11px] font-bold text-tinta shadow-sm"
      >
        Pieza única
      </span>
      <span
        v-if="sinStock"
        class="absolute bottom-3 right-3 rounded-full bg-superficie/95 px-2.5 py-1 text-[11px] font-bold text-granate ring-1 ring-borde/70 backdrop-blur-sm"
      >
        Sin stock
      </span>
      <span
        v-if="esReventa"
        class="absolute bottom-3 right-3 rounded-full bg-coral px-2.5 py-1 text-[11px] font-bold text-white shadow-sm"
      >
        Reventa
      </span>

      <button
        v-if="mostrarFavorito && !esReventa"
        type="button"
        class="absolute left-3 top-3 z-20 flex h-8 w-8 items-center justify-center rounded-full border shadow-sm backdrop-blur-sm transition hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema disabled:cursor-wait disabled:opacity-60"
        :class="esFavorito
          ? 'border-coral bg-coral text-white hover:bg-rojizo'
          : 'border-white/70 bg-superficie/95 text-coral hover:bg-coral hover:text-white'"
        :disabled="actualizandoFavorito"
        :aria-pressed="esFavorito"
        :aria-label="esFavorito
          ? `Quitar ${producto.titulo} de tus favoritos`
          : `Agregar ${producto.titulo} a tus favoritos`"
        :title="esFavorito ? 'Quitar de favoritos' : 'Agregar a favoritos'"
        @click.stop="emit('alternar-favorito', producto)"
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

    <div class="flex flex-1 flex-col p-3.5">
      <h3 class="line-clamp-1 font-display text-base font-bold leading-snug text-granate">
        {{ producto.titulo }}
        <span
          v-if="esReventa && producto.tituloOriginal"
          class="font-sans text-[11px] font-normal text-cafe/45"
        >
          · {{ producto.tituloOriginal }}
        </span>
      </h3>

      <router-link
        :to="`/perfil/${producto.nombreArtista}`"
        class="relative z-20 mt-2 flex w-fit items-center gap-2"
        @click.stop
      >
        <img
          v-if="producto.urlFotoArtista"
          :src="producto.urlFotoArtista"
          alt=""
          class="h-6 w-6 rounded-full object-cover ring-1 ring-borde/50"
        />
        <span
          v-else
          class="flex h-6 w-6 items-center justify-center rounded-full bg-rosa/40 text-[10px] font-bold text-granate"
        >
          {{ inicialArtista }}
        </span>
        <span class="text-xs font-medium text-cafe/70 transition hover:text-coral">
          @{{ producto.nombreArtista }}
        </span>
      </router-link>

      <p v-if="esReventa && producto.nombreVendedor" class="mt-2 line-clamp-1 text-xs text-cafe/55">
        En reventa por @{{ producto.nombreVendedor }}
      </p>
      <p v-else class="mt-2 line-clamp-1 text-xs text-cafe/55">
        {{ producto.nombreTipoProducto }}
        <template v-if="producto.nombreMedio"> · {{ producto.nombreMedio }}</template>
      </p>

      <router-link
        v-if="esReventa"
        :to="destino"
        class="relative z-20 mt-2 inline-flex w-fit items-center gap-1 text-[11px] font-semibold text-coral transition hover:text-rojizo"
        @click.stop
      >
        Ver publicación de reventa →
      </router-link>

      <div
        v-if="tieneResenias"
        class="mt-1.5 flex items-center justify-between gap-2 text-xs text-cafe/60"
      >
        <span class="flex items-center gap-1.5">
          <ValoracionEstrellas
            :model-value="estrellasPromedio"
            readonly
            tamano="sm"
            etiqueta="Calificación del producto"
          />
          <span class="font-semibold text-granate">
            {{ formatearPromedio(producto.calificacionPromedio) }}
          </span>
          <span>({{ producto.cantidadResenias }})</span>
        </span>
        <router-link
          v-if="!esReventa"
          :to="{ name: 'producto-publico', params: { id: producto.idProducto }, hash: '#resenias' }"
          class="relative z-20 shrink-0 font-semibold text-coral transition hover:text-rojizo"
          @click.stop
        >
          Ver reseñas
        </router-link>
      </div>

      <div class="mt-auto flex items-end justify-between gap-3 pt-4">
        <p class="font-display text-base font-extrabold text-coral">
          <span class="mr-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-cafe/45">
            {{ esReventa ? 'Precio' : 'Desde' }}
          </span>
          {{ precioMostrado }}
        </p>
        <span class="shrink-0 rounded-full bg-crema px-2.5 py-0.5 text-[11px] font-semibold text-cafe">
          {{ disponibilidad }}
        </span>
      </div>

      <button
        v-if="requiereAdvertencia"
        type="button"
        class="relative z-20 mt-3 rounded-lg border border-rosa px-3 py-1.5 text-xs font-medium text-cafe transition hover:bg-rosa/20"
        @click.stop="revelado = true"
      >
        Ver de todos modos
      </button>
    </div>
  </article>
</template>
