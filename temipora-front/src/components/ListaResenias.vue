<script setup>
import ResumenResenias from './ResumenResenias.vue'
import TarjetaReseniaPublica from './TarjetaReseniaPublica.vue'
import { ORDENES_RESENIAS, etiquetaOrden } from '../servicios/resenias'

const props = defineProps({
  resenias: { type: Array, default: () => [] },
  resumen: { type: Object, default: null },
  pagina: { type: Object, default: null },
  cargando: { type: Boolean, default: false },
  error: { type: String, default: '' },
  filtros: { type: Object, default: () => ({ calificacion: '', orden: 'RECIENTES' }) },
  servicios: { type: Array, default: () => [] },
  mostrarFiltroServicio: { type: Boolean, default: false },
  mostrarServicio: { type: Boolean, default: false },
  mostrarResumen: { type: Boolean, default: true },
})

const emit = defineEmits(['cambiar-filtros', 'cambiar-pagina'])

function cambiar(campo, valor) {
  emit('cambiar-filtros', { ...props.filtros, [campo]: valor })
}

function hayAnterior() {
  return Number(props.pagina?.pagina || 0) > 0
}

function haySiguiente() {
  return Boolean(props.pagina?.hayMas)
}
</script>

<template>
  <div class="space-y-5">
    <ResumenResenias v-if="mostrarResumen" :resumen="resumen" />

    <div class="flex flex-wrap gap-2">
      <select
        :value="filtros.calificacion ?? ''"
        class="rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe"
        aria-label="Filtrar por calificación"
        @change="cambiar('calificacion', $event.target.value)"
      >
        <option value="">Todas las calificaciones</option>
        <option v-for="n in 5" :key="n" :value="String(n)">{{ n }} estrellas</option>
      </select>

      <select
        v-if="mostrarFiltroServicio"
        :value="filtros.idServicio ?? ''"
        class="rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe"
        aria-label="Filtrar por servicio"
        @change="cambiar('idServicio', $event.target.value)"
      >
        <option value="">Todos los servicios</option>
        <option v-for="servicio in servicios" :key="servicio.idServicio" :value="String(servicio.idServicio)">
          {{ servicio.nombre }}
        </option>
      </select>

      <select
        :value="filtros.orden || 'RECIENTES'"
        class="rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe"
        aria-label="Ordenar reseñas"
        @change="cambiar('orden', $event.target.value)"
      >
        <option v-for="orden in ORDENES_RESENIAS" :key="orden.valor" :value="orden.valor">
          {{ etiquetaOrden(orden.valor) }}
        </option>
      </select>
    </div>

    <p v-if="cargando" class="py-6 text-center text-sm text-cafe/55">Cargando reseñas...</p>
    <p v-else-if="error" role="alert" class="py-6 text-center text-sm text-rojizo">{{ error }}</p>
    <p v-else-if="!resenias.length" class="py-6 text-center text-sm text-cafe/55">
      No hay reseñas para estos filtros.
    </p>

    <div v-else class="space-y-3">
      <TarjetaReseniaPublica
        v-for="resenia in resenias"
        :key="resenia.idResenia"
        :resenia="resenia"
        :mostrar-servicio="mostrarServicio"
      />
    </div>

    <div
      v-if="!cargando && !error && pagina && pagina.totalPaginas > 1"
      class="flex items-center justify-between gap-3 border-t border-rosa/40 pt-4"
    >
      <p class="text-xs text-cafe/50">
        Página {{ Number(pagina.pagina || 0) + 1 }} de {{ pagina.totalPaginas }}
      </p>
      <div class="flex gap-2">
        <button
          type="button"
          class="rounded-md border border-rosa px-3 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="!hayAnterior()"
          @click="emit('cambiar-pagina', Number(pagina.pagina || 0) - 1)"
        >
          Anterior
        </button>
        <button
          type="button"
          class="rounded-md border border-rosa px-3 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:opacity-40"
          :disabled="!haySiguiente()"
          @click="emit('cambiar-pagina', Number(pagina.pagina || 0) + 1)"
        >
          Siguiente
        </button>
      </div>
    </div>
  </div>
</template>
