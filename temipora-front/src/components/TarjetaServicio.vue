<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  servicio: { type: Object, required: true },
})

const router = useRouter()
const revelado = ref(false)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const esSensible = computed(() => !!props.servicio.contenidoSensible)
const textoSensible = computed(
  () => ETIQUETAS_SENSIBLE[props.servicio.contenidoSensible] || 'Contenido sensible',
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
  <article class="rounded-2xl border border-rosa/50 bg-blanco overflow-hidden flex flex-col">
    <button type="button" class="relative h-40 bg-rosa/20 w-full text-left" @click="irAlServicio">
      <img
        v-if="servicio.imagenPortada"
        :src="servicio.imagenPortada"
        :alt="servicio.nombre"
        class="w-full h-full object-cover transition duration-200"
        :class="esSensible && !revelado ? 'blur-[18px] scale-105' : ''"
      />
      <span
        v-if="esSensible && !revelado"
        class="absolute inset-0 flex items-center justify-center gap-2 bg-granate/20 text-white text-sm drop-shadow"
        :title="`Contenido sensible: ${textoSensible}`"
      >
        <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
        </svg>
        Contenido sensible
      </span>
    </button>

    <div class="p-4 flex-1 flex flex-col">
      <div class="flex items-start justify-between gap-2">
        <h3 class="text-granate">
          <button type="button" class="text-left hover:text-coral transition" @click="irAlServicio">
            {{ servicio.nombre }}
          </button>
        </h3>
        <span
          v-if="esSensible"
          class="shrink-0 rounded-full bg-coral/15 px-2.5 py-0.5 text-xs text-coral"
        >
          Sensible
        </span>
      </div>

      <button
        type="button"
        class="mt-1 text-left text-xs text-cafe/60 hover:text-coral transition"
        @click="irAlArtista"
      >
        Por @{{ servicio.nombreArtista }}
      </button>

      <p class="mt-1 text-xs text-cafe/60">{{ servicio.nombreTipoServicio }} · {{ servicio.nombreMedio }}</p>
      <p class="mt-3 text-sm text-cafe whitespace-pre-wrap line-clamp-2">{{ servicio.descripcion }}</p>

      <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-cafe/70">
        <span>Precio: <span class="text-granate">{{ servicio.precioBase }}</span></span>
        <span>Entrega: {{ textoEntrega(servicio) }}</span>
        <span>Cupos: {{ servicio.cupos }}</span>
      </div>

      <div
        v-if="esSensible && !revelado"
        class="mt-auto pt-4 flex items-center justify-between gap-3 border-t border-rosa/40 mt-4"
      >
        <span class="text-xs text-cafe/50">Oculto por contenido sensible</span>
        <button
          type="button"
          class="text-sm text-coral hover:text-rojizo transition underline"
          @click="revelado = true"
        >
          Ver de todos modos
        </button>
      </div>

      <div v-else-if="esSensible" class="mt-auto pt-4 flex items-center justify-between gap-3 border-t border-rosa/40 mt-4">
        <span class="text-xs text-cafe/50">Mostrando contenido sensible</span>
        <button type="button" class="text-sm text-cafe/60 hover:text-rojizo transition" @click="revelado = false">
          Ocultar
        </button>
      </div>

      <div class="mt-auto pt-4 flex items-center justify-end border-t border-rosa/40 mt-4">
        <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="irAlServicio">
          Ver servicio →
        </button>
      </div>
    </div>
  </article>
</template>
