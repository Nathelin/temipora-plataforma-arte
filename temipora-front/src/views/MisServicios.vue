<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'

const servicios = ref([])
const cargando = ref(true)
const error = ref('')
const filtro = ref('activos')
const aviso = ref(null)
let temporizadorAviso = null

const serviciosActivos = computed(() => servicios.value.filter((s) => s.estado))
const serviciosInactivos = computed(() => servicios.value.filter((s) => !s.estado))
const serviciosFiltrados = computed(() => {
  if (filtro.value === 'inactivos') return serviciosInactivos.value
  if (filtro.value === 'todos') return [...serviciosActivos.value, ...serviciosInactivos.value]
  return serviciosActivos.value
})
const opcionesFiltro = computed(() => [
  { valor: 'activos', texto: 'Activos', cantidad: serviciosActivos.value.length },
  { valor: 'inactivos', texto: 'Inactivos', cantidad: serviciosInactivos.value.length },
  { valor: 'todos', texto: 'Todos', cantidad: servicios.value.length },
])
const estadoVacio = computed(() => {
  if (filtro.value === 'activos') {
    return {
      titulo: 'No tenés servicios activos.',
      detalle: serviciosInactivos.value.length
        ? 'Revisá los que diste de baja o creá uno nuevo.'
        : 'Creá tu primer servicio para empezar a ofrecerlo.',
      accion: serviciosInactivos.value.length ? { texto: 'Ver inactivos', valor: 'inactivos' } : null,
    }
  }
  if (filtro.value === 'inactivos') {
    return {
      titulo: 'No tenés servicios inactivos.',
      detalle: 'Todos tus servicios están activos.',
      accion: serviciosActivos.value.length ? { texto: 'Ver activos', valor: 'activos' } : null,
    }
  }
  return {
    titulo: 'No hay servicios para mostrar.',
    detalle: 'Creá tu primer servicio para empezar.',
    accion: null,
  }
})

function mostrarAviso(tipo, texto) {
  aviso.value = { tipo, texto }
  clearTimeout(temporizadorAviso)
  temporizadorAviso = setTimeout(() => {
    aviso.value = null
  }, 4000)
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/mis-servicios')
    servicios.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus servicios.'
  } finally {
    cargando.value = false
  }
}

function textoEntrega(s) {
  return `${s.tiempoEntregaValor} ${(s.tiempoEntregaUnidad || '').toLowerCase()}`
}

async function alternarEstado(s) {
  const baja = s.estado
  const mensaje = baja
    ? `¿Dar de baja el servicio "${s.nombre}"? Dejará de aparecer en tus búsquedas y perfil.`
    : `¿Reactivar el servicio "${s.nombre}"?`
  if (!window.confirm(mensaje)) return
  try {
    const res = await api.post(`/servicios/${s.idServicio}/${baja ? 'baja' : 'activar'}`)
    s.estado = res.data.estado
    mostrarAviso(
      'exito',
      s.estado
        ? `“${s.nombre}” se reactivó y vuelve a estar visible.`
        : `“${s.nombre}” se dio de baja y ya no aparece en tus búsquedas ni perfil.`,
    )
  } catch (e) {
    mostrarAviso('error', e.response?.data?.message || 'No se pudo actualizar el estado.')
  }
}

onMounted(cargar)
onBeforeUnmount(() => clearTimeout(temporizadorAviso))
</script>

<template>
  <div class="mx-auto px-4 sm:px-8 py-8">
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl text-granate">Mis servicios</h1>
        <p class="mt-1 text-sm text-cafe/60">Los servicios que ofrecés a tus clientes.</p>
      </div>
      <router-link
        to="/mis-servicios/crear"
        class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
      >
        Crear servicio
      </router-link>
    </div>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else>
      <p v-if="!servicios.length" class="rounded-xl border border-dashed border-rosa/60 px-4 py-12 text-center text-sm text-cafe/50">
        Todavía no creaste servicios. Empezá con “Crear servicio”.
      </p>

      <template v-else>
        <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div
            class="flex w-full rounded-xl border border-rosa/60 bg-blanco p-1 sm:inline-flex sm:w-fit"
            role="group"
            aria-label="Filtrar servicios por estado"
          >
            <button
              v-for="opcion in opcionesFiltro"
              :key="opcion.valor"
              type="button"
              class="flex flex-1 items-center justify-center gap-2 whitespace-nowrap rounded-lg px-2 py-1.5 text-xs font-medium transition sm:flex-none sm:px-3 sm:text-sm"
              :class="filtro === opcion.valor
                ? 'bg-coral text-white shadow-sm'
                : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
              :aria-pressed="filtro === opcion.valor"
              @click="filtro = opcion.valor"
            >
              {{ opcion.texto }}
              <span
                class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
                :class="filtro === opcion.valor ? 'bg-white/20 text-white' : 'bg-rosa/30 text-granate'"
              >
                {{ opcion.cantidad }}
              </span>
            </button>
          </div>
          <p class="text-xs text-cafe/50">
            {{ serviciosFiltrados.length }} de {{ servicios.length }}
            {{ servicios.length === 1 ? 'servicio' : 'servicios' }}
          </p>
        </div>

        <div
          v-if="aviso"
          class="mb-5 flex items-start justify-between gap-3 rounded-xl border px-4 py-3 text-sm"
          :class="aviso.tipo === 'error'
            ? 'border-rojizo/30 bg-rojizo/5 text-rojizo'
            : 'border-green-600/20 bg-green-500/10 text-green-700'"
          role="status"
        >
          <p>{{ aviso.texto }}</p>
          <button
            type="button"
            class="shrink-0 opacity-60 transition hover:opacity-100"
            aria-label="Cerrar aviso"
            @click="aviso = null"
          >
            ×
          </button>
        </div>

        <div v-if="!serviciosFiltrados.length" class="rounded-xl border border-dashed border-rosa/60 px-4 py-12 text-center">
          <p class="text-sm font-medium text-granate">{{ estadoVacio.titulo }}</p>
          <p class="mt-1 text-sm text-cafe/50">{{ estadoVacio.detalle }}</p>
          <button
            v-if="estadoVacio.accion"
            type="button"
            class="mt-4 text-sm font-medium text-coral transition hover:text-rojizo"
            @click="filtro = estadoVacio.accion.valor"
          >
            {{ estadoVacio.accion.texto }}
          </button>
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          <div v-for="s in serviciosFiltrados" :key="s.idServicio" class="rounded-2xl border border-rosa/50 bg-blanco overflow-hidden flex flex-col">
            <div class="h-40 bg-rosa/20">
              <img v-if="s.imagenPortada" :src="s.imagenPortada" :alt="s.nombre" class="w-full h-full object-cover" />
            </div>
            <div class="p-4 flex-1 flex flex-col">
              <div class="flex items-start justify-between gap-2">
                <h2 class="text-granate">{{ s.nombre }}</h2>
                <span
                  class="shrink-0 rounded-full px-2.5 py-0.5 text-xs"
                  :class="s.estado ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
                >
                  {{ s.estado ? 'Activo' : 'Inactivo' }}
                </span>
              </div>
              <p class="mt-1 text-xs text-cafe/60">{{ s.nombreTipoServicio }} · {{ s.nombreMedio }}</p>
              <p class="mt-3 text-cafe whitespace-pre-wrap line-clamp-2">{{ s.descripcion }}</p>
              <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-cafe/70">
                <span>Precio: <span class="text-granate">{{ formatearPrecio(s.precioBase, s.moneda) }}</span></span>
                <span>Entrega: {{ textoEntrega(s) }}</span>
                <span>Cupos: {{ s.cupos }}</span>
              </div>

              <div class="mt-auto pt-4 flex items-center justify-end gap-3 border-t border-rosa/40 mt-4">
                <router-link :to="`/mis-servicios/${s.idServicio}/editar`" class="text-sm text-coral hover:text-rojizo transition">
                  Editar
                </router-link>
                <button type="button" class="text-sm text-cafe/60 hover:text-rojizo transition" @click="alternarEstado(s)">
                  {{ s.estado ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>
