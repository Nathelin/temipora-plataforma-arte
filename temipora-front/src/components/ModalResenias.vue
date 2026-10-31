<script setup>
import ListaResenias from './ListaResenias.vue'

defineProps({
  abierto: { type: Boolean, default: false },
  titulo: { type: String, default: 'Reseñas' },
  resenias: { type: Array, default: () => [] },
  resumen: { type: Object, default: null },
  pagina: { type: Object, default: null },
  cargando: { type: Boolean, default: false },
  error: { type: String, default: '' },
  filtros: { type: Object, default: () => ({ calificacion: '', orden: 'RECIENTES' }) },
  servicios: { type: Array, default: () => [] },
  mostrarFiltroServicio: { type: Boolean, default: false },
  mostrarServicio: { type: Boolean, default: false },
  filtroOpcion: { type: String, default: '' },
})

const emit = defineEmits([
  'cerrar',
  'cambiar-filtros',
  'cambiar-pagina',
  'quitar-filtro-opcion',
])
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[80] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    :aria-label="titulo"
  >
    <div class="absolute inset-0 bg-granate/40 backdrop-blur-sm" aria-hidden="true" @click="emit('cerrar')"></div>
    <div class="relative flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-rosa bg-blanco shadow-xl">
      <header class="flex items-center justify-between gap-3 border-b border-rosa/50 px-5 py-4">
        <h2 class="text-lg font-semibold text-granate">{{ titulo }}</h2>
        <button
          type="button"
          class="rounded-full border border-rosa px-3 py-1 text-sm text-cafe transition hover:bg-rosa/20"
          aria-label="Cerrar reseñas"
          @click="emit('cerrar')"
        >
          Cerrar
        </button>
      </header>

      <div class="overflow-y-auto p-5">
        <div
          v-if="filtroOpcion"
          class="mb-4 flex flex-wrap items-center gap-2 rounded-xl bg-rosa/15 px-3 py-2 text-sm text-cafe"
        >
          <span>Filtrando por:</span>
          <span class="rounded-full bg-coral/10 px-3 py-0.5 font-semibold text-granate">
            {{ filtroOpcion }}
          </span>
          <button
            type="button"
            class="ml-auto text-xs font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
            @click="emit('quitar-filtro-opcion')"
          >
            Quitar filtro
          </button>
        </div>

        <ListaResenias
          :resenias="resenias"
          :resumen="resumen"
          :pagina="pagina"
          :cargando="cargando"
          :error="error"
          :filtros="filtros"
          :servicios="servicios"
          :mostrar-filtro-servicio="mostrarFiltroServicio"
          :mostrar-servicio="mostrarServicio"
          @cambiar-filtros="emit('cambiar-filtros', $event)"
          @cambiar-pagina="emit('cambiar-pagina', $event)"
        />
      </div>
    </div>
  </div>
</template>
