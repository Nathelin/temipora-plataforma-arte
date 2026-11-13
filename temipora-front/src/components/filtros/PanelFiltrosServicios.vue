<script setup>
import { computed } from 'vue'
import GrupoFiltro from './GrupoFiltro.vue'
import FiltroCheckbox from './FiltroCheckbox.vue'
import FiltroRangoPrecio from './FiltroRangoPrecio.vue'
import FiltroEtiquetas from './FiltroEtiquetas.vue'
import { CONTENIDOS_SENSIBLES, ORDENES, cantidadActivos } from '../../servicios/filtrosServicio'

const props = defineProps({
  estado: { type: Object, required: true },
  tipos: { type: Array, default: () => [] },
  medios: { type: Array, default: () => [] },
  etiquetas: { type: Array, default: () => [] },
  categorias: { type: Array, default: () => [] },
  moneda: { type: String, default: 'ARS' },
  facetas: { type: Object, default: null },
  cargando: { type: Boolean, default: false },
})

const emit = defineEmits(['actualizar', 'limpiar'])

const activos = computed(() => cantidadActivos(props.estado))

const opcionesTipos = computed(() => (props.tipos || []).map((t) => ({
  valor: String(t.idTipoServicio),
  texto: t.nombre,
})))

const opcionesMedios = computed(() => (props.medios || []).map((m) => ({
  valor: String(m.idMedio),
  texto: m.nombre,
})))

const conteosTipos = computed(() => props.facetas?.tipos || {})
const conteosMedios = computed(() => props.facetas?.medios || {})
const conteosEtiquetas = computed(() => props.facetas?.etiquetas || {})
const conteosContenidos = computed(() => props.facetas?.contenidosSensibles || {})

const opcionesSensibles = computed(() => CONTENIDOS_SENSIBLES.map((c) => ({
  valor: c.valor,
  texto: c.texto,
})))

const monedasDisponibles = computed(() => {
  const mapa = props.facetas?.monedas
  if (!mapa) return []
  return Object.keys(mapa).filter((codigo) => Number(mapa[codigo]) > 0)
})

function actualizar(cambios) {
  emit('actualizar', cambios)
}

function cambiarLista(clave, valores) {
  actualizar({ [clave]: valores })
}
</script>

<template>
  <div class="rounded-[22px] border border-borde/60 bg-superficie">
    <div class="flex items-center justify-between gap-2 border-b border-borde/40 px-4 py-3">
      <h2 class="font-display text-base font-bold text-granate">Filtros</h2>
      <button
        v-if="activos > 0"
        type="button"
        class="text-xs font-semibold text-coral transition hover:text-rojizo"
        @click="emit('limpiar')"
      >
        Limpiar ({{ activos }})
      </button>
    </div>

    <div class="px-4" :class="cargando ? 'opacity-60 transition-opacity' : ''">
      <GrupoFiltro titulo="Búsqueda" :abierto="true" :activos="
        (estado.nombre ? 1 : 0) + (estado.nombreArtista ? 1 : 0) + (estado.textoLibre ? 1 : 0)
      ">
        <div class="space-y-2">
          <input
            :value="estado.textoLibre"
            type="search"
            placeholder="Buscar por nombre o descripción..."
            class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            @input="actualizar({ textoLibre: $event.target.value })"
          />
          <input
            :value="estado.nombreArtista"
            type="search"
            placeholder="Filtrar por artista (@usuario)"
            class="w-full rounded-lg border-0 bg-crema px-3 py-2 text-sm text-texto ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            @input="actualizar({ nombreArtista: $event.target.value })"
          />
          <label class="flex items-center justify-between gap-2 rounded-lg px-1.5 py-1.5 text-sm text-cafe">
            <span>Ordenar por</span>
            <select
              :value="estado.orden"
              class="rounded-lg border-0 bg-crema px-2 py-1.5 text-sm font-semibold text-cafe ring-1 ring-inset ring-borde/60 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              @change="actualizar({ orden: $event.target.value })"
            >
              <option v-for="o in ORDENES" :key="o.valor" :value="o.valor">{{ o.texto }}</option>
            </select>
          </label>
        </div>
      </GrupoFiltro>

      <GrupoFiltro
        v-if="opcionesTipos.length"
        titulo="Tipo de servicio"
        :abierto="true"
        :activos="estado.idsTiposServicio.length"
      >
        <FiltroCheckbox
          :opciones="opcionesTipos"
          :seleccionados="estado.idsTiposServicio"
          :conteos="conteosTipos"
          @cambiar="(valores) => cambiarLista('idsTiposServicio', valores)"
        />
      </GrupoFiltro>

      <GrupoFiltro
        v-if="opcionesMedios.length"
        titulo="Medio"
        :abierto="true"
        :activos="estado.idsMedios.length"
      >
        <FiltroCheckbox
          :opciones="opcionesMedios"
          :seleccionados="estado.idsMedios"
          :conteos="conteosMedios"
          @cambiar="(valores) => cambiarLista('idsMedios', valores)"
        />
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Precio"
        :abierto="true"
        :activos="(estado.precioMin !== '' ? 1 : 0) + (estado.precioMax !== '' ? 1 : 0)"
      >
        <FiltroRangoPrecio
          :precio-min="estado.precioMin"
          :precio-max="estado.precioMax"
          :moneda="moneda"
          @cambiar="actualizar"
        />
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Entrega"
        :activos="estado.tiempoMaxDias !== '' ? 1 : 0"
      >
        <div class="space-y-1.5">
          <p class="text-[11px] font-semibold uppercase tracking-[0.1em] text-cafe/50">
            Tiempo máximo
          </p>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="dias in [7, 14, 30, 60]"
              :key="dias"
              type="button"
              class="rounded-full border px-2.5 py-1 text-[11px] font-semibold transition"
              :class="String(estado.tiempoMaxDias) === String(dias)
                ? 'border-coral bg-coral/10 text-granate'
                : 'border-borde/60 text-cafe/70 hover:border-coral/40 hover:text-granate'"
              @click="actualizar({ tiempoMaxDias: String(estado.tiempoMaxDias) === String(dias) ? '' : String(dias) })"
            >
              {{ dias }} días
            </button>
          </div>
        </div>
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Disponibilidad"
        :activos="(estado.soloConCupos ? 1 : 0) + (estado.soloConListaEspera ? 1 : 0)"
      >
        <ul class="space-y-0.5">
          <li>
            <label class="flex cursor-pointer items-center gap-2 rounded-lg px-1.5 py-1.5 text-sm text-cafe transition hover:bg-rosa/20">
              <input
                type="checkbox"
                class="h-4 w-4 rounded border-borde text-coral focus:ring-coral"
                :checked="estado.soloConCupos"
                @change="actualizar({ soloConCupos: $event.target.checked })"
              />
              Solo con cupos disponibles
            </label>
          </li>
          <li>
            <label class="flex cursor-pointer items-center gap-2 rounded-lg px-1.5 py-1.5 text-sm text-cafe transition hover:bg-rosa/20">
              <input
                type="checkbox"
                class="h-4 w-4 rounded border-borde text-coral focus:ring-coral"
                :checked="estado.soloConListaEspera"
                @change="actualizar({ soloConListaEspera: $event.target.checked })"
              />
              Acepta lista de espera
            </label>
          </li>
        </ul>
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Valoración"
        :activos="estado.valoracionMinima !== '' ? 1 : 0"
      >
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="valor in [3, 4, 4.5]"
            :key="valor"
            type="button"
            class="rounded-full border px-2.5 py-1 text-[11px] font-semibold transition"
            :class="String(estado.valoracionMinima) === String(valor)
              ? 'border-coral bg-coral/10 text-granate'
              : 'border-borde/60 text-cafe/70 hover:border-coral/40 hover:text-granate'"
            @click="actualizar({ valoracionMinima: String(estado.valoracionMinima) === String(valor) ? '' : String(valor) })"
          >
            ★ {{ valor }} o más
          </button>
        </div>
      </GrupoFiltro>

      <GrupoFiltro
        titulo="Contenido sensible"
        :activos="estado.contenidosSensibles.length + estado.excluirContenidosSensibles.length"
      >
        <FiltroCheckbox
          :opciones="opcionesSensibles"
          :seleccionados="estado.contenidosSensibles"
          :excluidos="estado.excluirContenidosSensibles"
          :conteos="conteosContenidos"
          @cambiar="(valores) => cambiarLista('contenidosSensibles', valores)"
          @cambiar-excluidos="(valores) => cambiarLista('excluirContenidosSensibles', valores)"
        />
      </GrupoFiltro>

      <GrupoFiltro
        v-if="monedasDisponibles.length > 1"
        titulo="Moneda del precio original"
        :activos="estado.monedaDeclarada ? 1 : 0"
      >
        <p class="mb-2 text-[11px] leading-relaxed text-cafe/50">
          El rango de precio ya contempla todas las monedas convertidas.
          Este filtro sirve solo si querés ver únicamente servicios publicados en una.
        </p>
        <FiltroCheckbox
          :opciones="monedasDisponibles.map((c) => ({ valor: c, texto: c }))"
          :seleccionados="estado.monedaDeclarada ? [estado.monedaDeclarada] : []"
          @cambiar="(valores) => actualizar({ monedaDeclarada: valores[0] || '' })"
        />
      </GrupoFiltro>

      <FiltroEtiquetas
        v-if="etiquetas.length"
        :etiquetas="etiquetas"
        :categorias="categorias"
        :seleccionadas="estado.idsEtiquetas"
        :excluidas="estado.idsEtiquetasExcluidas"
        :conteos="conteosEtiquetas"
        @cambiar="actualizar"
      />

      <div class="py-3">
        <button
          v-if="activos > 0"
          type="button"
          class="w-full rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="emit('limpiar')"
        >
          Limpiar todos los filtros
        </button>
      </div>
    </div>
  </div>
</template>
