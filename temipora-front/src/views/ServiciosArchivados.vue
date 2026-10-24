<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import { formatearFechaSolicitud } from '../servicios/solicitudes'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import { useNotificaciones } from '../composables/useNotificaciones'

const { exito, error: notificarError } = useNotificaciones()

const servicios = ref([])
const cargando = ref(true)
const errorCarga = ref('')
const servicioARestaurar = ref(null)
const restaurando = ref(false)

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const res = await api.get('/admin/servicios/archivados')
    servicios.value = res.data || []
  } catch (e) {
    errorCarga.value = e.response?.data?.message || 'No se pudieron cargar los servicios archivados.'
  } finally {
    cargando.value = false
  }
}

async function confirmarRestauracion() {
  if (!servicioARestaurar.value || restaurando.value) return
  restaurando.value = true
  const servicio = servicioARestaurar.value
  try {
    await api.post(`/admin/servicios/${servicio.idServicio}/restaurar`)
    servicios.value = servicios.value.filter((s) => s.idServicio !== servicio.idServicio)
    servicioARestaurar.value = null
    exito(`“${servicio.nombre}” se restauró como servicio pausado.`)
  } catch (e) {
    notificarError(e.response?.data?.message || 'No se pudo restaurar el servicio.')
  } finally {
    restaurando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-8 sm:py-10">
    <header>
      <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Administración</p>
      <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
        Servicios archivados
      </h1>
      <p class="mt-2 max-w-2xl text-sm leading-relaxed text-cafe/65">
        Los servicios que los artistas dieron de baja definitiva. No son visibles para el artista ni
        para los clientes, pero se conservan por auditoría. Al restaurarlos vuelven como pausados.
      </p>
    </header>

    <div v-if="cargando" class="mt-8 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando servicios archivados...</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70"
        aria-hidden="true"
      ></div>
    </div>

    <div v-else-if="errorCarga" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ errorCarga }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <div
      v-else-if="!servicios.length"
      class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
    >
      <p class="text-base font-semibold text-granate">No hay servicios archivados.</p>
      <p class="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-cafe/60">
        Cuando un artista dé de baja definitiva un servicio, aparecerá acá.
      </p>
    </div>

    <ul v-else class="mt-8 space-y-3">
      <li
        v-for="servicio in servicios"
        :key="servicio.idServicio"
        class="flex flex-col gap-4 rounded-2xl border border-rosa/50 bg-blanco p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5"
      >
        <div class="flex min-w-0 items-start gap-4">
          <div class="h-16 w-16 shrink-0 overflow-hidden rounded-xl border border-rosa/50 bg-rosa/20">
            <img
              v-if="servicio.imagenPortada"
              :src="servicio.imagenPortada"
              alt=""
              class="h-full w-full object-cover"
            />
          </div>
          <div class="min-w-0">
            <p class="text-sm font-semibold text-granate sm:text-base">{{ servicio.nombre }}</p>
            <p class="mt-1 text-xs text-cafe/60">
              @{{ servicio.nombreArtista || 'artista' }} · {{ servicio.nombreTipoServicio }} ·
              {{ servicio.nombreMedio }}
            </p>
            <p class="mt-1 text-xs text-cafe/50">
              <span v-if="servicio.precioBase != null">
                {{ formatearPrecio(servicio.precioBaseMostrado ?? servicio.precioBase, servicio.monedaMostrada ?? servicio.moneda) }}
              </span>
              <span v-if="servicio.archivadoEn">
                · Archivado el {{ formatearFechaSolicitud(servicio.archivadoEn, { corta: true }) }}
              </span>
            </p>
          </div>
        </div>

        <button
          type="button"
          class="self-start rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20 sm:self-auto"
          @click="servicioARestaurar = servicio"
        >
          Restaurar
        </button>
      </li>
    </ul>

    <ModalConfirmacion
      :abierto="!!servicioARestaurar"
      titulo="¿Restaurar el servicio?"
      :mensaje="`“${servicioARestaurar?.nombre || ''}” volverá como servicio pausado. Las solicitudes que se rechazaron al archivarlo no se reabren.`"
      texto-confirmar="Restaurar servicio"
      texto-cancelar="Cancelar"
      @confirmar="confirmarRestauracion"
      @cerrar="servicioARestaurar = null"
    />
  </div>
</template>
