<script setup>
import { computed, ref, watch } from 'vue'
import api from '../servicios/api'
import { AMBITOS_RESENIA, ESCALAS_CRITERIO, POLARIDADES_OPCION } from '../servicios/resenias'

const AMBITO_CORTO = { SERVICIO: 'Servicio', CLIENTE: 'Cliente', PRODUCTO: 'Producto' }
const ESCALA_CORTA = { ESTRELLAS: 'Estrellas', OPCIONES_MULTIPLES: 'Chips' }

const claseCampo = 'block w-full rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral'

const criterios = ref([])
const cargando = ref(true)
const error = ref('')

const filtroAmbito = ref('')
const filtroEscala = ref('')
const filtroActivo = ref('')

const modalAbierto = ref(false)
const editando = ref(false)
const criterioEditado = ref(null)
const nombre = ref('')
const descripcion = ref('')
const observaciones = ref('')
const ambito = ref('SERVICIO')
const escala = ref('ESTRELLAS')
const obligatorio = ref(false)
const orden = ref('0')
const guardando = ref(false)
const errorForm = ref('')
const mensajeForm = ref('')

const nuevaOpcion = ref('')
const nuevaPolaridad = ref('POSITIVA')
const errorOpcion = ref('')
const guardandoOpcion = ref(false)

const modalConfirmacion = ref(null)

const tieneResenias = computed(() => (
  (criterioEditado.value?.reseniasAsociadas || 0) > 0
))

const deshabilitarAmbito = computed(() => editando.value && tieneResenias.value)

const cambioDeEscala = computed(() => (
  editando.value
  && tieneResenias.value
  && criterioEditado.value?.escala !== escala.value
))

const opcionesDelCriterio = computed(() => criterioEditado.value?.opciones || [])

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const params = {}
    if (filtroAmbito.value) params.ambito = filtroAmbito.value
    if (filtroEscala.value) params.escala = filtroEscala.value
    if (filtroActivo.value !== '') params.activo = filtroActivo.value === 'true'
    const res = await api.get('/admin/criterios-resenia', { params })
    criterios.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudieron cargar los criterios de evaluación.'
  } finally {
    cargando.value = false
  }
}

watch([filtroAmbito, filtroEscala, filtroActivo], cargar, { immediate: true })

function limpiarFormulario() {
  nombre.value = ''
  descripcion.value = ''
  observaciones.value = ''
  ambito.value = 'SERVICIO'
  escala.value = 'ESTRELLAS'
  obligatorio.value = false
  orden.value = '0'
  errorForm.value = ''
  mensajeForm.value = ''
  nuevaOpcion.value = ''
  nuevaPolaridad.value = 'POSITIVA'
  errorOpcion.value = ''
}

function abrirAlta() {
  limpiarFormulario()
  editando.value = false
  criterioEditado.value = null
  modalAbierto.value = true
}

function abrirEdicion(criterio) {
  limpiarFormulario()
  editando.value = true
  criterioEditado.value = criterio
  nombre.value = criterio.nombre
  descripcion.value = criterio.descripcion
  observaciones.value = criterio.observaciones || ''
  ambito.value = criterio.ambito
  escala.value = criterio.escala
  obligatorio.value = criterio.obligatorio
  orden.value = String(criterio.orden ?? 0)
  modalAbierto.value = true
}

function cerrarModal() {
  if (guardando.value || guardandoOpcion.value) return
  modalAbierto.value = false
}

async function guardar() {
  errorForm.value = ''
  mensajeForm.value = ''

  if (!nombre.value.trim()) {
    errorForm.value = 'El nombre es obligatorio.'
    return
  }
  if (!descripcion.value.trim()) {
    errorForm.value = 'La descripción es obligatoria.'
    return
  }
  const ordenNumero = Number(orden.value)
  if (!Number.isInteger(ordenNumero) || ordenNumero < 0) {
    errorForm.value = 'El orden debe ser un entero mayor o igual a 0.'
    return
  }

  guardando.value = true
  const payload = {
    nombre: nombre.value.trim(),
    descripcion: descripcion.value.trim(),
    observaciones: observaciones.value.trim() || null,
    ambito: ambito.value,
    escala: escala.value,
    obligatorio: obligatorio.value,
    orden: ordenNumero,
  }

  try {
    const res = editando.value
      ? await api.put(`/admin/criterios-resenia/${criterioEditado.value.idCriterioResenia}`, payload)
      : await api.post('/admin/criterios-resenia', payload)

    if (escala.value === 'OPCIONES_MULTIPLES') {
      editando.value = true
      criterioEditado.value = res.data
      mensajeForm.value = 'Criterio guardado. Ahora podés cargar las opciones.'
      await cargar()
    } else {
      modalAbierto.value = false
      await cargar()
    }
  } catch (e) {
    errorForm.value = e.response?.data?.message
      || 'No se pudo guardar el criterio.'
  } finally {
    guardando.value = false
  }
}

async function agregarOpcion() {
  errorOpcion.value = ''
  const etiqueta = nuevaOpcion.value.trim()
  if (!etiqueta) {
    errorOpcion.value = 'La etiqueta de la opción es obligatoria.'
    return
  }
  guardandoOpcion.value = true
  try {
    const res = await api.post(
      `/admin/criterios-resenia/${criterioEditado.value.idCriterioResenia}/opciones`,
      { etiqueta, polaridad: nuevaPolaridad.value, orden: opcionesDelCriterio.value.length },
    )
    criterioEditado.value = res.data
    nuevaOpcion.value = ''
    await cargar()
  } catch (e) {
    errorOpcion.value = e.response?.data?.message || 'No se pudo agregar la opción.'
  } finally {
    guardandoOpcion.value = false
  }
}

async function guardarOpcion(opcion) {
  errorOpcion.value = ''
  if (!opcion.etiqueta?.trim()) {
    errorOpcion.value = 'La etiqueta de la opción es obligatoria.'
    return
  }
  guardandoOpcion.value = true
  try {
    const res = await api.put(
      `/admin/criterios-resenia/${criterioEditado.value.idCriterioResenia}/opciones/${opcion.idOpcionCriterio}`,
      {
        etiqueta: opcion.etiqueta.trim(),
        polaridad: opcion.polaridad,
        orden: Number(opcion.orden) || 0,
      },
    )
    criterioEditado.value = res.data
    await cargar()
  } catch (e) {
    errorOpcion.value = e.response?.data?.message || 'No se pudo guardar la opción.'
  } finally {
    guardandoOpcion.value = false
  }
}

async function reactivarOpcion(opcion) {
  errorOpcion.value = ''
  guardandoOpcion.value = true
  try {
    const res = await api.post(
      `/admin/criterios-resenia/${criterioEditado.value.idCriterioResenia}/opciones/${opcion.idOpcionCriterio}/activar`,
    )
    criterioEditado.value = res.data
    await cargar()
  } catch (e) {
    errorOpcion.value = e.response?.data?.message || 'No se pudo reactivar la opción.'
  } finally {
    guardandoOpcion.value = false
  }
}

function pedirConfirmacion({ titulo, mensaje, boton, ejecutar }) {
  modalConfirmacion.value = {
    titulo, mensaje, boton, ejecutar, confirmando: false, error: '',
  }
}

function cerrarConfirmacion() {
  if (modalConfirmacion.value && !modalConfirmacion.value.confirmando) {
    modalConfirmacion.value = null
  }
}

async function confirmarAccion() {
  const accion = modalConfirmacion.value
  if (!accion || accion.confirmando) return
  accion.confirmando = true
  accion.error = ''
  try {
    await accion.ejecutar()
    modalConfirmacion.value = null
    await cargar()
  } catch (e) {
    accion.error = e.response?.data?.message || 'No se pudo completar la acción.'
    accion.confirmando = false
  }
}

function confirmarBaja(criterio) {
  pedirConfirmacion({
    titulo: '¿Dar de baja el criterio?',
    mensaje: `El criterio "${criterio.nombre}" dejará de ofrecerse al dejar reseñas. Las reseñas que ya lo usan se siguen viendo. Podés reactivarlo más adelante.`,
    boton: 'Dar de baja',
    ejecutar: () => api.delete(`/admin/criterios-resenia/${criterio.idCriterioResenia}`),
  })
}

function confirmarAlta(criterio) {
  pedirConfirmacion({
    titulo: '¿Reactivar el criterio?',
    mensaje: `El criterio "${criterio.nombre}" vuelve a estar disponible al dejar reseñas.`,
    boton: 'Reactivar',
    ejecutar: () => api.post(`/admin/criterios-resenia/${criterio.idCriterioResenia}/activar`),
  })
}

function confirmarBajaOpcion(opcion) {
  pedirConfirmacion({
    titulo: '¿Dar de baja la opción?',
    mensaje: `La opción "${opcion.etiqueta}" dejará de ofrecerse. Las reseñas que ya la eligieron la conservan.`,
    boton: 'Dar de baja',
    ejecutar: async () => {
      const res = await api.delete(
        `/admin/criterios-resenia/${criterioEditado.value.idCriterioResenia}/opciones/${opcion.idOpcionCriterio}`,
      )
      criterioEditado.value = res.data
    },
  })
}

function claseAmbito(valor) {
  if (valor === 'SERVICIO') return 'bg-coral/10 text-coral'
  if (valor === 'CLIENTE') return 'bg-granate/10 text-granate'
  return 'bg-cafe/10 text-cafe/60'
}
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-xl font-normal text-granate">Criterios de evaluación</h1>
        <p class="mt-1 text-sm text-cafe/60">
          Definí qué se puntúa al dejar una reseña: estrellas o chips de opciones múltiples.
        </p>
      </div>
      <button
        type="button"
        class="rounded-md bg-coral px-4 py-2 text-sm text-white shadow-sm transition hover:bg-rojizo"
        @click="abrirAlta"
      >
        Dar de alta
      </button>
    </div>

    <div class="mb-4 flex flex-wrap gap-3">
      <select v-model="filtroAmbito" class="rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe" aria-label="Filtrar por ámbito">
        <option value="">Todos los ámbitos</option>
        <option v-for="item in AMBITOS_RESENIA" :key="item.valor" :value="item.valor">
          {{ item.etiqueta }}
        </option>
      </select>
      <select v-model="filtroEscala" class="rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe" aria-label="Filtrar por escala">
        <option value="">Todas las escalas</option>
        <option v-for="item in ESCALAS_CRITERIO" :key="item.valor" :value="item.valor">
          {{ item.etiqueta }}
        </option>
      </select>
      <select v-model="filtroActivo" class="rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe" aria-label="Filtrar por estado">
        <option value="">Todos los estados</option>
        <option value="true">Activos</option>
        <option value="false">Inactivos</option>
      </select>
    </div>

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="overflow-hidden rounded-3xl bg-blanco shadow-xl shadow-rosa/40">
      <div class="overflow-x-auto">
        <table class="w-full min-w-160 text-left text-sm">
          <thead class="bg-rosa/40 text-granate">
            <tr>
              <th class="px-5 py-3 text-sm font-normal">Orden</th>
              <th class="px-5 py-3 text-sm font-normal">Criterio</th>
              <th class="px-5 py-3 text-sm font-normal">Ámbito</th>
              <th class="px-5 py-3 text-sm font-normal">Escala</th>
              <th class="px-5 py-3 text-sm font-normal">Obligatorio</th>
              <th class="px-5 py-3 text-sm font-normal">Opciones</th>
              <th class="px-5 py-3 text-sm font-normal">Estado</th>
              <th class="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="criterio in criterios"
              :key="criterio.idCriterioResenia"
              class="border-t border-rosa/50 transition hover:bg-rosa/20"
            >
              <td class="px-5 py-3 text-cafe/60">{{ criterio.orden }}</td>
              <td class="px-5 py-3">
                <p class="font-normal text-granate">{{ criterio.nombre }}</p>
                <p class="mt-0.5 text-xs text-cafe/55">{{ criterio.descripcion }}</p>
                <p v-if="criterio.reseniasAsociadas > 0" class="mt-0.5 text-xs text-coral">
                  Usado en {{ criterio.reseniasAsociadas }} reseña(s)
                </p>
              </td>
              <td class="px-5 py-3">
                <span class="rounded-full px-2.5 py-0.5 text-xs font-normal" :class="claseAmbito(criterio.ambito)">
                  {{ AMBITO_CORTO[criterio.ambito] || criterio.ambito }}
                </span>
              </td>
              <td class="px-5 py-3 text-cafe">
                {{ ESCALA_CORTA[criterio.escala] || criterio.escala }}
              </td>
              <td class="px-5 py-3 text-cafe">
                {{ criterio.obligatorio ? 'Sí' : 'No' }}
              </td>
              <td class="px-5 py-3 text-cafe/70">
                <template v-if="criterio.escala === 'OPCIONES_MULTIPLES'">
                  {{ (criterio.opciones || []).filter((o) => o.activo).length }}/{{ (criterio.opciones || []).length }}
                </template>
                <template v-else>—</template>
              </td>
              <td class="px-5 py-3">
                <span
                  class="rounded-full px-2.5 py-0.5 text-xs font-normal"
                  :class="criterio.activo ? 'bg-green-500/10 text-green-700' : 'bg-rojizo/10 text-rojizo'"
                >
                  {{ criterio.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td class="whitespace-nowrap px-5 py-3 text-right">
                <button
                  type="button"
                  class="mr-3 text-rojizo transition hover:text-coral"
                  @click="abrirEdicion(criterio)"
                >
                  Editar
                </button>
                <button
                  type="button"
                  class="text-cafe/50 transition hover:text-rojizo"
                  @click="criterio.activo ? confirmarBaja(criterio) : confirmarAlta(criterio)"
                >
                  {{ criterio.activo ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!criterios.length" class="p-8 text-center text-cafe/50">
        No hay criterios registrados.
      </p>
    </div>

    <div
      v-if="modalAbierto"
      class="fixed inset-0 z-50 flex items-center justify-center bg-granate/40 px-4"
      @click.self="cerrarModal"
    >
      <div class="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-blanco p-6 shadow-xl">
        <h2 class="mb-4 text-lg text-granate">
          {{ editando ? 'Modificar criterio' : 'Dar de alta criterio' }}
        </h2>

        <div class="space-y-5">
          <div>
            <label for="criterio-nombre" class="mb-2 block text-sm text-cafe">
              Nombre <span class="text-rojizo">*</span>
            </label>
            <input
              id="criterio-nombre"
              v-model="nombre"
              type="text"
              maxlength="50"
              :class="claseCampo"
            />
          </div>

          <div>
            <label for="criterio-descripcion" class="mb-2 block text-sm text-cafe">
              Descripción <span class="text-rojizo">*</span>
            </label>
            <textarea
              id="criterio-descripcion"
              v-model="descripcion"
              rows="2"
              maxlength="250"
              :class="[claseCampo, 'resize-none']"
            ></textarea>
            <p class="mt-1 text-right text-xs text-cafe/50">{{ descripcion.length }}/250</p>
          </div>

          <div>
            <label for="criterio-observaciones" class="mb-2 block text-sm text-cafe">
              Observaciones
            </label>
            <textarea
              id="criterio-observaciones"
              v-model="observaciones"
              rows="2"
              maxlength="500"
              :class="[claseCampo, 'resize-none']"
            ></textarea>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="criterio-ambito" class="mb-2 block text-sm text-cafe">
                Ámbito <span class="text-rojizo">*</span>
              </label>
              <select
                id="criterio-ambito"
                v-model="ambito"
                :disabled="deshabilitarAmbito"
                :class="[claseCampo, deshabilitarAmbito ? 'opacity-60' : '']"
              >
                <option v-for="item in AMBITOS_RESENIA" :key="item.valor" :value="item.valor">
                  {{ item.etiqueta }}
                </option>
              </select>
            </div>
            <div>
              <label for="criterio-escala" class="mb-2 block text-sm text-cafe">
                Escala <span class="text-rojizo">*</span>
              </label>
              <select
                id="criterio-escala"
                v-model="escala"
                :class="claseCampo"
              >
                <option v-for="item in ESCALAS_CRITERIO" :key="item.valor" :value="item.valor">
                  {{ item.etiqueta }}
                </option>
              </select>
            </div>
          </div>

          <div class="flex flex-wrap items-end gap-6">
            <label class="inline-flex items-center gap-2 text-sm text-cafe">
              <input v-model="obligatorio" type="checkbox" class="h-4 w-4 rounded border-rosa text-coral focus:ring-coral" />
              Obligatorio
            </label>
            <div class="w-28">
              <label for="criterio-orden" class="mb-2 block text-sm text-cafe">Orden</label>
              <input
                id="criterio-orden"
                v-model="orden"
                type="number"
                min="0"
                step="1"
                :class="claseCampo"
              />
            </div>
          </div>

          <p v-if="deshabilitarAmbito" class="rounded-lg bg-durazno/30 px-3 py-2 text-xs leading-relaxed text-granate">
            Este criterio ya tiene reseñas: no se puede cambiar el ámbito.
            Creá uno nuevo para el otro ámbito.
          </p>
          <p v-if="cambioDeEscala" class="rounded-lg bg-durazno/30 px-3 py-2 text-xs leading-relaxed text-granate">
            Las reseñas anteriores conservan la escala con la que se respondieron;
            las nuevas van a usar la escala que elijas ahora.
          </p>

          <div
            v-if="editando && escala === 'OPCIONES_MULTIPLES'"
            class="rounded-xl border border-rosa/40 bg-rosa/5 p-4"
          >
            <h3 class="text-sm font-semibold text-granate">Opciones del criterio</h3>
            <p class="mt-1 text-xs text-cafe/55">
              Se ofrecen como chips. Las positivas aparecen con calificaciones de 4 o 5
              y las negativas con 1 a 3. Las inactivas no se muestran.
            </p>

            <div class="mt-3 space-y-2">
              <div
                v-for="opcion in opcionesDelCriterio"
                :key="opcion.idOpcionCriterio"
                class="flex flex-wrap items-center gap-2 rounded-lg bg-blanco p-2"
              >
                <input
                  v-model="opcion.etiqueta"
                  type="text"
                  maxlength="50"
                  class="min-w-0 flex-1 rounded-md border border-rosa bg-blanco px-3 py-1.5 text-sm text-cafe"
                  :aria-label="`Etiqueta de la opción ${opcion.etiqueta}`"
                />
                <select
                  v-model="opcion.polaridad"
                  class="rounded-md border border-rosa bg-blanco px-2 py-1.5 text-xs text-cafe"
                  :aria-label="`Polaridad de ${opcion.etiqueta}`"
                >
                  <option
                    v-for="polaridad in POLARIDADES_OPCION"
                    :key="polaridad.valor"
                    :value="polaridad.valor"
                  >
                    {{ polaridad.etiqueta }}
                  </option>
                </select>
                <input
                  v-model.number="opcion.orden"
                  type="number"
                  min="0"
                  step="1"
                  class="w-20 rounded-md border border-rosa bg-blanco px-2 py-1.5 text-sm text-cafe"
                  :aria-label="`Orden de ${opcion.etiqueta}`"
                />
                <span
                  v-if="!opcion.activo"
                  class="rounded-full bg-rojizo/10 px-2 py-0.5 text-[11px] text-rojizo"
                >
                  Inactiva
                </span>
                <button
                  type="button"
                  class="text-xs font-semibold text-coral hover:text-rojizo"
                  :disabled="guardandoOpcion"
                  @click="guardarOpcion(opcion)"
                >
                  Guardar
                </button>
                <button
                  type="button"
                  class="text-xs text-cafe/50 hover:text-rojizo"
                  :disabled="guardandoOpcion"
                  @click="opcion.activo ? confirmarBajaOpcion(opcion) : reactivarOpcion(opcion)"
                >
                  {{ opcion.activo ? 'Dar de baja' : 'Reactivar' }}
                </button>
              </div>
              <p v-if="!opcionesDelCriterio.length" class="text-xs text-cafe/50">
                Todavía no hay opciones cargadas.
              </p>
            </div>

            <div class="mt-3 flex flex-wrap items-center gap-2">
              <input
                v-model="nuevaOpcion"
                type="text"
                maxlength="50"
                placeholder="Nueva opción"
                class="min-w-0 flex-1 rounded-md border border-rosa bg-blanco px-3 py-2 text-sm text-cafe"
                @keyup.enter="agregarOpcion"
              />
              <select
                v-model="nuevaPolaridad"
                class="rounded-md border border-rosa bg-blanco px-2 py-2 text-sm text-cafe"
                aria-label="Polaridad de la nueva opción"
              >
                <option
                  v-for="polaridad in POLARIDADES_OPCION"
                  :key="polaridad.valor"
                  :value="polaridad.valor"
                >
                  {{ polaridad.etiqueta }}
                </option>
              </select>
              <button
                type="button"
                class="rounded-md border border-coral/40 px-3 py-2 text-sm font-semibold text-coral transition hover:bg-coral/10"
                :disabled="guardandoOpcion"
                @click="agregarOpcion"
              >
                Agregar
              </button>
            </div>
            <p v-if="errorOpcion" class="mt-2 text-sm text-rojizo">{{ errorOpcion }}</p>
          </div>

          <p v-if="errorForm" class="text-sm text-rojizo">{{ errorForm }}</p>
          <p v-else-if="mensajeForm" class="text-sm text-green-700">{{ mensajeForm }}</p>
        </div>

        <div class="mt-6 flex items-center justify-end gap-3">
          <button
            type="button"
            class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
            @click="cerrarModal"
          >
            {{ mensajeForm ? 'Cerrar' : 'Cancelar' }}
          </button>
          <button
            type="button"
            class="rounded-md bg-coral px-4 py-2 text-sm text-white shadow-sm transition hover:bg-rojizo disabled:opacity-50"
            :disabled="guardando || guardandoOpcion"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="modalConfirmacion"
      class="fixed inset-0 z-[80] flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
    >
      <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrarConfirmacion"></div>
      <div class="relative w-full max-w-md rounded-xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20">
        <h2 class="text-lg text-granate">{{ modalConfirmacion.titulo }}</h2>
        <p class="mt-2 text-sm text-cafe/70">{{ modalConfirmacion.mensaje }}</p>
        <p v-if="modalConfirmacion.error" class="mt-3 text-sm text-rojizo">
          {{ modalConfirmacion.error }}
        </p>
        <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20"
            :disabled="modalConfirmacion.confirmando"
            @click="cerrarConfirmacion"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="rounded-md bg-rojizo px-4 py-2 text-sm text-white transition hover:bg-granate disabled:opacity-50"
            :disabled="modalConfirmacion.confirmando"
            @click="confirmarAccion"
          >
            {{ modalConfirmacion.confirmando ? 'Procesando...' : modalConfirmacion.boton }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
