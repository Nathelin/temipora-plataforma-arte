export const SECCIONES_CON_TABS = [
  {
    id: 'comunidad',
    tabs: [
      { etiqueta: 'Sanciones', to: '/admin/sanciones' },
      { etiqueta: 'Apelaciones', to: '/admin/apelaciones' },
      { etiqueta: 'Pruebas', to: '/admin/sanciones/pruebas' },
    ],
  },
  {
    id: 'informes',
    tabs: [
      { etiqueta: 'Ventas', to: '/admin/informes/ventas' },
    ],
  },
  {
    id: 'arte',
    tabs: [
      { etiqueta: 'Medios', to: '/admin/arte/medios' },
      { etiqueta: 'Tipos de arte', to: '/admin/arte/tipos' },
      { etiqueta: 'Materiales', to: '/admin/arte/materiales' },
    ],
  },
  {
    id: 'etiquetas',
    tabs: [
      { etiqueta: 'Etiquetas', to: '/admin/etiquetas' },
      { etiqueta: 'Categorías', to: '/admin/etiquetas/categorias' },
    ],
  },
  {
    id: 'resenas',
    tabs: [
      { etiqueta: 'Criterios', to: '/admin/resenas/criterios' },
      { etiqueta: 'Configuración', to: '/admin/resenas/configuracion' },
    ],
  },
  {
    id: 'moderacion',
    tabs: [
      { etiqueta: 'Pendientes', to: '/admin/resenas/reportadas' },
      { etiqueta: 'Historial', to: '/admin/resenas/historial' },
    ],
  },
  {
    id: 'monedas',
    tabs: [
      { etiqueta: 'Monedas', to: '/admin/ajustes/monedas' },
      { etiqueta: 'Cotización', to: '/admin/ajustes/cotizacion' },
    ],
  },
  {
    id: 'presupuestos',
    tabs: [
      { etiqueta: 'Vigencia', to: '/admin/ajustes/presupuestos' },
      { etiqueta: 'Reglas de pedidos', to: '/admin/ajustes/pedidos' },
    ],
  },
  {
    id: 'promociones',
    tabs: [
      { etiqueta: 'Configuración', to: '/admin/campanas/promociones' },
      { etiqueta: 'Descuentos aplicados', to: '/admin/campanas/promociones/descuentos' },
    ],
  },
  {
    id: 'sustituciones',
    tabs: [
      { etiqueta: 'Configuración', to: '/admin/campanas/sustituciones' },
      { etiqueta: 'Historial', to: '/admin/campanas/sustituciones/historial' },
    ],
  },
]

export function tabsDeRuta(path) {
  const seccion = SECCIONES_CON_TABS.find((s) => s.tabs.some((tab) => tab.to === path))
  return seccion ? seccion.tabs : []
}
