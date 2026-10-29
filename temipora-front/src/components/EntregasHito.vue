<script setup>
import { computed, reactive, ref } from 'vue'
import api from '../servicios/api'
import { formatearFechaHora } from '../servicios/pedidos'
import ArchivoProtegido from './ArchivoProtegido.vue'
import IconoDescarga from './IconoDescarga.vue'
import VisorImagenes from './VisorImagenes.vue'

const props = defineProps({
  idPedido: { type: Number, required: true },
  idHito: { type: Number, required: true },
  entregas: { type: Array, default: () => [] },
  puedeAgregarAlPortfolio: { type: Boolean, default: false },
  idsEnPortfolio: { type: Array, default: () => [] },
})

const emit = defineEmits(['agregar-al-portfolio'])

const descargaError = ref('')
const descargandoZip = ref(null)
const descargandoArchivo = ref(null)

const entregaVigente = computed(
  () => props.entregas.find((entrega) => entrega.vigente)
    || props.entregas[0]
    || null,
)

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
  return archivo.urlOriginal || archivo.urlPreview || ''
}

function esPreview(archivo) {
  return !archivo.urlOriginal && Boolean(archivo.urlPreview)
}

async function descargar(archivo) {
  if (!archivo.urlOriginal || descargandoArchivo.value) return
  descargaError.value = ''
  descargandoArchivo.value = archivo.idArchivo
  try {
    const res = await api.get(archivo.urlOriginal, { responseType: 'blob' })
    descargarBlob(res.data, archivo.nombre || 'archivo')
  } catch (e) {
    descargaError.value = 'No se pudo descargar el original.'
  } finally {
    descargandoArchivo.value = null
  }
}

function puedeZip(entrega) {
  return (entrega.archivos || [])
    .some((archivo) => Boolean(archivo.urlOriginal))
}

async function descargarZip(entrega) {
  if (!puedeZip(entrega) || descargandoZip.value) return
  descargaError.value = ''
  descargandoZip.value = entrega.idEntrega
  try {
    const res = await api.get(
      `/pedidos/${props.idPedido}/hitos/${props.idHito}/entregas/${entrega.idEntrega}/zip`,
      { responseType: 'blob' },
    )
    descargarBlob(
      res.data,
      `pedido-${props.idPedido}-entrega-v${entrega.numero}.zip`,
    )
  } catch (e) {
    descargaError.value = 'No se pudo descargar el ZIP.'
  } finally {
    descargandoZip.value = null
  }
}

function descargarBlob(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <section v-if="entregas.length" class="mt-4 space-y-3">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h4 class="text-sm font-semibold text-granate">Entregas</h4>
        <p class="text-xs text-cafe/55">Cada versión conserva su historial.</p>
      </div>
      <button
        v-if="entregaVigente && puedeZip(entregaVigente)"
        type="button"
        :disabled="descargandoZip === entregaVigente.idEntrega"
        class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
        @click="descargarZip(entregaVigente)"
      >
        <IconoDescarga />
        {{
          descargandoZip === entregaVigente.idEntrega
            ? 'Descargando...'
            : 'Descargar ZIP de la entrega'
        }}
      </button>
      <p
        v-else-if="entregaVigente"
        class="rounded-full border border-durazno bg-durazno/20 px-3 py-1.5 text-xs font-medium text-cafe/60"
      >
        El ZIP se habilita al pagar la última cuota.
      </p>
    </div>

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
          <button
            v-if="!entrega.vigente && puedeZip(entrega)"
            type="button"
            :disabled="descargandoZip === entrega.idEntrega"
            class="inline-flex min-h-8 items-center justify-center gap-1.5 rounded-full border border-coral/40 bg-coral/5 px-3 text-[11px] font-semibold text-coral transition hover:bg-coral/10 disabled:opacity-60"
            @click="descargarZip(entrega)"
          >
            <IconoDescarga />
            {{ descargandoZip === entrega.idEntrega ? 'Descargando...' : 'ZIP' }}
          </button>
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
              :disabled="descargandoArchivo === archivo.idArchivo"
              class="mt-2 inline-flex min-h-9 w-full items-center justify-center gap-2 rounded-full border border-coral/40 bg-blanco px-3 text-xs font-semibold text-coral transition hover:bg-coral/10 disabled:cursor-not-allowed disabled:opacity-60"
              @click="descargar(archivo)"
            >
              <IconoDescarga />
              {{
                descargandoArchivo === archivo.idArchivo
                  ? 'Descargando...'
                  : 'Descargar original'
              }}
            </button>

            <div
              v-if="puedeAgregarAlPortfolio
                && entrega.esFinal
                && entrega.vigente
                && archivo.mime?.startsWith('image/')"
              class="mt-2"
            >
              <span
                v-if="idsEnPortfolio.includes(archivo.idArchivo)"
                class="inline-flex min-h-9 w-full items-center justify-center gap-1.5 rounded-full border border-green-700/30 bg-green-700/10 px-3 text-xs font-semibold text-green-700"
              >
                ✓ En tu portfolio
              </span>
              <button
                v-else
                type="button"
                class="inline-flex min-h-9 w-full items-center justify-center gap-1.5 rounded-full border border-coral/40 bg-coral/5 px-3 text-xs font-semibold text-coral transition hover:bg-coral/10"
                @click="emit('agregar-al-portfolio', { archivo, entrega })"
              >
                Agregar al portfolio
              </button>
            </div>
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
