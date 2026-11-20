<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import api from '../servicios/api'
import { hayContenidoTerminos } from '../servicios/terminos'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import BarraGuardado from '../components/BarraGuardado.vue'
import AvisoCompletitudTerminos from '../components/AvisoCompletitudTerminos.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'

const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorGuardado = ref('')

const idUsuario = ref(null)
const version = ref(0)
const derechosCatalogo = ref([])
const modo = ref('editar')

const secciones = [
  { key: 'informacionGeneral', label: 'Información general' },
  { key: 'politicaRevisiones', label: 'Política de revisiones' },
  { key: 'politicaCancelaciones', label: 'Política de cancelaciones' },
  { key: 'usoArtista', label: 'Uso del artista' },
  { key: 'usoCliente', label: 'Uso del cliente' },
]

const PLACEHOLDERS = {
  informacionGeneral: 'Antes de comisionar, contame tu idea con la mayor cantidad de detalles posible (personajes, estilo, colores, referencias). Te confirmo si puedo tomarlo y el presupuesto final.',
  politicaRevisiones: 'Incluyo hasta 2 rondas de revisiones (boceto y color). Cambios grandes fuera de esas etapas pueden tener un costo extra.',
  politicaCancelaciones: 'Si cancelás antes de empezar, devuelvo el 100%. Si el pedido ya está en boceto, devuelvo el 50%. Con el lineart o color avanzado no hay reembolso.',
  usoArtista: 'Me reservo el derecho de mostrar la obra terminada en mi portafolio y redes, salvo que acordemos lo contrario.',
  usoCliente: 'El uso por defecto es personal. Para uso comercial (contenido o merchandising) hay un costo extra detallado en los derechos.',
}

const form = ref({
  informacionGeneral: '',
  politicaRevisiones: '',
  politicaCancelaciones: '',
  usoArtista: '',
  usoCliente: '',
  pagoExtra: '',
  queDibuja: '',
  queNoDibuja: '',
})

const estadoDerechos = ref({})
const estadoGuardado = ref(null)

const modal = ref({ abierto: false, titulo: '', descripcion: '' })
const confirmandoGuardado = ref(false)

// ---------------------------------------------------------------
// carga
// ---------------------------------------------------------------

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [perfilRes, derechosRes] = await Promise.all([
      api.get(`/perfil/${nombreUsuario}`),
      api.get('/derechos-termino'),
    ])
    idUsuario.value = perfilRes.data.idUsuario
    derechosCatalogo.value = derechosRes.data

    const tosRes = await api.get(`/usuarios/${idUsuario.value}/terminos`)
    aplicarTerminos(tosRes.data)
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar los términos.'
  } finally {
    cargando.value = false
  }
}

function aplicarTerminos(data) {
  version.value = data.version || 0
  form.value = {
    informacionGeneral: data.informacionGeneral || '',
    politicaRevisiones: data.politicaRevisiones || '',
    politicaCancelaciones: data.politicaCancelaciones || '',
    usoArtista: data.usoArtista || '',
    usoCliente: data.usoCliente || '',
    pagoExtra: data.pagoExtra || '',
    queDibuja: data.queDibuja || '',
    queNoDibuja: data.queNoDibuja || '',
  }

  const derechosGuardados = data.derechos || []
  const derechos = {}
  for (const d of derechosCatalogo.value) {
    const encontrado = derechosGuardados.find((x) => x.idDerechoTermino === d.idDerechoTermino)
    derechos[d.idDerechoTermino] = {
      disponible: d.codigo === 'PERSONAL' ? true : !!encontrado?.disponible,
      porcentajeExtra: encontrado?.porcentajeExtra ?? null,
    }
  }
  estadoDerechos.value = derechos

  estadoGuardado.value = instantanea()
}

function instantanea() {
  return JSON.stringify({
    ...form.value,
    estadoDerechos: estadoDerechos.value,
  })
}

const hayCambios = computed(() => estadoGuardado.value !== null && estadoGuardado.value !== instantanea())

// ---------------------------------------------------------------
// derechos / modal
// ---------------------------------------------------------------

function abrirDescripcion(titulo, descripcion) {
  modal.value = { abierto: true, titulo, descripcion }
}

function porcentajeDe(derecho) {
  if (derecho.codigo === 'PERSONAL') return null
  const valor = estadoDerechos.value[derecho.idDerechoTermino]?.porcentajeExtra
  if (valor === '' || valor === null || valor === undefined) return null
  const num = Number(valor)
  return Number.isNaN(num) ? null : num
}

// ---------------------------------------------------------------
// guardar
// ---------------------------------------------------------------

function hayContenido() {
  return hayContenidoTerminos({ form: form.value })
}

function solicitarGuardado() {
  mensaje.value = ''
  errorGuardado.value = ''
  if (!hayContenido()) {
    errorGuardado.value = 'Completá al menos una sección de tus términos.'
    return
  }
  confirmandoGuardado.value = true
}

async function guardar() {
  confirmandoGuardado.value = false
  mensaje.value = ''
  errorGuardado.value = ''
  guardando.value = true
  try {
    const payload = {
      informacionGeneral: form.value.informacionGeneral || null,
      politicaRevisiones: form.value.politicaRevisiones || null,
      politicaCancelaciones: form.value.politicaCancelaciones || null,
      usoArtista: form.value.usoArtista || null,
      usoCliente: form.value.usoCliente || null,
      pagoExtra: form.value.pagoExtra || null,
      queDibuja: form.value.queDibuja || null,
      queNoDibuja: form.value.queNoDibuja || null,
      derechos: derechosCatalogo.value.map((d) => ({
        idDerechoTermino: d.idDerechoTermino,
        disponible: !!estadoDerechos.value[d.idDerechoTermino]?.disponible,
        porcentajeExtra: porcentajeDe(d),
      })),
    }
    const r = await api.put(`/usuarios/${idUsuario.value}/terminos`, payload)
    aplicarTerminos(r.data)
    mensaje.value = 'Términos guardados correctamente.'
    modo.value = 'editar'
  } catch (e) {
    errorGuardado.value = e.response?.data?.message || 'No se pudieron guardar los términos.'
  } finally {
    guardando.value = false
  }
}

// ---------------------------------------------------------------
// cambios sin guardar
// ---------------------------------------------------------------

const { actualizar: marcarCambios, limpiar: limpiarCambios } = useCambiosSinGuardar()

watch(hayCambios, (pendiente) => {
  marcarCambios(pendiente, 'Tenés cambios sin guardar en tus términos. Si salís, se perderán.')
})

function antesDeSalir(event) {
  if (hayCambios.value && !guardando.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onMounted(() => {
  cargar()
  window.addEventListener('beforeunload', antesDeSalir)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', antesDeSalir)
  limpiarCambios()
})
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <div class="mb-6">
      <h2 class="text-lg text-granate font-bold">Términos y condiciones</h2>
      <p class="text-sm text-cafe/70">
        Define los usos que permites sobre tu arte. Se usan por defecto en tus productos y servicios.
      </p>
      <p v-if="version" class="text-xs text-cafe/50 mt-1">Versión {{ version }}</p>
    </div>

    <CargandoPagina v-if="cargando" tipo="lista" mensaje="Cargando términos…" :tarjetas="4" />
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-10">
      <div class="inline-flex rounded-lg border border-rosa p-0.5 text-sm">
        <button type="button" class="rounded-md px-3 py-1.5 transition" :class="modo === 'editar' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'" @click="modo = 'editar'">Editar</button>
        <button type="button" class="rounded-md px-3 py-1.5 transition" :class="modo === 'preview' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'" @click="modo = 'preview'">Vista previa</button>
      </div>

      <!-- ================= EDICIÓN ================= -->
      <template v-if="modo === 'editar'">
        <AvisoCompletitudTerminos :terminos="{ form }" />

        <!-- Secciones de texto -->
        <div class="space-y-8">
          <div v-for="s in secciones" :key="s.key">
            <label class="block text-sm text-cafe mb-2">{{ s.label }}</label>
            <textarea
              v-model="form[s.key]"
              rows="3"
              :placeholder="PLACEHOLDERS[s.key]"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
          </div>
        </div>

        <!-- Qué dibujo / qué no dibujo -->
        <div class="space-y-8 border-t border-rosa/40 pt-8">
          <div>
            <h3 class="text-base text-granate mb-1">¿Qué dibujo?</h3>
            <p class="text-sm text-cafe/60 mb-3">Contale al cliente qué temas, estilos o personajes hacés. Escribilo como una lista simple.</p>
            <textarea
              v-model="form.queDibuja"
              rows="4"
              maxlength="2000"
              placeholder="Ej.: OCs, fanart, ships, furries/anthro, chibi, personajes humanos, fondos simples, gore leve."
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
          </div>

          <div>
            <h3 class="text-base text-granate mb-1">¿Qué no dibujo?</h3>
            <p class="text-sm text-cafe/60 mb-3">Sé claro con lo que no hacés para evitar pedidos que no puedas tomar.</p>
            <textarea
              v-model="form.queNoDibuja"
              rows="4"
              maxlength="2000"
              placeholder="Ej.: mecha, gore explícito, contenido sexual, contenido con menores, personas reales, franquicias que no conozco."
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
          </div>
        </div>

        <!-- Derechos -->
        <div>
          <h3 class="text-base text-granate mb-1">Derechos de uso</h3>
          <p class="text-sm text-cafe/60 mb-4">Elegí qué permitís y, si cobrás extra, el porcentaje sobre el precio base. El cliente podrá elegir estos derechos al solicitar.</p>
          <div class="space-y-3">
            <div v-for="d in derechosCatalogo" :key="d.idDerechoTermino" class="rounded-lg border border-rosa/50 px-4 py-4">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div class="flex items-center gap-2">
                  <span class="text-sm text-cafe">{{ d.nombre }}</span>
                  <button
                    v-if="d.descripcion"
                    type="button"
                    class="flex h-5 w-5 items-center justify-center rounded-full text-cafe/40 hover:bg-rosa/20 hover:text-cafe transition"
                    :aria-label="`Qué incluye ${d.nombre}`"
                    @click="abrirDescripcion(d.nombre, d.descripcion)"
                  >
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" />
                    </svg>
                  </button>
                </div>

                <div v-if="d.codigo === 'PERSONAL'" class="rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs text-green-700">
                  Incluido
                </div>
                <div v-else class="flex items-center gap-3">
                  <label class="flex items-center gap-2 text-sm text-cafe cursor-pointer">
                    <input v-model="estadoDerechos[d.idDerechoTermino].disponible" type="checkbox" class="accent-coral" />
                    <span>Ofrezco</span>
                  </label>
                  <div class="flex items-center gap-1" v-if="estadoDerechos[d.idDerechoTermino].disponible">
                    <span class="text-sm text-cafe/60">+</span>
                    <input
                      v-model.number="estadoDerechos[d.idDerechoTermino].porcentajeExtra"
                      type="number"
                      min="0"
                      max="1000"
                      class="w-20 rounded-lg border-0 py-1.5 px-2 text-sm text-cafe text-right shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
                    />
                    <span class="text-sm text-cafe/60">%</span>
                  </div>
                </div>
              </div>
              <p v-if="d.requierePiezaUnica" class="mt-1 text-xs text-cafe/50">Solo aplica a piezas únicas (adoptables o pintura tradicional única).</p>
            </div>
          </div>
        </div>

        <!-- Pago extra -->
        <div>
          <label class="block text-sm text-cafe mb-2">Nota sobre precios (opcional)</label>
          <input v-model="form.pagoExtra" type="text" maxlength="250" placeholder="Ej.: ajustes por complejidad, urgencias, etc."
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>

        <BarraGuardado :hay-cambios="hayCambios" :guardando="guardando" texto="Guardar términos" @guardar="solicitarGuardado">
          <template #aviso>
            <p v-if="errorGuardado" class="text-rojizo text-sm">{{ errorGuardado }}</p>
            <p v-else-if="mensaje" class="text-green-700 text-sm">{{ mensaje }}</p>
          </template>
        </BarraGuardado>
      </template>

      <!-- ================= PREVIEW ================= -->
      <template v-else>
        <div class="space-y-6">
          <p class="text-xs text-cafe/50">Así vería el cliente tus términos.</p>

          <div v-for="s in secciones" :key="s.key" class="border-t border-rosa/40 pt-4">
            <h3 class="text-base font-medium text-granate mb-1">{{ s.label }}</h3>
            <p class="text-sm text-cafe whitespace-pre-wrap">{{ form[s.key] || '—' }}</p>
          </div>

          <div class="border-t border-rosa/40 pt-4">
            <h3 class="text-base font-medium text-granate mb-2">¿Qué dibujo?</h3>
            <p class="text-sm text-cafe whitespace-pre-line">{{ form.queDibuja || '—' }}</p>
          </div>

          <div class="border-t border-rosa/40 pt-4">
            <h3 class="text-base font-medium text-granate mb-2">¿Qué no dibujo?</h3>
            <p class="text-sm text-cafe whitespace-pre-line">{{ form.queNoDibuja || '—' }}</p>
          </div>

          <div class="border-t border-rosa/40 pt-4">
            <h3 class="text-base font-medium text-granate mb-2">Derechos de uso</h3>
            <ul class="space-y-1 text-sm text-cafe">
              <li v-for="d in derechosCatalogo.filter((x) => estadoDerechos[x.idDerechoTermino]?.disponible)" :key="d.idDerechoTermino" class="flex items-center justify-between gap-3">
                <span>{{ d.nombre }}</span>
                <span class="text-cafe/60">
                  {{ d.codigo === 'PERSONAL' ? 'Incluido' : `+${estadoDerechos[d.idDerechoTermino].porcentajeExtra ?? 0}%` }}
                </span>
              </li>
            </ul>
            <p v-if="!Object.values(estadoDerechos).some((d) => d.disponible)" class="text-sm text-cafe/40">Sin derechos definidos.</p>
          </div>

          <p v-if="form.pagoExtra" class="border-t border-rosa/40 pt-4 text-sm text-cafe whitespace-pre-wrap">
            <span class="text-xs text-cafe/50 block mb-1">Nota sobre precios</span>
            {{ form.pagoExtra }}
          </p>
        </div>
      </template>
    </div>

    <!-- Modal de descripción -->
    <div v-if="modal.abierto" class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4" @click.self="modal.abierto = false">
      <div class="w-full max-w-md bg-blanco rounded-2xl shadow-xl p-6">
        <h3 class="text-lg text-granate mb-2">{{ modal.titulo }}</h3>
        <p class="text-sm text-cafe/80 whitespace-pre-wrap">{{ modal.descripcion }}</p>
        <div class="mt-6 flex justify-end">
          <button type="button" class="px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm" @click="modal.abierto = false">
            Entendido
          </button>
        </div>
      </div>
    </div>

    <ModalConfirmacion
      :abierto="confirmandoGuardado"
      titulo="¿Guardar términos globales?"
      :mensaje="version > 0
        ? 'Se actualizarán tus términos globales. Los servicios que usan estos términos verán los cambios. Este cambio NO afecta a tus pedidos activos.'
        : 'Se crearán tus términos globales. Se usarán por defecto en tus servicios.'"
      texto-confirmar="Guardar términos"
      @confirmar="guardar"
      @cerrar="confirmandoGuardado = false"
    />
  </div>
</template>
