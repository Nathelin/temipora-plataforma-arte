<script setup>
import { reactive, ref } from 'vue'
import api from '../servicios/api'
import { formatearFechaHora } from '../servicios/pedidos'
import ArchivoProtegido from './ArchivoProtegido.vue'
import VisorImagenes from './VisorImagenes.vue'

const props = defineProps({
  idPedido: { type: Number, required: true },
  entregas: { type: Array, default: () => [] },
  verOriginal: { type: Boolean, default: false },
})

const descargaError = ref('')

const visor = reactive({
  abierto: false,
  imagenes: [],
  indice: 0,
  etiqueta: '',
  descripcion: '',
})

function gruposDe(entrega) {
  const grupos = new Map()
  for (const archivo of entrega.archivos || []) {
    const clave = archivo.dibujo?.idItemSolicitud ?? 'general'
    const etiqueta = archivo.dibujo
      ? `Dibujo ${(archivo.dibujo.orden ?? 0) + 1} — ${archivo.dibujo.nombre || 'Sin nombre'}`
      : 'General'
    if (!grupos.has(clave)) {
      grupos.set(clave, { clave, etiqueta, archivos: [] })
    }
    grupos.get(clave).archivos.push(archivo)
  }
  return [...grupos.values()]
}

function abrirVisor(entrega, grupo, archivo, src) {
  visor.imagenes = [src]
  visor.indice = 0
  visor.etiqueta = `${grupo.etiqueta} — Entrega v${entrega.numero}`
  visor.descripcion = archivo.nombre
  visor.abierto = true
}

function urlVisible(archivo) {
  if (props.verOriginal && archivo.urlOriginal) return archivo.urlOriginal
  return archivo.urlPreview || archivo.urlOriginal
}

function esPreview(archivo) {
  return !props.verOriginal && Boolean(archivo.urlPreview)
}

async function descargar(archivo) {
  if (!archivo.urlOriginal) return
  descargaError.value = ''
  try {
    const res = await api.get(archivo.urlOriginal, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = archivo.nombre || 'archivo'
    document.body.appendChild(enlace)
    enlace.click()
    enlace.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    descargaError.value = 'No se pudo descargar el original.'
  }
}
</script>

<template>
  <section v-if="entregas.length" class="mt-4 space-y-3">
    <h4 class="text-sm font-semibold text-granate">Entregas</h4>

    <p
      v-if="descargaError"
      class="rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-xs text-rojizo"
      role="alert"
    >
      {{ descargaError }}
    </p>

    <article
      v-for="entrega in entregas"
      :key="entrega.idEntrega"
      class="rounded-xl border border-rosa/40 bg-blanco p-3"
    >
      <header class="flex flex-wrap items-start justify-between gap-2">
        <div class="min-w-0">
          <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">
            Entrega v{{ entrega.numero }}
          </p>
          <p class="mt-0.5 text-sm font-semibold text-granate">
            @{{ entrega.autor?.nombreUsuario || '—' }}
          </p>
          <p class="text-xs text-cafe/50">{{ formatearFechaHora(entrega.fechaEntrega) }}</p>
        </div>
        <div class="flex flex-wrap gap-1.5">
          <span
            v-if="entrega.vigente"
            class="rounded-full bg-coral/10 px-2.5 py-1 text-[11px] font-semibold text-coral"
          >
            Vigente
          </span>
          <span
            v-else
            class="rounded-full bg-cafe/10 px-2.5 py-1 text-[11px] font-semibold text-cafe/60"
          >
            Reemplazada
          </span>
          <span
            v-if="entrega.aprobada"
            class="rounded-full bg-green-700/10 px-2.5 py-1 text-[11px] font-semibold text-green-700"
          >
            Aprobada
          </span>
        </div>
      </header>

      <p
        v-if="entrega.descripcion"
        class="mt-2 whitespace-pre-line text-sm leading-relaxed text-cafe/75"
      >
        {{ entrega.descripcion }}
      </p>

      <div
        v-if="entrega.revisionMotivo"
        class="mt-2 rounded-lg border border-rojizo/20 bg-rojizo/5 px-3 py-2"
      >
        <p class="text-[11px] font-semibold uppercase tracking-[0.12em] text-rojizo">
          Revisión solicitada
        </p>
        <p class="mt-1 whitespace-pre-line text-xs leading-relaxed text-cafe/75">
          {{ entrega.revisionMotivo }}
        </p>
        <p class="mt-1 text-[11px] text-cafe/45">
          {{ formatearFechaHora(entrega.revisionFecha) }}
          <span v-if="entrega.revisionPor"> · @{{ entrega.revisionPor.nombreUsuario }}</span>
        </p>
      </div>

      <div v-for="grupo in gruposDe(entrega)" :key="grupo.clave" class="mt-3">
        <p class="text-xs font-semibold text-cafe/60">{{ grupo.etiqueta }}</p>
        <div class="mt-2 grid gap-2 sm:grid-cols-2">
          <div
            v-for="archivo in grupo.archivos"
            :key="archivo.idArchivo"
            class="rounded-lg border border-rosa/30 bg-rosa/5 p-2"
          >
            <ArchivoProtegido
              v-if="urlVisible(archivo)"
              :url="urlVisible(archivo)"
              :alt="`${grupo.etiqueta} — ${archivo.nombre}`"
              @abrir="abrirVisor(entrega, grupo, archivo, $event)"
            />
            <p
              v-else
              class="rounded-lg border border-durazno bg-durazno/20 px-3 py-6 text-center text-xs text-cafe/60"
            >
              Se libera al pagar la última cuota.
            </p>
            <p class="mt-1 truncate text-[11px] font-medium text-cafe/70" :title="archivo.nombre">
              {{ archivo.nombre }}
            </p>
            <p class="text-[11px] text-cafe/45">
              {{ archivo.ancho }} × {{ archivo.alto }}
            </p>
            <p v-if="esPreview(archivo)" class="mt-1 text-[11px] font-medium text-coral">
              Preview protegida
            </p>
            <p
              v-if="esPreview(archivo) && !archivo.urlOriginal"
              class="text-[11px] text-cafe/50"
            >
              El original se libera al pagar la última cuota.
            </p>
            <button
              v-if="archivo.urlOriginal"
              type="button"
              class="mt-1 text-[11px] font-semibold text-coral underline-offset-2 hover:underline"
              @click="descargar(archivo)"
            >
              Descargar original
            </button>
          </div>
        </div>
      </div>
    </article>

    <VisorImagenes
      v-model:indice="visor.indice"
      :abierto="visor.abierto"
      :imagenes="visor.imagenes"
      :etiqueta="visor.etiqueta"
      :descripcion="visor.descripcion"
      @cerrar="visor.abierto = false"
    />
  </section>
</template>
