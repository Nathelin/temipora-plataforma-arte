<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import QrcodeVue from 'qrcode.vue'
import {
  cargarCertificado,
  descargarPruebaCertificado,
  descargarDocumentoCertificado,
} from '../servicios/certificados'
import { etiquetaEvento, hashCorto } from '../servicios/ledger'
import { formatearFechaHora } from '../servicios/fechas'

const props = defineProps({
  codigo: { type: String, required: true },
})

const certificado = ref(null)
const cargando = ref(true)
const error = ref('')
const descargando = ref('')
const errorDescarga = ref('')
const copiado = ref('')
const imprimiendo = ref(false)

const tieneAnclaje = computed(() => Boolean(certificado.value?.estadoAnclaje))
const confirmado = computed(() => Boolean(certificado.value?.bitcoinAltura))

const urlVerificacion = computed(() => {
  // La URL actual es siempre la correcta (funciona en localhost, devtunnel, etc.).
  if (typeof window !== 'undefined' && window.location?.href) {
    return window.location.href.replace(/([^:])\/{2,}/g, '$1/')
  }
  return (certificado.value?.urlVerificacion || '').replace(/([^:])\/{2,}/g, '$1/')
})

const selloTexto = computed(() => {
  if (confirmado.value) {
    return `SELLADO EN BITCOIN · BLOQUE ${certificado.value.bitcoinAltura} ·`
  }
  if (tieneAnclaje.value) {
    return 'SELLO EN BITCOIN · PENDIENTE ·'
  }
  return 'SIN SELLO EN BITCOIN ·'
})

const ariaSello = computed(() => (confirmado.value
  ? `Sellado en Bitcoin, bloque ${certificado.value.bitcoinAltura}`
  : 'Sello en Bitcoin pendiente de confirmación'))

function fechaCorta(valor) {
  if (!valor) return ''
  try {
    const d = new Date(valor)
    const dd = String(d.getDate()).padStart(2, '0')
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    return `${dd}·${mm}·${d.getFullYear()}`
  } catch {
    return ''
  }
}

const selloFecha = computed(() => fechaCorta(certificado.value?.fechaAnclaje))

const fechaRegistro = computed(() => fechaCorta(
  certificado.value?.trayectoria?.[0]?.ocurridoEn,
))

function descripcionEvento(evento, index) {
  if (index === 0) {
    return certificado.value?.nombreArtista
      ? `Creada por @${certificado.value.nombreArtista}`
      : 'Origen del registro'
  }
  return 'Cambio de propiedad registrado'
}

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

function imprimir() {
  // Abre el bloque de verificación momentáneamente para que también se imprima.
  imprimiendo.value = true
  window.setTimeout(() => {
    window.print()
    imprimiendo.value = false
  }, 80)
}

async function copiar(texto, clave) {
  if (!texto) return
  try {
    await navigator.clipboard.writeText(texto)
  } catch (e) {
    /* clipboard no disponible: ignoramos */
  }
  copiado.value = clave
  window.setTimeout(() => {
    if (copiado.value === clave) copiado.value = ''
  }, 1500)
}

function guardarBlob(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

async function descargar(tipo) {
  if (!certificado.value?.codigoCertificado) return
  const codigo = certificado.value.codigoCertificado
  descargando.value = tipo
  errorDescarga.value = ''
  try {
    if (tipo === 'ots') {
      guardarBlob(await descargarPruebaCertificado(codigo), `certificado-${codigo}.ots`)
    } else {
      guardarBlob(await descargarDocumentoCertificado(codigo), `certificado-${codigo}.txt`)
    }
  } catch (e) {
    errorDescarga.value = 'No se pudo descargar el archivo. Probá de nuevo.'
  } finally {
    descargando.value = ''
  }
}

onMounted(() => {
  document.body.classList.add('imprimiendo-certificado')
  cargar()
})
onBeforeUnmount(() => {
  document.body.classList.remove('imprimiendo-certificado')
})
watch(() => props.codigo, cargar)
</script>

<template>
  <div class="certificado-print mx-auto w-full max-w-3xl px-4 py-10 sm:px-8">
    <div class="no-print mb-6 flex justify-end">
      <button
        type="button"
        class="inline-flex min-h-10 items-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 motion-reduce:transition-none"
        @click="imprimir"
      >
        Imprimir / Guardar PDF
      </button>
    </div>

    <CargandoPagina v-if="cargando" tipo="detalle" mensaje="Cargando certificado…" />
    <p v-else-if="error" role="alert" class="py-20 text-center text-rojizo">{{ error }}</p>

    <article
      v-else-if="certificado"
      class="rounded-2xl border border-borde/70 bg-superficie p-6 shadow-sm sm:p-10"
    >
      <!-- Encabezado -->
      <header>
        <div class="min-w-0">
          <div class="font-display text-base font-bold text-granate">Temipora</div>
          <div class="mt-1 text-xs text-cafe/60">
            Certificado de procedencia
            <span v-if="certificado.codigoCertificado">· N.º {{ certificado.codigoCertificado }}</span>
          </div>
        </div>
      </header>

      <!-- Título + sello (a la altura del nombre del producto) -->
      <div class="mt-7 flex items-start justify-between gap-6">
        <div class="min-w-0">
          <h1 class="font-display text-3xl font-extrabold leading-tight tracking-tight text-granate sm:text-4xl">
            {{ certificado.titulo || 'Obra sin título' }}
          </h1>
          <p class="mt-1 font-display text-base italic text-cafe/60">
            Pieza única<span v-if="certificado.nombreVariante"> · {{ certificado.nombreVariante }}</span>
          </p>
        </div>

        <svg
          class="h-24 w-24 shrink-0 text-coral sm:h-28 sm:w-28"
          viewBox="0 0 120 120"
          role="img"
          :aria-label="ariaSello"
        >
          <defs>
            <path id="sello-circ" d="M60,60 m-47,0 a47,47 0 1,1 94,0 a47,47 0 1,1 -94,0" />
          </defs>
          <circle cx="60" cy="60" r="57" fill="none" stroke="currentColor" stroke-width="1.5" />
          <circle cx="60" cy="60" r="33" fill="none" stroke="currentColor" stroke-width="1" />
          <text class="font-sans" font-size="9.5" font-weight="600" letter-spacing="2.2" fill="currentColor">
            <textPath href="#sello-circ">{{ selloTexto }}</textPath>
          </text>
          <path
            v-if="confirmado"
            d="M44 61l11 11 21-24"
            fill="none"
            stroke="currentColor"
            stroke-width="4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <circle v-else cx="60" cy="60" r="6" fill="currentColor" opacity="0.45" />
          <text x="60" y="88" text-anchor="middle" class="font-mono" font-size="7" fill="currentColor">
            {{ selloFecha }}
          </text>
        </svg>
      </div>

      <!-- Obra + datos -->
      <section class="mt-8 grid gap-8 sm:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
        <figure class="m-0">
          <img
            v-if="certificado.imagenUrl"
            :src="certificado.imagenUrl"
            :alt="certificado.titulo || 'Obra'"
            class="block aspect-[4/5] w-full rounded-md border border-borde/60 object-cover"
          />
          <div
            v-else
            class="grid aspect-[4/5] w-full place-items-center rounded-md border border-borde/60 bg-crema/40 text-sm text-cafe/45"
          >
            Sin imagen
          </div>
          <figcaption class="mt-2.5 text-xs text-cafe/50">
            Imagen de la pieza registrada en Temipora.
          </figcaption>
        </figure>

        <dl class="m-0">
          <div class="border-b border-rosa/40 py-3 first:pt-0">
            <dt class="text-xs text-cafe/55">Artista</dt>
            <dd class="mt-0.5 font-display text-lg text-granate">
              <router-link
                v-if="certificado.nombreArtista"
                :to="{ name: 'perfil', params: { nombreUsuario: certificado.nombreArtista } }"
                class="hover:underline"
              >
                @{{ certificado.nombreArtista }}
              </router-link>
              <span v-else class="text-cafe/50">—</span>
            </dd>
          </div>
          <div class="border-b border-rosa/40 py-3">
            <dt class="text-xs text-cafe/55">Propietario actual</dt>
            <dd class="mt-0.5 font-display text-lg text-granate">
              <template v-if="certificado.nombrePropietarioActual">@{{ certificado.nombrePropietarioActual }}</template>
              <span v-else class="text-cafe/50">Privado</span>
            </dd>
          </div>
          <div class="border-b border-rosa/40 py-3">
            <dt class="text-xs text-cafe/55">Tipo</dt>
            <dd class="mt-0.5 text-base font-semibold text-cafe">Pieza única</dd>
          </div>
          <div class="border-b border-rosa/40 py-3">
            <dt class="text-xs text-cafe/55">Código del certificado</dt>
            <dd class="mt-0.5 font-mono text-sm text-cafe">{{ certificado.codigoCertificado || '—' }}</dd>
          </div>
          <div class="py-3">
            <dt class="text-xs text-cafe/55">Integridad</dt>
            <dd class="mt-1">
              <span
                class="rounded-full px-2.5 py-0.5 text-[11px] font-bold"
                :class="certificado.integridad
                  ? 'bg-green-500/10 text-green-700'
                  : 'bg-rojizo/10 text-rojizo'"
              >
                {{ certificado.integridad ? 'Verificado' : 'Con diferencias' }}
              </span>
              <span v-if="fechaRegistro" class="ml-2 text-xs text-cafe/50">Registrado el {{ fechaRegistro }}</span>
            </dd>
          </div>
        </dl>
      </section>

      <hr class="my-8 border-rosa/40" />

      <!-- Historial -->
      <section>
        <h2 class="font-display text-xl font-bold text-granate">Historial de propiedad</h2>
        <p class="mt-1 max-w-prose text-sm leading-relaxed text-cafe/60">
          Cada cambio de manos quedó registrado en orden y no se puede modificar
          sin que el sello lo delate.
        </p>

        <ol class="mt-5 space-y-4">
          <li
            v-for="(evento, i) in certificado.trayectoria"
            :key="evento.secuencia"
            class="flex gap-3"
          >
            <div class="flex flex-col items-center">
              <span
                class="mt-1.5 h-3 w-3 shrink-0 rounded-full border-2"
                :class="i === certificado.trayectoria.length - 1
                  ? 'border-coral bg-coral'
                  : 'border-cafe/40 bg-superficie'"
              ></span>
              <span
                v-if="i < certificado.trayectoria.length - 1"
                class="mt-1 w-px flex-1 bg-rosa/50"
              ></span>
            </div>
            <div class="min-w-0 flex-1 pb-1">
              <div class="flex flex-wrap items-baseline justify-between gap-2">
                <span class="text-sm font-semibold text-cafe">{{ etiquetaEvento(evento.tipo) }}</span>
                <time class="font-mono text-[11px] text-cafe/55">{{ formatearFechaHora(evento.ocurridoEn) }}</time>
              </div>
              <p class="mt-0.5 text-xs text-cafe/55">{{ descripcionEvento(evento, i) }}</p>
              <p v-if="!evento.verificado" class="mt-0.5 text-[11px] font-semibold text-rojizo">
                Este registro presenta diferencias.
              </p>
            </div>
          </li>
          <li v-if="!certificado.trayectoria?.length" class="text-xs text-cafe/55">
            Sin movimientos registrados.
          </li>
        </ol>
      </section>

      <hr class="my-8 border-rosa/40" />

      <!-- Prueba -->
      <section>
        <div
          class="flex items-center gap-4 rounded-xl border p-5"
          :class="confirmado ? 'border-coral/50 bg-coral/5' : 'border-borde bg-crema/30'"
        >
          <div
            class="grid h-10 w-10 shrink-0 place-items-center rounded-full text-white"
            :class="confirmado ? 'bg-coral' : 'bg-cafe/40'"
            aria-hidden="true"
          >
            <span v-if="confirmado" class="text-lg font-bold">✓</span>
            <span v-else class="text-lg font-bold">…</span>
          </div>
          <div class="min-w-0">
            <strong class="block font-display text-base text-granate">
              <template v-if="confirmado">Confirmado en la blockchain de Bitcoin</template>
              <template v-else-if="tieneAnclaje">Sello creado, esperando confirmación de Bitcoin</template>
              <template v-else>Todavía sin sello en Bitcoin</template>
            </strong>
            <span class="text-sm leading-relaxed text-cafe/60">
              <template v-if="confirmado">
                Bloque n.º {{ certificado.bitcoinAltura }}<template v-if="certificado.fechaAnclaje"> · {{ formatearFechaHora(certificado.fechaAnclaje) }}</template>. Este historial ya existía en esa fecha.
              </template>
              <template v-else-if="tieneAnclaje">
                Suele tardar entre unos minutos y algunas horas; se actualiza solo.
              </template>
              <template v-else>
                El sello se generará automáticamente.
              </template>
            </span>
          </div>
        </div>

        <div v-if="certificado.hashRaiz" class="mt-5">
          <div class="grid grid-cols-1 items-center gap-2 border-b border-rosa/40 py-3 sm:grid-cols-[150px_1fr_auto] sm:gap-3">
            <span class="text-xs text-cafe/55">Huella del registro</span>
            <code class="break-all font-mono text-xs text-cafe" :title="certificado.hashRaiz">
              {{ hashCorto(certificado.hashRaiz) }}
            </code>
            <button
              type="button"
              class="no-print justify-self-start rounded-full border border-rosa bg-blanco px-2.5 py-1 text-[11px] font-semibold text-granate transition hover:bg-rosa/20 sm:justify-self-end"
              @click="copiar(certificado.hashRaiz, 'raiz')"
            >
              {{ copiado === 'raiz' ? 'Copiado' : 'Copiar' }}
            </button>
          </div>
          <div
            v-if="certificado.hashBitcoin"
            class="grid grid-cols-1 items-center gap-2 border-b border-rosa/40 py-3 sm:grid-cols-[150px_1fr_auto] sm:gap-3"
          >
            <span class="text-xs text-cafe/55">Raíz del bloque</span>
            <code class="break-all font-mono text-xs text-cafe" :title="certificado.hashBitcoin">
              {{ hashCorto(certificado.hashBitcoin) }}
            </code>
            <button
              type="button"
              class="no-print justify-self-start rounded-full border border-rosa bg-blanco px-2.5 py-1 text-[11px] font-semibold text-granate transition hover:bg-rosa/20 sm:justify-self-end"
              @click="copiar(certificado.hashBitcoin, 'bloque')"
            >
              {{ copiado === 'bloque' ? 'Copiado' : 'Copiar' }}
            </button>
          </div>
        </div>

        <details
          v-if="tieneAnclaje"
          :open="imprimiendo ? true : undefined"
          class="mt-6 border-y border-rosa/40"
        >
          <summary class="flex cursor-pointer list-none items-center justify-between py-4 font-display text-base font-bold text-granate">
            Cómo verificarlo por tu cuenta
            <span class="text-lg text-coral" aria-hidden="true">+</span>
          </summary>
          <div class="pb-5">
            <p class="rounded-md bg-green-500/10 px-4 py-3 text-xs leading-relaxed text-green-700">
              <strong>Sin criptomonedas.</strong> No hace falta billetera ni pagar
              nada: no minamos Bitcoin ni usamos criptomonedas. Solo se publica una
              huella digital (hash) del registro.
            </p>
            <ol class="mt-4 list-decimal space-y-1 pl-5 text-xs leading-relaxed text-cafe/65">
              <li>Descargá la <b>prueba (.ots)</b> y el <b>documento (.txt)</b>.</li>
              <li>Entrá a <b>opentimestamps.org</b> y abrí la sección <b>“Verify”</b>.</li>
              <li>Arrastrá los dos archivos.</li>
              <li>El sitio muestra la fecha y el bloque donde se selló el certificado.</li>
            </ol>
            <div class="no-print mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                class="rounded-full border border-rosa bg-blanco px-3 py-1.5 text-[11px] font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
                :disabled="descargando === 'ots'"
                @click="descargar('ots')"
              >
                {{ descargando === 'ots' ? 'Descargando…' : 'Descargar prueba (.ots)' }}
              </button>
              <button
                type="button"
                class="rounded-full border border-rosa bg-blanco px-3 py-1.5 text-[11px] font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-50"
                :disabled="descargando === 'doc'"
                @click="descargar('doc')"
              >
                {{ descargando === 'doc' ? 'Descargando…' : 'Descargar documento (.txt)' }}
              </button>
              <a
                href="https://opentimestamps.org/"
                target="_blank"
                rel="noopener"
                class="rounded-full border border-coral/40 bg-coral/10 px-3 py-1.5 text-[11px] font-semibold text-coral transition hover:bg-coral/20"
              >
                Abrir OpenTimestamps ↗
              </a>
            </div>
            <p v-if="errorDescarga" role="alert" class="mt-3 text-[11px] text-rojizo">
              {{ errorDescarga }}
            </p>
          </div>
        </details>

        <p v-else class="mt-6 text-xs leading-relaxed text-cafe/55">
          El sello en Bitcoin de esta pieza todavía se está preparando. Volvé a
          entrar más tarde para poder verificarla por tu cuenta.
        </p>
      </section>

      <!-- Pie -->
      <footer class="mt-8 flex items-center gap-5 border-t border-rosa/40 pt-6">
        <div v-if="urlVerificacion" class="shrink-0 rounded-md border border-rosa/40 bg-white p-2">
          <QrcodeVue
            :value="urlVerificacion"
            :size="104"
            level="M"
            background="#ffffff"
            foreground="#000000"
          />
        </div>
        <p class="min-w-0 flex-1 text-xs leading-relaxed text-cafe/55">
          <b class="font-semibold text-cafe">Escaneá para ver este certificado en línea.</b>
          Si cambia el propietario, el certificado se actualiza y el historial
          anterior queda registrado. No reemplaza los derechos de autor.
        </p>
      </footer>
    </article>
  </div>
</template>

<style>
@media print {
  @page {
    margin: 12mm;
  }

  /* Solo se imprime el certificado: el resto de la app queda oculto. */
  body.imprimiendo-certificado * {
    visibility: hidden !important;
  }

  body.imprimiendo-certificado .certificado-print,
  body.imprimiendo-certificado .certificado-print * {
    visibility: visible !important;
  }

  body.imprimiendo-certificado .certificado-print {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    max-width: none;
    padding: 0 !important;
    margin: 0 !important;
    background: #fff !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  /* Fuerza la paleta clara dentro del certificado al imprimir, para que en
     modo oscuro el texto no salga claro sobre fondo blanco. */
  body.imprimiendo-certificado .certificado-print {
    --color-fondo: #fff6f1;
    --color-superficie: #ffffff;
    --color-superficie-alt: #fff0e9;
    --color-texto: #4a1d2e;
    --color-texto-suave: #6e3b4d;
    --color-borde: #ffc2d1;
    --color-acento: #c2185b;
    --color-acento-suave: #c2185b;
    --color-acento-hover: #9a1047;
    --color-acento-contraste: #ffffff;
    --color-inverso: #4a1d2e;
    --color-inverso-suave: #ffc2d1;
    --color-inverso-contraste: #fff6f1;
    --color-tinta: #4a1d2e;

    --color-crema: #fff6f1;
    --color-rosa: #ffc2d1;
    --color-durazno: #ffd9c7;
    --color-coral: #c2185b;
    --color-coral-vivo: #ff8a7a;
    --color-rojizo: #9a1047;
    --color-granate: #4a1d2e;
    --color-cafe: #6e3b4d;
    --color-blanco: #ffffff;
    --color-mantequilla: #ffe8a3;
  }

  body.imprimiendo-certificado .text-green-700,
  body.imprimiendo-certificado .text-green-600 {
    color: #15803d !important;
  }

  body.imprimiendo-certificado .no-print {
    display: none !important;
  }

  body.imprimiendo-certificado .certificado-print article {
    box-shadow: none !important;
    border: 1px solid #d9c9bd !important;
    break-inside: avoid;
  }

  body.imprimiendo-certificado summary::-webkit-details-marker {
    display: none;
  }
}

/* Oculta el marcador nativo del <details> en pantalla. */
.certificado-print summary::-webkit-details-marker {
  display: none;
}
</style>
