[README.md](https://github.com/user-attachments/files/24629948/README.md)
# App de Control de Hipertensión 🩺💙

Este proyecto es una aplicación Android nativa desarrollada en Kotlin para ayudar a pacientes con hipertensión a llevar un control detallado de su salud. Fue creada con el objetivo de facilitar el seguimiento médico y mejorar la calidad de vida del usuario.

## 🚀 Funcionalidades Principales

* **Recordatorios de Medicamentos:** Sistema robusto de alarmas (locales y persistentes al reinicio) para no olvidar ninguna dosis.
* **Registro de Síntomas:** Catálogo visual para reportar malestares (mareos, dolor de cabeza, etc.).
* **Bitácora de Presión:** Historial de mediciones.
* **Guía de Alimentación:** Base de datos de alimentos recomendados y prohibidos.
* **Perfil del Paciente:** Datos generales y de contacto.

## 🛠️ Requisitos Técnicos

* Android Studio (Última versión recomendada, Koala o superior).
* Kotlin 1.9+.
* Dispositivo físico o emulador con Android 8.0 (Oreo) o superior.

## ⚙️ Configuración Inicial (IMPORTANTE)

Este proyecto utiliza **Firebase** para la autenticación y base de datos. Por seguridad, el archivo de configuración `google-services.json` **NO** está incluido en este repositorio. Debes generar el tuyo propio para que la app funcione.

### Pasos para compilar:

1.  **Crea tu proyecto en Firebase:**
    * Ve a [Firebase Console](https://console.firebase.google.com/).
    * Crea un nuevo proyecto llamado "HipertensionApp".

2.  **Agrega la App Android:**
    * Registra una nueva app con el nombre de paquete: `com.example.apphipertension` (Revisa el `build.gradle` o `AndroidManifest.xml` para confirmar el paquete exacto).
    * Descarga el archivo `google-services.json`.

3.  **Instala la llave:**
    * Copia el archivo `google-services.json` que descargaste.
    * Pégalo dentro de la carpeta `app/` de este proyecto (al mismo nivel que `src` y `build.gradle`).

4.  **Habilita los servicios en Firebase:**
    * **Authentication:** Habilita el método de "Email/Password".
    * **Firestore Database:** Crea una base de datos.
    * **Reglas de Firestore:** Configura las reglas de seguridad para permitir lectura/escritura a usuarios autenticados.

### 🍎 Importar la Base de Datos de Alimentos

La app necesita una lista de alimentos para funcionar correctamente en la sección de dieta.
1.  Busca el archivo `alimentos_data.json` incluido en la carpeta raíz de este repositorio.
2.  Deberás importar estos datos a una colección en Firestore llamada exactamente: `alimentos_mexico`.
    * *Tip:* Puedes usar un script o ingresarlos manualmente si son pocos, respetando la estructura del JSON.

### 📊 Índices Compuestos de Firestore (Requeridos)

Para que los historiales y filtros funcionen sin errores, es **obligatorio** crear los siguientes índices en tu consola de Firebase (Sección Firestore > Índices). Si no los creas, la app se cerrará al intentar consultar estas listas.

| Colección ID | Campos indexados (Orden es importante) | Ámbito |
| :--- | :--- | :--- |
| `actividades_fisicas` | `fecha` (Desc), `hora` (Desc) | Colección |
| `actividades_fisicas` | `fecha` (Asc), `hora` (Asc) | Colección |
| `mediciones` | `date` (Desc), `time` (Desc) | Colección |
| `medicamentos` | `fecha` (Asc), `hora` (Asc) | Colección |
| `sintomas` | `fecha` (Desc), `hora` (Desc) | Colección |
| `sintomas` | `fecha` (Asc), `hora` (Asc) | Colección |

## 📄 Notas para Desarrolladores

El proyecto está diseñado para ser retomado y mejorado. Algunas áreas de oportunidad son:
* Mejorar la interfaz de usuario (UI/UX).
* Implementar notificaciones push remotas.
* Agregar gráficas de evolución de presión arterial.

---
*Proyecto liberado con fines educativos y de salud.*
