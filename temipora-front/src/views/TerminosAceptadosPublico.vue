<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import CargandoPagina from '../components/CargandoPagina.vue'
import { cargarTerminosAceptados } from '../servicios/terminosAceptados'
import { formatearFechaHora } from '../servicios/fechas'

const props = defineProps({
  codigo: { type: String, required: true },
})

const datos = ref(null)
const cargando = ref(true)
const error = ref('')

const contenido = computed(() => datos.value?.contenido || null)

const alcance = computed(() => {
  if (!datos.value) return '—'
  return datos.value.delServicio
    ? `Del servicio «${datos.value.servicio || ''}»`
    : 'Generales del artista'
})

const bloques = computed(() => {
  const c = contenido.value
  if (!c) return []
  return [
    { titulo: 'Información general', texto: c.informacionGeneral },
    { titulo: 'Qué dibuja', texto: c.queDibuja },
    { titulo: 'Qué no dibuja', texto: c.queNoDibuja },
    { titulo: 'Uso del artista', texto: c.usoArtista },
    { titulo: 'Uso del cliente', texto: c.usoCliente },
    { titulo: 'Política de revisiones', texto: c.politicaRevisiones },
    { titulo: 'Política de cancelaciones', texto: c.politicaCancelaciones },
    { titulo: 'Pago extra', texto: c.pagoExtra },
  ].filter((bloque) => bloque.texto)
})

const derechos = computed(() => (contenido.value?.derechos || [])
  .filter((derecho) => derecho.disponible))

const derechosNoIncluidos = computed(() => (contenido.value?.derechos || [])
  .filter((derecho) => !derecho.disponible))

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    datos.value = await cargarTerminosAceptados(props.codigo)
  } catch (e) {
    datos.value = null
    error.value = e?.response?.data?.message
      || 'No encontramos una aceptación de términos con ese código.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
watch(() => props.codigo, cargar)
</script>

<template>
  <div class="mx-auto max-w-3xl px-4 py-10 sm:py-14">
    <CargandoPagina v-if="cargando" tipo="detalle" mensaje="Cargando términos…" />
    <p v-else-if="error" role="alert" class="py-20 text-center text-rojizo">
      {{ error }}
    </p>

    <article
      v-else-if="datos"
      class="rounded-2xl border border-borde/70 bg-superficie p-6 shadow-sm sm:p-10"
    >
      <header>
        <div class="font-display text-base font-bold text-granate">Temipora</div>
        <div class="mt-1 text-xs text-cafe/60">
          Términos aceptados
          <span v-if="datos.codigo">· {{ datos.codigo }}</span>
        </div>
      </header>

      <h1 class="mt-7 font-display text-3xl font-extrabold leading-tight tracking-tight text-granate sm:text-4xl">
        Condiciones aceptadas
      </h1>
      <p class="mt-1 font-display text-base italic text-cafe/60">
        <template v-if="datos.idPedido">Pedido #{{ datos.idPedido }} · </template>
        {{ formatearFechaHora(datos.aceptadosEn) }}
      </p>

      <dl class="mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2">
        <div>
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Cliente</dt>
          <dd class="mt-1 text-sm text-cafe">{{ datos.cliente ? `@${datos.cliente}` : '—' }}</dd>
        </div>
        <div>
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Artista</dt>
          <dd class="mt-1 text-sm text-cafe">{{ datos.artista ? `@${datos.artista}` : '—' }}</dd>
        </div>
        <div>
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Servicio</dt>
          <dd class="mt-1 text-sm text-cafe">{{ datos.servicio || '—' }}</dd>
        </div>
        <div>
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Alcance</dt>
          <dd class="mt-1 text-sm text-cafe">{{ alcance }}</dd>
        </div>
        <div>
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Versión de los términos</dt>
          <dd class="mt-1 text-sm text-cafe">v{{ datos.version ?? '—' }}</dd>
        </div>
        <div>
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Aceptados</dt>
          <dd class="mt-1 text-sm text-cafe">{{ formatearFechaHora(datos.aceptadosEn) }}</dd>
        </div>
        <div class="sm:col-span-2">
          <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Huella del texto aceptado (SHA-256)</dt>
          <dd class="mt-1 break-all font-mono text-xs leading-relaxed text-cafe/70">{{ datos.huella }}</dd>
        </div>
      </dl>

      <section v-if="contenido" class="mt-10">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-cafe/55">Texto aceptado</h2>
        <div class="mt-4 space-y-5">
          <div v-for="bloque in bloques" :key="bloque.titulo">
            <h3 class="text-sm font-semibold text-granate">{{ bloque.titulo }}</h3>
            <p class="mt-1 whitespace-pre-line text-sm leading-relaxed text-cafe">{{ bloque.texto }}</p>
          </div>
        </div>
      </section>

      <section v-if="derechos.length" class="mt-10">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-cafe/55">Derechos incluidos</h2>
        <ul class="mt-4 space-y-3">
          <li
            v-for="derecho in derechos"
            :key="derecho.codigo || derecho.nombre"
            class="rounded-xl border border-borde/70 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <span class="text-sm font-semibold text-cafe">{{ derecho.nombre }}</span>
              <span
                v-if="derecho.porcentajeExtra"
                class="shrink-0 text-xs font-semibold text-granate"
              >
                +{{ derecho.porcentajeExtra }}%
              </span>
            </div>
            <p v-if="derecho.descripcion" class="mt-1 text-xs leading-relaxed text-cafe/70">
              {{ derecho.descripcion }}
            </p>
            <p v-if="derecho.requierePiezaUnica" class="mt-1 text-[11px] text-cafe/50">
              Aplica solo a piezas únicas.
            </p>
          </li>
        </ul>
        <p v-if="derechosNoIncluidos.length" class="mt-3 text-xs text-cafe/55">
          No incluidos: {{ derechosNoIncluidos.map((derecho) => derecho.nombre).join(' · ') }}
        </p>
      </section>

      <section class="mt-10 border-t border-borde/70 pt-6">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-cafe/55">Términos de la plataforma</h2>
        <dl class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Versión</dt>
            <dd class="mt-1 text-sm text-cafe">v{{ datos.plataformaVersion ?? '—' }}</dd>
          </div>
          <div>
            <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Aceptados</dt>
            <dd class="mt-1 text-sm text-cafe">{{ formatearFechaHora(datos.plataformaAceptadosEn) }}</dd>
          </div>
          <div v-if="datos.plataformaTitulo" class="sm:col-span-2">
            <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Documento</dt>
            <dd class="mt-1 text-sm text-cafe">{{ datos.plataformaTitulo }}</dd>
          </div>
          <div v-if="datos.plataformaHuella" class="sm:col-span-2">
            <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/55">Huella (SHA-256)</dt>
            <dd class="mt-1 break-all font-mono text-xs leading-relaxed text-cafe/70">{{ datos.plataformaHuella }}</dd>
          </div>
        </dl>
      </section>

      <p class="mt-10 text-xs leading-relaxed text-cafe/55">
        Este registro reproduce el texto que el cliente aceptó al enviar la
        solicitud del pedido. La huella SHA-256 identifica ese texto exacto: si
        el artista modifica sus términos después, la huella de este pedido no
        cambia y sirve para acreditar qué condiciones se aceptaron.
      </p>
    </article>
  </div>
</template>
