<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { useSesion } from '../composables/useSesion'
import { useAccesoEnlace } from '../composables/useAccesoEnlace'
import { usePagoHito } from '../composables/usePagoHito'
import { formatearPrecio } from '../servicios/moneda'
import { gruposDeRespuestas } from '../servicios/solicitudes'
import SubirEntregable from '../components/SubirEntregable.vue'
import CargandoPagina from '../components/CargandoPagina.vue'

const router = useRouter()
const { limpiarSesionLocal } = useSesion()
const { consumirSiCorresponde } = useAccesoEnlace()

const cargando = ref(true)
const error = ref('')
const sancion = ref(null)
const alcance = ref(localStorage.getItem('alcance') || '')
const items = ref([])
const deudas = ref([])
const apelaciones = ref([])
const contacto = ref('')
const mensaje = ref('')
const enviando = ref(false)
const aviso = ref('')
const errorApelacion = ref('')
const errorPago = ref('')
const pagandoId = ref(null)

const esPagos = computed(() => alcance.value === 'PAGOS')
const titulo = computed(() => (esPagos.value
  ? 'Solo podés regularizar tus pagos pendientes'
  : 'Solo podés entregar los pedidos pendientes'))
const descripcion = computed(() => (esPagos.value
  ? 'Tu cuenta quedó limitada porque no pagaste la entrega final de un pedido. Podés pagar la deuda, ver tus notificaciones y apelar; el resto de la plataforma queda bloqueado hasta que regularices.'
  : 'Tu cuenta quedó limitada porque no entregaste a tiempo. Podés subir los entregables que debés; el resto de la plataforma queda bloqueado hasta que un administrador revise tu caso.'))

const tienePendiente = computed(
  () => apelaciones.value.some((a) => a.estado === 'PENDIENTE'),
)
const puedeApelar = computed(() => Boolean(sancion.value) && !tienePendiente.value)

const { pagarHito } = usePagoHito({
  onExito: (msg) => { aviso.value = msg },
  onError: (msg) => { errorPago.value = msg },
  onNoAutorizado: () => router.push({ name: 'login' }),
})

function fecha(valor) {
  if (!valor) return ''
  return new Date(valor).toLocaleString('es-AR')
}

function montoDeuda(deuda) {
  return formatearPrecio(
    deuda.montoCobro ?? deuda.monto,
    deuda.monedaCobro || deuda.moneda,
  )
}

function dibujosDe(solicitud) {
  return (solicitud?.items || []).map((item) => ({
    ...item,
    grupos: gruposDeRespuestas(item),
  }))
}

function salirDeRestriccion() {
  localStorage.setItem('restringido', '0')
  localStorage.setItem('alcance', '')
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [sancionRes, apelacionesRes, contactoRes] = await Promise.all([
      api.get('/apelaciones/sancion-activa'),
      api.get('/apelaciones'),
      api.get('/contacto').catch(() => ({ data: {} })),
    ])
    contacto.value = contactoRes.data?.email || ''

    const actual = sancionRes.data?.sancionActual || null
    if (!sancionRes.data?.sancionado || !actual) {
      salirDeRestriccion()
      router.replace({ name: 'explorar' })
      return
    }
    sancion.value = actual
    alcance.value = actual.alcance || alcance.value
    apelaciones.value = apelacionesRes.data || []

    if (esPagos.value) {
      items.value = []
      const deudasRes = await api.get('/pedidos/deudas')
      deudas.value = deudasRes.data || []
      return
    }

    deudas.value = []
    const listaRes = await api.get('/pedidos/restringido')
    const resumenes = listaRes.data || []
    const [detalles, solicitudes] = await Promise.all([
      Promise.all(resumenes.map((p) => api.get(`/pedidos/${p.idPedido}`))),
      Promise.all(resumenes.map((p) => (p.idSolicitud
        ? api.get(`/solicitudes-servicio/${p.idSolicitud}`)
          .then((r) => r.data)
          .catch(() => null)
        : Promise.resolve(null)))),
    ])
    const solicitudPorPedido = new Map(
      resumenes.map((p, indice) => [p.idPedido, solicitudes[indice]]),
    )
    items.value = detalles.flatMap((r) => {
      const pedido = r.data
      const solicitud = solicitudPorPedido.get(pedido.idPedido) || null
      return (pedido.hitos || [])
        .filter((hito) => hito.puedeEntregar)
        .map((hito) => ({ pedido, hito, solicitud }))
    })
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar tu panel de restricción.'
  } finally {
    cargando.value = false
  }
}

async function alEntregar() {
  aviso.value = 'Entrega subida. El cliente tiene que aprobarla.'
  await cargar()
}

async function pagarDeuda(deuda) {
  if (pagandoId.value) return
  pagandoId.value = deuda.idHitoPedido
  errorPago.value = ''
  aviso.value = ''
  try {
    await pagarHito(
      { idHitoPedido: deuda.idHitoPedido, nombre: deuda.nombreHito },
      deuda.idPedido,
    )
    await cargar()
  } finally {
    pagandoId.value = null
  }
}

async function apelar() {
  if (!sancion.value || !mensaje.value.trim()) {
    errorApelacion.value = 'Contá brevemente por qué pedís la revisión.'
    return
  }
  enviando.value = true
  errorApelacion.value = ''
  aviso.value = ''
  try {
    await api.post(`/apelaciones/sancion/${sancion.value.idSancion}`, {
      mensaje: mensaje.value.trim(),
    })
    mensaje.value = ''
    aviso.value = 'Apelación enviada. Un administrador la va a revisar.'
    const res = await api.get('/apelaciones')
    apelaciones.value = res.data || []
  } catch (e) {
    errorApelacion.value = e.response?.data?.message
      || 'No se pudo enviar la apelación.'
  } finally {
    enviando.value = false
  }
}

function cerrarSesion() {
  limpiarSesionLocal()
  router.push({ name: 'login' })
}

onMounted(async () => {
  const acceso = await consumirSiCorresponde()
  if (acceso.habiaEnlace) return
  await cargar()
})
</script>

<template>
  <div class="min-h-screen bg-crema px-4 py-10">
    <div class="mx-auto max-w-3xl space-y-6">
      <header class="rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/30">
        <p class="text-xs font-semibold uppercase tracking-wide text-coral">
          Cuenta restringida
        </p>
        <h1 class="mt-1 text-2xl font-bold text-granate">
          {{ titulo }}
        </h1>
        <p class="mt-2 text-sm leading-relaxed text-cafe/70">
          {{ descripcion }}
        </p>
        <div v-if="sancion" class="mt-4 rounded-xl bg-rosa/10 px-4 py-3 text-sm text-cafe/80">
          <p>{{ sancion.motivo }}</p>
          <p v-if="sancion.fechaFin" class="mt-1 text-xs text-cafe/50">
            Vigente hasta el {{ fecha(sancion.fechaFin) }}.
          </p>
        </div>
        <div class="mt-4 flex flex-wrap gap-3">
          <button
            type="button"
            class="rounded-full border border-rosa px-4 py-2 text-sm font-semibold text-cafe transition hover:bg-rosa/20"
            @click="cerrarSesion"
          >
            Cerrar sesión
          </button>
          <a
            v-if="contacto"
            :href="`mailto:${contacto}`"
            class="rounded-full border border-rosa px-4 py-2 text-sm font-semibold text-cafe transition hover:bg-rosa/20"
          >
            Escribir a soporte
          </a>
        </div>
      </header>

      <CargandoPagina
        v-if="cargando"
        tipo="lista"
        :mensaje="esPagos ? 'Cargando pagos pendientes…' : 'Cargando entregas pendientes…'"
        :tarjetas="2"
      />
      <p v-else-if="error" class="rounded-2xl bg-blanco p-6 text-center text-rojizo">
        {{ error }}
      </p>

      <template v-else>
        <section v-if="esPagos" class="space-y-4">
          <h2 class="text-lg font-bold text-granate">Pagos pendientes</h2>
          <p v-if="!deudas.length" class="rounded-2xl bg-blanco p-6 text-sm text-cafe/60">
            No tenés pagos pendientes. Si ya regularizaste la deuda, la cuenta se
            habilita sola.
          </p>
          <article
            v-for="deuda in deudas"
            :key="`${deuda.idPedido}-${deuda.idHitoPedido}`"
            class="rounded-2xl bg-blanco p-5 shadow-lg shadow-rosa/20"
          >
            <p class="text-sm font-semibold text-granate">
              Pedido #{{ deuda.idPedido }} · {{ deuda.nombreServicio }}
            </p>
            <p class="mt-1 text-xs text-cafe/60">
              Artista: @{{ deuda.nombreUsuarioArtista }}
            </p>
            <p class="mt-2 text-sm text-cafe">
              {{ deuda.nombreHito }} ·
              <span class="font-semibold">{{ montoDeuda(deuda) }}</span>
            </p>
            <p v-if="deuda.fechaLimitePago" class="mt-1 text-xs text-rojizo">
              Venció el {{ fecha(deuda.fechaLimitePago) }}
              <span v-if="deuda.diasAtraso > 0">
                · {{ deuda.diasAtraso }} día(s) de atraso
              </span>
            </p>
            <button
              v-if="deuda.puedePagar"
              type="button"
              :disabled="pagandoId === deuda.idHitoPedido"
              class="mt-3 rounded-full bg-coral px-5 py-2 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
              @click="pagarDeuda(deuda)"
            >
              {{ pagandoId === deuda.idHitoPedido ? 'Redirigiendo…' : 'Pagar' }}
            </button>
          </article>
          <p v-if="errorPago" class="rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-sm text-rojizo" role="alert">
            {{ errorPago }}
          </p>
        </section>

        <section v-else class="space-y-4">
          <h2 class="text-lg font-bold text-granate">Entregas pendientes</h2>
          <p v-if="!items.length" class="rounded-2xl bg-blanco p-6 text-sm text-cafe/60">
            No tenés entregables pendientes. Si tu caso fue revisado, un
            administrador puede habilitarte la cuenta.
          </p>
          <article
            v-for="item in items"
            :key="`${item.pedido.idPedido}-${item.hito.idHitoPedido}`"
            class="rounded-2xl bg-blanco p-5 shadow-lg shadow-rosa/20"
          >
            <p class="text-sm font-semibold text-granate">
              Pedido #{{ item.pedido.idPedido }} · {{ item.pedido.nombreServicio }}
            </p>
            <p class="mt-1 text-xs text-cafe/60">
              Hito: {{ item.hito.nombre }}
              <span v-if="item.pedido.fechaEntregaPrevista">
                · entrega prevista {{ item.pedido.fechaEntregaPrevista }}
              </span>
            </p>

            <details
              v-if="item.solicitud"
              class="mt-3 rounded-xl border border-rosa/40 bg-rosa/5 px-3 py-2"
            >
              <summary class="cursor-pointer text-sm font-semibold text-granate">
                Qué pidió el cliente
              </summary>
              <div class="mt-3 space-y-4">
                <div
                  v-for="(dibujo, indiceDibujo) in dibujosDe(item.solicitud)"
                  :key="dibujo.idItemSolicitud || indiceDibujo"
                >
                  <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">
                    {{ dibujo.nombre }}
                  </p>
                  <div
                    v-for="grupo in dibujo.grupos"
                    :key="grupo.clave"
                    class="mt-2"
                  >
                    <p class="text-sm font-semibold text-cafe">
                      {{ grupo.enunciado }}
                    </p>
                    <div
                      v-if="grupo.codigoTipo === 'SUBIR_IMAGEN'"
                      class="mt-2 flex flex-wrap gap-2"
                    >
                      <a
                        v-for="(url, indiceUrl) in grupo.valores"
                        :key="`${url}-${indiceUrl}`"
                        :href="url"
                        target="_blank"
                        rel="noopener"
                        class="block h-16 w-16 overflow-hidden rounded-lg ring-1 ring-rosa/40"
                      >
                        <img
                          :src="url"
                          alt="Imagen de referencia del pedido"
                          class="h-full w-full object-cover"
                          loading="lazy"
                        />
                      </a>
                    </div>
                    <p
                      v-else-if="grupo.codigoTipo === 'TEXTO'"
                      class="mt-1 whitespace-pre-line text-sm text-cafe/75"
                    >
                      {{ grupo.valores.join('\n') }}
                    </p>
                    <div v-else class="mt-1 flex flex-wrap gap-2">
                      <span
                        v-for="(valor, indiceValor) in grupo.valores"
                        :key="`${valor}-${indiceValor}`"
                        class="rounded-full bg-blanco px-2.5 py-0.5 text-xs text-cafe ring-1 ring-rosa/40"
                      >
                        {{ valor }}
                      </span>
                    </div>
                  </div>
                  <p
                    v-if="!dibujo.grupos.length"
                    class="mt-1 text-xs text-cafe/50"
                  >
                    Este ítem no tiene respuestas registradas.
                  </p>
                </div>
                <p
                  v-if="!item.solicitud.items?.length"
                  class="text-sm text-cafe/60"
                >
                  La solicitud no tiene detalle del formulario.
                </p>
              </div>
            </details>

            <SubirEntregable
              :id-pedido="item.pedido.idPedido"
              :hito="item.hito"
              :dibujos="item.pedido.dibujos || []"
              @entregado="alEntregar"
            />
          </article>
        </section>

        <section class="rounded-2xl bg-blanco p-5 shadow-lg shadow-rosa/20">
          <h2 class="text-lg font-bold text-granate">Apelar la sanción</h2>
          <p class="mt-1 text-sm text-cafe/60">
            Contá qué pasó. Un administrador va a revisar tu caso y puede revertir
            la sanción.
          </p>

          <p
            v-if="aviso"
            class="mt-3 rounded-lg border border-coral/30 bg-coral/10 px-3 py-2 text-sm text-granate"
            role="status"
          >
            {{ aviso }}
          </p>
          <p
            v-if="errorApelacion"
            class="mt-3 rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-sm text-rojizo"
            role="alert"
          >
            {{ errorApelacion }}
          </p>

          <template v-if="puedeApelar">
            <textarea
              v-model="mensaje"
              rows="4"
              maxlength="2000"
              placeholder="Ej. Estuve internado y no pude subir el avance. Adjunto la justificación por soporte."
              class="mt-3 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            ></textarea>
            <button
              type="button"
              :disabled="enviando"
              class="mt-3 rounded-full bg-coral px-5 py-2 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
              @click="apelar"
            >
              {{ enviando ? 'Enviando…' : 'Enviar apelación' }}
            </button>
          </template>
          <p v-else class="mt-3 text-sm text-cafe/60">
            Ya tenés una apelación pendiente. Vas a recibir una notificación con la
            respuesta.
          </p>

          <div v-if="apelaciones.length" class="mt-5 space-y-3">
            <h3 class="text-sm font-semibold text-cafe">Historial</h3>
            <div
              v-for="apelacion in apelaciones"
              :key="apelacion.idApelacion"
              class="rounded-xl border border-rosa/40 px-3 py-2 text-xs text-cafe/70"
            >
              <p class="font-semibold text-cafe">
                {{ apelacion.estado }}
                <span class="font-normal text-cafe/50">· {{ fecha(apelacion.creadaEn) }}</span>
              </p>
              <p class="mt-1 whitespace-pre-line">{{ apelacion.mensaje }}</p>
              <p v-if="apelacion.respuesta" class="mt-2 border-t border-rosa/30 pt-2">
                <span class="font-semibold">Respuesta:</span> {{ apelacion.respuesta }}
              </p>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>
