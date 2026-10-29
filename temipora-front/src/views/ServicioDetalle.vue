<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import VistaPreviaTerminos from '../components/VistaPreviaTerminos.vue'
import ResumenResenias from '../components/ResumenResenias.vue'
import TarjetaReseniaPublica from '../components/TarjetaReseniaPublica.vue'
import ModalResenias from '../components/ModalResenias.vue'
import { cargarReseniasServicio } from '../servicios/resenias'
import { esImagenVerificada } from '../servicios/imagenes'
import { useNotificaciones } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()

const servicio = ref(null)
const terminos = ref(null)
const disponibilidad = ref(null)
const terminosValidos = ref(false)
const cargando = ref(true)
const error = ref('')
const autenticado = ref(!!localStorage.getItem('token'))
const rol = ref(localStorage.getItem('rol') || '')
const esFavorito = ref(false)
const cargandoEstadoFavorito = ref(false)
const actualizandoFavorito = ref(false)

const resumenResenias = ref(null)
const reseniasDestacadas = ref([])
const reseniasLista = ref([])
const paginaResenias = ref(null)
const cargandoResenias = ref(false)
const errorResenias = ref('')
const modalReseniasAbierto = ref(false)
const cargandoListaResenias = ref(false)
const errorListaResenias = ref('')
const filtrosResenias = reactive({ calificacion: '', orden: 'RECIENTES' })

const CAMPOS_TERMINOS = [
  'informacionGeneral',
  'politicaRevisiones',
  'politicaCancelaciones',
  'usoArtista',
  'usoCliente',
  'queDibuja',
  'queNoDibuja',
]

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
const hayTerminos = computed(() => {
  const t = terminos.value
  if (!t) return false
  const hayTexto = [...CAMPOS_TERMINOS, 'pagoExtra']
    .some((k) => (t[k] || '').trim())
  return hayTexto || (t.derechos || []).some((d) => d.disponible)
})

const mostrarSensible = ref(false)
const indiceImagen = ref(0)

const ETIQUETAS_SENSIBLE = {
  VIOLENCIA: 'Violencia',
  DESNUDOS: 'Desnudos',
  GENERAL: 'Contenido sensible general',
}

const esSensible = computed(() => !!servicio.value?.contenidoSensible)
const requiereAdvertencia = computed(() => !!servicio.value?.requiereAdvertencia)
const tiposSensibles = computed(() => {
  const tipos = servicio.value?.contenidosSensibles || []
  return tipos.map((t) => ETIQUETAS_SENSIBLE[t] || t)
})
const textoSensible = computed(() => tiposSensibles.value.join(' · ') || 'Contenido sensible')
const esDuenio = computed(() => {
  const actual = localStorage.getItem('nombreUsuario') || ''
  return actual.toLowerCase() === (servicio.value?.nombreArtista || '').toLowerCase()
})
const puedeContratar = computed(() => rol.value === 'CLIENTE' || rol.value === 'ARTISTA')
const puedeMarcarFavorito = computed(
  () =>
    autenticado.value &&
    puedeContratar.value &&
    !esDuenio.value &&
    servicio.value?.estado === 'PUBLICADO',
)
const puedeSolicitar = computed(
  () =>
    autenticado.value &&
    puedeContratar.value &&
    !esDuenio.value &&
    terminosValidos.value &&
    !!disponibilidad.value?.puedeSolicitar,
)
const mensajeSolicitud = computed(() => {
  if (esDuenio.value) return ''
  if (!autenticado.value) return 'Iniciá sesión para solicitar este servicio.'
  if (!puedeContratar.value) return 'Solo las cuentas de cliente o artista pueden solicitar servicios.'
  if (!terminosValidos.value) return 'El artista todavía no configuró los términos y condiciones de este servicio.'
  return disponibilidad.value?.mensaje || ''
})

const precioMostrado = computed(() =>
  formatearPrecio(
    servicio.value?.precioBaseMostrado ?? servicio.value?.precioBase,
    servicio.value?.monedaMostrada ?? servicio.value?.moneda,
  ),
)
const precioConvertido = computed(
  () =>
    !!servicio.value?.monedaMostrada &&
    servicio.value.monedaMostrada !== servicio.value.moneda,
)

const galeria = computed(() => {
  const s = servicio.value
  if (!s) return []
  const portada = s.imagenPortada ? [{ url: s.imagenPortada, portada: true }] : []
  const ejemplos = (s.imagenes || [])
    .filter((url) => url && url !== s.imagenPortada)
    .map((url) => ({ url, portada: false }))
  return [...portada, ...ejemplos]
})

const imagenActiva = computed(() => galeria.value[indiceImagen.value] || null)
const activaEsPortada = computed(() => !!imagenActiva.value?.portada)
const activaEsVerificada = computed(() => esImagenVerificada(imagenActiva.value?.url))
const mostrarBlur = computed(
  () => requiereAdvertencia.value && activaEsPortada.value && !mostrarSensible.value,
)

function textoEntrega(s) {
  return `${s.tiempoEntregaValor} ${(s.tiempoEntregaUnidad || '').toLowerCase()}`
}

function seleccionarImagen(i) {
  indiceImagen.value = i
}

function irAlArtista() {
  router.push({ name: 'perfil', params: { nombreUsuario: servicio.value.nombreArtista } })
}

function irAServicios() {
  router.push({ name: 'servicios' })
}

function irASolicitar() {
  if (!autenticado.value) {
    router.push({ name: 'login' })
    return
  }
  router.push({ name: 'solicitud-servicio', params: { id: servicio.value.idServicio } })
}

function aplicarSolicitudPublica(data) {
  servicio.value = data.servicio
  terminos.value = data.terminos
  disponibilidad.value = data.disponibilidad
  terminosValidos.value = !!data.terminosValidos
}

async function cargarEstadoFavorito() {
  esFavorito.value = false
  if (!puedeMarcarFavorito.value) return

  cargandoEstadoFavorito.value = true
  try {
    const res = await api.get(`/favoritos/servicios/${servicio.value.idServicio}`)
    esFavorito.value = !!res.data?.esFavorito
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
    }
  } finally {
    cargandoEstadoFavorito.value = false
  }
}

async function alternarFavoritoServicio() {
  if (!autenticado.value) {
    router.push({ name: 'login' })
    return
  }
  if (
    !puedeMarcarFavorito.value ||
    actualizandoFavorito.value ||
    cargandoEstadoFavorito.value
  ) {
    return
  }

  actualizandoFavorito.value = true
  try {
    if (esFavorito.value) {
      await api.delete(`/favoritos/servicios/${servicio.value.idServicio}`)
      esFavorito.value = false
      exito(`Quitaste "${servicio.value.nombre}" de tus favoritos.`)
    } else {
      await api.post(`/favoritos/servicios/${servicio.value.idServicio}`)
      esFavorito.value = true
      exito(`Agregaste "${servicio.value.nombre}" a tus favoritos.`)
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    notificarError(e.response?.data?.message || 'No se pudieron actualizar tus favoritos.')
  } finally {
    actualizandoFavorito.value = false
  }
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/servicios/${route.params.id}/solicitud-publica`)
    aplicarSolicitudPublica(res.data)
  } catch (e) {
    try {
      const servicioRes = await api.get(`/servicios/${route.params.id}`)
      servicio.value = servicioRes.data
      const [terminosRes, disponibilidadRes] = await Promise.all([
        api.get(`/servicios/${route.params.id}/terminos-publicos`).catch(() => null),
        api.get(`/servicios/${route.params.id}/disponibilidad`).catch(() => null),
      ])
      terminos.value = terminosRes?.data || null
      disponibilidad.value = disponibilidadRes?.data || null
      terminosValidos.value =
        !!terminos.value?.idTerminos &&
        Number(terminos.value?.version || 0) > 0
    } catch (e2) {
      error.value = e2.response?.data?.message || 'No se pudo cargar el servicio.'
    }
  } finally {
    cargando.value = false
  }
  if (servicio.value) {
    void cargarReseniasResumen()
  }
}

async function cargarReseniasResumen() {
  cargandoResenias.value = true
  errorResenias.value = ''
  try {
    const { data } = await cargarReseniasServicio(route.params.id, {
      pagina: 0,
      tamanio: 3,
    })
    resumenResenias.value = data.resumen || null
    reseniasDestacadas.value = data.pagina?.contenido || []
  } catch {
    errorResenias.value = 'No se pudieron cargar las reseñas.'
  } finally {
    cargandoResenias.value = false
  }
}

function parametrosResenias(pagina) {
  const params = {
    pagina,
    tamanio: 10,
    orden: filtrosResenias.orden || 'RECIENTES',
  }
  if (filtrosResenias.calificacion) {
    params.calificacion = Number(filtrosResenias.calificacion)
  }
  return params
}

async function cargarListaResenias(pagina = 0) {
  cargandoListaResenias.value = true
  errorListaResenias.value = ''
  try {
    const { data } = await cargarReseniasServicio(
      route.params.id,
      parametrosResenias(pagina),
    )
    reseniasLista.value = data.pagina?.contenido || []
    paginaResenias.value = data.pagina || null
    if (data.resumen) resumenResenias.value = data.resumen
  } catch (e) {
    errorListaResenias.value = e.response?.data?.message
      || 'No se pudieron cargar las reseñas.'
  } finally {
    cargandoListaResenias.value = false
  }
}

function abrirResenias() {
  modalReseniasAbierto.value = true
  void cargarListaResenias(0)
}

function cerrarResenias() {
  modalReseniasAbierto.value = false
}

function cambiarFiltrosResenias(filtros) {
  filtrosResenias.calificacion = filtros.calificacion ?? ''
  filtrosResenias.orden = filtros.orden || 'RECIENTES'
  void cargarListaResenias(0)
}

function cambiarPaginaResenias(pagina) {
  void cargarListaResenias(pagina)
}

function manejarTecla(event) {
  if (!servicio.value || event.key !== 'Escape') return
  if (mostrarSensible.value) mostrarSensible.value = false
}

onMounted(async () => {
  await cargar()
  void cargarEstadoFavorito()
  window.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => window.removeEventListener('keydown', manejarTecla))
</script>

<template>
  <div class="mx-auto px-4 sm:px-8 py-8">
    <button type="button" class="text-sm text-granate hover:text-coral transition" @click="irAServicios">
      ← Volver a servicios
    </button>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando servicio...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <template v-else-if="servicio">
      <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_22rem] gap-8 mt-4">
        <!-- Galería -->
        <div>
          <div class="relative rounded-2xl overflow-hidden bg-rosa/20 aspect-video">
            <img
              v-if="imagenActiva"
              :src="imagenActiva.url"
              :alt="servicio.nombre"
              class="w-full h-full object-cover transition duration-200"
              :class="mostrarBlur ? 'blur-[18px] scale-105' : ''"
            />
            <div
              v-if="mostrarBlur"
              class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-granate/30 text-center px-6"
            >
              <svg class="h-10 w-10 text-white drop-shadow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
              </svg>
              <p class="text-sm font-medium text-white drop-shadow">Contenido sensible</p>
              <span class="rounded-full bg-blanco/90 px-3 py-0.5 text-xs text-cafe">{{ textoSensible }}</span>
              <button
                type="button"
                class="mt-1 rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
                @click="mostrarSensible = true"
              >
                Ver de todos modos
              </button>
            </div>
            <div
              v-if="requiereAdvertencia && activaEsPortada && mostrarSensible"
              class="absolute left-3 top-3 rounded-full bg-granate/70 px-3 py-1 text-xs text-inverso-contraste"
            >
              Mostrando contenido sensible
              <button type="button" class="ml-2 underline hover:text-white" @click="mostrarSensible = false">
                Ocultar
              </button>
            </div>
            <span
              v-if="activaEsVerificada"
              class="absolute right-3 top-3 z-10 inline-flex items-center gap-1 rounded-full bg-blanco/90 px-2 py-1 text-[11px] font-semibold text-green-700 shadow ring-1 ring-green-700/20"
            >
              <svg class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 0 1 0 1.414l-7.25 7.25a1 1 0 0 1-1.414 0l-3.25-3.25a1 1 0 1 1 1.414-1.414l2.543 2.543 6.543-6.543a1 1 0 0 1 1.414 0Z" clip-rule="evenodd" />
              </svg>
              Verificado
            </span>
          </div>

          <div v-if="galeria.length > 1" class="mt-3 grid grid-cols-5 sm:grid-cols-8 gap-2">
            <button
              v-for="(img, i) in galeria"
              :key="i"
              type="button"
              class="relative aspect-square rounded-lg overflow-hidden bg-rosa/20 border-2 transition"
              :class="indiceImagen === i ? 'border-coral' : 'border-transparent hover:border-rosa'"
              :aria-label="img.portada ? 'Ver portada' : `Ver imagen de ejemplo ${i}`"
              @click="seleccionarImagen(i)"
            >
              <img :src="img.url" alt="" class="w-full h-full object-cover" />
              <span
                v-if="esImagenVerificada(img.url)"
                class="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-green-700 text-[10px] font-bold text-white shadow"
                role="img"
                aria-label="Imagen verificada"
                title="Imagen verificada"
              >
                ✓
              </span>
              <span
                v-if="img.portada"
                class="absolute bottom-0 inset-x-0 bg-granate/60 text-white text-[10px] px-1 py-0.5 text-center"
              >
                Portada
              </span>
            </button>
          </div>

          <div class="mt-6">
            <h2 class="text-xl text-granate">{{ servicio.nombre }}</h2>
            <button
              type="button"
              class="mt-1 text-sm text-cafe/60 hover:text-coral transition"
              @click="irAlArtista"
            >
              Por @{{ servicio.nombreArtista }}
            </button>
            <p class="mt-4 text-sm text-cafe leading-relaxed whitespace-pre-wrap">{{ servicio.descripcion }}</p>
          </div>
        </div>

        <!-- Datos del servicio -->
        <aside class="lg:sticky lg:top-16 h-fit rounded-2xl border border-rosa/50 bg-blanco p-6">
          <p class="text-2xl text-granate">{{ precioMostrado }}</p>
          <p class="mt-1 text-xs text-cafe/50">
            Precio base por dibujo
            <span v-if="precioConvertido"> · equivalente</span>
          </p>
          <p v-if="precioConvertido" class="mt-1 text-xs text-cafe/50">
            El artista lo cargó como {{ formatearPrecio(servicio.precioBase, servicio.moneda) }}
          </p>

          <dl class="mt-6 space-y-4 text-sm border-t border-rosa/60 pt-6">
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Tipo</dt>
              <dd class="text-right text-cafe">
                <span class="block">{{ servicio.nombreTipoServicio }}</span>
                <span v-if="servicio.descripcionTipoServicio" class="mt-1 block text-xs leading-relaxed text-cafe/60">
                  {{ servicio.descripcionTipoServicio }}
                </span>
              </dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Medio</dt>
              <dd class="text-cafe">{{ servicio.nombreMedio }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Entrega estimada</dt>
              <dd class="text-cafe">{{ textoEntrega(servicio) }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Cupos disponibles</dt>
              <dd class="text-cafe">
                {{ disponibilidad ? `${disponibilidad.cuposDisponibles} de ${disponibilidad.cuposTotales}` : servicio.cupos }}
              </dd>
            </div>
            <div v-if="disponibilidad?.maxDibujosPorSolicitud" class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Máximo por solicitud</dt>
              <dd class="text-cafe">{{ disponibilidad.maxDibujosPorSolicitud }} dibujo(s)</dd>
            </div>
            <div v-if="disponibilidad?.modoRecepcion" class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Recepción</dt>
              <dd class="text-cafe">
                {{ disponibilidad.modoRecepcion === 'SOLO_ESPERA'
                  ? 'Solo lista de espera'
                  : (disponibilidad.modoRecepcion === 'CERRADA' ? 'Cerrada' : 'Abierta') }}
              </dd>
            </div>
            <div v-if="disponibilidad?.permiteListaEspera" class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Lista de espera</dt>
              <dd class="text-cafe">Disponible</dd>
            </div>
            <div v-if="esSensible" class="flex justify-between gap-3">
              <dt class="text-xs text-cafe/50">Contenido sensible</dt>
              <dd class="text-coral text-right">{{ textoSensible }}</dd>
            </div>
          </dl>

          <div v-if="resumenResenias" class="mt-4 border-t border-rosa/60 pt-4">
            <ResumenResenias
              :resumen="resumenResenias"
              compacto
              clickable
              @ver-todas="abrirResenias"
            />
          </div>

          <div class="mt-6 space-y-3 border-t border-rosa/60 pt-6">
            <button
              v-if="!esDuenio && !autenticado"
              type="button"
              class="w-full rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm"
              @click="irASolicitar"
            >
              Iniciá sesión para solicitar
            </button>
            <button
              v-else-if="!esDuenio"
              type="button"
              class="w-full rounded-md bg-coral px-4 py-2 text-sm text-white hover:bg-rojizo transition shadow-sm disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="!puedeSolicitar"
              @click="irASolicitar"
            >
              Solicitar servicio
            </button>
            <button
              v-if="puedeMarcarFavorito"
              type="button"
              class="flex w-full items-center justify-center gap-2 rounded-md border px-4 py-2 text-sm font-semibold transition disabled:cursor-wait disabled:opacity-60"
              :class="esFavorito
                ? 'border-coral bg-coral text-white hover:bg-rojizo'
                : 'border-coral/50 bg-superficie text-granate hover:bg-rosa/25'"
              :disabled="actualizandoFavorito || cargandoEstadoFavorito"
              :aria-pressed="esFavorito"
              @click="alternarFavoritoServicio"
            >
              <svg
                class="h-4 w-4"
                viewBox="0 0 24 24"
                :fill="esFavorito ? 'currentColor' : 'none'"
                stroke="currentColor"
                stroke-width="1.8"
                aria-hidden="true"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
              </svg>
              {{ actualizandoFavorito ? 'Guardando...' : esFavorito ? 'En favoritos' : 'Guardar en favoritos' }}
            </button>
            <button
              type="button"
              class="w-full rounded-md border border-rosa px-4 py-2 text-sm text-granate hover:bg-rosa/20 transition"
              @click="irAlArtista"
            >
              Ver perfil del artista
            </button>
            <router-link
              v-if="esDuenio"
              :to="`/panel/servicios/${servicio.idServicio}/editar`"
              class="block w-full rounded-md border border-rosa px-4 py-2 text-sm text-granate text-center hover:bg-rosa/20 transition"
            >
              Editar servicio
            </router-link>
          </div>

          <p v-if="!esDuenio" class="mt-6 text-xs text-cafe/50 leading-relaxed border-t border-rosa/60 pt-4">
            {{ mensajeSolicitud || 'Revisá los términos y condiciones antes de enviar tu solicitud.' }}
          </p>
        </aside>
      </div>

      <section class="mt-10 rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-xl text-granate">Reseñas</h2>
          <p class="text-xs text-cafe/50">Lo que dicen los clientes de este servicio</p>
        </div>

        <div class="mt-4">
          <ResumenResenias
            :resumen="resumenResenias"
            clickable
            @ver-todas="abrirResenias"
          />
        </div>

        <p v-if="cargandoResenias" class="mt-4 text-sm text-cafe/55">Cargando reseñas...</p>
        <p v-else-if="errorResenias" class="mt-4 text-sm text-rojizo">{{ errorResenias }}</p>
        <div v-else-if="reseniasDestacadas.length" class="mt-5 space-y-3">
          <TarjetaReseniaPublica
            v-for="resenia in reseniasDestacadas"
            :key="resenia.idResenia"
            :resenia="resenia"
          />
        </div>
      </section>

      <section v-if="hayTerminos" class="mt-10 rounded-2xl border border-rosa/50 bg-blanco p-6 sm:p-8">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-xl text-granate">Términos y condiciones</h2>
          <span
            class="rounded-full px-3 py-1 text-xs"
            :class="terminos.propio ? 'bg-coral/10 text-granate' : 'bg-rosa/30 text-cafe/70'"
          >
            {{ terminos.propio ? 'De este servicio' : 'Generales del artista' }}
          </span>
        </div>
        <div class="mt-6">
          <VistaPreviaTerminos
            :terminos="terminosParaMostrar"
            :derechos-catalogo="derechosTerminos"
            :mostrar-aviso="false"
            :ocultar-vacios="true"
          />
        </div>
      </section>

      <ModalResenias
        :abierto="modalReseniasAbierto"
        titulo="Reseñas del servicio"
        :resenias="reseniasLista"
        :resumen="resumenResenias"
        :pagina="paginaResenias"
        :cargando="cargandoListaResenias"
        :error="errorListaResenias"
        :filtros="filtrosResenias"
        @cerrar="cerrarResenias"
        @cambiar-filtros="cambiarFiltrosResenias"
        @cambiar-pagina="cambiarPaginaResenias"
      />
    </template>
  </div>
</template>
