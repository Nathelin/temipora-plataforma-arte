<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import api from '../servicios/api'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'
import BarraGuardado from '../components/BarraGuardado.vue'

const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorGuardado = ref('')

const idUsuario = ref(null)
const version = ref(0)
const temasCatalogo = ref([])
const derechosCatalogo = ref([])
const modo = ref('editar')

const secciones = [
  { key: 'informacionGeneral', label: 'Información general' },
  { key: 'politicaRevisiones', label: 'Política de revisiones' },
  { key: 'politicaCancelaciones', label: 'Política de cancelaciones' },
  { key: 'usoArtista', label: 'Uso del artista' },
  { key: 'usoCliente', label: 'Uso del cliente' },
]

const PLANTILLA = {
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
  tiempoEntrega: '',
})

const estadosTemas = ref({})
const temasPersonalizados = ref([])
const estadoDerechos = ref({})
const estadoGuardado = ref(null)
let uid = 0

const OPCIONES_ESTADO = [
  { valor: 'SI', texto: 'Sí' },
  { valor: 'NO', texto: 'No' },
  { valor: 'CONSULTAR', texto: 'Consultar' },
]

const modal = ref({ abierto: false, titulo: '', descripcion: '' })

// ---------------------------------------------------------------
// carga
// ---------------------------------------------------------------

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [perfilRes, temasRes, derechosRes] = await Promise.all([
      api.get(`/perfil/${nombreUsuario}`),
      api.get('/temas-termino'),
      api.get('/derechos-termino'),
    ])
    idUsuario.value = perfilRes.data.idUsuario
    temasCatalogo.value = temasRes.data
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
    tiempoEntrega: data.tiempoEntrega || '',
  }

  const temasGuardados = data.temas || []
  const estados = {}
  for (const t of temasCatalogo.value) {
    const encontrado = temasGuardados.find((x) => !x.personalizado && x.idTemaTermino === t.idTemaTermino)
    estados[t.idTemaTermino] = encontrado?.estado || ''
  }
  estadosTemas.value = estados

  temasPersonalizados.value = temasGuardados
    .filter((x) => x.personalizado)
    .map((x) => ({ uid: uid++, nombre: x.nombre || '', estado: x.estado || '' }))

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
    estadosTemas: estadosTemas.value,
    temasPersonalizados: temasPersonalizados.value.map((t) => ({ nombre: t.nombre, estado: t.estado })),
    estadoDerechos: estadoDerechos.value,
  })
}

const hayCambios = computed(() => estadoGuardado.value !== null && estadoGuardado.value !== instantanea())

// ---------------------------------------------------------------
// temas / derechos / modal
// ---------------------------------------------------------------

function setEstado(idTema, opcion) {
  const actual = estadosTemas.value[idTema]
  estadosTemas.value[idTema] = actual === opcion ? '' : opcion
}

function setEstadoPersonalizado(tema, opcion) {
  tema.estado = tema.estado === opcion ? '' : opcion
}

function agregarTema() {
  temasPersonalizados.value.push({ uid: uid++, nombre: '', estado: '' })
}

function quitarTema(uidTema) {
  temasPersonalizados.value = temasPersonalizados.value.filter((t) => t.uid !== uidTema)
}

function chipActivo(valor) {
  if (valor === 'SI') return 'bg-green-600 text-white'
  if (valor === 'NO') return 'bg-rojizo text-white'
  return 'bg-coral text-white'
}

function abrirDescripcion(titulo, descripcion) {
  modal.value = { abierto: true, titulo, descripcion }
}

function usarPlantilla() {
  for (const [key, texto] of Object.entries(PLANTILLA)) {
    if (!form.value[key].trim()) form.value[key] = texto
  }
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
  const hayTexto = [...secciones.map((s) => s.key), 'tiempoEntrega']
    .some((k) => (form.value[k] || '').trim())
  const hayTemas = Object.values(estadosTemas.value).some((v) => v)
    || temasPersonalizados.value.some((t) => t.nombre.trim() && t.estado)
  const hayDerechos = Object.values(estadoDerechos.value).some((d) => d.disponible)
  return hayTexto || hayTemas || hayDerechos
}

async function guardar() {
  mensaje.value = ''
  errorGuardado.value = ''
  if (!hayContenido()) {
    errorGuardado.value = 'Completá al menos una sección de tus términos.'
    return
  }

  guardando.value = true
  try {
    const payload = {
      informacionGeneral: form.value.informacionGeneral || null,
      politicaRevisiones: form.value.politicaRevisiones || null,
      politicaCancelaciones: form.value.politicaCancelaciones || null,
      usoArtista: form.value.usoArtista || null,
      usoCliente: form.value.usoCliente || null,
      pagoExtra: form.value.pagoExtra || null,
      tiempoEntrega: form.value.tiempoEntrega || null,
      temas: [
        ...temasCatalogo.value
          .filter((t) => estadosTemas.value[t.idTemaTermino])
          .map((t) => ({ idTemaTermino: t.idTemaTermino, estado: estadosTemas.value[t.idTemaTermino] })),
        ...temasPersonalizados.value
          .filter((t) => t.nombre.trim() && t.estado)
          .map((t) => ({ nombre: t.nombre.trim(), estado: t.estado })),
      ],
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

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-10">
      <div class="inline-flex rounded-lg border border-rosa p-0.5 text-sm">
        <button type="button" class="rounded-md px-3 py-1.5 transition" :class="modo === 'editar' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'" @click="modo = 'editar'">Editar</button>
        <button type="button" class="rounded-md px-3 py-1.5 transition" :class="modo === 'preview' ? 'bg-coral text-white' : 'text-cafe hover:bg-rosa/20'" @click="modo = 'preview'">Vista previa</button>
      </div>

      <!-- ================= EDICIÓN ================= -->
      <template v-if="modo === 'editar'">
        <div class="flex justify-end">
          <button type="button" class="text-sm text-coral hover:text-rojizo transition" @click="usarPlantilla">
            Usar plantilla sugerida
          </button>
        </div>

        <!-- Secciones de texto -->
        <div class="space-y-8">
          <div v-for="s in secciones" :key="s.key">
            <label class="block text-sm text-cafe mb-2">{{ s.label }}</label>
            <textarea
              v-model="form[s.key]"
              rows="3"
              class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
            ></textarea>
          </div>
        </div>

        <!-- Plazos -->
        <div>
          <label class="block text-sm text-cafe mb-2">Tiempo estimado de entrega</label>
          <input v-model="form.tiempoEntrega" type="text" maxlength="120" placeholder="Ej.: 2 a 4 semanas"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>

        <!-- Derechos -->
        <div>
          <h3 class="text-base text-granate mb-1">Derechos de uso</h3>
          <p class="text-sm text-cafe/60 mb-4">Elegí qué permitís y, si cobrás extra, el porcentaje sobre el precio base.</p>
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

        <!-- Qué hago / qué no hago -->
        <div>
          <h3 class="text-base text-granate mb-1">¿Qué hago y qué no?</h3>
          <p class="text-sm text-cafe/60 mb-4">Marcá tu disponibilidad para cada tema y sumá los que quieras.</p>
          <div class="space-y-3">
            <div v-for="t in temasCatalogo" :key="t.idTemaTermino" class="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-rosa/50 px-4 py-3.5">
              <div class="flex items-center gap-2">
                <span class="text-sm text-cafe">{{ t.nombre }}</span>
                <button
                  v-if="t.descripcion"
                  type="button"
                  class="flex h-5 w-5 items-center justify-center rounded-full text-cafe/40 hover:bg-rosa/20 hover:text-cafe transition"
                  :aria-label="`Qué significa ${t.nombre}`"
                  @click="abrirDescripcion(t.nombre, t.descripcion)"
                >
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" />
                  </svg>
                </button>
              </div>
              <div class="inline-flex rounded-md border border-rosa p-0.5 text-xs">
                <button
                  v-for="op in OPCIONES_ESTADO"
                  :key="op.valor"
                  type="button"
                  class="rounded px-2.5 py-1 transition"
                  :class="estadosTemas[t.idTemaTermino] === op.valor ? chipActivo(op.valor) : 'text-cafe/60 hover:bg-rosa/20'"
                  @click="setEstado(t.idTemaTermino, op.valor)"
                >
                  {{ op.texto }}
                </button>
              </div>
            </div>
          </div>

          <!-- Temas propios -->
          <div v-for="t in temasPersonalizados" :key="t.uid" class="mt-2 flex flex-wrap items-center gap-3 rounded-lg border border-rosa/50 bg-rosa/5 px-4 py-2.5">
            <input
              v-model="t.nombre"
              type="text"
              maxlength="120"
              placeholder="Tema propio"
              class="min-w-40 flex-1 rounded-lg border-0 py-1.5 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
            <div class="inline-flex rounded-md border border-rosa p-0.5 text-xs">
              <button
                v-for="op in OPCIONES_ESTADO"
                :key="op.valor"
                type="button"
                class="rounded px-2.5 py-1 transition"
                :class="t.estado === op.valor ? chipActivo(op.valor) : 'text-cafe/60 hover:bg-rosa/20'"
                @click="setEstadoPersonalizado(t, op.valor)"
              >
                {{ op.texto }}
              </button>
            </div>
            <button type="button" class="text-cafe/50 hover:text-rojizo transition" aria-label="Quitar tema" @click="quitarTema(t.uid)">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <button type="button" class="mt-2 text-sm text-coral hover:text-rojizo transition" @click="agregarTema">
            + Agregar tema propio
          </button>
        </div>

        <!-- Pago extra -->
        <div>
          <label class="block text-sm text-cafe mb-2">Nota sobre precios (opcional)</label>
          <input v-model="form.pagoExtra" type="text" maxlength="250" placeholder="Ej.: ajustes por complejidad, urgencias, etc."
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>

        <BarraGuardado :hay-cambios="hayCambios" :guardando="guardando" texto="Guardar términos" @guardar="guardar">
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
            <p class="text-xs text-cafe/50">Tiempo estimado de entrega</p>
            <p class="text-sm text-cafe">{{ form.tiempoEntrega || '—' }}</p>
          </div>

          <div class="border-t border-rosa/40 pt-4">
            <h3 class="text-base font-medium text-granate mb-2">¿Qué hago y qué no?</h3>
            <ul class="space-y-1 text-sm text-cafe">
              <li v-for="t in temasCatalogo.filter((x) => estadosTemas[x.idTemaTermino])" :key="t.idTemaTermino" class="flex items-center justify-between gap-3">
                <span>{{ t.nombre }}</span>
                <span class="rounded-full px-2.5 py-0.5 text-xs" :class="chipActivo(estadosTemas[t.idTemaTermino])">{{ OPCIONES_ESTADO.find((o) => o.valor === estadosTemas[t.idTemaTermino])?.texto }}</span>
              </li>
              <li v-for="t in temasPersonalizados.filter((x) => x.nombre.trim() && x.estado)" :key="t.uid" class="flex items-center justify-between gap-3">
                <span>{{ t.nombre }}</span>
                <span class="rounded-full px-2.5 py-0.5 text-xs" :class="chipActivo(t.estado)">{{ OPCIONES_ESTADO.find((o) => o.valor === t.estado)?.texto }}</span>
              </li>
            </ul>
            <p v-if="!Object.values(estadosTemas).some((v) => v) && !temasPersonalizados.some((t) => t.estado)" class="text-sm text-cafe/40">Sin temas definidos.</p>
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
  </div>
</template>
