<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { useSesion } from '../composables/useSesion'
import SubirEntregable from '../components/SubirEntregable.vue'
import CargandoPagina from '../components/CargandoPagina.vue'

const router = useRouter()
const { limpiarSesionLocal } = useSesion()

const cargando = ref(true)
const error = ref('')
const sancion = ref(null)
const items = ref([])
const apelaciones = ref([])
const contacto = ref('')
const mensaje = ref('')
const enviando = ref(false)
const aviso = ref('')
const errorApelacion = ref('')

const tienePendiente = computed(
  () => apelaciones.value.some((a) => a.estado === 'PENDIENTE'),
)
const puedeApelar = computed(() => Boolean(sancion.value) && !tienePendiente.value)

function fecha(valor) {
  if (!valor) return ''
  return new Date(valor).toLocaleString('es-AR')
}

function salirDeRestriccion() {
  localStorage.setItem('restringido', '0')
  localStorage.setItem('alcance', '')
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [sancionRes, listaRes, apelacionesRes, contactoRes] = await Promise.all([
      api.get('/apelaciones/sancion-activa'),
      api.get('/pedidos/restringido'),
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
    apelaciones.value = apelacionesRes.data || []

    const resumenes = listaRes.data || []
    const detalles = await Promise.all(
      resumenes.map((p) => api.get(`/pedidos/${p.idPedido}`)),
    )
    items.value = detalles.flatMap((r) => {
      const pedido = r.data
      return (pedido.hitos || [])
        .filter((hito) => hito.puedeEntregar)
        .map((hito) => ({ pedido, hito }))
    })
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar tu panel de entregas.'
  } finally {
    cargando.value = false
  }
}

async function alEntregar() {
  aviso.value = 'Entrega subida. El cliente tiene que aprobarla.'
  await cargar()
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

onMounted(cargar)
</script>

<template>
  <div class="min-h-screen bg-crema px-4 py-10">
    <div class="mx-auto max-w-3xl space-y-6">
      <header class="rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/30">
        <p class="text-xs font-semibold uppercase tracking-wide text-coral">
          Cuenta restringida
        </p>
        <h1 class="mt-1 text-2xl font-bold text-granate">
          Solo podés entregar los pedidos pendientes
        </h1>
        <p class="mt-2 text-sm leading-relaxed text-cafe/70">
          Tu cuenta quedó limitada porque no entregaste a tiempo. Podés subir los
          entregables que debés; el resto de la plataforma queda bloqueado hasta
          que un administrador revise tu caso.
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

      <CargandoPagina v-if="cargando" tipo="lista" mensaje="Cargando entregas pendientes…" :tarjetas="2" />
      <p v-else-if="error" class="rounded-2xl bg-blanco p-6 text-center text-rojizo">
        {{ error }}
      </p>

      <template v-else>
        <section class="space-y-4">
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
