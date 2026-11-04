<script setup>
import { computed, ref } from 'vue'
import { AlertTriangle, Check, Copy, X } from '@lucide/vue'
import {
  actorVisible,
  formatearFecha,
  formatearFechaRelativa,
  nombreAccion,
  nombreCategoria,
  nombreEntidad,
  nombreOrigen,
} from '../../servicios/auditoriaCatalogo'

const props = defineProps({
  evento: { type: Object, default: null },
})

const emit = defineEmits(['cerrar', 'filtrar'])

const copiado = ref(false)

const cambios = computed(() => props.evento?.cambios ?? [])

async function copiarRequestId() {
  if (!props.evento?.requestId) return
  try {
    await navigator.clipboard.writeText(props.evento.requestId)
    copiado.value = true
    setTimeout(() => {
      copiado.value = false
    }, 1500)
  } catch {
    copiado.value = false
  }
}
</script>

<template>
  <div v-if="!evento" class="flex h-full min-h-64 flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-rosa/60 px-6 py-10 text-center">
    <p class="text-sm font-semibold text-granate">Detalle del evento</p>
    <p class="max-w-56 text-sm text-cafe/55">
      Seleccioná una fila de la tabla para ver los valores anteriores y nuevos, el actor y la operación completa.
    </p>
  </div>

  <div v-else class="flex flex-col gap-5">
    <header class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <div class="flex flex-wrap items-center gap-2">
          <span class="rounded-full bg-rosa/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-granate">
            {{ nombreCategoria(evento.categoria) }}
          </span>
          <span
            v-if="evento.critica"
            class="inline-flex items-center gap-1 rounded-full bg-coral/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-coral"
          >
            <AlertTriangle class="h-3 w-3" aria-hidden="true" />
            Crítica
          </span>
        </div>
        <h2 class="mt-2 text-base font-semibold leading-snug text-granate">
          {{ nombreAccion(evento.accion) }}
        </h2>
        <p class="mt-1 text-sm text-cafe/65">{{ evento.resumen }}</p>
      </div>
      <button
        type="button"
        class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-cafe/50 transition-colors hover:bg-rosa/20 hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
        aria-label="Cerrar detalle"
        @click="emit('cerrar')"
      >
        <X class="h-4 w-4" aria-hidden="true" />
      </button>
    </header>

    <dl class="grid grid-cols-[92px_minmax(0,1fr)] gap-x-3 gap-y-2 text-xs">
      <dt class="text-cafe/50">Fecha</dt>
      <dd class="text-cafe">
        {{ formatearFecha(evento.ocurridoEn) }}
        <span class="text-cafe/45">· {{ formatearFechaRelativa(evento.ocurridoEn) }}</span>
      </dd>

      <dt class="text-cafe/50">Actor</dt>
      <dd class="truncate text-cafe">{{ actorVisible(evento) }}</dd>

      <dt class="text-cafe/50">Origen</dt>
      <dd class="text-cafe">{{ nombreOrigen(evento.origen) }}</dd>

      <dt class="text-cafe/50">Entidad</dt>
      <dd class="truncate text-cafe">
        {{ nombreEntidad(evento.entidad) }}
        <span v-if="evento.entidadId" class="text-cafe/45">#{{ evento.entidadId }}</span>
      </dd>

      <dt class="text-cafe/50">Evento</dt>
      <dd class="tabular-nums text-cafe">#{{ evento.idEventoAuditoria }}</dd>

      <template v-if="evento.requestId">
        <dt class="text-cafe/50">Operación</dt>
        <dd class="flex items-center gap-2">
          <span class="min-w-0 break-all font-mono text-[11px] text-cafe/70">{{ evento.requestId }}</span>
          <button
            type="button"
            class="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-cafe/40 transition-colors hover:bg-rosa/20 hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
            :aria-label="copiado ? 'Identificador copiado' : 'Copiar identificador de la operación'"
            @click="copiarRequestId"
          >
            <Check v-if="copiado" class="h-3.5 w-3.5" aria-hidden="true" />
            <Copy v-else class="h-3.5 w-3.5" aria-hidden="true" />
          </button>
        </dd>
      </template>

      <template v-if="evento.motivo">
        <dt class="text-cafe/50">Motivo</dt>
        <dd class="text-cafe">{{ evento.motivo }}</dd>
      </template>
    </dl>

    <section>
      <h3 class="mb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/50">
        Cambios
      </h3>
      <div v-if="cambios.length" class="overflow-x-auto rounded-xl border border-rosa/40">
        <table class="w-full text-left text-xs">
          <thead>
            <tr class="border-b border-rosa/40 text-[10px] uppercase tracking-wide text-cafe/50">
              <th class="px-3 py-2 font-semibold">Campo</th>
              <th class="px-3 py-2 font-semibold">Antes</th>
              <th class="px-3 py-2 font-semibold">Después</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="cambio in cambios"
              :key="cambio.campo"
              class="border-b border-rosa/25 last:border-0"
            >
              <td class="px-3 py-2 align-top text-cafe/70">{{ cambio.etiqueta }}</td>
              <td class="bg-rosa/15 px-3 py-2 align-top text-cafe/60">{{ cambio.anterior ?? '—' }}</td>
              <td class="bg-coral/5 px-3 py-2 align-top font-medium text-granate">{{ cambio.nuevo ?? '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="text-xs text-cafe/50">
        Esta acción no modificó campos: solo registró el evento.
      </p>
    </section>

    <section class="flex flex-col gap-1.5 border-t border-rosa/40 pt-3 text-sm">
      <button
        v-if="evento.actorNombre"
        type="button"
        class="rounded-md py-1 text-left text-xs font-semibold text-coral transition-colors hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
        @click="emit('filtrar', { tipo: 'actor', valor: evento.actorNombre })"
      >
        Ver eventos de este actor
      </button>
      <button
        v-if="evento.entidad"
        type="button"
        class="rounded-md py-1 text-left text-xs font-semibold text-coral transition-colors hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
        @click="emit('filtrar', { tipo: 'entidad', entidad: evento.entidad, entidadId: evento.entidadId })"
      >
        Ver historial de esta entidad
      </button>
      <button
        v-if="evento.requestId && evento.eventosOperacion > 1"
        type="button"
        class="rounded-md py-1 text-left text-xs font-semibold text-coral transition-colors hover:text-granate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 motion-reduce:transition-none"
        @click="emit('filtrar', { tipo: 'requestId', valor: evento.requestId })"
      >
        Ver toda la operación ({{ evento.eventosOperacion }} eventos)
      </button>
    </section>
  </div>
</template>
