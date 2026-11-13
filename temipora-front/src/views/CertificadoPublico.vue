<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { cargarCertificado } from '../servicios/certificados'
import { etiquetaEvento, hashCorto, infoAnclaje } from '../servicios/ledger'
import { formatearFechaHora } from '../servicios/fechas'

const props = defineProps({
  codigo: { type: String, required: true },
})

const certificado = ref(null)
const cargando = ref(true)
const error = ref('')

const anclaje = computed(() => infoAnclaje(
  Boolean(certificado.value?.estadoAnclaje),
  certificado.value?.bitcoinAltura,
))

const urlVerificacion = computed(() => {
  // La URL actual es siempre la correcta (funciona en localhost, devtunnel, etc.).
  if (typeof window !== 'undefined' && window.location?.href) {
    return window.location.href.replace(/([^:])\/{2,}/g, '$1/')
  }
  return (certificado.value?.urlVerificacion || '').replace(/([^:])\/{2,}/g, '$1/')
})

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    certificado.value = await cargarCertificado(props.codigo)
  } catch (e) {
    certificado.value = null
    error.value = e.response?.status === 404
      ? 'No encontramos un certificado con ese código.'
      : (e.response?.data?.message || 'No se pudo cargar el certificado.')
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
watch(() => props.codigo, cargar)
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-4 py-10 sm:px-8">
    <p class="text-center text-xs font-semibold uppercase tracking-[0.18em] text-cafe/45">
      Certificado de autenticidad
    </p>

    <p v-if="cargando" class="py-20 text-center text-cafe/55">Cargando certificado…</p>
    <p v-else-if="error" role="alert" class="py-20 text-center text-rojizo">{{ error }}</p>

    <article
      v-else-if="certificado"
      class="mt-6 overflow-hidden rounded-[24px] border border-rosa/50 bg-superficie shadow-sm"
    >
      <header class="bg-gradient-to-br from-granate to-coral px-6 py-6 text-white">
        <p class="text-xs uppercase tracking-[0.16em] text-white/70">Pieza única</p>
        <h1 class="mt-1 font-display text-2xl font-extrabold leading-tight">
          {{ certificado.titulo || 'Obra sin título' }}
        </h1>
        <p v-if="certificado.nombreVariante" class="mt-1 text-sm text-white/85">
          {{ certificado.nombreVariante }}
        </p>
      </header>

      <div class="grid gap-6 p-6 sm:grid-cols-[minmax(0,1fr)_auto]">
        <div class="min-w-0 space-y-5">
          <div class="flex flex-wrap items-center gap-2">
            <span
              class="rounded-full px-2.5 py-0.5 text-[11px] font-bold"
              :class="certificado.integridad
                ? 'bg-green-500/10 text-green-700'
                : 'bg-rojizo/10 text-rojizo'"
            >
              {{ certificado.integridad ? 'Historial verificado' : 'Historial con diferencias' }}
            </span>
            <span
              v-if="anclaje"
              class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-bold"
              :class="anclaje.clase"
            >
              <span class="h-1.5 w-1.5 rounded-full" :class="anclaje.punto" aria-hidden="true"></span>
              {{ anclaje.texto }}<template v-if="anclaje.bitcoinAltura"> · bloque {{ anclaje.bitcoinAltura }}</template>
            </span>
            <span
              v-else
              class="rounded-full bg-cafe/10 px-2.5 py-0.5 text-[11px] font-semibold text-cafe/60"
            >
              Sello en Bitcoin pendiente
            </span>
          </div>

          <dl class="grid gap-3 sm:grid-cols-2">
            <div class="rounded-2xl border border-borde/60 bg-crema/40 p-3">
              <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/50">Creador original</dt>
              <dd class="mt-1 text-sm font-semibold text-granate">
                <router-link
                  v-if="certificado.nombreArtista"
                  :to="{ name: 'perfil', params: { nombreUsuario: certificado.nombreArtista } }"
                  class="hover:underline"
                >
                  @{{ certificado.nombreArtista }}
                </router-link>
                <span v-else>—</span>
              </dd>
            </div>
            <div class="rounded-2xl border border-borde/60 bg-crema/40 p-3">
              <dt class="text-[11px] font-semibold uppercase tracking-wide text-cafe/50">Propietario actual</dt>
              <dd class="mt-1 text-sm font-semibold text-granate">
                <template v-if="certificado.nombrePropietarioActual">@{{ certificado.nombrePropietarioActual }}</template>
                <span v-else class="text-cafe/50">Privado</span>
              </dd>
            </div>
          </dl>

          <div>
            <h2 class="text-sm font-bold text-granate">Trayectoria de propiedad</h2>
            <ol class="mt-2 space-y-2">
              <li
                v-for="evento in certificado.trayectoria"
                :key="evento.secuencia"
                class="flex items-center justify-between gap-3 rounded-xl border border-rosa/40 bg-blanco px-3 py-2"
              >
                <span class="flex items-center gap-2 text-sm text-cafe">
                  <span class="rounded-full bg-coral/10 px-2 py-0.5 text-[10px] font-bold text-coral">
                    #{{ evento.secuencia }}
                  </span>
                  {{ etiquetaEvento(evento.tipo) }}
                </span>
                <span class="text-[11px] text-cafe/55">{{ formatearFechaHora(evento.ocurridoEn) }}</span>
              </li>
              <li v-if="!certificado.trayectoria?.length" class="text-xs text-cafe/55">
                Sin movimientos registrados.
              </li>
            </ol>
          </div>

          <div class="rounded-2xl border border-borde/60 bg-crema/30 p-3 text-[11px] leading-relaxed text-cafe/60">
            <p>
              <strong class="text-cafe/75">Raíz sellada (Merkle):</strong>
              <span class="font-mono break-all">{{ hashCorto(certificado.hashRaiz) }}</span>
            </p>
            <p v-if="certificado.hashBitcoin" class="mt-1">
              <strong class="text-cafe/75">Raíz del bloque de Bitcoin:</strong>
              <span class="font-mono break-all">{{ hashCorto(certificado.hashBitcoin) }}</span>
            </p>
          </div>
        </div>

        <aside class="flex flex-col items-center gap-3 sm:w-44">
          <img
            v-if="certificado.imagenUrl"
            :src="certificado.imagenUrl"
            :alt="certificado.titulo"
            class="h-40 w-40 rounded-2xl border border-rosa/50 object-cover"
          />
          <div
            v-if="urlVerificacion"
            class="rounded-2xl border border-rosa/50 bg-white p-3 shadow-sm"
          >
            <QrcodeVue
              :value="urlVerificacion"
              :size="150"
              level="M"
              background="#ffffff"
              foreground="#000000"
            />
          </div>
          <p class="text-center text-[10px] leading-snug text-cafe/50">
            Escaneá para verificar este certificado.
          </p>
          <router-link
            v-if="certificado.idProducto"
            :to="{ name: 'producto-publico', params: { id: certificado.idProducto } }"
            class="text-[11px] font-semibold text-coral hover:underline"
          >
            Ver la publicación →
          </router-link>
        </aside>
      </div>

      <footer class="border-t border-rosa/40 px-6 py-4 text-center text-[10px] leading-relaxed text-cafe/45">
        Este certificado acredita la existencia e integridad del historial de la
        pieza, sellado en la blockchain de Bitcoin mediante OpenTimestamps. No
        reemplaza los derechos de autor ni constituye asesoramiento legal.
      </footer>
    </article>
  </div>
</template>
