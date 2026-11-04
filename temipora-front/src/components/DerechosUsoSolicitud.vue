<script setup>
import { computed } from 'vue'
import { formatearPrecio } from '../servicios/moneda'

const props = defineProps({
  derechos: { type: Array, default: () => [] },
  precioBase: { type: Number, default: 0 },
  moneda: { type: String, default: 'ARS' },
})

const seleccionados = defineModel({ type: Array, default: () => [] })

const porcentajeTotal = computed(() => props.derechos
  .filter((d) => seleccionados.value.includes(d.idDerechoTermino))
  .reduce((total, d) => total + (Number(d.porcentajeExtra) || 0), 0))

const montoEstimado = computed(
  () => Math.round((Number(props.precioBase) || 0) * porcentajeTotal.value) / 100,
)

function alternar(idDerechoTermino, marcado) {
  const siguiente = new Set(seleccionados.value)
  if (marcado) siguiente.add(idDerechoTermino)
  else siguiente.delete(idDerechoTermino)
  seleccionados.value = [...siguiente]
}
</script>

<template>
  <div class="rounded-xl border border-rosa/50 bg-blanco p-4">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h3 class="text-base font-medium text-granate">Derechos de uso</h3>
        <p class="mt-1 text-sm text-cafe/60">
          El uso personal está incluido. Si necesitás usarlo comercialmente, marcalo acá.
        </p>
      </div>
      <span
        v-if="porcentajeTotal > 0"
        class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
      >
        +{{ porcentajeTotal }}% estimado
      </span>
    </div>

    <p
      v-if="!derechos.length"
      class="mt-4 rounded-lg border border-dashed border-rosa/60 px-3 py-2 text-sm text-cafe/55"
    >
      El artista no ofrece derechos comerciales adicionales para este servicio.
    </p>

    <div v-else class="mt-4 space-y-2">
      <label
        v-for="d in derechos"
        :key="d.idDerechoTermino"
        class="flex cursor-pointer items-start gap-3 rounded-lg border px-3 py-2.5 transition"
        :class="seleccionados.includes(d.idDerechoTermino)
          ? 'border-coral/40 bg-coral/5'
          : 'border-rosa/40 bg-blanco hover:border-coral/30'"
      >
        <input
          type="checkbox"
          class="mt-0.5 h-4 w-4 accent-coral"
          :checked="seleccionados.includes(d.idDerechoTermino)"
          @change="alternar(d.idDerechoTermino, $event.target.checked)"
        />
        <span class="min-w-0 flex-1">
          <span class="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
            <span class="text-sm font-medium text-cafe">{{ d.nombre }}</span>
            <span class="text-sm font-semibold text-granate">
              +{{ d.porcentajeExtra ?? 0 }}%
            </span>
          </span>
          <span v-if="d.descripcion" class="mt-0.5 block text-xs leading-relaxed text-cafe/55">
            {{ d.descripcion }}
          </span>
        </span>
      </label>
    </div>

    <p v-if="porcentajeTotal > 0" class="mt-3 text-xs leading-relaxed text-cafe/60">
      Recargo estimado sobre el precio base de este dibujo:
      <span class="font-semibold text-granate">{{ formatearPrecio(montoEstimado, moneda) }}</span>.
      El artista confirmará el total final en el presupuesto.
    </p>
  </div>
</template>
