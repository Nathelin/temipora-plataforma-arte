<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'

const router = useRouter()
const servicios = ref([])
const cargando = ref(true)
const error = ref('')

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
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo actualizar el estado.'
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-screen-xl px-4 sm:px-8 py-8">
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

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <div v-for="s in servicios" :key="s.idServicio" class="rounded-2xl border border-rosa/50 bg-blanco overflow-hidden flex flex-col">
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
  </div>
</template>
