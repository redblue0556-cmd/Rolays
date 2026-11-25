# ROLAYS — Liquid Glass AI Studio (Android / Kotlin + Jetpack Compose)

ROLAYS adalah aplikasi seluler Android berbasis **Kotlin & Jetpack Compose (Material 3)** dengan estetika **Liquid Glass** (frosted translucency, refraksi cahaya, border spekular, dan navigasi gestur cair interaktif) serta integrasi **Firebase AI Logic (Gemini 3.8 Flash & Nano Banana 2.1)**.

---

## 1. Arsitektur Informasi & User Flow ("Pilih Chatbot Medsos")

```text
[Tab AI Chatbot]
       │
       ▼
[SocialPlatformPickerScreen] (Grid 3 Kolom x 4 Baris + Search + Universal AI)
  ├── Bilah Pencarian ("Cari platform medsos...") -> Filter real-time seluruh katalog
  ├── 12 Kartu Platform Utama (TikTok, Instagram, WhatsApp, X, YouTube, Facebook,
  │    LinkedIn, Telegram, Threads, Pinterest, Reddit, Discord)
  ├── Kartu Lebar "Semua Medsos (Universal AI)"
  └── "Lihat platform lainnya..." (Snapchat, Twitch, LINE)
       │  (Tap kartu -> spring press scale 0.94f + ripple -> slide transition)
       ▼
[SocialPlatformChatRoom] (Ruang Obrolan Spesifik Platform)
  ├── Header platform + tombol kembali (<) ke Grid 3x4 (plus BackHandler)
  ├── Quick Prompt Chips khusus platform terpilih (Hook FYP, Utas X, Balas WA, dll.)
  ├── Riwayat obrolan terpisah per-platform (conversationsByPlatform[platform.id])
  └── Lampiran Foto (Photo Picker) + Streaming Gemini 3.8 Flash + Salin & Bagikan
```

---

## 2. Spesifikasi Desain UI/UX (Liquid Glass Tokens)

| Token | Nilai | Penggunaan |
|---|---|---|
| Latar Belakang Utama | `#060914` -> `#0D1429` | Midnight-indigo & deep violet background + `img_liquid_aura_bg` |
| Aksen Cyan Cair | `#3CEBFF` | Highlight refraktif, indikator aktif, label Gemini |
| Aksen Biru Glasial | `#4F86FF` | Aksen sekunder, tombol aksi utama |
| Aksen Violet Prisma | `#9E7BFF` | Gradien kartu Universal AI & aura ambien |
| Permukaan Kaca | `#FFFFFF` (`12%–20%` alpha) + `#0A1020` (`72%–86%` alpha) | Frosted glass gradient fill |
| Border Spekular | `#FFFFFF` (`36%–68%` alpha) -> `#3CEBFF` (`20%–90%` alpha) | Rim catchlight tipis `1dp–1.5dp` |
| Backdrop Blur | `22.dp` (API 31+ `Modifier.blur`) | Efek frosted glass pada bottom bar & latar aura |
| Tipografi | `Space Grotesk` (Display), `Plus Jakarta Sans` (UI/Body), `JetBrains Mono` (Badge/Telemetry) | Bundled lokal di `res/font/` |

---

## 3. Implementasi Navigasi Gestur Press-and-Drag (`InteractiveLiquidGlassNavBar.kt`)

- **Tap Cepat**: Memindahkan droplet kaca cair menggunakan `spring(dampingRatio = 0.56f, stiffness = Spring.StiffnessMediumLow)` langsung ke tab tujuan (`Enhance HD`, `Downloader`, `AI Chatbot`, `Gemini 3.8`).
- **Press-and-Hold (`>= 160ms`) + Drag**:
  - Mengaktifkan *Fluid Drag Mode* (`isFluidDragging = true`) lengkap dengan `HapticFeedbackType.LongPress`, skala bar `1.04f`, dan pembesaran droplet `1.12f`.
  - Droplet bergerak kontinu mengikuti jari (`Animatable.snapTo`), meregang horizontal sesuai kecepatan geser (`dragVelocityStretch`), dan menampilkan tooltip kapsul nama tab yang sedang disorot.
  - Memberikan efek tarik vertikal (*dampedY*) dan elastisitas tepi (*overscrollX*) sebelum memantul kembali dengan `spring()` saat jari dilepas.

---

## 4. Integrasi API & Pemisahan Modul

- **HD Enhancer (`feature/enhancer/`)**: Pemrosesan lokal `ImageEnhancementEngine` (upscaling HD, sharpening 3x3, `ColorMatrix`, vignette), slider **Before/After** berbasis `drawWithContent { clipRect(...) }`, **AI Auto-HD** via `gemini-3.8-flash` (structured JSON `Schema.obj`), dan **AI Restorasi** via `gemini-nano-banana-2.1`.
- **Media Downloader (`feature/downloader/`)**: Ekstraksi metadata TikTok & YouTube melalui `OkHttpClient`, resolusi stream TikTok tanpa watermark, penyimpanan scoped `MediaStore.Downloads`, dan generator caption AI.
- **Social AI Chatbot (`feature/socialchat/`)**: Katalog 12+ platform media sosial (`SocialPlatformCatalog.kt`) dengan `SocialPlatformPickerScreen` 3x4 dan ruang chat terpisah per-platform via `Firebase.ai.generativeModel("gemini-3.8-flash")`.
- **Modul Independen Gemini 3.8 Flash (`feature/geminiflash/`)**: Tab ke-4 dengan `GeminiFlashViewModel`, `GenerativeModel`, mode engine, instruksi sistem kustom, dan sesi multi-turn `Chat` yang terisolasi sepenuhnya.
