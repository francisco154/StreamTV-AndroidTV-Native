# StreamTV - Android TV Native App

App nativa de Android TV para streaming de radios en vivo y demos musicales.

## Características

- **Reproductor nativo** con ExoPlayer (Media3) - soporta MP3, AAC, HLS, SHOUTcast, Icecast
- **UI optimizada para TV** con AndroidX Leanback y navegación D-pad
- **Login seguro** con credenciales hardcoded
- **Carga dinámica** de contenido desde JSON API
- **Caché de imágenes** con Glide + OkHttp (maneja redirecciones ibb.co, Telegram)
- **Now Playing bar** con control de reproducción
- **Pantalla de reproductor** con cover art, descripción, y controles

## Datos

La app carga su contenido desde:
`https://demotester-v2.vercel.app/api/export/categories.json`

### Categorías soportadas:
- **Radios en Vivo** (22 emisoras) - MP3, AAC, HLS, SHOUTcast
- **Demos** (10 canciones) - Audio files con metadata (artista, mood, tags)

### Emisoras problemáticas que SÍ funcionan:
- Radio Continental (MP3)
- FM Del Sol (AAC / Icecast HTTP/1.0 legacy)
- Radio Universidad (MP3 / puerto no-estándar)
- Radio Sensation (AAC+ / SHOUTcast `;stream.nsv`)

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk-21
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Tech Stack

- Java 11 + Android SDK 36
- ExoPlayer Media3 1.6.0
- AndroidX Leanback
- OkHttp 4.12.0
- Gson 2.11.0
- Glide 4.16.0 + OkHttp integration

## Autor

Cervinof017@gmail.com

## Versión

v2.0 - Native Android TV App
