<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'
import { formatearFechaHora } from '../servicios/fechas'
import ModalConfirmacion from './ModalConfirmacion.vue'

const usuario = ref('ArtistaMoroso')
const estado = ref(null)
const cargando = ref(false)
const ejecutando = ref('')
const mensaje = ref('')
const error = ref('')
const ciclo = ref(null)

const confirmacion = ref({
  abierto: false,
  accion: '',
  titulo: '',
  mensaje: '',
  textoConfirmar: 'Confirmar',
  destructivo: false,
})

const nombresAlcance = {
  ENTREGAS: 'Nivel 1 · Solo entregas',
  ACCESO: 'Nivel 2 · Cuenta inhabilitada',
  VENTAS: 'Ventas suspendidas',
  COMPRAS: 'Compras suspendidas',
  PAGOS: 'Solo pagos',
}

const textosConfirmacion = {
  nivel1: {
    titulo: 'Aplicar Nivel 1 de prueba',
    mensaje: 'Se revierte cualquier sanción automática activa, se reabre un pedido en mora del artista y se ejecuta el ciclo real. La cuenta queda limitada a entregar los pedidos abiertos y el catálogo se pausa.',
    textoConfirmar: 'Aplicar Nivel 1',
    destructivo: false,
  },
  nivel2: {
    titulo: 'Forzar Nivel 2',
    mensaje: 'Se vence la ventana de la restricción a entregas y se ejecuta el ciclo real. La cuenta queda inhabilitada por completo.',
    textoConfirmar: 'Forzar Nivel 2',
    destructivo: true,
  },
  curar: {
    titulo: 'Simular aprobación del cliente',
    mensaje: 'Se marca el entregable pendiente como aprobado, se ejecuta el ciclo real y se reincorpora al artista. Si está en Nivel 2, también se revierte la inhabilitación como administrador.',
    textoConfirmar: 'Aprobar y reincorporar',
    destructivo: false,
  },
  revertir: {
    titulo: 'Revertir todo',
    mensaje: 'Se revierte la sanción, se restaura el catálogo y el pedido queda reabierto con 7 días de plazo para que no vuelva a sancionarse solo.',
    textoConfirmar: 'Revertir todo',
    destructivo: true,
  },
}

function fecha(valor) {
  if (!valor) return '—'
  return formatearFechaHora(valor) || String(valor)
}

function nombreAlcance(alcance) {
  return nombresAlcance[alcance] || alcance || '—'
}

function textoEstado(valor) {
  return String(valor || '—').replaceAll('_', ' ').toLowerCase()
}

async function cargarEstado() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/sanciones/pruebas/estado', {
      params: { usuario: usuario.value.trim() || 'ArtistaMoroso' },
    })
    estado.value = res.data || null
  } catch (e) {
    estado.value = null
    error.value = e.response?.data?.message
      || 'No se pudo cargar el estado del laboratorio.'
  } finally {
    cargando.value = false
  }
}

async function ejecutar(accion) {
  if (ejecutando.value) return
  ejecutando.value = accion
  error.value = ''
  mensaje.value = ''
  ciclo.value = null
  try {
    const params = { usuario: usuario.value.trim() || 'ArtistaMoroso' }
    const res = await api.post(
      `/admin/sanciones/pruebas/${accion}`,
      null,
      { params },
    )
    mensaje.value = res.data?.mensaje || 'Acción ejecutada.'
    ciclo.value = res.data?.ciclo || null
    estado.value = res.data?.estado || null
    if (!estado.value) await cargarEstado()
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo ejecutar la acción de prueba.'
  } finally {
    ejecutando.value = ''
  }
}

function pedirConfirmacion(accion) {
  const textos = textosConfirmacion[accion]
  if (!textos) return
  confirmacion.value = {
    abierto: true,
    accion,
    ...textos,
  }
}

function confirmarAccion() {
  const accion = confirmacion.value.accion
  confirmacion.value.abierto = false
  if (accion) ejecutar(accion)
}

onMounted(cargarEstado)
</script>

<template>
  <section class="mb-6 rounded-3xl border border-durazno/70 bg-blanco p-5 shadow-xl shadow-rosa/30">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-coral">
          Laboratorio
        </p>
        <h2 class="mt-1 font-display text-lg font-extrabold text-granate">
          Pruebas del ciclo de sanciones
        </h2>
        <p class="mt-1 max-w-2xl text-sm text-cafe/60">
          Ejecutá el ciclo real sobre una cuenta de artista sin esperar a los jobs.
          Las acciones no crean pedidos: reutilizan y reabren uno en curso.
        </p>
      </div>
      <span class="rounded-full bg-durazno/40 px-3 py-1 text-[11px] font-bold uppercase tracking-wide text-granate">
        Solo pruebas
      </span>
    </div>

    <div class="mt-4 flex flex-wrap items-end gap-2">
      <label class="min-w-56 flex-1">
        <span class="block text-xs font-semibold uppercase tracking-wide text-cafe/50">
          Usuario artista
        </span>
        <input
          v-model="usuario"
          type="text"
          class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          @keyup.enter="cargarEstado"
        />
      </label>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
        :disabled="cargando || Boolean(ejecutando)"
        @click="cargarEstado"
      >
        {{ cargando ? 'Actualizando…' : 'Actualizar estado' }}
      </button>
    </div>

    <div class="mt-4 flex flex-wrap gap-2">
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-40"
        :disabled="Boolean(ejecutando)"
        @click="ejecutar('procesar')"
      >
        {{ ejecutando === 'procesar' ? 'Procesando…' : 'Procesar sanciones' }}
      </button>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-coral/40 bg-blanco px-5 text-sm font-semibold text-coral transition hover:bg-coral/10 disabled:opacity-40"
        :disabled="Boolean(ejecutando)"
        @click="pedirConfirmacion('nivel1')"
      >
        {{ ejecutando === 'nivel1' ? 'Aplicando…' : 'Aplicar Nivel 1' }}
      </button>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 bg-blanco px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-40"
        :disabled="Boolean(ejecutando)"
        @click="pedirConfirmacion('nivel2')"
      >
        {{ ejecutando === 'nivel2' ? 'Escalando…' : 'Forzar Nivel 2' }}
      </button>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:opacity-40"
        :disabled="Boolean(ejecutando)"
        @click="pedirConfirmacion('curar')"
      >
        {{ ejecutando === 'curar' ? 'Aprobando…' : 'Simular aprobación' }}
      </button>
      <button
        type="button"
        class="inline-flex min-h-10 items-center justify-center rounded-full border border-cafe/30 bg-blanco px-5 text-sm font-semibold text-cafe transition hover:bg-crema disabled:opacity-40"
        :disabled="Boolean(ejecutando)"
        @click="pedirConfirmacion('revertir')"
      >
        {{ ejecutando === 'revertir' ? 'Revirtiendo…' : 'Revertir todo' }}
      </button>
    </div>

    <p
      v-if="mensaje"
      class="mt-4 rounded-lg border border-coral/30 bg-coral/10 px-3 py-2 text-sm text-granate"
      role="status"
    >
      {{ mensaje }}
    </p>
    <p
      v-if="error"
      class="mt-4 rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-sm text-rojizo"
      role="alert"
    >
      {{ error }}
    </p>
    <p v-if="ciclo" class="mt-3 text-xs text-cafe/60">
      Ciclo: {{ ciclo.morasRegistradas }} mora(s) · {{ ciclo.nivel1Aplicados }}
      Nivel 1 · {{ ciclo.sancionesEscaladas }} escalada(s) ·
      {{ ciclo.sancionesCuradas }} curada(s) · {{ ciclo.pedidosCerrados }}
      cerrado(s).
    </p>

    <p v-if="cargando" class="mt-5 text-sm text-cafe/55">
      Cargando estado de {{ usuario || 'ArtistaMoroso' }}…
    </p>

    <div v-else-if="estado" class="mt-5 grid gap-4 lg:grid-cols-[minmax(0,1fr)_16rem]">
      <div class="space-y-3">
        <div class="rounded-2xl border border-rosa/40 bg-rosa/5 p-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">
              Sanción activa
            </p>
            <span
              class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
              :class="estado.sancion ? 'bg-rojizo/10 text-rojizo' : 'bg-coral/10 text-granate'"
            >
              {{ estado.sancion ? nombreAlcance(estado.sancion.alcance) : 'Sin sanción' }}
            </span>
          </div>
          <template v-if="estado.sancion">
            <p class="mt-2 text-sm text-cafe">
              {{ estado.sancion.motivo }}
            </p>
            <p class="mt-2 text-xs text-cafe/60">
              Origen {{ estado.sancion.origen }} · Estado
              {{ estado.sancion.estadoEfectivo }} · Inicio
              {{ fecha(estado.sancion.fechaInicio) }}
              <span v-if="estado.sancion.fechaFin">
                · Fin {{ fecha(estado.sancion.fechaFin) }}
              </span>
              <span v-if="estado.sancion.fechaEscalada">
                · Escalada {{ fecha(estado.sancion.fechaEscalada) }}
              </span>
            </p>
          </template>
          <p v-else class="mt-2 text-sm text-cafe/60">
            No hay una sanción activa para @{{ estado.usuario }}.
          </p>
        </div>

        <div
          v-for="pedido in estado.pedidos"
          :key="pedido.idPedido"
          class="rounded-2xl border border-rosa/40 bg-blanco p-4"
        >
          <div class="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p class="text-sm font-semibold text-granate">
                Pedido #{{ pedido.idPedido }} · {{ pedido.nombreServicio }}
              </p>
              <p class="text-xs text-cafe/60">
                Cliente @{{ pedido.cliente }} · {{ textoEstado(pedido.estado) }}
              </p>
            </div>
            <span
              class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
              :class="pedido.entregaAtrasada ? 'bg-rojizo/10 text-rojizo' : 'bg-coral/10 text-granate'"
            >
              {{ pedido.entregaAtrasada ? 'Entrega atrasada' : 'En plazo' }}
            </span>
          </div>
          <p class="mt-1 text-xs text-cafe/60">
            Límite {{ pedido.limiteEntrega || '—' }} · Mora desde
            {{ fecha(pedido.moraDesde) }}
          </p>

          <ul class="mt-3 space-y-1.5">
            <li
              v-for="hito in pedido.hitos"
              :key="hito.idHitoPedido"
              class="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-crema/70 px-2.5 py-1.5 text-xs"
            >
              <span class="font-semibold text-cafe">
                {{ hito.nombre }}
                <span class="font-normal text-cafe/50">({{ hito.tipo }})</span>
              </span>
              <span class="text-cafe/60">
                Pago: {{ textoEstado(hito.estadoPago) }} · Entrega:
                {{ textoEstado(hito.estadoEntrega) }}
                <span v-if="hito.plazoVigente">
                  · plazo {{ fecha(hito.plazoVigente) }}
                </span>
              </span>
              <span
                v-if="hito.exigible"
                class="rounded-full bg-rojizo/10 px-2 py-0.5 text-[11px] font-semibold text-rojizo"
              >
                Exigible
              </span>
              <span
                v-else-if="hito.requiereEntrega"
                class="rounded-full bg-rosa/40 px-2 py-0.5 text-[11px] font-semibold text-cafe"
              >
                Entregable
              </span>
            </li>
          </ul>
        </div>

        <p
          v-if="!estado.pedidos.length"
          class="rounded-2xl bg-blanco p-4 text-sm text-cafe/55 ring-1 ring-rosa/40"
        >
          El artista no tiene pedidos en curso.
        </p>
      </div>

      <aside class="space-y-3">
        <div class="rounded-2xl border border-rosa/40 bg-crema/60 p-4">
          <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">
            Configuración
          </p>
          <dl class="mt-2 space-y-1 text-xs text-cafe/70">
            <div class="flex justify-between gap-2">
              <dt>Gracia de entrega</dt>
              <dd class="font-semibold text-cafe">{{ estado.diasGraciaEntrega ?? '—' }} día(s)</dd>
            </div>
            <div class="flex justify-between gap-2">
              <dt>Sanción de ventas</dt>
              <dd class="font-semibold text-cafe">{{ estado.diasSancionVentas ?? '—' }} día(s)</dd>
            </div>
            <div class="flex justify-between gap-2">
              <dt>Cierre tras bloqueo</dt>
              <dd class="font-semibold text-cafe">{{ estado.diasCierreTrasBloqueo ?? '—' }} día(s)</dd>
            </div>
          </dl>
        </div>

        <div class="rounded-2xl border border-rosa/40 bg-crema/60 p-4">
          <p class="text-xs font-semibold uppercase tracking-wide text-cafe/50">
            Catálogo pausado
          </p>
          <ul v-if="estado.pausados?.length" class="mt-2 space-y-1 text-xs text-cafe/70">
            <li v-for="item in estado.pausados" :key="`${item.tipo}-${item.id}`">
              {{ item.tipo }} · {{ item.nombre }}
            </li>
          </ul>
          <p v-else class="mt-2 text-xs text-cafe/55">Sin publicaciones pausadas.</p>
        </div>
      </aside>
    </div>

    <ModalConfirmacion
      :abierto="confirmacion.abierto"
      :titulo="confirmacion.titulo"
      :mensaje="confirmacion.mensaje"
      :texto-confirmar="confirmacion.textoConfirmar"
      :destructivo="confirmacion.destructivo"
      @confirmar="confirmarAccion"
      @cerrar="confirmacion.abierto = false"
    />
  </section>
</template>
