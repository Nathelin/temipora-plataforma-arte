<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, CircleCheckBig, RefreshCw, Tags, TriangleAlert, Users } from '@lucide/vue'
import AdminPageHeader from '../components/admin/AdminPageHeader.vue'
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

const atenciones = computed(() => {
  const items = []
  if (datos.value.reportes) {
    items.push({
      clave: 'reportes',
      icono: TriangleAlert,
      cantidad: datos.value.reportes,
      etiqueta: datos.value.reportes === 1 ? 'reseña reportada' : 'reseñas reportadas',
      detalle: 'Revisalas y decidí si se ocultan o se restauran.',
      to: '/admin/resenas/reportadas',
    })
  }
  if (datos.value.etiquetas) {
    items.push({
      clave: 'etiquetas',
      icono: Tags,
      cantidad: datos.value.etiquetas,
      etiqueta: datos.value.etiquetas === 1 ? 'etiqueta propuesta' : 'etiquetas propuestas',
      detalle: 'Los artistas esperan que las apruebes o rechaces.',
      to: '/admin/etiquetas?estado=PENDIENTE',
    })
  }
  return items
})

const atajos = [
  { nombre: 'Usuarios registrados', to: '/admin/usuarios' },
  { nombre: 'Etiquetas pendientes', to: '/admin/etiquetas?estado=PENDIENTE' },
  { nombre: 'Moderación de reseñas', to: '/admin/resenas/reportadas' },
]

onMounted(cargar)
</script>

<template>
  <div class="px-4 py-8 sm:px-8">
    <AdminPageHeader
      eyebrow="Administración"
      titulo="Resumen"
      descripcion="El estado de la plataforma y lo que requiere tu atención hoy."
    >
      <template #acciones>
        <button
          type="button"
          class="inline-flex min-h-10 items-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
          :disabled="cargando"
          @click="cargar"
        >
          <RefreshCw
            class="h-4 w-4"
            :class="cargando ? 'animate-spin motion-reduce:animate-none' : ''"
            aria-hidden="true"
          />
          {{ cargando ? 'Actualizando...' : 'Actualizar' }}
        </button>
      </template>
    </AdminPageHeader>

    <div v-if="cargando" class="mt-8 space-y-4" role="status" aria-live="polite">
      <span class="sr-only">Cargando resumen...</span>
      <div class="h-52 animate-pulse rounded-3xl border border-rosa/40 bg-blanco/70 motion-reduce:animate-none" aria-hidden="true"></div>
      <div class="grid gap-4 sm:grid-cols-2">
        <div v-for="n in 2" :key="n" class="h-36 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70 motion-reduce:animate-none" aria-hidden="true"></div>
      </div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 motion-reduce:transition-none"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <section class="mt-8 rounded-3xl border border-coral/25 bg-coral/5 p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-display text-xl font-bold text-granate">Requiere tu atención</h2>
          <span
            v-if="atenciones.length"
            class="rounded-full bg-coral/10 px-3 py-1 text-xs font-semibold text-coral tabular-nums"
          >
            {{ atenciones.length }}
          </span>
        </div>

        <div v-if="atenciones.length" class="mt-4 grid gap-3 sm:grid-cols-2">
          <router-link
            v-for="atencion in atenciones"
            :key="atencion.clave"
            :to="atencion.to"
            class="group flex flex-col rounded-2xl border border-coral/20 bg-blanco/80 p-4 transition duration-150 hover:-translate-y-0.5 hover:border-coral/50 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)] motion-reduce:transition-none motion-reduce:transform-none"
          >
            <div class="flex items-start justify-between gap-3">
              <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-coral/10 text-coral">
                <component :is="atencion.icono" class="h-5 w-5" :stroke-width="1.9" aria-hidden="true" />
              </span>
              <span class="inline-flex items-center gap-1 text-sm font-semibold text-coral">
                Revisar
                <ArrowRight class="h-4 w-4 transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transition-none motion-reduce:transform-none" aria-hidden="true" />
              </span>
            </div>

            <p class="mt-3 font-display text-4xl font-extrabold leading-none text-granate tabular-nums">
              {{ atencion.cantidad }}
            </p>
            <p class="mt-1 text-sm font-semibold text-granate">{{ atencion.etiqueta }}</p>
            <p class="mt-1 text-xs leading-relaxed text-cafe/60">{{ atencion.detalle }}</p>
          </router-link>
        </div>

        <div
          v-else
          class="mt-4 flex items-center gap-3 rounded-2xl border border-rosa/40 bg-blanco/70 px-4 py-6"
        >
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-green-500/10 text-green-700">
            <CircleCheckBig class="h-5 w-5" :stroke-width="1.9" aria-hidden="true" />
          </span>
          <div>
            <p class="text-sm font-semibold text-granate">Todo al día</p>
            <p class="text-xs text-cafe/60">No hay reseñas reportadas ni etiquetas pendientes.</p>
          </div>
        </div>
      </section>

      <div class="mt-6 grid gap-4 sm:grid-cols-2">
        <router-link
          to="/admin/usuarios"
          class="rounded-2xl border border-rosa/60 bg-blanco p-5 transition duration-150 hover:-translate-y-0.5 hover:border-coral/40 hover:shadow-[0_12px_28px_rgba(74,29,46,0.08)] motion-reduce:transition-none motion-reduce:transform-none"
        >
          <div class="flex items-start justify-between gap-3">
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">
              Usuarios registrados
            </p>
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-rosa/25 text-coral">
              <Users class="h-4.5 w-4.5" :stroke-width="1.75" aria-hidden="true" />
            </span>
          </div>
          <p class="mt-3 font-display text-3xl font-extrabold text-granate tabular-nums">
            {{ datos.usuarios }}
          </p>
          <p class="mt-1 text-xs text-cafe/55">Cuentas creadas en la plataforma</p>
        </router-link>

        <section class="rounded-2xl border border-rosa/60 bg-blanco p-5">
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/50">Atajos</p>
          <ul class="mt-3 space-y-1">
            <li v-for="atajo in atajos" :key="atajo.to">
              <router-link
                :to="atajo.to"
                class="group flex min-h-9 items-center justify-between gap-3 rounded-lg px-2 text-sm text-cafe transition-colors hover:bg-rosa/15 hover:text-granate motion-reduce:transition-none"
              >
                <span class="truncate">{{ atajo.nombre }}</span>
                <ArrowRight class="h-4 w-4 shrink-0 text-cafe/40 transition-transform duration-150 group-hover:translate-x-0.5 motion-reduce:transition-none motion-reduce:transform-none" aria-hidden="true" />
              </router-link>
            </li>
          </ul>
        </section>
      </div>

      <p class="mt-6 text-xs text-cafe/45">
        Próximamente: métricas de pedidos, estado de integraciones y actividad reciente.
      </p>
    </template>
  </div>
</template>
