<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  titulo: { type: String, required: true },
  secciones: { type: Array, required: true },
  inferior: { type: Array, default: () => [] },
  badges: { type: Object, default: () => ({}) },
})

const route = useRoute()
const menuAbierto = ref(false)

const todos = computed(() => props.secciones.flatMap((seccion) => seccion.items))
const esActiva = (item) => (item.base
  ? route.path.startsWith(item.base)
  : route.path === item.to)
const nombreActivo = computed(
  () => todos.value.find(esActiva)?.nombre || props.titulo,
)

function badgeDe(item) {
  const valor = Number(props.badges[item.to] || 0)
  if (!Number.isFinite(valor) || valor <= 0) return ''
  return valor > 99 ? '99+' : String(valor)
}
</script>

<template>
  <div class="w-full bg-crema text-cafe">
    <div class="mx-auto max-w-screen-2xl px-4 py-8 sm:px-8 lg:flex lg:gap-8">
      <aside class="mb-4 shrink-0 lg:mb-0 lg:w-64">
        <div class="lg:sticky lg:top-16">
          <h1 class="sr-only font-display text-xl font-bold text-granate lg:not-sr-only lg:mb-4 lg:block lg:px-1">{{ titulo }}</h1>

          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 rounded-xl border border-rosa bg-blanco px-4 py-3 text-sm font-medium text-granate shadow-sm transition hover:bg-rosa/10 lg:hidden"
            :aria-expanded="menuAbierto"
            @click="menuAbierto = !menuAbierto"
          >
            <span>{{ nombreActivo }}</span>
            <svg
              class="h-4 w-4 shrink-0 transition-transform"
              :class="menuAbierto ? 'rotate-180' : ''"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
            </svg>
          </button>

          <nav
            class="mt-2 space-y-5 lg:mt-0 lg:block"
            :class="menuAbierto ? 'block' : 'hidden'"
            :aria-label="titulo"
          >
            <div v-for="seccion in secciones" :key="seccion.titulo">
              <p class="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-cafe/40">
                {{ seccion.titulo }}
              </p>
              <div class="space-y-1">
                <router-link
                  v-for="item in seccion.items"
                  :key="item.to"
                  :to="item.to"
                  class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition"
                  :class="esActiva(item)
                    ? 'bg-rosa/40 font-medium text-granate'
                    : 'text-cafe hover:bg-rosa/20'"
                  :aria-current="esActiva(item) ? 'page' : undefined"
                  @click="menuAbierto = false"
                >
                  <svg
                    class="h-5 w-5 shrink-0"
                    :class="esActiva(item) ? 'text-coral' : 'text-cafe/50'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.7"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" :d="item.icono" />
                  </svg>
                  <span>{{ item.nombre }}</span>
                  <span
                    v-if="badgeDe(item)"
                    class="ml-auto inline-flex min-w-5 items-center justify-center rounded-full bg-coral px-1.5 py-0.5 text-[11px] font-bold text-white"
                    :aria-label="`${badgeDe(item)} pendientes`"
                  >
                    {{ badgeDe(item) }}
                  </span>
                </router-link>
              </div>
            </div>

            <div v-if="inferior.length" class="space-y-1 border-t border-rosa/50 pt-4">
              <router-link
                v-for="item in inferior"
                :key="item.nombre"
                :to="item.to"
                class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-cafe transition hover:bg-rosa/20"
                @click="menuAbierto = false"
              >
                <svg class="h-5 w-5 shrink-0 text-cafe/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" :d="item.icono" />
                </svg>
                <span>{{ item.nombre }}</span>
              </router-link>
            </div>
          </nav>
        </div>
      </aside>

      <div class="min-w-0 flex-1">
        <div class="mx-auto w-full max-w-6xl">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>
