# Temipora

Plataforma web para conectar artistas con clientes que buscan arte personalizado o piezas listas para comprar. Los artistas publican su portfolio, ofrecen servicios a medida y venden productos digitales; los clientes exploran el catálogo, solicitan presupuestos, pagan, siguen el avance de sus pedidos, reciben sus entregas y dejan reseñas.

Trabajo final de la carrera **Analista en Sistemas de Computación** de la **Universidad Nacional de Misiones**.

> **Estado**: sistema implementado y funcional de punta a punta para el alcance presentado en el trabajo final. Cubre autenticación, perfiles, portfolio, servicios, productos, pedidos, pagos, mensajería, reseñas, moderación, administración, informes, auditoría y registro de autenticidad con anclaje en Bitcoin.

---

### DEMO: https://youtu.be/ul04Hp9WDDU 
### MANUAL DE USUARIO: https://github.com/Nathelin/temipora-plataforma-arte/blob/main/documentacion/manual/Temipora%20%E2%80%94%20Manual%20de%20usuario.pdf

## Índice

- [De qué trata el proyecto](#de-qué-trata-el-proyecto)
- [Funcionalidades](#funcionalidades)
  - [Visitantes](#visitantes)
  - [Clientes](#clientes)
  - [Artistas](#artistas)
  - [Administradores](#administradores)
- [Tecnologías](#tecnologías)
- [Arquitectura general](#arquitectura-general)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Prerrequisitos](#prerrequisitos)
- [Variables de entorno](#variables-de-entorno)
- [Base de datos y migraciones](#base-de-datos-y-migraciones)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
- [API REST](#api-rest)
- [Roles y permisos](#roles-y-permisos)
- [Flujos principales](#flujos-principales)
- [Integraciones externas](#integraciones-externas)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Tests y calidad](#tests-y-calidad)
- [Documentación](#documentación)
- [Alcance y limitaciones conocidas](#alcance-y-limitaciones-conocidas)

---

## De qué trata el proyecto

Temipora nace para resolver un problema concreto: los artistas suelen vender arte personalizado por redes sociales, con acuerdos informales, sin seguimiento de pedidos y sin mecanismos claros de protección para ambas partes.

La plataforma propone:

- **Descubrimiento**: catálogos públicos de servicios y productos, búsqueda, filtros, favoritos y recomendaciones.
- **Contratación formal**: solicitud de servicio, presupuesto del artista, hitos de pago y entrega, seguimiento del pedido.
- **Pagos**: integración con MercadoPago (OAuth, checkout y webhooks).
- **Protección**: reseñas con moderación, sanciones, auditoría de acciones administrativas y registro de autenticidad de piezas únicas.
- **Comunicación**: mensajería privada en tiempo real con solicitudes de contacto y notificaciones.
- **Portfolio profesional**: imágenes, obras verificadas, galería pública de pedidos y contenido sensible con advertencias.
- **Administración**: catálogos maestros, comisiones, promociones, reportes, términos y condiciones versionados.

### Roles

| Rol | Descripción |
|---|---|
| `CLIENTE` | Compra productos y contrata servicios a medida. |
| `ARTISTA` | Ofrece servicios, vende productos, gestiona su portfolio y sus pedidos. También puede comprar. |
| `ADMIN` | Administra usuarios, catálogos, configuración, moderación, reportes y auditoría. |

---

## Funcionalidades

### Visitantes

- Landing pública, catálogo de servicios y catálogo de productos.
- Búsqueda y filtros por artista, tipo, precio, medio, etiquetas, contenido sensible y disponibilidad.
- Perfil público de artistas y clientes, portfolio y galería pública de pedidos.
- Detalle público de productos, servicios y reventas.
- Consulta pública de certificados de autenticidad.
- Registro e inicio de sesión.

### Clientes

- **Cuenta y perfil**: registro, login con JWT, perfil público, edición de datos, apariencia con temas, privacidad y preferencias de contenido sensible.
- **Seguridad**: cambio de contraseña, gestión de teléfono con confirmación, baja de cuenta y reactivación.
- **Descubrimiento**: explorar, favoritos de artistas, servicios y productos, colección personal y ofertas personalizadas.
- **Servicios a medida**: solicitud con formulario dinámico del artista, lista de espera cuando no hay cupos, recepción de presupuesto, pago, seguimiento de hitos, descarga de entregas y reseña final.
- **Productos**: carrito, checkout, compra de variantes, descarga de entregables, certificado de autenticidad y reventa de piezas únicas.
- **Comunicación**: solicitudes de mensaje, conversaciones en tiempo real, indicador de escritura y notificaciones.
- **Reseñas**: crear, modificar, eliminar y reportar reseñas; historial de reportes.

### Artistas

- Todo lo disponible para clientes, más:
- **Panel del artista**: resumen, métricas, pedidos activos y tareas pendientes.
- **Servicios**: alta, modificación y baja lógica; precio base, cupos, tiempos, imágenes, etiquetas, términos y formulario de pedidos propio o global.
- **Productos**: borradores, publicación, variantes, stock, licencias de uso, imágenes, entregables digitales, pausa y archivo.
- **Solicitudes y presupuestos**: aceptar o rechazar solicitudes, enviar presupuestos con recargos, hitos de pago, tiempos de entrega y derechos de uso.
- **Pedidos**: avances por etapas, entregas parciales y entrega final, publicación de piezas en el portfolio como **verificadas**.
- **Portfolio**: carga y edición de obras, materiales, medios, etiquetas, contenido sensible y visibilidad.
- **Ventas e informes**: ventas, liquidaciones, comisiones y descarga de informes en PDF.
- **Etiquetas**: proponer etiquetas, consultar su estado y volver a solicitar las rechazadas.
- **Cobros**: vinculación de cuenta de MercadoPago mediante OAuth.
- **Términos y formularios**: configuración global de términos de uso y servicio y del formulario de pedidos.

### Administradores

- **Resumen general** y alertas operativas.
- **Usuarios**: listado, detalle, sanciones y bajas de cuenta.
- **Catálogos maestros**: tipos de producto, tipos de servicio, etiquetas y categorías, medios, tipos de arte, materiales, países y monedas.
- **Operación**: configuración de pedidos e hitos, presupuestos, comisión de la plataforma, sustituciones de artistas, mensajería y servicios archivados.
- **Promociones**: campañas de reactivación de usuarios inactivos, ofertas y descuentos.
- **Reseñas**: criterios de evaluación, moderación de reportes e historial.
- **Términos de la plataforma**: edición y versionado.
- **Informes**: ventas, comisiones y reportes PDF.
- **Auditoría**: registro de acciones administrativas con filtros y detalle de cambios.
- **Anclaje en Bitcoin**: estado de los lotes del registro de autenticidad y su sellado con OpenTimestamps.
- **Notificaciones** del sistema.

---

## Tecnologías

### Backend

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje de programación |
| Spring Boot | 4.1.1 | Framework principal |
| Spring WebMVC | — | API REST |
| Spring Data JPA / Hibernate | — | Persistencia y ORM |
| Spring Security | — | Autenticación y autorización |
| JJWT | 0.13.0 | Generación y validación de JWT |
| Flyway | — | Migraciones de base de datos |
| PostgreSQL | 42.7.3 (driver) | Base de datos principal |
| H2 | — | Dependencia disponible para escenarios en memoria |
| Lombok | 1.18.42 | Reducción de boilerplate |
| Spring Validation | — | Validación de requests |
| Spring Mail + Thymeleaf | — | Envío de correos con plantillas HTML |
| Spring WebSocket | — | Mensajería en tiempo real (STOMP) |
| libphonenumber | 8.13.40 | Validación y formateo de teléfonos |
| webp-imageio | 0.1.6 | Lectura de imágenes WebP |
| OpenPDF | 1.3.43 | Generación de informes PDF |
| JaCoCo | 0.8.12 | Cobertura de tests (mínimo 80% de líneas) |
| Maven Wrapper | — | Build reproducible sin Maven global |

### Frontend

| Tecnología | Versión | Uso |
|---|---|---|
| Vue | 3.5.x | Framework de UI |
| Vue Router | 4.6.x | Enrutado SPA y guardas por rol |
| Vite | 8.1.x | Bundler y dev server |
| Tailwind CSS | 4.3.x | Utilidades de estilos |
| Axios | 1.20.x | Cliente HTTP |
| `@stomp/stompjs` | 7.5.x | WebSocket sobre STOMP |
| `qrcode.vue` | 3.6.x | Códigos QR de certificados |
| `@lucide/vue` | 1.67.x | Iconografía |
| `libphonenumber-js` | 1.13.x | Validación de teléfonos en el cliente |
| Fontsource | — | Tipografías Bricolage Grotesque y DM Sans |

### Infraestructura y desarrollo

- **PostgreSQL** como base de datos.
- **Mailpit** (opcional) como servidor SMTP local con interfaz web.
- **GitHub Actions** para integración continua.
- **Túneles HTTPS** (ngrok, Cloudflare Tunnel o VS Code Dev Tunnels) para probar webhooks de MercadoPago en local.

---

## Arquitectura general

```text
Navegador (SPA Vue 3)
   │  HTTP/JSON + JWT                 WebSocket STOMP
   ▼                                      │
API REST Spring Boot ◄────────────────────┘
   │
   ├── Spring Data JPA ──► PostgreSQL (Flyway + Hibernate)
   ├── Almacenamiento público ──► uploads/
   ├── Almacenamiento privado ──► storage-privado/
   ├── MercadoPago ──► OAuth, checkout y webhooks
   ├── OpenTimestamps ──► sellado de lotes en Bitcoin
   ├── SMTP ──► Mailpit (desarrollo) o servidor real
   └── API de cotización ──► dolarapi.com
```

- El **frontend** es una SPA que consume la API con `axios`; en desarrollo, Vite hace proxy de `/api`, `/uploads` y `/ws` hacia `http://localhost:8080`.
- El **backend** es stateless: la sesión se representa con un JWT firmado que el frontend guarda en `localStorage`.
- Los **archivos públicos** (avatares, banners, portfolio, imágenes de catálogo) viven en `uploads/`; los **entregables y originales** viven en `storage-privado/` y se sirven mediante endpoints controlados y derivadas de preview.
- Los **correos** se renderizan con Thymeleaf y se envían por SMTP.
- La **mensajería** usa WebSocket/STOMP con autenticación por token.

---

## Estructura del repositorio

```text
temipora/
├── backend-springboot/                 # API REST (Spring Boot)
│   ├── src/main/java/com/temipora/backend_springboot/
│   │   ├── controladores/              # Endpoints REST por módulo
│   │   ├── servicios/                  # Lógica de negocio
│   │   │   ├── pagos/                  # Proveedores de pago (MercadoPago / simulado)
│   │   │   └── opentimestamps/         # Cliente y códec OpenTimestamps
│   │   ├── repositorios/               # Acceso a datos (Spring Data JPA)
│   │   ├── modelo/                    # Entidades, DTOs, enums y requests
│   │   ├── config/                    # Seguridad, JWT, WebSocket, seeders, errores
│   │   ├── eventos/                    # Eventos de dominio
│   │   └── tareas/                     # Jobs programados
│   ├── src/main/resources/
│   │   ├── application.properties      # Configuración principal
│   │   ├── application-local.properties# Overrides locales (ignorado por git)
│   │   ├── db/migration/               # Migraciones Flyway (V0_1 … V115)
│   │   ├── templates/                  # Plantillas de correo
│   │   └── static/
│   ├── src/test/java/                  # Tests unitarios, de integración y de queries
│   ├── uploads/                        # Archivos públicos (ignorado por git)
│   ├── storage-privado/                # Entregables y originales (ignorado por git)
│   ├── mvnw / mvnw.cmd                 # Maven Wrapper
│   └── pom.xml
├── temipora-front/                     # SPA (Vue 3 + Vite)
│   ├── src/
│   │   ├── components/                 # Componentes reutilizables
│   │   │   ├── admin/                  # Componentes del panel admin
│   │   │   ├── filtros/                # Filtros de catálogo
│   │   │   ├── icons/
│   │   │   └── variantes/              # Selector de variantes de producto
│   │   ├── views/                      # Páginas de la aplicación
│   │   ├── layouts/                    # Main, Admin, Artist, Client, Configuración
│   │   ├── composables/                # Lógica reutilizable (subidas, confirmaciones, etc.)
│   │   ├── servicios/                  # Clientes de API por módulo
│   │   ├── router/                     # Rutas y guardas por rol
│   │   ├── assets/                     # Imágenes, íconos y estilos
│   │   ├── App.vue
│   │   ├── main.js
│   │   └── style.css
│   ├── public/
│   ├── index.html
│   ├── vite.config.js                  # Proxy, alias y túneles permitidos
│   └── package.json
├── .github/workflows/maven.yml         # CI: tests + cobertura
└── README.md
```

---

## Prerrequisitos

| Herramienta | Versión | Observaciones |
|---|---|---|
| **JDK** | 21 | Necesario para compilar y ejecutar el backend. |
| **Maven** | 3.9+ | Opcional: el repositorio incluye `mvnw` / `mvnw.cmd`. |
| **Node.js** | `^22.18.0 \|\| >=24.12.0` | Ver `engines` en `temipora-front/package.json`. |
| **npm** | Incluido con Node | Instalación de dependencias del frontend. |
| **PostgreSQL** | 16 o superior | Base de datos `temipora` en `localhost:5432` (configurable). |
| **Mailpit** | Opcional | SMTP local en `localhost:1025` y UI en `localhost:8025`. |
| **Cuenta de MercadoPago** | Opcional | Solo para probar pagos reales/sandbox. Sin credenciales se usa el proveedor simulado. |

> No se requiere Docker para ejecutar el proyecto. Mailpit puede correrse con Docker si se prefiere.

---

## Variables de entorno

El backend lee variables de entorno al arrancar. Las obligatorias son las de base de datos y JWT; el resto tiene valores por defecto o es opcional según la funcionalidad que se quiera probar.

### Base de datos y seguridad (obligatorias)

| Variable | Descripción |
|---|---|
| `BD_USUARIO` | Usuario de PostgreSQL. |
| `BD_CONTRASENIA` | Contraseña de PostgreSQL. |
| `JWT_KEY` | Clave secreta para firmar los JWT. Usar una cadena larga y aleatoria. |
| `JWT_TIME` | Expiración del token en milisegundos (por ejemplo, `86400000` = 24 h). |

### Reservadas para almacenamiento S3

| Variable | Descripción |
|---|---|
| `AWS_ACCESS` | Access key de AWS. Reservada para un futuro storage S3; hoy el almacenamiento es local. |
| `AWS_KEY` | Secret key de AWS. Reservada para un futuro storage S3. |
| `AWS_BUCKET` | Nombre del bucket S3. Reservada para un futuro storage S3. |

> La configuración actual referencia estas variables, pero el código activo usa almacenamiento local. Se pueden definir con valores dummy (`local`) para evitar placeholders sin resolver.

### Administrador inicial

| Variable | Descripción |
|---|---|
| `ADMIN_CORREO` | Correo del administrador inicial. |
| `ADMIN_CONTRASENIA` | Contraseña del administrador inicial. |
| `ADMIN_NOMBRE_USUARIO` | Nombre de usuario del administrador inicial. |

> `RolSeeder` crea automáticamente los roles `CLIENTE`, `ARTISTA` y `ADMIN` al arrancar. `AdminSeeder` solo crea el usuario administrador si las tres variables `ADMIN_*` están definidas y todavía no existe ningún admin. Si no se definen, el sistema arranca sin administrador.

### Aplicación

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `APP_FRONTEND_URL` | `http://localhost:5173` | URL del frontend usada en retornos de pago, correos y certificados. |
| `TEMIPORA_CONTACTO_EMAIL` | `temiporasupport@gmail.com` | Correo de contacto público de la plataforma. |
| `TEMIPORA_REACTIVACION_PROGRAMADA` | `true` | Habilita el envío diario de promociones de reactivación. |

### Correo

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `TEMIPORA_MAIL_HOST` | `localhost` | Host SMTP. |
| `TEMIPORA_MAIL_PORT` | `1025` | Puerto SMTP (Mailpit). |
| `TEMIPORA_MAIL_USERNAME` | vacío | Usuario SMTP. |
| `TEMIPORA_MAIL_PASSWORD` | vacío | Contraseña SMTP. |
| `TEMIPORA_MAIL_AUTH` | `false` | Habilita autenticación SMTP. |
| `TEMIPORA_MAIL_STARTTLS` | `false` | Habilita STARTTLS. |
| `TEMIPORA_MAIL_HABILITADO` | `true` | Permite desactivar el envío de correos. |
| `TEMIPORA_MAIL_FROM` | `no-responder@temipora.local` | Remitente. |
| `TEMIPORA_MAIL_FROM_NAME` | `Temipora` | Nombre del remitente. |

### MercadoPago

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `MP_ACCESS_TK` | vacío | Access token de MercadoPago. |
| `TEMIPORA_MP_CLIENT_ID` | vacío | Client ID de la aplicación OAuth. |
| `TEMIPORA_MP_CLIENT_SECRET` | vacío | Client secret de la aplicación OAuth. |
| `MP_PUBLIC_KEY` | vacío | Public key de MercadoPago. |
| `PAGOS_TOKEN_KEY` | vacío | Clave AES-256 en Base64 para cifrar tokens de MercadoPago. |
| `MP_NOTIFICATION_URL` | vacío | URL pública del webhook de pagos. |
| `TEMIPORA_WEBHOOK` / `MP_WEBHOOK_SECRET` | vacío | Secreto para validar la firma de los webhooks. |
| `MP_SANDBOX` | `false` | Activa el entorno sandbox. |
| `MP_BINARY_MODE` | `false` | Aprueba o rechaza pagos sin estados intermedios. |
| `MP_COMISION_SANDBOX` | `false` | Envía `marketplace_fee` también en sandbox. |
| `MP_OAUTH_TEST_TOKEN` | valor de `MP_SANDBOX` | Pide un token de prueba en el canje OAuth. |
| `MP_ARANCEL_ESTIMADO` | `3.49` | Arancel estimado de MercadoPago (porcentaje). |
| `MP_ARANCEL_IVA` | `21` | IVA del arancel (porcentaje). |
| `MP_OAUTH_URL` | `https://auth.mercadopago.com.ar/authorization` | URL de autorización OAuth. |

> El proveedor de pagos se elige con la propiedad `pagos.proveedor` (`simulado` por defecto, `mercadopago` para el proveedor real). En un clon limpio, `pagos.proveedor=simulado`; el archivo local ignorado por git puede sobrescribirlo.

### Anclaje en Bitcoin

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `TEMIPORA_ANCLAJE_HABILITADO` | `true` | Habilita el job de anclaje del ledger. |
| `TEMIPORA_ANCLAJE_CALENDARIO` | Calendarios públicos de OpenTimestamps | URLs separadas por coma. |
| `TEMIPORA_ANCLAJE_LOTE` | `500` | Cantidad máxima de eventos por lote. |

### Ejemplo para desarrollo local

**PowerShell**

```powershell
$env:BD_USUARIO = "postgres"
$env:BD_CONTRASENIA = "postgres"
$env:JWT_KEY = "cambiar-por-una-clave-larga-y-aleatoria-de-32-o-mas-caracteres"
$env:JWT_TIME = "86400000"
$env:AWS_ACCESS = "local"
$env:AWS_KEY = "local"
$env:AWS_BUCKET = "local"
$env:ADMIN_CORREO = "admin@temipora.local"
$env:ADMIN_CONTRASENIA = "admin1234"
$env:ADMIN_NOMBRE_USUARIO = "admin"
```

## Base de datos y migraciones

- Motor: **PostgreSQL**.
- URL por defecto: `jdbc:postgresql://localhost:5432/temipora`.
- Esquema: se construye con **Flyway** (`src/main/resources/db/migration`) y se completa con `spring.jpa.hibernate.ddl-auto=update`.
- `spring.flyway.baseline-on-migrate=true`, `baseline-version=0` y `out-of-order=true` permiten aplicar migraciones base después de otras ya aplicadas.
- Las migraciones aplicadas **no deben modificarse**: cualquier cambio de esquema se agrega como una migración nueva.


## Cómo ejecutar el proyecto

### 1. Base de datos

Crear la base `temipora` y verificar que PostgreSQL esté escuchando en `localhost:5432`.

### 2. Backend

**Windows (PowerShell)**

```powershell
cd backend-springboot
.\mvnw.cmd spring-boot:run
```

El backend queda disponible en **`http://localhost:8080`**. Al arrancar, Flyway aplica las migraciones pendientes y los seeders crean los roles y el administrador inicial si corresponde.

### 3. Frontend

```bash
cd temipora-front
npm install
npm run dev
```

El dev server queda en **`http://localhost:5173`**. Vite reenvía `/api`, `/uploads` y `/ws` a `http://localhost:8080`, por lo que no hay que configurar URLs del backend en el frontend.

### 4. Servidor de correo local (opcional)

Con Docker:

```bash
docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit
```

- SMTP: `localhost:1025`
- Interfaz web: `http://localhost:8025`

Si no se levanta Mailpit, los envíos de correo no van a poder entregarse. Se pueden desactivar por completo con `TEMIPORA_MAIL_HABILITADO=false`.

### 5. Pagos (opcional)

Sin credenciales, el backend usa el proveedor **simulado**, suficiente para probar el flujo de compra y de presupuestos. Para usar MercadoPago:

1. Definir las variables `MP_*` y `TEMIPORA_MP_*`.
2. Configurar `pagos.proveedor=mercadopago` en `application-local.properties`.
3. Exponer el backend o el frontend con un túnel HTTPS y configurar `MP_NOTIFICATION_URL` con la URL pública del webhook.
4. Para el OAuth de artistas, configurar la URL de retorno en la aplicación de MercadoPago.

Los orígenes permitidos por CORS son `localhost:5173`, `*.ngrok-free.dev`, `*.ngrok-free.app`, `*.trycloudflare.com` y `*.devtunnels.ms`.

### Build de producción

**Backend**

```powershell
cd backend-springboot
.\mvnw.cmd clean package
java -jar target/backend-springboot-0.0.1-SNAPSHOT.jar
```

**Frontend**

```bash
cd temipora-front
npm run build
```

El build queda en `temipora-front/dist/`. Para previsualizarlo:

```bash
npm run preview
```

También existe `npm run demo`, que compila y sirve el build estático en el puerto `5173` (útil para demos con túnel, sin HMR).

---

## API REST

La API se organiza por módulos. Todas las rutas usan el prefijo `/api`.

| Módulo | Rutas base | Acceso |
|---|---|---|
| Autenticación | `/api/auth/login`, `/api/auth/registrarse`, `/api/auth/acceso`, `/api/auth/reactivar-cuenta` | Público |
| Disponibilidad de usuario | `/api/checkNombreUsuario` | Público |
| Subida de archivos | `/api/upload` | Público |
| Usuarios y perfiles | `/api/usuarios`, `/api/perfil` | Autenticado / público según endpoint |
| Portfolio | `/api/portfolio` | Público / artista para escritura |
| Galería pública | `/api/galeria` | Público |
| Colección | `/api/coleccion` | Autenticado |
| Servicios | `/api/servicios` | Público / artista para gestión |
| Solicitudes de servicio | `/api/solicitudes-servicio` | Cliente / artista |
| Presupuestos | `/api/solicitudes-servicio/{id}/presupuesto` | Artista / cliente |
| Pedidos | `/api/pedidos` | Cliente / artista / admin |
| Productos | `/api/productos` (catálogo público), `/api/mis-productos` (gestión) | Público / artista |
| Carrito | `/api/carrito` | Cliente / artista |
| Compras | `/api/compras` | Cliente / artista |
| Pagos | `/api/pagos`, `/api/pagos/simulado`, `/api/pagos/mercadopago/**` | Autenticado / webhooks públicos |
| Reventas | `/api/reventas` | Público / cliente |
| Certificados | `/api/certificados` | Público |
| Reseñas | `/api/resenias` | Público / autenticado |
| Favoritos | `/api/favoritos` | Autenticado |
| Mensajería | `/api/conversaciones`, `/ws` | Autenticado |
| Notificaciones | `/api/notificaciones` | Autenticado |
| Etiquetas | `/api/etiquetas`, `/api/etiquetas/categorias` | Público / admin / artista |
| Catálogos maestros | `/api/tipos-producto`, `/api/tipos-servicio`, `/api/medios`, `/api/tipos-arte`, `/api/materiales`, `/api/paises`, `/api/monedas` | Público / admin |
| Términos | `/api/terminos-plataforma`, `/api/usuarios/{id}/terminos` | Público / autenticado |
| Comisiones | `/api/comisiones` | Público / admin |
| Sustituciones | `/api/sustituciones` | Público / autenticado |
| Ofertas | `/api/ofertas` | Público / cliente |
| Administración | `/api/admin/**` | `ADMIN` |

> La autorización fina se implementa con `@PreAuthorize` en los controladores. Los endpoints públicos están declarados explícitamente en `SecurityConfig`.

---

## Autoría

Desarrollado por **Menacho, Natalia Agustina** para el Trabajo Final de la carrera **Analista en Sistemas de Computación**, **Universidad Nacional de Misiones**.
