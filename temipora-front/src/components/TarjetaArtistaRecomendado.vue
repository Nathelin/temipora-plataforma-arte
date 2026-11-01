<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'

const props = defineProps({
  artista: { type: Object, required: true },
})

const nombre = computed(
  () => props.artista.nombreDisplay || props.artista.nombreUsuario || 'Artista',
)
const inicial = computed(() => (nombre.value || '?').charAt(0).toUpperCase())
const precio = computed(() =>
  props.artista.precioDesde != null
    ? formatearPrecio(props.artista.precioDesde, props.artista.moneda)
    : '',
)
const promedio = computed(() => {
  const valor = Number(props.artista.calificacionPromedio)
  return Number.isFinite(valor) && valor > 0 ? valor.toFixed(2) : null
})
const rutaPerfil = computed(() => ({
  name: 'perfil',
  params: { nombreUsuario: props.artista.nombreUsuario },
}))
</script>

<template>
  <router-link
    :to="rutaPerfil"
    class="group flex h-full flex-col overflow-hidden rounded-[28px] border border-borde/60 bg-superficie shadow-[0_12px_30px_rgba(74,29,46,0.08)] transition hover:-translate-y-0.5 hover:shadow-[0_18px_38px_rgba(74,29,46,0.14)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
  >
    <div class="relative aspect-[4/3] overflow-hidden bg-rosa/30">
      <img
        v-if="artista.imagenRepresentativa"
        :src="artista.imagenRepresentativa"
        :alt="`Trabajo de ${nombre}`"
        class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
        loading="lazy"
      />
      <div
        v-else
        class="flex h-full w-full items-center justify-center font-display text-5xl font-extrabold text-granate/25"
      >
        {{ inicial }}
      </div>
      <span
        v-if="artista.motivo"
        class="absolute left-3 top-3 rounded-full bg-superficie/90 px-3 py-1 text-xs font-bold text-granate shadow-sm backdrop-blur"
      >
        {{ artista.motivo }}
      </span>
    </div>

    <div class="flex flex-1 flex-col gap-3 p-5">
      <div class="flex items-center gap-3">
        <img
          v-if="artista.urlFotoPerfil"
          :src="artista.urlFotoPerfil"
          :alt="nombre"
          class="h-11 w-11 rounded-full object-cover ring-2 ring-rosa/40"
          loading="lazy"
        />
        <span
          v-else
          class="inline-flex h-11 w-11 items-center justify-center rounded-full bg-rosa/40 font-display text-lg font-extrabold text-granate"
        >
          {{ inicial }}
        </span>
        <div class="min-w-0">
          <p class="truncate font-display text-lg font-extrabold text-granate">
            {{ nombre }}
          </p>
          <p class="truncate text-sm text-cafe/60">@{{ artista.nombreUsuario }}</p>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
        <span v-if="promedio" class="inline-flex items-center gap-1 font-semibold text-granate">
          <span aria-hidden="true" class="text-coral">★</span>
          {{ promedio }}
          <span class="font-normal text-cafe/50">({{ artista.cantidadResenias }})</span>
        </span>
        <span v-if="precio" class="font-semibold text-coral">Desde {{ precio }}</span>
      </div>

      <p v-if="artista.nombreServicio" class="line-clamp-1 text-sm text-cafe/70">
        {{ artista.nombreServicio }}
      </p>

      <div v-if="artista.etiquetasDestacadas?.length" class="mt-auto flex flex-wrap gap-1.5">
        <span
          v-for="etiqueta in artista.etiquetasDestacadas"
          :key="etiqueta"
          class="rounded-full bg-rosa/25 px-2.5 py-1 text-xs font-semibold text-granate"
        >
          {{ etiqueta }}
        </span>
      </div>
    </div>
  </router-link>
</template>
