<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  UNIDADES_ENTREGA,
  gruposDeRespuestas,
  textoTiempoEntrega,
} from '../servicios/solicitudes'
import { useNotificaciones } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()
const { exito } = useNotificaciones()

const idSolicitud = computed(() => Number(route.params.idSolicitud))
const idActual = Number(localStorage.getItem('idUsuario'))
const fechaActual = new Date()
const hoy = `${fechaActual.getFullYear()}-${String(fechaActual.getMonth() + 1).padStart(2, '0')}-${String(fechaActual.getDate()).padStart(2, '0')}`

const solicitud = ref(null)
const cargando = ref(true)
const errorCarga = ref('')
const enviando = ref(false)
const errorEnvio = ref('')
const paso = ref(1)

const pasos = [
  { numero: 1, titulo: 'Entrega' },
  { numero: 2, titulo: 'Dibujos' },
  { numero: 3, titulo: 'Resumen' },
]

const form = ref({
  tiempoEntregaValor: 1,
  tiempoEntregaUnidad: 'SEMANAS',
  fechaVencimiento: '',
  mensaje: '',
  items: [],
})

const esArtista = computed(
  () => solicitud.value?.artista?.idUsuario === idActual,
)
const estadoPresupuestable = computed(
  () => ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.value?.estado),
)
const puedePresupuestar = computed(
  () => esArtista.value
    && estadoPresupuestable.value
    && !solicitud.value?.presupuesto
    && solicitud.value?.puedePresupuestar === true,
)
const motivoBloqueo = computed(() => {
  if (cargando.value || !solicitud.value) return ''
  if (!esArtista.value) return 'No podés presupuestar esta solicitud.'
  if (solicitud.value.presupuesto) {
    return 'Ya enviaste un presupuesto para esta solicitud.'
  }
  if (!estadoPresupuestable.value) {
    return 'Esta solicitud ya no se puede presupuestar.'
  }
  return solicitud.value.motivoNoPresupuestable
    || 'No podés presupuestar esta solicitud todavía.'
})
const puedeEditarServicio = computed(
  () => solicitud.value?.estadoServicio !== 'ARCHIVADO',
)

const items = computed(() => form.value.items)
const moneda = computed(() => solicitud.value?.moneda || 'ARS')
const entregaTexto = computed(() => textoTiempoEntrega(
  form.value.tiempoEntregaValor,
  form.value.tiempoEntregaUnidad,
))

function formatear(valor) {
  return formatearPrecio(valor, moneda.value)
}

function baseItem() {
  const base = Number(solicitud.value?.precioBase ?? 0)
  return Number.isFinite(base) && base > 0 ? base : 0
}

function valorRecargo(recargo) {
  const valor = Number(String(recargo?.valor ?? '').replace(',', '.'))
  return Number.isFinite(valor) && valor > 0 ? valor : 0
}

function subtotalItem(item) {
  let fijos = 0
  let porcentajes = 0
  for (const recargo of item.recargos || []) {
    const valor = valorRecargo(recargo)
    if (recargo.tipo === 'PORCENTAJE') porcentajes += valor
    else fijos += valor
  }
  const base = baseItem()
  return Math.round((base + fijos + (base * porcentajes) / 100) * 100) / 100
}

const totalBase = computed(
  () => Math.round(items.value.length * baseItem() * 100) / 100,
)
const totalFinal = computed(
  () => Math.round(items.value.reduce((total, item) => total + subtotalItem(item), 0) * 100) / 100,
)
const totalRecargos = computed(
  () => Math.round((totalFinal.value - totalBase.value) * 100) / 100,
)

function agregarRecargo(item) {
  if (item.recargos.length >= 20) return
  item.recargos.push({
    descripcion: '',
    tipo: 'PORCENTAJE',
    valor: '',
  })
}

function quitarRecargo(item, indice) {
  item.recargos.splice(indice, 1)
}

function resumenRespuestas(grupo) {
  if (grupo.codigoTipo === 'SUBIR_IMAGEN') {
    const cantidad = grupo.valores.length
    return `${cantidad} ${cantidad === 1 ? 'imagen adjunta' : 'imágenes adjuntas'}`
  }
  return grupo.valores.join(', ')
}

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const res = await api.get(`/solicitudes-servicio/${idSolicitud.value}`)
    solicitud.value = res.data

    if (!puedePresupuestar.value) return

    form.value.items = (res.data.items || []).map((item) => ({
      idItemSolicitud: item.idItemSolicitud,
      nombre: item.nombre,
      nota: '',
      recargos: [],
      respuestas: gruposDeRespuestas(item),
    }))
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (e.response?.status === 403 || e.response?.status === 404) {
      errorCarga.value = 'No podés presupuestar esta solicitud o ya no existe.'
    } else {
      errorCarga.value = e.response?.data?.message || 'No se pudo cargar la solicitud.'
    }
  } finally {
    cargando.value = false
  }
}

function validarPaso1() {
  errorEnvio.value = ''
  const valor = Number(form.value.tiempoEntregaValor)
  if (!Number.isFinite(valor) || valor < 1) {
    errorEnvio.value = 'Ingresá un tiempo máximo de entrega de al menos 1.'
    return false
  }
  if (!form.value.tiempoEntregaUnidad) {
    errorEnvio.value = 'Elegí la unidad del tiempo de entrega.'
    return false
  }
  if (form.value.fechaVencimiento) {
    const fecha = new Date(`${form.value.fechaVencimiento}T23:59:59`)
    if (Number.isNaN(fecha.getTime()) || fecha <= new Date()) {
      errorEnvio.value = 'La fecha de vencimiento debe ser futura.'
      return false
    }
  }
  return true
}

function validarPaso2() {
  errorEnvio.value = ''
  for (const item of items.value) {
    if ((item.nota || '').length > 250) {
      errorEnvio.value = `La nota de "${item.nombre}" no puede superar los 250 caracteres.`
      return false
    }
    for (const recargo of item.recargos) {
      if (!recargo.descripcion?.trim()) {
        errorEnvio.value = `Completá la descripción del recargo de "${item.nombre}".`
        return false
      }
      if (valorRecargo(recargo) <= 0) {
        errorEnvio.value = `Ingresá un valor mayor a cero en el recargo de "${item.nombre}".`
        return false
      }
      if (recargo.tipo === 'PORCENTAJE' && valorRecargo(recargo) > 1000) {
        errorEnvio.value = 'El porcentaje de un recargo no puede superar el 1000%.'
        return false
      }
    }
  }
  return true
}

function validarPaso3() {
  errorEnvio.value = ''
  if ((form.value.mensaje || '').length > 500) {
    errorEnvio.value = 'El mensaje no puede superar los 500 caracteres.'
    return false
  }
  return true
}

function siguiente() {
  if (paso.value === 1 && !validarPaso1()) return
  if (paso.value === 2 && !validarPaso2()) return
  paso.value = Math.min(3, paso.value + 1)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function anterior() {
  errorEnvio.value = ''
  paso.value = Math.max(1, paso.value - 1)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function volver() {
  router.push({
    name: 'solicitud-detalle',
    params: { idSolicitud: idSolicitud.value },
  })
}

async function enviar() {
  if (enviando.value) return
  if (!validarPaso1() || !validarPaso2() || !validarPaso3()) return

  enviando.value = true
  errorEnvio.value = ''
  try {
    const payload = {
      tiempoEntregaValor: Number(form.value.tiempoEntregaValor),
      tiempoEntregaUnidad: form.value.tiempoEntregaUnidad,
      fechaVencimiento: form.value.fechaVencimiento || null,
      mensaje: form.value.mensaje?.trim() || null,
      items: items.value.map((item) => ({
        idItemSolicitud: item.idItemSolicitud,
        nota: item.nota?.trim() || null,
        recargos: (item.recargos || []).map((recargo) => ({
          descripcion: recargo.descripcion.trim(),
          tipo: recargo.tipo,
          valor: valorRecargo(recargo),
        })),
      })),
    }

    await api.post(`/solicitudes-servicio/${idSolicitud.value}/presupuesto`, payload)
    exito('Presupuesto enviado. El cliente ya recibió la notificación.')
    router.push({
      name: 'solicitud-detalle',
      params: { idSolicitud: idSolicitud.value },
    })
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorEnvio.value = e.response?.data?.message || 'No se pudo enviar el presupuesto.'
  } finally {
    enviando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a la solicitud
    </button>

    <div
      v-if="cargando"
      class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center text-sm text-cafe/60"
    >
      Cargando solicitud...
    </div>

    <div
      v-else-if="errorCarga"
      class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center"
    >
      <p class="text-sm text-cafe/70">{{ errorCarga }}</p>
      <div class="mt-4 flex flex-wrap justify-center gap-2">
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargar"
        >
          Reintentar
        </button>
        <button
          type="button"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="volver"
        >
          Volver a la solicitud
        </button>
      </div>
    </div>

    <div
      v-else-if="!puedePresupuestar"
      class="mt-8 rounded-2xl border border-durazno bg-durazno/25 px-6 py-12 text-center"
    >
      <p class="text-lg font-semibold text-granate">No podés presupuestar esta solicitud</p>
      <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/70">
        {{ motivoBloqueo }}
      </p>
      <div class="mt-5 flex flex-wrap justify-center gap-2">
        <router-link
          v-if="puedeEditarServicio"
          :to="{ name: 'servicio-editar', params: { id: solicitud.idServicio } }"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Editar servicio para ampliar cupos
        </router-link>
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="volver"
        >
          Volver a la solicitud
        </button>
      </div>
    </div>

    <template v-else>
      <header class="mt-6">
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Enviar presupuesto</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Solicitud #{{ idSolicitud }}
        </h1>
        <p class="mt-2 text-sm text-cafe/65">
          {{ solicitud.nombreServicio || 'Servicio' }}
          <span aria-hidden="true"> · </span>
          {{ solicitud.cantidadDibujos }} {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }}
          <span aria-hidden="true"> · </span>
          Precio base {{ formatear(solicitud.precioBase) }} por dibujo
        </p>
      </header>

      <ol class="mt-6 flex items-center gap-2" aria-label="Pasos del presupuesto">
        <li
          v-for="p in pasos"
          :key="p.numero"
          class="flex min-w-0 flex-1 items-center gap-2"
        >
          <span
            class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border text-xs font-semibold"
            :class="paso >= p.numero
              ? 'border-coral bg-coral text-white'
              : 'border-rosa bg-blanco text-cafe/50'"
          >
            {{ p.numero }}
          </span>
          <span
            class="hidden truncate text-xs sm:block"
            :class="paso === p.numero ? 'font-semibold text-granate' : 'text-cafe/50'"
          >
            {{ p.titulo }}
          </span>
          <span
            v-if="p.numero < pasos.length"
            class="h-px flex-1 bg-rosa/60"
            aria-hidden="true"
          ></span>
        </li>
      </ol>

      <form class="mt-6 space-y-5" @submit.prevent="enviar">
        <section
          v-if="paso === 1"
          class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
        >
          <h2 class="text-xl font-semibold text-granate">Tiempo de entrega</h2>
          <p class="mt-1 text-sm text-cafe/60">
            Definí el máximo de tiempo desde que el cliente paga hasta que entregás el trabajo.
          </p>

          <div class="mt-5 grid gap-4 sm:grid-cols-2">
            <div>
              <label for="tiempo-entrega" class="block text-sm text-cafe">
                Tiempo máximo <span class="text-rojizo">*</span>
              </label>
              <input
                id="tiempo-entrega"
                v-model.number="form.tiempoEntregaValor"
                type="number"
                min="1"
                step="1"
                required
                class="mt-2 block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>

            <div>
              <label for="unidad-entrega" class="block text-sm text-cafe">
                Unidad <span class="text-rojizo">*</span>
              </label>
              <select
                id="unidad-entrega"
                v-model="form.tiempoEntregaUnidad"
                required
                class="mt-2 block w-full rounded-lg border-0 bg-blanco px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              >
                <option
                  v-for="(info, codigo) in UNIDADES_ENTREGA"
                  :key="codigo"
                  :value="codigo"
                >
                  {{ info.plural }}
                </option>
              </select>
            </div>
          </div>

          <p class="mt-3 text-xs text-cafe/50">
            Entrega estimada: <span class="font-semibold text-cafe">{{ entregaTexto }}</span>
          </p>

          <div class="mt-6 border-t border-rosa/40 pt-5">
            <label for="fecha-vencimiento" class="block text-sm text-cafe">
              Vencimiento del presupuesto <span class="text-cafe/40">(opcional)</span>
            </label>
            <input
              id="fecha-vencimiento"
              v-model="form.fechaVencimiento"
              type="date"
              :min="hoy"
              class="mt-2 block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral sm:max-w-xs"
            />
            <p class="mt-2 text-xs leading-relaxed text-cafe/50">
              Si no elegís una fecha, se usa el vencimiento por defecto configurado por el sistema.
            </p>
          </div>
        </section>

        <section v-else-if="paso === 2" class="space-y-4">
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Recargos por dibujo</h2>
            <p class="mt-1 text-sm text-cafe/60">
              Podés agregar recargos fijos o porcentuales a cada dibujo. La base es
              {{ formatear(solicitud.precioBase) }} por dibujo.
            </p>
          </div>

          <article
            v-for="item in items"
            :key="item.idItemSolicitud"
            class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Dibujo
                </p>
                <h3 class="mt-1 text-lg font-semibold text-granate">{{ item.nombre }}</h3>
              </div>
              <div class="text-right">
                <p class="text-xs text-cafe/50">Subtotal</p>
                <p class="text-lg font-semibold text-granate">{{ formatear(subtotalItem(item)) }}</p>
              </div>
            </div>

            <p class="mt-2 text-xs text-cafe/50">
              Base {{ formatear(baseItem()) }}
            </p>

            <details
              v-if="item.respuestas.length"
              class="mt-4 rounded-xl border border-rosa/40 bg-rosa/5 px-3 py-2"
            >
              <summary class="cursor-pointer text-xs font-semibold text-coral">
                Ver respuestas del cliente
              </summary>
              <dl class="mt-3 space-y-3 text-xs">
                <div v-for="grupo in item.respuestas" :key="grupo.clave">
                  <dt class="font-semibold text-cafe/70">{{ grupo.enunciado }}</dt>
                  <dd class="mt-0.5 whitespace-pre-line leading-relaxed text-cafe/60">
                    {{ resumenRespuestas(grupo) }}
                  </dd>
                </div>
              </dl>
            </details>

            <div class="mt-5 space-y-3">
              <div
                v-for="(recargo, indice) in item.recargos"
                :key="`${item.idItemSolicitud}-recargo-${indice}`"
                class="grid gap-2 rounded-xl border border-rosa/40 bg-rosa/5 p-3 sm:grid-cols-[minmax(0,1fr)_10rem_8rem_auto]"
              >
                <div>
                  <label class="block text-xs text-cafe/60">Descripción</label>
                  <input
                    v-model="recargo.descripcion"
                    type="text"
                    maxlength="100"
                    placeholder="Ej. Personaje extra"
                    class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  />
                </div>

                <div>
                  <label class="block text-xs text-cafe/60">Tipo</label>
                  <select
                    v-model="recargo.tipo"
                    class="mt-1 block w-full rounded-lg border-0 bg-blanco px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  >
                    <option value="PORCENTAJE">Porcentaje (%)</option>
                    <option value="FIJO">Monto fijo</option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs text-cafe/60">
                    {{ recargo.tipo === 'PORCENTAJE' ? 'Porcentaje' : 'Monto' }}
                  </label>
                  <input
                    v-model="recargo.valor"
                    type="text"
                    inputmode="decimal"
                    placeholder="0"
                    class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  />
                </div>

                <div class="flex items-end">
                  <button
                    type="button"
                    class="inline-flex min-h-9 items-center justify-center rounded-full border border-rojizo/30 px-3 text-xs font-semibold text-rojizo transition hover:bg-rojizo/10"
                    @click="quitarRecargo(item, indice)"
                  >
                    Quitar
                  </button>
                </div>
              </div>

              <button
                type="button"
                :disabled="item.recargos.length >= 20"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10 disabled:cursor-not-allowed disabled:opacity-50"
                @click="agregarRecargo(item)"
              >
                + Agregar recargo
              </button>
            </div>

            <div class="mt-5">
              <label :for="`nota-${item.idItemSolicitud}`" class="block text-sm text-cafe">
                Nota para el cliente <span class="text-cafe/40">(opcional)</span>
              </label>
              <textarea
                :id="`nota-${item.idItemSolicitud}`"
                v-model="item.nota"
                rows="2"
                maxlength="250"
                placeholder="Ej. Incluye dos personajes y fondo simple."
                class="mt-2 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              ></textarea>
              <p class="mt-1 text-right text-[11px] text-cafe/40">
                {{ (item.nota || '').length }}/250
              </p>
            </div>
          </article>
        </section>

        <section v-else class="space-y-4">
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Mensaje para el cliente</h2>
            <p class="mt-1 text-sm text-cafe/60">
              Podés explicar el presupuesto o dejar una aclaración.
            </p>
            <textarea
              v-model="form.mensaje"
              rows="4"
              maxlength="500"
              placeholder="Hola, te paso el presupuesto de tu solicitud..."
              class="mt-4 block w-full rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            ></textarea>
            <p class="mt-1 text-right text-[11px] text-cafe/40">
              {{ (form.mensaje || '').length }}/500
            </p>
          </div>

          <div class="rounded-2xl border border-coral/30 bg-coral/5 p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Resumen</h2>

            <dl class="mt-4 space-y-3 text-sm">
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Entrega máxima</dt>
                <dd class="text-right font-medium text-cafe">{{ entregaTexto }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Vencimiento</dt>
                <dd class="text-right font-medium text-cafe">
                  {{ form.fechaVencimiento || 'Según la configuración del sistema' }}
                </dd>
              </div>
              <div class="flex items-start justify-between gap-4 border-t border-coral/15 pt-3">
                <dt class="text-cafe/55">Precio base</dt>
                <dd class="text-right font-medium text-cafe">{{ formatear(totalBase) }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Recargos</dt>
                <dd class="text-right font-medium text-cafe">{{ formatear(totalRecargos) }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4 border-t border-coral/15 pt-3">
                <dt class="text-base font-semibold text-granate">Total</dt>
                <dd class="text-right text-2xl font-bold text-granate">{{ formatear(totalFinal) }}</dd>
              </div>
            </dl>
          </div>

          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h3 class="text-sm font-semibold text-granate">Detalle por dibujo</h3>
            <ul class="mt-3 space-y-2">
              <li
                v-for="item in items"
                :key="item.idItemSolicitud"
                class="flex items-center justify-between gap-3 rounded-xl bg-rosa/10 px-3 py-2 text-sm"
              >
                <span class="min-w-0 truncate text-cafe">{{ item.nombre }}</span>
                <span class="shrink-0 font-semibold text-granate">
                  {{ formatear(subtotalItem(item)) }}
                </span>
              </li>
            </ul>
          </div>
        </section>

        <p
          v-if="errorEnvio"
          class="rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm leading-relaxed text-rojizo"
          role="alert"
        >
          {{ errorEnvio }}
        </p>

        <div class="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-between">
          <button
            v-if="paso > 1"
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
            @click="anterior"
          >
            Anterior
          </button>
          <span v-else></span>

          <button
            v-if="paso < 3"
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
            @click="siguiente"
          >
            Siguiente
          </button>
          <button
            v-else
            type="submit"
            :disabled="enviando"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
          >
            {{ enviando ? 'Enviando...' : 'Enviar presupuesto' }}
          </button>
        </div>
      </form>
    </template>
  </div>
</template>
