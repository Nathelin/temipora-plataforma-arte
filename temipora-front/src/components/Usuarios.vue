<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import SancionCuentaModal from './SancionCuentaModal.vue'

const router = useRouter()
const usernameSearch = ref('')
const emailSearch = ref('')
const cargando = ref(true)
const error = ref('')
const abierto = ref(null)
const nombreAdmin = ref(localStorage.getItem('nombreUsuario') || '')
const filtroRol = ref('todos')
const filtroEstado = ref('todos')
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const usuarios = ref([]) // content of current page
const usuarioSancionar = ref(null)
let debounceTimer
let requestId = 0

// Build params object for API call
function getParams() {
  return {
    nombreUsuario: usernameSearch.value.trim() === '' ? null : usernameSearch.value.trim(),
    correo: emailSearch.value.trim() === '' ? null : emailSearch.value.trim(),
    estado: filtroEstado.value === 'todos' ? null : (filtroEstado.value === 'activo'),
    rol: filtroRol.value === 'todos' ? null : filtroRol.value,
    page: currentPage.value,
    size: pageSize.value,
    sort: 'idUsuario,asc'
  }
}

async function fetchUsers() {
  const currentRequestId = ++requestId
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/usuarios', { params: getParams() })
    if (currentRequestId !== requestId) return

    const data = res.data ?? {}
    const content = Array.isArray(data) ? data : data.content
    usuarios.value = Array.isArray(content) ? content : []
    totalElements.value = Array.isArray(data)
      ? data.length
      : Number(data.totalElements ?? usuarios.value.length)
    totalPages.value = Array.isArray(data)
      ? (usuarios.value.length ? 1 : 0)
      : Number(data.totalPages ?? (usuarios.value.length ? 1 : 0))
  } catch (e) {
    if (currentRequestId !== requestId) return

    if (e.response && e.response.status === 401) {
      error.value = 'Sesión expirada o inválida. Inicia sesión nuevamente.'
      router.push({ name: 'login' })
    } else if (e.response && e.response.status === 403) {
      error.value = 'No autorizado. Solo administradores pueden ver esta vista.'
    } else {
      error.value = 'No se pudieron cargar los usuarios.'
    }
    // Reset data on error
    usuarios.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    cargando.value = false
  }
}

function limpiarFiltros() {
  usernameSearch.value = ''
  emailSearch.value = ''
  filtroRol.value = 'todos'
  filtroEstado.value = 'todos'
  currentPage.value = 0
  fetchUsers()
}

function anteriorPagina() {
  if (currentPage.value > 0) {
    currentPage.value--
    fetchUsers()
  }
}

function siguientePagina() {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
    fetchUsers()
  }
}

function alternarDetalle(id) {
  abierto.value = abierto.value === id ? null : id
}

function abrirSancion(usuario) {
  usuarioSancionar.value = usuario
}

function verSancion(usuario) {
  router.push({ name: 'admin-sanciones', query: { texto: usuario.nombreUsuario } })
}

function alAplicarSancion() {
  fetchUsers()
}

function cerrarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('rol')
  localStorage.removeItem('nombreUsuario')
  router.push({ name: 'login' })
}

// Watch for filter changes to refetch from page 0
watch([usernameSearch, emailSearch, filtroRol, filtroEstado], () => {
  currentPage.value = 0
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchUsers, 300)
})

onMounted(fetchUsers)
onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <div>
    <main class="flex-1 w-full mx-auto px-4 sm:px-8 py-8 overflow-auto">
      <div class="flex flex-col gap-4 mb-6">
        <AdminPageHeader
          eyebrow="Comunidad"
          titulo="Usuarios registrados"
          descripcion="Buscá cuentas por nombre o correo, filtrá por rol y estado."
        >
          <template #acciones>
            <span class="text-sm text-cafe/60 tabular-nums">
              {{ usuarios.length }} de {{ totalElements }} mostrados
            </span>
          </template>
        </AdminPageHeader>

        <div class="grid grid-cols-1 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto_auto_auto] gap-3">
          <input
            v-model="usernameSearch"
            type="search"
            placeholder="Buscar por nombre de usuario..."
            class="min-w-0 rounded-lg border-0 py-2.5 px-4 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
          <input
            v-model="emailSearch"
            type="email"
            placeholder="Buscar por correo..."
            class="min-w-0 rounded-lg border-0 py-2.5 px-4 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />

          <select
            v-model="filtroRol"
            aria-label="Filtrar por rol"
            class="rounded-lg border-0 py-2.5 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="todos">Todos los roles</option>
            <option value="ADMIN">Administradores</option>
            <option value="ARTISTA">Artistas</option>
            <option value="CLIENTE">Clientes</option>
          </select>

          <select
            v-model="filtroEstado"
            aria-label="Filtrar por estado"
            class="rounded-lg border-0 py-2.5 px-3 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="todos">Todos los estados</option>
            <option value="activo">Activos</option>
            <option value="inactivo">Inactivos</option>
          </select>

          <button
            class="inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition-colors hover:bg-rosa/20 disabled:opacity-50 motion-reduce:transition-none"
            @click="limpiarFiltros"
          >
            Limpiar
          </button>
        </div>
      </div>

      <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando usuarios...</p>
      <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

      <div v-else class="bg-blanco rounded-3xl overflow-hidden shadow-xl shadow-rosa/40">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm min-w-160">
            <thead class="bg-rosa/40 text-granate">
              <tr>
                <th class="px-5 py-3 font-normal text-sm">ID</th>
                <th class="px-5 py-3 font-normal text-sm">Usuario</th>
                <th class="px-5 py-3 font-normal text-sm">Rol</th>
                <th class="px-5 py-3 font-normal text-sm">Correo</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              <template v-for="u in usuarios" :key="u.idUsuario">
                <tr class="border-t border-rosa/50 hover:bg-rosa/20 transition">
                  <td class="px-5 py-3 text-cafe/50 font-mono">{{ u.idUsuario }}</td>
                  <td class="px-5 py-3 text-granate font-normal">
                    {{ u.nombreUsuario }}
                    <span
                      v-if="u.sancionado"
                      class="ml-2 rounded-full bg-rojizo/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-rojizo"
                    >
                      Inhabilitada
                    </span>
                  </td>
                  <td class="px-5 py-3">
                    <span
                      class="px-2.5 py-0.5 rounded-full text-xs font-normal tracking-wide"
                      :class="{
                        'bg-rojizo/10 text-rojizo': u.rol === 'ADMIN',
                        'bg-coral/10 text-coral': u.rol === 'ARTISTA',
                        'bg-durazno/40 text-cafe': u.rol === 'CLIENTE',
                      }"
                    >
                      {{ u.rol }}
                    </span>
                  </td>
                  <td class="px-5 py-3 text-granate">{{ u.correo }}</td>
                  <td class="px-5 py-3 text-right">
                    <div class="flex items-center justify-end gap-3">
                      <button
                        v-if="!u.sancionado"
                        class="text-sm font-semibold text-rojizo hover:text-coral transition"
                        @click.stop="abrirSancion(u)"
                      >
                        Inhabilitar
                      </button>
                      <button
                        v-else
                        class="text-sm font-semibold text-granate hover:text-coral transition"
                        @click.stop="verSancion(u)"
                      >
                        Ver sanción
                      </button>
                      <button
                        class="text-rojizo hover:text-coral transition"
                        @click="alternarDetalle(u.idUsuario)"
                      >
                        {{ abierto === u.idUsuario ? 'Cerrar' : 'Detalles' }}
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="abierto === u.idUsuario" class="border-t border-rosa/50 bg-rosa/10">
                  <td colspan="5" class="px-5 py-5">
                    <div class="flex flex-wrap gap-5">
                      <div
                        v-if="u.urlFotoPerfil"
                        class="w-24 h-24 rounded-full overflow-hidden border-2 border-coral shrink-0"
                      >
                        <img
                          :src="u.urlFotoPerfil"
                          :alt="u.nombreUsuario"
                          class="w-full h-full object-cover"
                        />
                      </div>
                      <div
                        v-else
                        class="w-24 h-24 rounded-full bg-coral/20 flex items-center justify-center text-3xl text-granate shrink-0"
                      >
                        {{ (u.nombreUsuario || '?').charAt(0).toUpperCase() }}
                      </div>

                      <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-10 gap-y-2 text-sm">
                        <p><span class="text-rojizo">Nombre:</span> {{ u.nombre || '—' }}</p>
                        <p><span class="text-rojizo">Apellido:</span> {{ u.apellido || '—' }}</p>
                        <p><span class="text-rojizo">Correo:</span> {{ u.correo || '—' }}</p>
                        <p><span class="text-rojizo">Teléfono:</span> {{ u.telefonoEnmascarado || '—' }}</p>
                        <p><span class="text-rojizo">Sexo:</span> {{ u.sexo || '—' }}</p>
                        <p><span class="text-rojizo">Nacimiento:</span> {{ u.fechaNacimiento || '—' }}</p>
                        <p>
                          <span class="text-rojizo">Creada:</span>
                          {{ u.fechaCreacion ? new Date(u.fechaCreacion).toLocaleString() : '—' }}
                        </p>
                        <p>
                          <span class="text-rojizo">Última conexión:</span>
                          {{
                            u.fechaUltimaConexion
                              ? new Date(u.fechaUltimaConexion).toLocaleString()
                              : '—'
                          }}
                        </p>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <div v-if="!usuarios.length" class="p-8 text-cafe/50 text-center">
          {{ totalElements ? 'No hay usuarios que coincidan con los filtros.' : 'No hay usuarios registrados.' }}
        </div>

        <div v-else class="flex justify-between items-center mt-4 px-5">
          <span class="text-sm text-cafe/60">
            Página {{ currentPage + 1 }} de {{ totalPages > 0 ? totalPages : 1 }}
          </span>
          <div class="flex gap-2">
            <button
              :disabled="currentPage === 0"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
              @click="anteriorPagina"
            >
              Anterior
            </button>
            <button
              :disabled="currentPage >= totalPages - 1 || totalPages === 0"
              class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-[13px] font-semibold text-cafe transition-colors hover:bg-rosa/20 disabled:opacity-40 motion-reduce:transition-none"
              @click="siguientePagina"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </main>

    <SancionCuentaModal
      :abierto="Boolean(usuarioSancionar)"
      :usuario="usuarioSancionar"
      @cerrar="usuarioSancionar = null"
      @aplicada="alAplicarSancion"
    />
  </div>
</template>