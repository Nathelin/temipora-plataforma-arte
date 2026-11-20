<script setup>
import { onMounted, ref } from 'vue'
import { listarReventas } from '../servicios/reventas'
import TarjetaProducto from '../components/TarjetaProducto.vue'
import PageHeader from '../components/PageHeader.vue'

const reventas = ref([])
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const pagina = ref(0)
const hayMas = ref(false)
const totalElementos = ref(0)
const tamanio = 24

async function cargar({ reiniciar = true } = {}) {
  if (reiniciar) {
    pagina.value = 0
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  error.value = ''
  try {
    const data = await listarReventas({ pagina: pagina.value, tamanio })
    if (reiniciar) {
      reventas.value = data.contenido || []
    } else {
      reventas.value = [...reventas.value, ...(data.contenido || [])]
    }
    totalElementos.value = data.totalElementos || 0
    hayMas.value = !!data.hayMas
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar las reventas.'
  } finally {
    cargando.value = false
    cargandoMas.value = false
  }
}

function cargarMas() {
  pagina.value += 1
  cargar({ reiniciar: false })
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-screen-2xl px-4 py-8 sm:px-8 sm:py-10">
    <PageHeader
      eyebrow="Reventa"
      titulo="Piezas que vuelven a estar disponibles"
      descripcion="Piezas únicas que sus dueños ponen en venta. El pago se acredita al revendedor."
    >
      <template #acciones>
        <router-link
          to="/productos"
          class="text-sm font-bold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
        >
          Ver todos los productos →
        </router-link>
      </template>
    </PageHeader>

    <CargandoPagina v-if="cargando" mensaje="Cargando reventas…" />
    <p v-else-if="error" role="alert" class="py-16 text-center text-rojizo">{{ error }}</p>

    <template v-else>
      <p class="mt-6 text-xs text-cafe/55">
        {{ totalElementos }}
        {{ totalElementos === 1 ? 'reventa activa' : 'reventas activas' }}
      </p>

      <div
        v-if="!reventas.length"
        class="mt-6 rounded-2xl border border-dashed border-rosa/60 px-4 py-16 text-center"
      >
        <p class="text-sm font-medium text-granate">
          Todavía no hay reventas activas.
        </p>
        <p class="mt-1 text-sm text-cafe/50">
          Cuando alguien ponga en venta una pieza única, va a aparecer acá.
        </p>
      </div>

      <div v-else class="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
        <TarjetaProducto
          v-for="publicacion in reventas"
          :key="publicacion.idPublicacionReventa"
          :producto="publicacion"
        />
      </div>

      <div v-if="hayMas" class="mt-8 flex justify-center">
        <button
          type="button"
          :disabled="cargandoMas"
          class="rounded-lg border border-rosa px-5 py-2.5 text-sm font-medium text-granate transition hover:bg-rosa/20 disabled:opacity-50"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
      </div>
    </template>
  </div>
</template>
