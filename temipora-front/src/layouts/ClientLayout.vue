<script setup>
import { computed } from 'vue'
import AccountLayout from '../components/AccountLayout.vue'

const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const ICONOS = {
  pedidos: 'M2.25 13.5h3.86a2.25 2.25 0 0 1 2.012 1.244l.256.512a2.25 2.25 0 0 0 2.013 1.244h3.218a2.25 2.25 0 0 0 2.013-1.244l.256-.512a2.25 2.25 0 0 1 2.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 0 0-2.15-1.588H6.911a2.25 2.25 0 0 0-2.15 1.588L2.35 13.177a2.25 2.25 0 0 0-.1.661Z',
  solicitudes: 'M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m5.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z',
  cuenta: 'M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z',
  perfil: 'M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z',
}

const secciones = [
  {
    titulo: 'Mi actividad',
    items: [
      { nombre: 'Mis pedidos', to: '/pedidos', base: '/pedidos', icono: ICONOS.pedidos },
      { nombre: 'Mis solicitudes', to: '/solicitudes', base: '/solicitudes', icono: ICONOS.solicitudes },
    ],
  },
]

const inferior = computed(() => {
  const items = [
    { nombre: 'Configuración de cuenta', to: '/configuracion/perfil', icono: ICONOS.cuenta },
  ]
  if (nombreUsuario) {
    items.push({
      nombre: 'Ver mi perfil público',
      to: { name: 'perfil', params: { nombreUsuario } },
      icono: ICONOS.perfil,
    })
  }
  return items
})
</script>

<template>
  <AccountLayout
    titulo="Mis compras"
    :secciones="secciones"
    :inferior="inferior"
  >
    <router-view />
  </AccountLayout>
</template>
