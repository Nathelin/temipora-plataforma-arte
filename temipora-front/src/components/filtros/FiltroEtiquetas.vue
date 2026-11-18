<script setup>
import { computed, ref } from 'vue'
import GrupoFiltro from './GrupoFiltro.vue'

const props = defineProps({
  etiquetas: { type: Array, default: () => [] },
  categorias: { type: Array, default: () => [] },
  seleccionadas: { type: Array, default: () => [] },
  excluidas: { type: Array, default: () => [] },
  conteos: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['cambiar'])

const busqueda = ref('')
const abiertas = ref(new Set())

const seleccion = computed(() => new Set((props.seleccionadas || []).map(String)))
const exclusion = computed(() => new Set((props.excluidas || []).map(String)))

const grupos = computed(() => {
  const texto = busqueda.value.trim().toLowerCase()
  const porCategoria = new Map()
  for (const categoria of props.categorias || []) {
    porCategoria.set(String(categoria.idCategoriaEtiqueta), {
      id: String(categoria.idCategoriaEtiqueta),
      nombre: categoria.nombre || 'Sin categoría',
      etiquetas: [],
    })
  }
  for (const etiqueta of props.etiquetas || []) {
    const clave = etiqueta.idCategoria != null ? String(etiqueta.idCategoria) : ''
    if (!porCategoria.has(clave)) {
      porCategoria.set(clave, {
        id: clave,
        nombre: etiqueta.nombreCategoria || 'Sin categoría',
        etiquetas: [],
      })
    }
    if (texto && !etiqueta.nombre.toLowerCase().includes(texto)) continue
    porCategoria.get(clave).etiquetas.push(etiqueta)
  }
  return [...porCategoria.values()]
    .filter((grupo) => grupo.etiquetas.length > 0)
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
})

const activas = computed(() => seleccion.value.size + exclusion.value.size)

function estaAbierta(id) {
  if (busqueda.value.trim()) return true
  if (abiertas.value.has(id)) return true
  // Una categoría con etiquetas elegidas siempre se ve.
  const ids = [...seleccion.value, ...exclusion.value]
  return (props.etiquetas || []).some(
    (e) => String(e.idCategoria) === id && ids.includes(String(e.idEtiqueta)),
  )
}

function alternarCategoria(id) {
  const siguientes = new Set(abiertas.value)
  if (siguientes.has(id)) siguientes.delete(id)
  else siguientes.add(id)
  abiertas.value = siguientes
}

function cantidad(etiqueta) {
  const valor = props.conteos?.[String(etiqueta.idEtiqueta)]
  return Number.isFinite(Number(valor)) ? Number(valor) : null
}

function alternar(etiqueta) {
  const id = String(etiqueta.idEtiqueta)
  if (seleccion.value.has(id)) {
    emit('cambiar', {
      idsEtiquetas: props.seleccionadas.filter((v) => String(v) !== id),
      idsEtiquetasExcluidas: props.excluidas,
    })
    return
  }
  if (exclusion.value.has(id)) {
    emit('cambiar', {
      idsEtiquetas: [...props.seleccionadas, id],
      idsEtiquetasExcluidas: props.excluidas.filter((v) => String(v) !== id),
    })
    return
  }
  emit('cambiar', {
    idsEtiquetas: [...props.seleccionadas, id],
    idsEtiquetasExcluidas: props.excluidas,
  })
}

function alternarExclusion(etiqueta) {
  const id = String(etiqueta.idEtiqueta)
  if (exclusion.value.has(id)) {
    emit('cambiar', {
      idsEtiquetas: props.seleccionadas,
      idsEtiquetasExcluidas: props.excluidas.filter((v) => String(v) !== id),
    })
    return
  }
  emit('cambiar', {
    idsEtiquetas: props.seleccionadas.filter((v) => String(v) !== id),
    idsEtiquetasExcluidas: [...props.excluidas, id],
  })
}

function limpiar() {
  busqueda.value = ''
  emit('cambiar', { idsEtiquetas: [], idsEtiquetasExcluidas: [] })
}
</script>

<template>
  <GrupoFiltro titulo="Etiquetas" :activos="activas">
    <div class="space-y-3">
      <input
        v-model="busqueda"
        type="search"
        placeholder="Buscar etiqueta..."
        class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
      />

      <p v-if="!grupos.length" class="px-1 py-2 text-xs text-cafe/50">
        No hay etiquetas que coincidan.
      </p>

      <div v-for="grupo in grupos" :key="grupo.id" class="rounded-lg">
        <button
          type="button"
          class="flex w-full items-center justify-between gap-2 rounded-lg px-1.5 py-1.5 text-left text-xs font-semibold uppercase tracking-[0.1em] text-cafe/55 transition hover:bg-rosa/20"
          :aria-expanded="estaAbierta(grupo.id)"
          @click="alternarCategoria(grupo.id)"
        >
          <span class="min-w-0 break-words">{{ grupo.nombre }}</span>
          <svg
            class="h-3.5 w-3.5 shrink-0 transition-transform"
            :class="estaAbierta(grupo.id) ? 'rotate-180' : ''"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
          </svg>
        </button>

        <ul v-show="estaAbierta(grupo.id)" class="space-y-0.5 pb-1 pl-1">
          <li
            v-for="etiqueta in grupo.etiquetas"
            :key="etiqueta.idEtiqueta"
            class="group flex items-center gap-1 rounded-lg px-1.5 py-1 transition hover:bg-rosa/20"
          >
            <label class="flex min-w-0 flex-1 cursor-pointer items-start gap-2 text-sm text-cafe">
              <input
                type="checkbox"
                class="mt-0.5 h-4 w-4 shrink-0 rounded border-borde text-coral focus:ring-coral"
                :checked="seleccion.has(String(etiqueta.idEtiqueta))"
                @change="alternar(etiqueta)"
              />
              <span
                class="min-w-0 flex-1 break-words leading-snug"
                :class="exclusion.has(String(etiqueta.idEtiqueta)) ? 'line-through opacity-50' : ''"
              >
                {{ etiqueta.nombre }}
              </span>
              <span v-if="cantidad(etiqueta) !== null" class="shrink-0 text-[11px] text-cafe/45">
                ({{ cantidad(etiqueta) }})
              </span>
            </label>
            <button
              type="button"
              class="inline-flex h-6 min-w-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
              :class="exclusion.has(String(etiqueta.idEtiqueta))
                ? 'border-rojizo bg-rojizo text-white'
                : 'border-coral/40 bg-coral/10 text-coral hover:border-coral hover:bg-coral hover:text-white'"
              :aria-label="exclusion.has(String(etiqueta.idEtiqueta))
                ? `Dejar de excluir ${etiqueta.nombre}`
                : `Excluir ${etiqueta.nombre}`"
              :title="exclusion.has(String(etiqueta.idEtiqueta)) ? 'Dejar de excluir' : 'Excluir'"
              @click="alternarExclusion(etiqueta)"
            >
              −
            </button>
          </li>
        </ul>
      </div>

      <button
        v-if="activas > 0"
        type="button"
        class="text-[11px] font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
        @click="limpiar"
      >
        Quitar etiquetas
      </button>
    </div>
  </GrupoFiltro>
</template>
