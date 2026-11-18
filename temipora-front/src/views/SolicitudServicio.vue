<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import VistaPreviaTerminos from '../components/VistaPreviaTerminos.vue'
import FormularioSolicitud from '../components/FormularioSolicitud.vue'
import DerechosUsoSolicitud from '../components/DerechosUsoSolicitud.vue'

const route = useRoute()
const router = useRouter()

const idServicio = computed(() => Number(route.params.id))
const cargando = ref(true)
const errorCarga = ref('')
const solicitudPublica = ref(null)
const paso = ref(1)
const errores = ref([])
const errorEnvio = ref('')
const conflictoSustitucion = ref(false)
const enviando = ref(false)
const enviado = ref(false)
const solicitud = ref(null)
const indiceItem = ref(0)
const aceptaTerminos = ref(false)
const cantidad = ref(1)
const aceptaEspera = ref(false)
const prioridadDefinida = ref(false)
const items = ref([crearItem()])
const urlsSubidas = new Set()
const oferta = ref(null)

const idOferta = computed(() => {
  const valor = Number(route.query.oferta)
  return Number.isFinite(valor) && valor > 0 ? valor : null
})
const idSustitucion = computed(() => {
  const valor = Number(route.query.sustitucion)
  return Number.isFinite(valor) && valor > 0 ? valor : null
})
const ofertaAplicable = computed(() =>
  oferta.value?.vigente ? oferta.value : null,
)
const descuentoOferta = computed(() => {
  const valor = Number(ofertaAplicable.value?.porcentajeDescuento)
  return Number.isFinite(valor) ? valor : 0
})

const PASOS = [
  { n: 1, label: 'Términos' },
  { n: 2, label: 'Dibujos y cupos' },
  { n: 3, label: 'Formulario' },
  { n: 4, label: 'Prioridad' },
  { n: 5, label: 'Revisión' },
]

const pasosVisibles = computed(() =>
  PASOS.filter((p) => p.n !== 4 || items.value.length > 1),
)

const CAMPOS_TERMINOS = [
  'informacionGeneral',
  'politicaRevisiones',
  'politicaCancelaciones',
  'usoArtista',
  'usoCliente',
  'queDibuja',
  'queNoDibuja',
]

const servicio = computed(() => solicitudPublica.value?.servicio || null)
const terminos = computed(() => solicitudPublica.value?.terminos || null)
const formulario = computed(() => solicitudPublica.value?.formulario || null)
const preguntas = computed(() => formulario.value?.preguntas || [])
const disponibilidad = computed(() => solicitudPublica.value?.disponibilidad || null)
const terminosValidos = computed(() => !!solicitudPublica.value?.terminosValidos)
const pasoActual = computed(
  () => pasosVisibles.value.find((p) => p.n === paso.value) || pasosVisibles.value[0],
)
const numeroPasoActual = computed(
  () => pasosVisibles.value.findIndex((p) => p.n === paso.value) + 1,
)

const terminosParaMostrar = computed(() => {
  const t = terminos.value
  if (!t) return null
  const form = {}
  for (const key of CAMPOS_TERMINOS) form[key] = t[key] || ''
  form.pagoExtra = t.pagoExtra || ''
  const estadoDerechos = {}
  for (const d of t.derechos || []) {
    estadoDerechos[d.idDerechoTermino] = {
      disponible: !!d.disponible,
      porcentajeExtra: d.porcentajeExtra,
    }
  }
  return { form, estadoDerechos }
})

const derechosTerminos = computed(() => terminos.value?.derechos || [])
const derechosOfertados = computed(() => derechosTerminos.value.filter(
  (d) => d.disponible && d.codigo !== 'PERSONAL' && !d.requierePiezaUnica,
))
const precioBaseParaDerechos = computed(
  () => Number(servicio.value?.precioBaseMostrado ?? servicio.value?.precioBase ?? 0),
)
const monedaParaDerechos = computed(
  () => servicio.value?.monedaMostrada ?? servicio.value?.moneda ?? 'ARS',
)
const precioMostrado = computed(() => {
  if (!servicio.value) return ''
  return formatearPrecio(
    servicio.value.precioBaseMostrado ?? servicio.value.precioBase,
    servicio.value.monedaMostrada ?? servicio.value.moneda,
  )
})

const cuposDisponibles = computed(() => disponibilidad.value?.cuposDisponibles ?? 0)
const cuposTotales = computed(() => disponibilidad.value?.cuposTotales ?? 0)
const tieneSolicitudActiva = computed(() => !!disponibilidad.value?.tieneSolicitudActiva)
const modoRecepcion = computed(() => disponibilidad.value?.modoRecepcion || 'ABIERTA')
const maxDibujos = computed(() => disponibilidad.value?.maxDibujosPorSolicitud || 3)
const esSoloEspera = computed(() => modoRecepcion.value === 'SOLO_ESPERA')
const estaCerrado = computed(() => modoRecepcion.value === 'CERRADA')
const puedeSolicitar = computed(() => !!disponibilidad.value?.puedeSolicitar)

const planValido = computed(() => {
  if (!disponibilidad.value || !puedeSolicitar.value || estaCerrado.value) return false
  if (cantidad.value < 1 || cantidad.value > maxDibujos.value) return false
  if (esSoloEspera.value && !aceptaEspera.value) return false
  return true
})

const mensajePlan = computed(() => {
  if (!disponibilidad.value) return ''
  if (tieneSolicitudActiva.value) {
    return 'Ya tenés una solicitud activa para este servicio.'
  }
  if (estaCerrado.value) {
    return 'El artista no está recibiendo solicitudes para este servicio.'
  }
  if (esSoloEspera.value) {
    if (!aceptaEspera.value) {
      return 'Aceptá la lista de espera para enviar la solicitud.'
    }
    return 'La solicitud completa quedará en lista de espera. No se promueve automáticamente.'
  }
  if (cuposDisponibles.value <= 0) {
    return 'No hay cupos libres. La solicitud quedará pendiente y el artista la presupuestará cuando se libere un cupo.'
  }
  return 'La solicitud completa ocupará un solo cupo. El precio base es por dibujo.'
})

const solicitudCreada = computed(() => solicitud.value)

function crearItem() {
  return { nombre: '', respuestas: {}, derechos: [] }
}

function derechosElegidos(item) {
  const ids = Array.isArray(item?.derechos) ? item.derechos : []
  return derechosOfertados.value.filter((d) => ids.includes(d.idDerechoTermino))
}

function scrollArriba() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const res = await api.get(`/servicios/${idServicio.value}/solicitud-publica`)
    solicitudPublica.value = res.data
    if (idOferta.value) {
      try {
        const ofertaRes = await api.get(`/ofertas/${idOferta.value}`)
        oferta.value = ofertaRes.data
      } catch (e) {
        oferta.value = null
        const status = e.response?.status
        if (status === 403) {
          errorCarga.value =
            'Esta oferta no pertenece a tu cuenta. Iniciá sesión con la cuenta correcta para usarla.'
          return
        }
        if (status === 404) {
          errorCarga.value = 'La oferta no existe o ya no está disponible.'
          return
        }
        errorCarga.value =
          'No pudimos validar la oferta de este link. Probá de nuevo en un rato.'
        return
      }
    }
    ajustarCantidad()
  } catch (e) {
    errorCarga.value =
      e.response?.data?.message || 'No se pudo cargar la información del servicio.'
  } finally {
    cargando.value = false
  }
}

function ajustarCantidad() {
  let n = Number(cantidad.value)
  if (!Number.isFinite(n)) n = 1
  n = Math.min(maxDibujos.value, Math.max(1, Math.trunc(n)))
  cantidad.value = n
  const previos = items.value
  items.value = Array.from({ length: n }, (_, i) => previos[i] || crearItem())
  if (indiceItem.value >= n) indiceItem.value = n - 1
  if (n === 1) prioridadDefinida.value = false
}

function valoresDe(item, pregunta) {
  const valores = item?.respuestas?.[pregunta.idPregunta]
  return Array.isArray(valores) ? valores.filter((v) => String(v).trim()) : []
}

function erroresDeItem(item, indice) {
  const errs = []
  const nombre = (item.nombre || '').trim()
  if (!nombre) errs.push(`Dibujo ${indice + 1}: poné un nombre.`)

  for (const pregunta of preguntas.value) {
    if (pregunta.codigoTipo === 'ENCABEZADO') continue
    const valores = valoresDe(item, pregunta)
    if (pregunta.obligatoria && !valores.length) {
      errs.push(`Dibujo ${indice + 1}: respondé “${pregunta.enunciado}”.`)
    }
    if (pregunta.codigoTipo === 'SUBIR_IMAGEN' && valores.length > 10) {
      errs.push(`Dibujo ${indice + 1}: “${pregunta.enunciado}” admite hasta 10 imágenes.`)
    }
  }
  return errs
}

function itemCompleto(item, indice) {
  return erroresDeItem(item, indice).length === 0
}

function validarTodosLosItems() {
  return items.value.flatMap((item, indice) => erroresDeItem(item, indice))
}

function continuar() {
  errores.value = []

  if (paso.value === 1) {
    if (!terminosValidos.value) {
      errores.value = ['El artista todavía no configuró los términos y condiciones.']
      return
    }
    if (!aceptaTerminos.value) {
      errores.value = ['Tenés que aceptar los términos y condiciones para continuar.']
      return
    }
  }

  if (paso.value === 2) {
    if (!planValido.value) {
      errores.value = [mensajePlan.value || 'No se puede enviar la solicitud con esta cantidad.']
      return
    }
  }

  if (paso.value === 3) {
    const problemas = validarTodosLosItems()
    if (problemas.length) {
      errores.value = problemas
      return
    }
  }

  if (paso.value < 5) {
    paso.value = paso.value === 3 && items.value.length === 1 ? 5 : paso.value + 1
    scrollArriba()
  }
}

function anterior() {
  errores.value = []
  errorEnvio.value = ''
  if (paso.value === 5 && items.value.length === 1) {
    paso.value = 3
    scrollArriba()
  } else if (paso.value > 1) {
    paso.value -= 1
    scrollArriba()
  }
}

function irAPaso(n) {
  if (n < paso.value) {
    errores.value = []
    errorEnvio.value = ''
    paso.value = n
    scrollArriba()
  }
}

function siguienteDibujo() {
  const problemas = erroresDeItem(items.value[indiceItem.value], indiceItem.value)
  if (problemas.length) {
    errores.value = problemas
    return
  }
  errores.value = []
  if (indiceItem.value < items.value.length - 1) {
    indiceItem.value += 1
    scrollArriba()
  } else {
    continuar()
  }
}

function anteriorDibujo() {
  errores.value = []
  if (indiceItem.value > 0) {
    indiceItem.value -= 1
    scrollArriba()
  } else {
    anterior()
  }
}

function moverItem(indice, delta) {
  const destino = indice + delta
  if (destino < 0 || destino >= items.value.length) return
  const copia = [...items.value]
  const [movido] = copia.splice(indice, 1)
  copia.splice(destino, 0, movido)
  items.value = copia
}

function registrarImagen(url) {
  if (url) urlsSubidas.add(url)
}

function olvidarImagen(url) {
  urlsSubidas.delete(url)
}

async function enviar() {
  errores.value = []
  errorEnvio.value = ''
  conflictoSustitucion.value = false

  if (!terminosValidos.value || !aceptaTerminos.value) {
    paso.value = 1
    errores.value = ['Tenés que aceptar los términos y condiciones.']
    return
  }
  if (!planValido.value) {
    paso.value = 2
    errores.value = [mensajePlan.value || 'Revisá la cantidad de dibujos y los cupos.']
    return
  }
  const problemas = validarTodosLosItems()
  if (problemas.length) {
    paso.value = 3
    errores.value = problemas
    return
  }

  enviando.value = true
  try {
    const definirPrioridad = items.value.length > 1 && prioridadDefinida.value
    const payload = {
      idServicio: idServicio.value,
      idOferta: ofertaAplicable.value?.idOferta ?? null,
      idSustitucion: idSustitucion.value,
      idTerminos: terminos.value.idTerminos,
      terminosVersion: terminos.value.version,
      aceptaTerminos: true,
      aceptaListaEspera: esSoloEspera.value ? aceptaEspera.value : false,
      prioridadDefinida: definirPrioridad,
      items: items.value.map((item, indice) => ({
        nombre: item.nombre.trim(),
        prioridad: definirPrioridad ? indice + 1 : null,
        derechos: item.derechos || [],
        respuestas: preguntas.value
          .filter((p) => p.codigoTipo !== 'ENCABEZADO')
          .map((p) => ({
            idPregunta: p.idPregunta,
            valores: item.respuestas?.[p.idPregunta] || [],
          })),
      })),
    }

    const res = await api.post('/solicitudes-servicio', payload)
    solicitud.value = res.data
    enviado.value = true
    paso.value = 6
    scrollArriba()
  } catch (e) {
    const mensaje =
      e.response?.data?.message || 'No se pudo enviar la solicitud. Intentá de nuevo.'
    if (e.response?.status === 409) {
      conflictoSustitucion.value = !!idSustitucion.value
      await cargar()
      paso.value = 1
      aceptaTerminos.value = false
      errores.value = [mensaje]
    } else {
      errorEnvio.value = mensaje
    }
  } finally {
    enviando.value = false
  }
}

function volverAlServicio() {
  router.push({ name: 'servicio-detalle', params: { id: idServicio.value } })
}

function irAServicios() {
  router.push({ name: 'servicios' })
}

onMounted(cargar)

onBeforeUnmount(() => {
  if (!enviado.value && urlsSubidas.size) {
    api.delete('/upload', { data: [...urlsSubidas] }).catch(() => {})
  }
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 sm:px-8 py-8">
    <button type="button" class="text-sm text-granate hover:text-coral transition" @click="volverAlServicio">
      ← Volver al servicio
    </button>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando solicitud...</p>
    <p v-else-if="errorCarga" class="text-rojizo text-center py-10">{{ errorCarga }}</p>

    <template v-else-if="solicitudPublica">
      <div v-if="paso !== 6" class="mt-5">
        <h1 class="font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">Solicitar servicio</h1>
        <p v-if="servicio" class="mt-1 text-sm text-cafe/65">
          {{ servicio.nombre }} · por @{{ servicio.nombreArtista }}
        </p>

        <div
          v-if="oferta"
          class="mt-4 rounded-2xl border px-4 py-3 text-sm"
          :class="ofertaAplicable
            ? 'border-coral/30 bg-coral/5 text-granate'
            : 'border-rojizo/30 bg-rojizo/5 text-rojizo'"
        >
          <template v-if="ofertaAplicable">
            <p class="font-bold">Descuento de reactivación del {{ descuentoOferta }}%</p>
            <p class="mt-1 text-cafe/75">
              Lo financia Temipora. Se aplica sobre el presupuesto que envíe el artista.
            </p>
          </template>
          <template v-else>
            <p class="font-bold">La oferta venció</p>
            <p class="mt-1 text-cafe/75">Podés enviar la solicitud sin descuento.</p>
          </template>
        </div>

        <nav aria-label="Pasos de la solicitud" class="mt-6">
          <ol class="mx-auto flex w-full max-w-3xl items-start">
            <li
              v-for="(p, i) in pasosVisibles"
              :key="p.n"
              class="flex items-start"
              :class="i < pasosVisibles.length - 1 ? 'flex-1' : ''"
            >
              <button
                type="button"
                class="group flex w-16 shrink-0 flex-col items-center gap-2 sm:w-24"
                :disabled="p.n >= paso"
                @click="irAPaso(p.n)"
              >
                <span
                  class="flex h-9 w-9 items-center justify-center rounded-full border text-sm transition"
                  :class="paso === p.n
                    ? 'border-coral bg-coral text-white shadow-md shadow-coral/20'
                    : (p.n < paso ? 'border-coral/40 bg-coral/10 text-coral' : 'border-rosa bg-blanco text-cafe/40')"
                >
                  <svg v-if="p.n < paso" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                  </svg>
                  <span v-else>{{ p.n }}</span>
                </span>
                <span
                  class="hidden text-center text-[11px] leading-tight transition sm:block"
                  :class="paso === p.n ? 'text-granate' : 'text-cafe/50'"
                >
                  {{ p.label }}
                </span>
              </button>
              <span
                v-if="i < pasosVisibles.length - 1"
                class="mt-4.5 h-px flex-1 transition"
                :class="p.n < paso ? 'bg-coral/50' : 'bg-rosa/70'"
              ></span>
            </li>
          </ol>
          <p class="mt-4 text-center text-xs text-cafe/50 sm:hidden">
            Paso {{ numeroPasoActual }} de {{ pasosVisibles.length }} · {{ pasoActual.label }}
          </p>
        </nav>
      </div>

      <div v-if="errores.length" class="mt-4 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3">
        <ul class="list-disc pl-5 text-sm text-rojizo space-y-1">
          <li v-for="(e, i) in errores" :key="i">{{ e }}</li>
        </ul>
        <router-link
          v-if="conflictoSustitucion"
          :to="{ name: 'servicio-alternativas', params: { id: idServicio }, query: { disparador: 'INTENTO_SOLICITUD' } }"
          class="mt-2 inline-block text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
        >
          La alternativa que elegiste se llenó. Ver otras opciones →
        </router-link>
      </div>

      <!-- Paso 1: términos -->
      <section v-if="paso === 1" class="mt-6 rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
        <h2 class="text-xl text-granate">Términos y condiciones</h2>
        <p class="mt-1 text-sm text-cafe/60">
          Revisá las condiciones del artista. La versión aceptada quedará guardada con tu solicitud.
        </p>

        <div v-if="!terminosValidos" class="mt-5 rounded-xl border border-dashed border-rojizo/40 bg-rojizo/5 px-4 py-5 text-sm text-rojizo">
          El artista todavía no configuró los términos y condiciones de este servicio. No es posible enviar la solicitud.
        </div>

        <div v-else class="mt-5">
          <VistaPreviaTerminos
            :terminos="terminosParaMostrar"
            :derechos-catalogo="derechosTerminos"
            :mostrar-aviso="false"
            :ocultar-vacios="true"
          />
          <label class="mt-6 flex items-start gap-2 border-t border-rosa/50 pt-5 text-sm text-cafe">
            <input v-model="aceptaTerminos" type="checkbox" class="mt-0.5 accent-coral" />
            <span>
              Acepto los términos y condiciones del servicio
              <span class="block text-xs text-cafe/50 mt-0.5">Versión {{ terminos?.version }}</span>
            </span>
          </label>
        </div>
      </section>

      <!-- Paso 2: dibujos y cupos -->
      <section v-else-if="paso === 2" class="mt-6 space-y-5">
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
          <h2 class="text-xl text-granate">Dibujos y cupos</h2>
          <p class="mt-1 text-sm text-cafe/60">
            La solicitud completa ocupa un solo cupo, sin importar cuántos dibujos incluya.
          </p>

          <div v-if="servicio" class="mt-4 rounded-xl border border-rosa/40 bg-rosa/10 px-4 py-3">
            <p class="text-xs uppercase tracking-wide text-cafe/50">
              Precio base <span class="font-semibold text-granate">por dibujo</span>
            </p>
            <p class="mt-1 text-lg text-granate">{{ precioMostrado }}</p>
            <p class="text-xs text-cafe/50">El total base se multiplica por la cantidad de dibujos solicitados.</p>
          </div>

          <div class="mt-5 grid gap-4 sm:grid-cols-2">
            <!--div class="rounded-xl bg-rosa/20 p-4">
              <p class="text-xs uppercase tracking-wide text-cafe/50">Cupos totales</p>
              <p class="mt-1 text-2xl text-granate">{{ cuposTotales }}</p>
            </div-->
            <div class="rounded-xl bg-rosa/20 p-4">
              <p class="text-xs uppercase tracking-wide text-cafe/50">Cupos disponibles</p>
              <p class="mt-1 text-2xl text-granate">{{ cuposDisponibles }}</p>
            </div>
            <div class="rounded-xl bg-rosa/20 p-4">
              <p class="text-xs uppercase tracking-wide text-cafe/50">Recepción</p>
              <p class="mt-1 text-sm text-granate">
                {{ esSoloEspera
                  ? 'Solo lista de espera'
                  : (estaCerrado
                    ? 'Cerrada'
                    : (cuposDisponibles <= 0 ? 'Abierta · sin cupos' : 'Abierta')) }}
              </p>
            </div>
          </div>

          <div class="mt-6 max-w-xs">
            <label class="block text-sm text-cafe mb-2">Cantidad de dibujos <span class="text-rojizo">*</span></label>
            <input
              v-model.number="cantidad"
              type="number"
              min="1"
              :max="maxDibujos"
              step="1"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              @change="ajustarCantidad"
            />
            <p class="mt-1 text-xs text-cafe/50">Máximo {{ maxDibujos }} dibujos por solicitud.</p>
          </div>
        </div>

        <div
          v-if="mensajePlan"
          class="rounded-2xl border p-5"
          :class="planValido ? 'border-rosa/50 bg-rosa/10' : 'border-rojizo/30 bg-rojizo/5'"
        >
          <p class="text-sm" :class="planValido ? 'text-cafe' : 'text-rojizo'">{{ mensajePlan }}</p>

          <router-link
            v-if="cuposDisponibles <= 0 && !estaCerrado"
            :to="{ name: 'servicio-alternativas', params: { id: idServicio }, query: { disparador: 'INTENTO_SOLICITUD' } }"
            class="mt-2 inline-block text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            Ver artistas con cupo →
          </router-link>

          <label v-if="esSoloEspera" class="mt-4 flex items-start gap-2 text-sm text-cafe">
            <input v-model="aceptaEspera" type="checkbox" class="mt-0.5 accent-coral" />
            <span>
              Acepto que mi solicitud quede en lista de espera.
              <span class="block text-xs text-cafe/50 mt-0.5">La lista de espera no se promueve automáticamente.</span>
            </span>
          </label>
        </div>
      </section>

      <!-- Paso 3: formulario -->
      <section v-else-if="paso === 3" class="mt-6 rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 class="text-xl text-granate">Formulario de cada dibujo</h2>
            <p class="mt-1 text-sm text-cafe/60">Completá las preguntas para el dibujo que estás editando.</p>
          </div>
          <span class="rounded-full bg-rosa/30 px-3 py-1 text-xs text-cafe/70">
            {{ indiceItem + 1 }} de {{ items.length }}
          </span>
        </div>

        <div class="mt-5 flex flex-wrap gap-2">
          <button
            v-for="(item, i) in items"
            :key="i"
            type="button"
            class="flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs transition"
            :class="i === indiceItem ? 'border-coral bg-coral/10 text-granate' : 'border-rosa/60 text-cafe/60 hover:border-coral'"
            @click="indiceItem = i"
          >
            <span
              class="h-2 w-2 rounded-full"
              :class="itemCompleto(item, i) ? 'bg-green-600' : 'bg-rosa'"
            ></span>
            <span>{{ item.nombre || `Dibujo ${i + 1}` }}</span>
          </button>
        </div>

        <div class="mt-6">
          <label class="block text-sm text-cafe mb-2">Nombre del dibujo <span class="text-rojizo">*</span></label>
          <input
            v-model="items[indiceItem].nombre"
            type="text"
            maxlength="100"
            placeholder="Ej: Retrato de mi personaje"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>

        <div class="mt-6">
          <FormularioSolicitud
            v-model="items[indiceItem].respuestas"
            :preguntas="preguntas"
            @imagen-subida="registrarImagen"
            @imagen-eliminada="olvidarImagen"
          />
        </div>

        <div class="mt-6">
          <DerechosUsoSolicitud
            v-model="items[indiceItem].derechos"
            :derechos="derechosOfertados"
            :precio-base="precioBaseParaDerechos"
            :moneda="monedaParaDerechos"
          />
        </div>

        <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-rosa/50 pt-5">
          <button type="button" class="text-sm text-cafe/60 hover:text-granate transition" @click="anteriorDibujo">
            ← Anterior
          </button>
          <button
            type="button"
            class="rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
            @click="siguienteDibujo"
          >
            {{ indiceItem < items.length - 1 ? 'Siguiente dibujo →' : (items.length === 1 ? 'Revisar solicitud →' : 'Definir prioridad →') }}
          </button>
        </div>
      </section>

      <!-- Paso 4: prioridad -->
      <section v-else-if="paso === 4" class="mt-6 rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
        <h2 class="text-xl text-granate">Prioridad de los dibujos</h2>
        <p class="mt-1 text-sm text-cafe/60">
          El orden queda dentro de la misma solicitud. La solicitud no se divide en varios pedidos.
        </p>

        <label class="mt-5 flex items-start gap-2 text-sm text-cafe">
          <input v-model="prioridadDefinida" type="checkbox" class="mt-0.5 accent-coral" />
          <span>
            Quiero definir el orden de prioridad
            <span class="block text-xs text-cafe/50 mt-0.5">Si no lo activás, se respeta el orden en que cargaste los dibujos.</span>
          </span>
        </label>

        <div class="mt-5 space-y-2">
          <div
            v-for="(item, i) in items"
            :key="i"
            class="flex items-center gap-3 rounded-xl border border-rosa/50 px-3 py-2"
          >
            <span class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-rosa/30 text-xs font-medium text-granate">
              {{ i + 1 }}
            </span>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm text-cafe">{{ item.nombre || `Dibujo ${i + 1}` }}</p>
              <p class="text-xs text-cafe/50">Orden dentro de la solicitud</p>
            </div>
            <div v-if="prioridadDefinida" class="flex gap-1">
              <button
                type="button"
                class="rounded border border-rosa px-2 py-1 text-xs text-cafe hover:bg-rosa/20 disabled:opacity-30"
                :disabled="i === 0"
                aria-label="Subir prioridad"
                @click="moverItem(i, -1)"
              >
                ↑
              </button>
              <button
                type="button"
                class="rounded border border-rosa px-2 py-1 text-xs text-cafe hover:bg-rosa/20 disabled:opacity-30"
                :disabled="i === items.length - 1"
                aria-label="Bajar prioridad"
                @click="moverItem(i, 1)"
              >
                ↓
              </button>
            </div>
          </div>
        </div>

        <p v-if="mensajePlan" class="mt-4 text-xs text-cafe/50">{{ mensajePlan }}</p>
      </section>

      <!-- Paso 5: revisión -->
      <section v-else-if="paso === 5" class="mt-6 space-y-5">
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
          <h2 class="text-xl text-granate">Revisión de la solicitud</h2>
          <p class="mt-1 text-sm text-cafe/60">Verificá la información antes de enviarla al artista.</p>

          <dl class="mt-5 grid gap-3 text-sm sm:grid-cols-2">
            <div class="rounded-xl bg-rosa/15 p-3">
              <dt class="text-xs text-cafe/50">Servicio</dt>
              <dd class="mt-1 text-cafe">{{ servicio?.nombre }}</dd>
            </div>
            <div class="rounded-xl bg-rosa/15 p-3">
              <dt class="text-xs text-cafe/50">Precio base <span class="font-semibold text-granate">por dibujo</span></dt>
              <dd class="mt-1 text-cafe">{{ precioMostrado }}</dd>
            </div>
            <div class="rounded-xl bg-rosa/15 p-3">
              <dt class="text-xs text-cafe/50">Dibujos</dt>
              <dd class="mt-1 text-cafe">{{ items.length }}</dd>
            </div>
            <div class="rounded-xl bg-rosa/15 p-3">
              <dt class="text-xs text-cafe/50">Términos aceptados</dt>
              <dd class="mt-1 text-cafe">Versión {{ terminos?.version }}</dd>
            </div>
          </dl>

          <div class="mt-5 rounded-xl border border-rosa/50 p-4">
            <p class="text-sm text-cafe">{{ mensajePlan }}</p>
            <div class="mt-3 flex flex-wrap items-center gap-2 text-sm">
              <span class="rounded-full bg-rosa/30 px-3 py-1 text-cafe">
                {{ esSoloEspera ? 'Lista de espera' : 'Solicitud pendiente' }}
              </span>
              <span class="text-cafe/60">1 cupo · {{ items.length }} dibujo(s)</span>
            </div>
            <p class="mt-3 text-xs text-cafe/50">
              El precio base es <strong class="font-semibold text-granate">por dibujo</strong>, no por solicitud.
            </p>
          </div>

          <div class="mt-5">
            <p class="text-xs uppercase tracking-wide text-cafe/50">Dibujos en el orden final</p>
            <ol class="mt-2 space-y-2">
              <li
                v-for="(item, i) in items"
                :key="i"
                class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-rosa/40 px-3 py-2 text-sm"
              >
                <span class="min-w-0">
                  <span class="block text-cafe">{{ i + 1 }}. {{ item.nombre }}</span>
                  <span class="mt-0.5 block text-xs text-cafe/55">
                    {{ derechosElegidos(item).length
                      ? derechosElegidos(item)
                        .map((d) => `${d.nombre} (+${d.porcentajeExtra ?? 0}%)`)
                        .join(' · ')
                      : 'Uso personal' }}
                  </span>
                </span>
                <span class="text-xs text-cafe/50">
                  {{ esSoloEspera ? 'Lista de espera' : 'Pendiente' }}
                  <template v-if="prioridadDefinida"> · Prioridad {{ i + 1 }}</template>
                </span>
              </li>
            </ol>
          </div>

          <p v-if="errorEnvio" class="mt-5 rounded-xl border border-rojizo/30 bg-rojizo/5 px-4 py-3 text-sm text-rojizo">
            {{ errorEnvio }}
          </p>

          <p
            v-if="conflictoSustitucion"
            class="mt-3 rounded-xl border border-borde/60 bg-crema px-4 py-3 text-sm text-cafe"
          >
            La alternativa que habías elegido dejó de tener cupo.
            <router-link
              :to="{ name: 'servicio-alternativas', params: { id: idServicio }, query: { disparador: 'INTENTO_SOLICITUD' } }"
              class="font-semibold text-coral underline decoration-coral/40 underline-offset-2 transition hover:text-rojizo"
            >
              Ver otras opciones →
            </router-link>
          </p>

          <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-rosa/50 pt-5">
            <button type="button" class="text-sm text-cafe/60 hover:text-granate transition" @click="anterior">
              ← Volver
            </button>
            <button
              type="button"
              class="rounded-md bg-coral px-5 py-2.5 text-sm text-white hover:bg-rojizo transition shadow-sm disabled:opacity-60"
              :disabled="enviando || !planValido"
              @click="enviar"
            >
              {{ enviando ? 'Enviando...' : 'Enviar solicitud' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Éxito -->
      <section v-else-if="paso === 6" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
        <div class="flex items-start gap-3">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-100 text-green-700">✓</span>
          <div>
            <h2 class="font-display text-2xl font-extrabold text-granate">Solicitud enviada</h2>
            <p class="mt-1 text-sm text-cafe/65">
              El artista recibió tu solicitud para “{{ servicio?.nombre }}”.
            </p>
          </div>
        </div>

        <div v-if="solicitudCreada" class="mt-6 rounded-xl bg-rosa/20 p-4">
          <p class="text-xs text-cafe/50">
            {{ solicitudCreada.estado === 'EN_LISTA_ESPERA' ? 'Lista de espera' : 'Solicitud pendiente' }}
          </p>
          <p class="mt-1 text-lg text-granate">
            {{ solicitudCreada.cantidadDibujos }} dibujo(s) · 1 cupo
          </p>
          <p v-if="solicitudCreada.estado === 'EN_LISTA_ESPERA'" class="mt-1 text-xs text-cafe/60">
            Posición {{ solicitudCreada.posicionListaEspera }}. No se promueve automáticamente.
          </p>
          <p v-else class="mt-1 text-xs text-cafe/60">
            Quedó registrada para revisión del artista.
          </p>
        </div>

        <div class="mt-6 flex flex-wrap gap-3 border-t border-rosa/50 pt-5">
          <button
            type="button"
            class="rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
            @click="volverAlServicio"
          >
            Volver al servicio
          </button>
          <button
            type="button"
            class="rounded-md border border-rosa px-4 py-2 text-sm text-granate hover:bg-rosa/20 transition"
            @click="irAServicios"
          >
            Ver más servicios
          </button>
        </div>
      </section>

      <div v-if="paso !== 6" class="mt-6 flex items-center justify-between gap-3">
        <button
          type="button"
          class="text-sm text-cafe/60 hover:text-granate transition disabled:opacity-30"
          :disabled="paso === 1"
          @click="anterior"
        >
          ← Anterior
        </button>
        <button
          v-if="paso < 5"
          type="button"
          class="rounded-md bg-coral px-5 py-2.5 text-sm text-white hover:bg-rojizo transition shadow-sm disabled:opacity-60 disabled:cursor-not-allowed"
          :disabled="paso === 1 && (!terminosValidos || !aceptaTerminos)"
          @click="continuar"
        >
          Continuar →
        </button>
      </div>
    </template>
  </div>
</template>
