<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'

const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const errorAccion = ref('')
const mensaje = ref('')
const config = ref(null)
const porcentajeServicio = ref('10')
const porcentajeProducto = ref('10')
const porcentajeReventa = ref('10')

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/configuracion-comision')
    config.value = res.data
    porcentajeServicio.value = res.data?.porcentajeServicio ?? '10'
    porcentajeProducto.value = res.data?.porcentajeProducto ?? '10'
    porcentajeReventa.value = res.data?.porcentajeReventa ?? '10'
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo cargar la configuración de comisiones.'
  } finally {
    cargando.value = false
  }
}

function formatearFecha(valor) {
  if (!valor) return '—'
  try {
    return new Date(valor).toLocaleString('es-AR')
  } catch {
    return valor
  }
}

function validar(valor, etiqueta) {
  const numero = Number(String(valor).replace(',', '.'))
  if (!Number.isFinite(numero)) {
    return `La comisión de ${etiqueta} debe ser un número.`
  }
  if (numero < 0 || numero > 50) {
    return `La comisión de ${etiqueta} debe estar entre 0 y 50.`
  }
  if (Math.round(numero * 100) !== numero * 100) {
    return `La comisión de ${etiqueta} admite hasta 2 decimales.`
  }
  return ''
}

async function guardar() {
  errorAccion.value = ''
  mensaje.value = ''
  const errorServicio = validar(porcentajeServicio.value, 'servicios')
  const errorProducto = validar(porcentajeProducto.value, 'productos')
  const errorReventa = validar(porcentajeReventa.value, 'reventas')
  if (errorServicio || errorProducto || errorReventa) {
    errorAccion.value = errorServicio || errorProducto || errorReventa
    return
  }

  guardando.value = true
  try {
    const res = await api.put('/admin/configuracion-comision', {
      porcentajeServicio: Number(String(porcentajeServicio.value).replace(',', '.')),
      porcentajeProducto: Number(String(porcentajeProducto.value).replace(',', '.')),
      porcentajeReventa: Number(String(porcentajeReventa.value).replace(',', '.')),
    })
    config.value = res.data
    porcentajeServicio.value = res.data.porcentajeServicio
    porcentajeProducto.value = res.data.porcentajeProducto
    porcentajeReventa.value = res.data.porcentajeReventa
    mensaje.value = 'Comisiones guardadas.'
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudieron guardar las comisiones.'
  } finally {
    guardando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto w-full flex-1 px-4 py-8 sm:px-8">
    <AdminPageHeader
      class="mb-6"
      eyebrow="Comercio"
      titulo="Comisión Temipora"
      descripcion="Porcentaje que Temipora descuenta a cada venta."
    />

    <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
    <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

    <div v-else class="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <div class="space-y-6 rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
        <div>
          <h2 class="mb-4 text-base text-granate">Porcentajes</h2>

          <label for="comision-servicio" class="mb-2 block text-sm text-cafe">
            Comisión de servicios <span class="text-rojizo">*</span>
          </label>
          <div class="flex items-center gap-2">
            <input
              id="comision-servicio"
              v-model="porcentajeServicio"
              type="number"
              min="0"
              max="50"
              step="0.01"
              class="block w-40 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <span class="text-sm text-cafe/60">%</span>
          </div>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            Se descuenta del precio del artista. El cliente paga el mismo precio final del presupuesto.
          </p>

          <label for="comision-producto" class="mb-2 mt-6 block text-sm text-cafe">
            Comisión de productos <span class="text-rojizo">*</span>
          </label>
          <div class="flex items-center gap-2">
            <input
              id="comision-producto"
              v-model="porcentajeProducto"
              type="number"
              min="0"
              max="50"
              step="0.01"
              class="block w-40 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <span class="text-sm text-cafe/60">%</span>
          </div>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            Reservada para la venta de productos. La comisión de productos y la de servicios son independientes.
          </p>

          <label for="comision-reventa" class="mb-2 mt-6 block text-sm text-cafe">
            Comisión de reventas <span class="text-rojizo">*</span>
          </label>
          <div class="flex items-center gap-2">
            <input
              id="comision-reventa"
              v-model="porcentajeReventa"
              type="number"
              min="0"
              max="50"
              step="0.01"
              class="block w-40 rounded-lg border-0 px-4 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <span class="text-sm text-cafe/60">%</span>
          </div>
          <p class="mt-2 text-xs leading-relaxed text-cafe/50">
            Se descuenta al revendedor sobre el precio de reventa. En esta versión la reventa no paga al artista original.
          </p>
        </div>

        <p v-if="errorAccion" class="text-sm text-rojizo">{{ errorAccion }}</p>
        <p v-else-if="mensaje" class="text-sm text-green-700">{{ mensaje }}</p>

        <div class="flex flex-wrap items-center gap-3 pt-2">
          <button
            :disabled="guardando"
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-rojizo disabled:opacity-50 motion-reduce:transition-none"
            @click="guardar"
          >
            {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
          </button>
          <router-link
            to="/admin/ajustes/terminos-plataforma"
            class="text-sm font-medium text-coral transition hover:text-rojizo"
          >
            Publicar términos de la plataforma
          </router-link>
        </div>
      </div>

      <aside class="h-fit rounded-2xl border border-rosa/50 bg-blanco p-6 lg:sticky lg:top-16">
        <h2 class="mb-4 text-base text-granate">Estado actual</h2>
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Servicios</dt>
            <dd class="text-cafe">{{ config.porcentajeServicio }}%</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Productos</dt>
            <dd class="text-cafe">{{ config.porcentajeProducto }}%</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Reventas</dt>
            <dd class="text-cafe">{{ config.porcentajeReventa }}%</dd>
          </div>
          <div class="flex justify-between gap-3">
            <dt class="text-xs text-cafe/50">Última modificación</dt>
            <dd class="text-right text-cafe">
              {{ formatearFecha(config.fechaActualizacion) }}
            </dd>
          </div>
        </dl>
        <p class="mt-4 border-t border-rosa/40 pt-4 text-xs leading-relaxed text-cafe/50">
          Los presupuestos ya enviados conservan el porcentaje con el que se emitieron.
          Si cambiás la comisión, conviene publicar una versión nueva de los términos de la plataforma.
        </p>
      </aside>
    </div>
  </div>
</template>
