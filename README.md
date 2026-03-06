# QuickPart (PI2DAM)

Aplicación Android para la gestión de inventario y pedidos a proveedores, pensada para talleres/almacenes pequeños.  
Incluye control de usuarios, stock, pedidos, chat interno y exportación de informes a PDF, todo integrado con Firebase.

---

## Tabla de contenidos

- [Características principales](#características-principales)  
- [Tecnologías](#tecnologías)  
- [Arquitectura funcional](#arquitectura-funcional)
  - [Roles y permisos](#roles-y-permisos)
  - [Módulos de la app](#módulos-de-la-app)
- [Modelo de datos en Firestore](#modelo-de-datos-en-firestore)
- [Reglas de seguridad](#reglas-de-seguridad)
- [Puesta en marcha](#puesta-en-marcha)
- [Estado del proyecto](#estado-del-proyecto)

---

## Características principales

- Autenticación con **Firebase Authentication** (Email/Password).
- Control de accesos con dos roles:
  - **admin** (máx. 2 activos)
  - **worker** (empleado, máx. 10 activos)
- Gestión de:
  - **Usuarios** (CRUD, solo admin).
  - **Productos** (CRUD, stock, precio, umbral de stock bajo).
  - **Proveedores** (CRUD, con desvinculación automática de productos al borrarlos).
- **Pedidos a proveedores**:
  - Creación de pedidos sobre stock disponible.
  - Descuento automático de stock al crear.
  - Devolución de stock al **cancelar** pedidos.
- **Dashboard**:
  - Resumen mensual de inversión, unidades, pedidos y productos más vendidos.
- **Exportación a PDF**:
  - Informe de stock actual.
  - Informe de pedidos del mes.
- **Chat de stock** (en tiempo real, con Firestore):
  - Canal `chats/stock` para comunicación interna del equipo.
- **Mapa de almacenes**:
  - Google Maps con almacenes de ejemplo (Madrid, Barcelona, Valencia).
- Pantalla de **Ayuda** con FAQs, buscador y acceso rápido a secciones clave.

---

## Tecnologías

- **Lenguaje:** Kotlin
- **Android:** minSdk 24, target/compileSdk 36
- **UI:** AndroidX, Material Components
- **Backend (BaaS):** Firebase
  - Authentication
  - Cloud Firestore
- **Otros:**
  - Google Maps SDK for Android
  - Generación de PDF con `PdfDocument`

---

## Arquitectura funcional

### Roles y permisos

La app maneja dos tipos de usuario (colección `users`):

- `admin`
  - Puede gestionar usuarios (CRUD).
  - Puede gestionar proveedores (CRUD).
  - Puede gestionar productos y pedidos.
  - Puede cancelar cualquier pedido.
- `worker`
  - Puede usar la app (Home, productos, pedidos, dashboard, chat, mapa, ayuda).
  - Sin acceso a la sección de usuarios.
  - Sin permisos de administración sobre proveedores.

Límites (implementados en `PiRepository`):

- Hasta **2 usuarios admin activos**.
- Hasta **10 usuarios worker activos**.

### Módulos de la app

#### Autenticación

- **Login (`MainActivity`)**
  - Inicio de sesión con email y contraseña (Firebase Auth).
  - Tras login, se valida que exista un perfil activo en `users/{uid}`:
    - Si no existe o está inactivo → “No autorizado”.
    - Si es válido → se guarda en `Session` y se navega a **Home**.
  - Accesos a:
    - Registro (`RegisterActivity`)
    - Restablecer contraseña (`ResetPasswordActivity`)
    - Ayuda (`HelpActivity`)

- **Registro (`RegisterActivity`)**
  - Crea usuario en Firebase Auth.
  - Llama a `createEmployeeProfileWithLimits` para crear el perfil en `users` respetando límites:
    - Primeros usuarios cubren plazas de admin (hasta 2).
    - Resto se crean como worker (hasta 10).
  - Si no hay “plazas” libres, revierte el usuario de Auth.

- **Restablecer contraseña (`ResetPasswordActivity`)**
  - Si NO hay sesión:
    - Envía email de restablecimiento con `sendPasswordResetEmail`.
  - Si hay sesión:
    - Permite cambiar la contraseña directamente con `updatePassword`.

#### Navegación y menú

- **AppMenu + AppMenuSheet**
  - Icono tipo “hamburguesa” en la barra superior que abre un bottom sheet con accesos a:
    - Home, Usuarios, Productos, Proveedores, Dashboard, Chat, Pedidos, Pago, Mapa, Ayuda, Cerrar sesión.
  - Ajusta automáticamente lo que se muestra según rol y si hay sesión activa.

- **MenuActivity**
  - Pantalla alternativa con botones grandes a las mismas secciones.
  - El botón de Usuarios solo es visible para admin.

#### Página principal (Home)

- **HomeActivity**
  - Muestra:
    - Saludo personalizado (nombre + rol + fecha).
    - Resumen de inventario en tiempo real (nº productos, unidades, productos con stock bajo, valor total).
    - Estadísticas del mes en curso:
      - Inversión total, unidades totales, número de pedidos y cancelados.
      - Top 5 productos más vendidos (por unidades).
  - Buscador rápido de productos que abre `ProductsActivity` con la búsqueda aplicada.

#### Gestión de usuarios (solo admin)

- **UsersActivity**
  - Lista todos los perfiles de `users`.
  - Solo accesible si el usuario actual es admin.
  - Botón “Crear usuario” → `UserFormActivity`.

- **UserFormActivity**
  - Alta:
    - Crea usuario en Firebase Auth usando un `FirebaseApp` secundario.
    - Crea perfil en `users` respetando límites de rol.
  - Edición:
    - Permite cambiar nombre, rol (dentro de límites) y estado activo.
  - Borrado:
    - Elimina el **perfil** de Firestore (`users/{uid}`); la cuenta de Auth se gestiona desde la consola Firebase.

#### Gestión de productos

- **ProductsActivity**
  - Listener en `products` y `suppliers`.
  - Filtros y ordenaciones:
    - Texto (nombre, SKU, ubicación, proveedor).
    - Stock (todos / solo stock bajo).
    - Proveedor (todos / con proveedor / sin proveedor).
    - Orden (nombre, precio asc/desc).
  - Acciones:
    - Crear producto (`ProductFormActivity`).
    - Editar producto (`ProductFormActivity` con ID).
    - Exportar **stock actual a PDF**.

- **ProductFormActivity**
  - Formulario de creación/edición de productos:
    - Nombre, SKU, ubicación, proveedor, stock, precio, umbral de stock bajo.
  - Guarda con `PiRepository.upsertProduct`.
  - Borrado con `PiRepository.deleteProduct`.

- **ProductActivity**
  - Pantalla sencilla de ejemplo para una pieza concreta (no ligada al CRUD principal).

#### Gestión de proveedores

- **SuppliersActivity**
  - Lista de proveedores (`suppliers`), ordenada alfabéticamente.
  - Acciones:
    - Crear proveedor (`SupplierFormActivity`).
    - Editar proveedor (`SupplierFormActivity` con ID).

- **SupplierFormActivity**
  - Creación/edición de un proveedor (nombre obligatorio).
  - Borrado con `PiRepository.deleteSupplier`:
    - Limpia `supplierId` en todos los productos que lo usaban antes de eliminarlo.

#### Pedidos a proveedores

- **OrdersActivity**
  - Lista todos los pedidos en `orders`, ordenados por fecha.
  - Muestra estado, nº de líneas, fecha, etc.
  - Botones:
    - Crear pedido (`CreateOrderActivity`).
    - Exportar pedidos del mes a PDF.
  - Cancelar pedido:
    - Llama a `PiRepository.cancelOrder`:
      - Marca el pedido como `CANCELLED`.
      - Devuelve stock a cada producto implicado.

- **CreateOrderActivity**
  - Carga todos los productos con `stock > 0`.
  - El usuario elige cantidades para cada producto.
  - Al enviar:
    - Crea un documento en `orders` con `items` y `createdByUid`.
    - Descuenta stock de cada producto en una transacción Firestore.

#### Dashboard

- **DashboardActivity**
  - Resumen mensual de pedidos:
    - Inversión total, unidades totales, nº de pedidos y cancelados.
    - Top 5 productos más vendidos con porcentaje relativo.

#### Chat de stock

- **ChatActivity**
  - Canal único `chats/stock/messages`.
  - Muestra últimos mensajes en tiempo real.
  - Cada mensaje contiene:
    - UID, nombre y rol del remitente.
    - Texto y fecha/hora.
  - Solo empleados activos pueden leer y enviar mensajes.

#### Mapa de almacenes

- **WarehousesMapActivity**
  - Mapa de Google con marcadores de almacenes de ejemplo:
    - Madrid, Barcelona, Valencia.
  - Ajusta la cámara para encuadrar todos los almacenes.

#### Ayuda y soporte

- **HelpActivity**
  - Buscador de FAQs (login, contraseña, productos, pedidos, usuarios, mapa).
  - Tarjetas de preguntas frecuentes expandibles.
  - Acciones rápidas:
    - Restablecer contraseña.
    - Ir a productos (solo si hay sesión válida).
    - Abrir menú principal.
  - Botón “Contactar soporte”:
    - Abre cliente de correo con email, asunto y cuerpo preconfigurados.
    - Si no hay app de correo, copia el email al portapapeles.

---

## Modelo de datos en Firestore

Colecciones principales:

1. `users/{uid}`
   - `email: string`
   - `name: string`
   - `role: "admin" | "worker"`
   - `active: boolean`
   - `createdAt: timestamp`
   - `updatedAt: timestamp`

2. `products/{productId}`
   - `name: string`
   - `sku: string`
   - `location: string`
   - `supplierId: string` (vacío si no tiene proveedor)
   - `stock: number`
   - `price: number`
   - `lowStockThreshold: number`
   - `updatedAt: timestamp`

3. `suppliers/{supplierId}`
   - `name: string`
   - `updatedAt: timestamp`

4. `orders/{orderId}`
   - `status: "CREATED" | "CANCELLED"`
   - `createdByUid: string`
   - `items: [ { productId, qty, priceSnapshot } ]`
   - `createdAt: timestamp`
   - `cancelledAt: timestamp | null`

5. `chats/{chatId}/messages/{msgId}`
   - `senderUid: string`
   - `senderName: string`
   - `senderRole: string`
   - `text: string`
   - `createdAt: timestamp`

Para una descripción paso a paso de la configuración de Firebase desde cero, se incluye el archivo `GUIA_FIREBASE_PI2DAM.txt` en el proyecto.

---

## Reglas de seguridad

En el archivo `firestore.rules` se definen funciones auxiliares y permisos:

- `signedIn()`, `myUid()`, `getProfileData()`
- `isActiveEmployee()` → usuario autenticado con perfil `active = true`.
- `isAdmin()` → empleado activo con `role = 'admin'`.

Resumen de permisos:

- `users`:
  - Leer: cualquier usuario autenticado.
  - Crear: uno mismo tras registrarse o admin.
  - Actualizar/borrar: solo admin.
- `products`:
  - Leer/crear/editar/borrar: empleados activos.
- `suppliers`:
  - Leer: empleados activos.
  - Crear/editar/borrar: admin.
- `orders`:
  - Leer/crear: empleados activos.
  - Actualizar (cancelar): admin o creador del pedido.
  - Borrar: solo admin.
- `chats/*/messages/*`:
  - Leer/crear: empleados activos.
  - Borrar: admin.
  - Update deshabilitado (historial inmutable).

---

## Puesta en marcha

1. **Configurar Firebase**
   - Crear proyecto en Firebase Console.
   - Activar **Authentication** (Email/Password).
   - Crear base de datos **Cloud Firestore**.
2. **Registrar la app Android en Firebase**
   - Package name: `com.example.pi2dam`.
   - Descargar `google-services.json` y colocarlo en `app/google-services.json`.
3. **Aplicar reglas de Firestore**
   - Copiar el contenido de `firestore.rules` al apartado “Rules” en la consola de Firebase.
4. **Ejecutar la app**
   - Abrir el proyecto en Android Studio.
   - Sincronizar Gradle.
   - Ejecutar en un dispositivo/emulador (API 24 o superior).
5. **Crear usuarios iniciales**
   - Registrar usuarios desde la app:
     - Los primeros cubrirán plazas de admin (hasta 2).
   - A partir de ahí, crear el resto de empleados desde la sección **Usuarios** (solo admin).

---

## Estado del proyecto

Proyecto desarrollado como **PI de 2º de DAM** por AARON CASADO, BEATRIZ CAÑIZARES, JAVIER GONZALEZ Y HAOWEN HUANG
Se centra en la funcionalidad y en la integración con Firebase; la lógica de negocio y la estructura están pensadas para ser fácilmente ampliables (más roles, más validaciones, informes adicionales, etc.).
