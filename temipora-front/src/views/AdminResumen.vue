<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../servicios/api'

const cargando = ref(true)
const error = ref('')
const datos = ref({ usuarios: 0, reportes: 0, etiquetas: 0 })

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [usuarios, reportes, etiquetas] = await Promise.all([
      api.get('/usuarios', { params: { page: 0, size: 1, sort: 'idUsuario,asc' } }),
      api.get('/admin/resenias/reportadas', { params: { pagina: 0, tamanio: 1 } }),
      api.get('/admin/etiquetas/conteo-pendientes'),
    ])
    datos.value = {
      usuarios: Number(usuarios.data?.totalElements ?? 0),
      reportes: Number(reportes.data?.totalElementos ?? 0),
      etiquetas: Number(etiquetas.data?.pendientes ?? 0),
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el resumen.'
  } finally {
    cargando.value = false
  }
}

const tarjetas = computed(() => [
  {
    etiqueta: 'Usuarios registrados',
    valor: datos.value.usuarios,
    detalle: 'Cuentas creadas en la plataforma',
    to: '/admin/usuarios',
    icono: 'M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z',
  },
  {
    etiqueta: 'Reseñas reportadas',
    valor: datos.value.reportes,
    detalle: 'Esperando moderación',
    to: '/admin/resenas/reportadas',
    icono: 'M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z',
  },
  {
    etiqueta: 'Etiquetas propuestas',
    valor: datos.value.etiquetas,
    detalle: 'Pendientes de aprobación',
    to: '/admin/etiquetas?estado=PENDIENTE',
    icono: 'M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z',
  },
])

const atenciones = computed(() => {
  const items = []
  if (datos.value.reportes) {
    items.push({
      clave: 'reportes',
      titulo: datos.value.reportes === 1
        ? 'Hay una reseña reportada'
        : `Hay ${datos.value.reportes} reseñas reportadas`,
      detalle: 'Revisalas y decidí si se ocultan o se restauran.',
      to: '/admin/resenas/reportadas',
    })
  }
  if (datos.value.etiquetas) {
    items.push({
      clave: 'etiquetas',
      titulo: datos.value.etiquetas === 1
        ? 'Hay una etiqueta propuesta'
        : `Hay ${datos.value.etiquetas} etiquetas propuestas`,
      detalle: 'Los artistas esperan que las apruebes o rechaces.',
      to: '/admin/etiquetas?estado=PENDIENTE',
    })
  }
  return items
})

onMounted(cargar)
</script>

<template>
  <div class="px-4 py-8 sm:px-8">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Administración</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Resumen
        </h1>
        <p class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
          El estado de la plataforma y lo que requiere tu atención hoy.
        </p>
      </div>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
        :disabled="cargando"
        @click="cargar"
      >
        {{ cargando ? 'Actualizando...' : 'Actualizar' }}
      </button>
    </header>

    <div v-if="cargando" class="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando resumen...</span>
      <div v-for="n in 3" :key="n" class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70" aria-hidden="true"></div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <div class="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <router-link
          v-for="tarjeta in tarjetas"
          :key="tarjeta.etiqueta"
          :to="tarjeta.to"
          class="group rounded-2xl border border-rosa/60 bg-blanco p-5 transition hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)]"
        >
          <div class="flex items-start justify-between gap-3">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">{{ tarjeta.etiqueta }}</p>
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-rosa/25 text-coral">
              <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" :d="tarjeta.icono" />
              </svg>
            </span>
          </div>
          <p class="mt-3 font-display text-3xl font-extrabold text-granate">{{ tarjeta.valor }}</p>
          <p class="mt-1 text-xs text-cafe/55">{{ tarjeta.detalle }}</p>
        </router-link>
      </div>

      <section class="mt-8 rounded-2xl border border-rosa/60 bg-blanco p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">Requiere tu atención</h2>
          <span
            v-if="atenciones.length"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral"
          >
            {{ atenciones.length }}
          </span>
        </div>

        <ul v-if="atenciones.length" class="mt-4 space-y-2">
          <li v-for="atencion in atenciones" :key="atencion.clave">
            <router-link
              :to="atencion.to"
              class="flex items-center justify-between gap-4 rounded-xl border border-rosa/50 px-4 py-3 transition hover:border-coral/40 hover:bg-rosa/10"
            >
              <span class="min-w-0">
                <span class="block text-sm font-semibold text-granate">{{ atencion.titulo }}</span>
                <span class="mt-0.5 block text-xs text-cafe/60">{{ atencion.detalle }}</span>
              </span>
              <span class="shrink-0 text-sm font-semibold text-coral">Revisar →</span>
            </router-link>
          </li>
        </ul>

        <p v-else class="mt-4 rounded-xl border border-rosa/40 bg-rosa/10 px-4 py-6 text-center text-sm text-cafe/60">
          No hay nada pendiente de moderación.
        </p>
      </section>

      <p class="mt-6 text-xs text-cafe/45">
        Próximamente: métricas de pedidos, estado de integraciones y actividad reciente.
      </p>
    </template>
  </div>
</template>
