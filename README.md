# ROLAYS — Liquid Glass AI Studio (Android / Kotlin + Jetpack Compose)

ROLAYS adalah aplikasi seluler Android berbasis **Kotlin & Jetpack Compose (Material 3)** dengan estetika **Liquid Glass** (frosted translucency, refraksi cahaya, dan navigasi gestur cair interaktif) serta integrasi **Firebase AI Logic (Gemini 3.8 Flash & Nano Banana 2.1)**.

## Fitur Utama

1. **Interactive Liquid Glass UI & Fluid Gesture Navigation Bar**
   - Tampilan kaca cair transparan berlapis gradien spekular, rim highlight, dan latar belakang kaustik animasi.
   - **Press-and-Hold Fluid Drag Bar** (`InteractiveLiquidGlassNavBar.kt`): Pengguna dapat mengetuk tab atau **menahan (press-and-hold) lalu menggeser** bar secara real-time dengan respons droplet elastis, peregangan kecepatan (*velocity stretch*), dan umpan balik haptik, serta adaptasi otomatis ke `LiquidGlassNavRail` pada layar lebar/tablet.

2. **Peningkat Foto & Video AI (HD Enhancer)**
   - Pembanding interaktif **Sebelum & Sesudah (Before/After Slider)** yang dapat digeser langsung.
   - Pemrosesan gambar lokal berkecepatan tinggi (`ImageEnhancementEngine.kt`: upscaling HD, *unsharp-mask sharpening*, matriks warna kontras/kecerahan/saturasi/kehangatan, vignette).
   - **AI Auto-HD** menggunakan `gemini-3.8-flash` (structured JSON `Schema.obj`) serta **AI Restorasi** menggunakan `gemini-nano-banana-2.1`.
   - Dukungan pemilihan foto & video tanpa izin penyimpanan luas melalui **Android Photo Picker** (`PickVisualMedia`) dan ekspor langsung ke `Pictures/ROLAYS`.

3. **Downloader Media Tanpa Watermark**
   - Ekstraksi metadata tautan **TikTok** & **YouTube** (`MediaDownloaderRepository.kt`) dengan fitur **Tempel (Paste)** satu ketuk, pemilihan kualitas (1080p Full HD, 720p HD, Audio MP3), dan penyimpanan ke `Downloads/ROLAYS`.
   - Integrasi **Gemini AI Caption & Hashtag Generator** untuk konten yang diambil.

4. **Chatbot AI Universal untuk Media Sosial**
   - Asisten AI lintas platform (**Instagram, TikTok, WhatsApp, X / Twitter**) dengan streaming respons real-time, lampiran gambar multimodal, serta tombol **Salin** dan **Bagikan**.

5. **Modul Khusus Independen "Gemini 3.8 Flash AI"**
   - Tab, `ViewModel`, konfigurasi sistem, dan sesi multi-turn `Chat` yang **dipisahkan secara tegas** dari fitur lainnya (`feature/geminiflash/`) untuk performa dan fokus maksimal.

## Struktur Direktori Utama

```text
app/src/main/java/com/example/
├── MainActivity.kt
├── core/
│   └── AppCheckHelper.kt
├── ui/
│   ├── RolaysApp.kt
│   ├── components/
│   │   ├── InteractiveLiquidGlassNavBar.kt
│   │   └── LiquidGlassSurface.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── feature/
    ├── enhancer/
    │   ├── EnhancerScreen.kt
    │   ├── EnhancerViewModel.kt
    │   ├── ImageEnhancementEngine.kt
    │   └── MediaEnhancerModels.kt
    ├── downloader/
    │   ├── DownloaderScreen.kt
    │   ├── DownloaderViewModel.kt
    │   └── MediaDownloaderRepository.kt
    ├── socialchat/
    │   ├── SocialAiScreen.kt
    │   └── SocialAiViewModel.kt
    └── geminiflash/
        ├── GeminiFlashScreen.kt
        └── GeminiFlashViewModel.kt
```
