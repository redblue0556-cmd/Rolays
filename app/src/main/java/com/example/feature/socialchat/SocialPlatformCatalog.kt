package com.example.feature.socialchat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PlatformIconKind {
    TIKTOK,
    INSTAGRAM,
    WHATSAPP,
    X_TWITTER,
    YOUTUBE,
    FACEBOOK,
    LINKEDIN,
    TELEGRAM,
    THREADS,
    PINTEREST,
    REDDIT,
    DISCORD,
    UNIVERSAL,
    SNAPCHAT,
    TWITCH,
    LINE
}

data class SocialPlatformSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val iconKind: PlatformIconKind,
    val roleHint: String,
    val quickPrompts: List<Pair<String, String>>,
    val isExtra: Boolean = false
)

object SocialPlatformCatalog {

    val universalPlatform = SocialPlatformSpec(
        id = "universal",
        title = "Semua Medsos (Universal AI)",
        subtitle = "Asisten AI serbaguna lintas semua platform media sosial",
        primaryColor = Color(0xFF3CEBFF),
        secondaryColor = Color(0xFF9E7BFF),
        iconKind = PlatformIconKind.UNIVERSAL,
        roleHint = "semua platform media sosial sekaligus (Instagram, TikTok, WhatsApp, X, YouTube, LinkedIn, Threads, Discord, dll.)",
        quickPrompts = listOf(
            "Kampanye Lintas Medsos" to "Buatkan rencana konten 1 minggu sekaligus untuk Instagram Reels, TikTok, dan X tentang peluncuran produk baru.",
            "Caption Multi-Format" to "Buat 3 versi caption (pendek untuk X, storytelling untuk Instagram, hook cepat untuk TikTok) untuk topik yang sama.",
            "Balas Pesan Pelanggan" to "Buatkan template balasan DM & WhatsApp yang ramah, cepat, dan persuasif."
        )
    )

    val corePlatforms: List<SocialPlatformSpec> = listOf(
        SocialPlatformSpec(
            id = "tiktok",
            title = "TikTok AI",
            subtitle = "Script, FYP Hook & Tren",
            primaryColor = Color(0xFF00F2EA),
            secondaryColor = Color(0xFFFF0050),
            iconKind = PlatformIconKind.TIKTOK,
            roleHint = "TikTok (hook 3 detik pertama, skrip video pendek, ide tren FYP, sound & hashtag)",
            quickPrompts = listOf(
                "Hook FYP 3 Detik" to "Buatkan 5 kalimat hook pembuka TikTok 3 detik pertama yang bikin penonton berhenti scroll.",
                "Skrip Video 30s" to "Buatkan skrip video TikTok durasi 30 detik lengkap dengan arahan visual, teks layar, dan CTA.",
                "Riset Hashtag FYP" to "Berikan kombinasi hashtag TikTok (niche + broad) yang optimal untuk masuk FYP."
            )
        ),
        SocialPlatformSpec(
            id = "instagram",
            title = "Instagram AI",
            subtitle = "Caption, DM Auto & Reels",
            primaryColor = Color(0xFFDD2A7B),
            secondaryColor = Color(0xFFF58529),
            iconKind = PlatformIconKind.INSTAGRAM,
            roleHint = "Instagram (Caption estetik, skrip Reels, ide Carousel, balasan DM otomatis & Bio)",
            quickPrompts = listOf(
                "Caption Estetik" to "Buatkan 3 pilihan caption Instagram yang engaging dengan CTA simpan & bagikan.",
                "Ide Carousel 6 Slide" to "Susun konsep konten Instagram Carousel 6 slide yang edukatif dan mudah dipahami.",
                "Auto-Reply DM" to "Buatkan balasan DM Instagram yang ramah untuk menanyakan harga & link pembelian."
            )
        ),
        SocialPlatformSpec(
            id = "whatsapp",
            title = "WhatsApp AI",
            subtitle = "Balas Cepat & Pesan Bisnis",
            primaryColor = Color(0xFF25D366),
            secondaryColor = Color(0xFF128C7E),
            iconKind = PlatformIconKind.WHATSAPP,
            roleHint = "WhatsApp & WhatsApp Business (balas chat cepat, follow-up klien, pesan broadcast & greeting sopan)",
            quickPrompts = listOf(
                "Balas Chat Klien" to "Buatkan balasan WhatsApp Bisnis yang sopan, hangat, dan profesional untuk pelanggan baru.",
                "Pesan Broadcast" to "Tulis pesan broadcast WhatsApp promosi yang tidak terasa spam namun menarik minat baca.",
                "Follow-Up Order" to "Buatkan pesan follow-up ramah untuk pelanggan yang belum menyelesaikan pembayaran."
            )
        ),
        SocialPlatformSpec(
            id = "x_twitter",
            title = "X (Twitter) AI",
            subtitle = "Utas Viral & Opini Tren",
            primaryColor = Color(0xFF8899A6),
            secondaryColor = Color(0xFF1DA1F2),
            iconKind = PlatformIconKind.X_TWITTER,
            roleHint = "X / Twitter (Utas/Thread viral terstruktur, opini tajam, punchy tweet & balasan cerdas)",
            quickPrompts = listOf(
                "Utas Viral 6 Part" to "Buatkan thread X (Twitter) 6 bagian yang rapi, bernomor, dan punya hook kuat di tweet pertama.",
                "Tweet Singkat Punchy" to "Buatkan 5 ide cuitan singkat maksimal 200 karakter yang memicu diskusi.",
                "Reply Cerdas" to "Buatkan 3 gaya balasan cerdas (humoris, informatif, suportif) untuk postingan yang sedang ramai."
            )
        ),
        SocialPlatformSpec(
            id = "youtube",
            title = "YouTube AI",
            subtitle = "Ide Konten & Judul Video",
            primaryColor = Color(0xFFFF0000),
            secondaryColor = Color(0xFFB80000),
            iconKind = PlatformIconKind.YOUTUBE,
            roleHint = "YouTube & YouTube Shorts (Judul high-CTR, deskripsi SEO, konsep thumbnail & outline video)",
            quickPrompts = listOf(
                "Judul High-CTR" to "Buatkan 5 judul video YouTube yang bikin penasaran tanpa clickbait murahan.",
                "Deskripsi SEO" to "Susun deskripsi video YouTube lengkap dengan timestamp dan kata kunci SEO.",
                "Konsep Thumbnail" to "Berikan ide teks singkat dan komposisi visual thumbnail YouTube yang kontras dan menarik."
            )
        ),
        SocialPlatformSpec(
            id = "facebook",
            title = "Facebook AI",
            subtitle = "Copywriting & Kelola Grup",
            primaryColor = Color(0xFF1877F2),
            secondaryColor = Color(0xFF0A4DA6),
            iconKind = PlatformIconKind.FACEBOOK,
            roleHint = "Facebook Page, Ads & Grup Komunitas (copywriting konversi, storytelling, & moderasi diskusi grup)",
            quickPrompts = listOf(
                "Copywriting FB Ads" to "Buatkan teks iklan Facebook dengan formula AIDA (Attention, Interest, Desire, Action).",
                "Postingan Grup" to "Buatkan topik diskusi hangat untuk meningkatkan interaksi anggota di Grup Facebook.",
                "Pengumuman Page" to "Tulis pengumuman resmi halaman Facebook dengan nada ramah dan informatif."
            )
        ),
        SocialPlatformSpec(
            id = "linkedin",
            title = "LinkedIn AI",
            subtitle = "Karier & Personal Branding",
            primaryColor = Color(0xFF0A66C2),
            secondaryColor = Color(0xFF38A3FF),
            iconKind = PlatformIconKind.LINKEDIN,
            roleHint = "LinkedIn (thought leadership post, ringkasan profil About, pesan networking & personal branding)",
            quickPrompts = listOf(
                "Post Insight Karier" to "Buatkan postingan LinkedIn tentang pelajaran karier/proyek yang inspiratif namun tidak terkesan pamer.",
                "Optimasi Bio About" to "Bantu tulis ulang bagian 'About' profil LinkedIn agar lebih profesional dan berorientasi hasil.",
                "Pesan Networking" to "Buatkan pesan undangan koneksi LinkedIn yang sopan kepada rekruter atau mentor."
            )
        ),
        SocialPlatformSpec(
            id = "telegram",
            title = "Telegram AI",
            subtitle = "Grup & Bot Otomatisasi",
            primaryColor = Color(0xFF26A5E4),
            secondaryColor = Color(0xFF0088CC),
            iconKind = PlatformIconKind.TELEGRAM,
            roleHint = "Telegram Channel & Grup (pengumuman channel, pesan sambutan bot, FAQ terstruktur & format Markdown)",
            quickPrompts = listOf(
                "Pesan Channel" to "Buatkan pengumuman Telegram Channel dengan format rapi, poin jelas, dan tautan CTA.",
                "Welcome Bot" to "Buatkan pesan sambutan otomatis bot Telegram untuk anggota baru yang masuk grup.",
                "FAQ Komunitas" to "Susun daftar FAQ ringkas untuk disematkan (pinned message) di grup Telegram."
            )
        ),
        SocialPlatformSpec(
            id = "threads",
            title = "Threads AI",
            subtitle = "Diskusi & Konten Viral",
            primaryColor = Color(0xFFDADEE8),
            secondaryColor = Color(0xFF6B7280),
            iconKind = PlatformIconKind.THREADS,
            roleHint = "Threads (postingan percakapan natural, pemantik opini, micro-storytelling santai)",
            quickPrompts = listOf(
                "Pemantik Diskusi" to "Buatkan 4 postingan Threads santai yang memancing balasan dan percakapan organik.",
                "Micro-Story" to "Tulis cerita singkat 3 paragraf yang relatable untuk dibagikan di Threads.",
                "Series Harian" to "Buatkan ide konten berseri harian di Threads untuk membangun audiens loyal."
            )
        ),
        SocialPlatformSpec(
            id = "pinterest",
            title = "Pinterest AI",
            subtitle = "Ide Visual & Deskripsi Pin",
            primaryColor = Color(0xFFE60023),
            secondaryColor = Color(0xFFFF4D6D),
            iconKind = PlatformIconKind.PINTEREST,
            roleHint = "Pinterest (judul & deskripsi Pin kaya kata kunci SEO, ide papan moodboard & konsep desain vertikal)",
            quickPrompts = listOf(
                "Deskripsi Pin SEO" to "Buatkan judul dan deskripsi Pinterest Pin yang kaya kata kunci pencarian visual.",
                "Ide Moodboard" to "Rancangkan 5 ide papan (board) Pinterest bertema estetika modern & inspiratif.",
                "Teks Overlay Pin" to "Berikan 5 variasi teks overlay singkat untuk desain Pin vertikal 2:3."
            )
        ),
        SocialPlatformSpec(
            id = "reddit",
            title = "Reddit AI",
            subtitle = "Diskusi Komunitas & Tren",
            primaryColor = Color(0xFFFF4500),
            secondaryColor = Color(0xFFFF8717),
            iconKind = PlatformIconKind.REDDIT,
            roleHint = "Reddit (postingan subreddit bernilai tinggi tanpa spam, AMA prep, rangkuman diskusi & komentar analitis)",
            quickPrompts = listOf(
                "Post Subreddit" to "Buatkan draf postingan Reddit yang informatif, jujur, dan mengikuti budaya diskusi komunitas.",
                "Ide Topik AMA" to "Susun pembukaan dan daftar poin menarik untuk sesi Ask Me Anything (AMA).",
                "Komentar Analitis" to "Bantu susun komentar diskusi yang logis, berimbang, dan membantu di utas Reddit."
            )
        ),
        SocialPlatformSpec(
            id = "discord",
            title = "Discord AI",
            subtitle = "Moderasi & Chat Komunitas",
            primaryColor = Color(0xFF5865F2),
            secondaryColor = Color(0xFF8B95FF),
            iconKind = PlatformIconKind.DISCORD,
            roleHint = "Discord Server (aturan server, pengumuman role/event dengan format markdown Discord, & balasan moderasi)",
            quickPrompts = listOf(
                "Aturan Server" to "Buatkan daftar aturan Discord Server (#rules) yang jelas, ramah, dan terstruktur dengan emoji.",
                "Pengumuman Event" to "Tulis pengumuman event komunitas Discord lengkap dengan @everyone callout dan jadwal.",
                "Respon Moderasi" to "Buatkan contoh pesan peringatan moderator yang tegas namun tetap sopan."
            )
        )
    )

    val extraPlatforms: List<SocialPlatformSpec> = listOf(
        SocialPlatformSpec(
            id = "snapchat",
            title = "Snapchat AI",
            subtitle = "Story & Spotlight Cepat",
            primaryColor = Color(0xFFFFFC00),
            secondaryColor = Color(0xFFFFB800),
            iconKind = PlatformIconKind.SNAPCHAT,
            roleHint = "Snapchat (ide Spotlight singkat, teks Story spontan & filter caption)",
            quickPrompts = listOf(
                "Ide Spotlight" to "Buatkan 3 ide video vertikal cepat untuk Snapchat Spotlight.",
                "Teks Story" to "Buatkan 5 caption singkat dan santai untuk Snapchat Story harian."
            ),
            isExtra = true
        ),
        SocialPlatformSpec(
            id = "twitch",
            title = "Twitch AI",
            subtitle = "Stream Title & Chat Bot",
            primaryColor = Color(0xFF9146FF),
            secondaryColor = Color(0xFFC084FC),
            iconKind = PlatformIconKind.TWITCH,
            roleHint = "Twitch (judul livestream menarik, perintah chat bot, & jadwal streaming)",
            quickPrompts = listOf(
                "Judul Stream" to "Buatkan 5 judul livestream Twitch yang unik dan mengundang penonton baru.",
                "Command Bot" to "Susun daftar command chat bot (!socials, !jadwal, !discord) yang seru."
            ),
            isExtra = true
        ),
        SocialPlatformSpec(
            id = "line",
            title = "LINE AI",
            subtitle = "Official Account & Blast",
            primaryColor = Color(0xFF06C755),
            secondaryColor = Color(0xFF00A846),
            iconKind = PlatformIconKind.LINE,
            roleHint = "LINE Official Account (pesan greeting, rich menu copy & promo pelanggan)",
            quickPrompts = listOf(
                "Greeting OA" to "Buatkan pesan sambutan LINE Official Account untuk pelanggan baru.",
                "Promo Rich Menu" to "Tulis label singkat dan jelas untuk 6 tombol Rich Menu LINE."
            ),
            isExtra = true
        )
    )

    val allPlatforms: List<SocialPlatformSpec> = corePlatforms + extraPlatforms
}

@Composable
fun SocialPlatformIconBadge(
    spec: SocialPlatformSpec,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = spec.primaryColor.copy(alpha = 0.7f),
                spotColor = spec.primaryColor.copy(alpha = 0.85f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        spec.primaryColor.copy(alpha = 0.92f),
                        spec.secondaryColor.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (spec.iconKind) {
            PlatformIconKind.TIKTOK -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(9.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val note = Path().apply {
                        moveTo(w * 0.58f, h * 0.15f)
                        lineTo(w * 0.58f, h * 0.70f)
                        cubicTo(w * 0.58f, h * 0.88f, w * 0.25f, h * 0.88f, w * 0.25f, h * 0.66f)
                        cubicTo(w * 0.25f, h * 0.50f, w * 0.42f, h * 0.46f, w * 0.50f, h * 0.50f)
                        moveTo(w * 0.58f, h * 0.20f)
                        quadraticTo(w * 0.72f, h * 0.36f, w * 0.85f, h * 0.36f)
                    }
                    drawPath(note, color = Color(0xFF0A0F1D), style = stroke)
                }
            }
            PlatformIconKind.INSTAGRAM -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val s = Stroke(width = this.size.width * 0.12f)
                    drawRoundRect(
                        color = Color.White,
                        cornerRadius = CornerRadius(this.size.width * 0.28f),
                        style = s
                    )
                    drawCircle(
                        color = Color.White,
                        radius = this.size.width * 0.22f,
                        style = s
                    )
                    drawCircle(
                        color = Color.White,
                        radius = this.size.width * 0.07f,
                        center = Offset(this.size.width * 0.74f, this.size.height * 0.26f)
                    )
                }
            }
            PlatformIconKind.WHATSAPP -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(9.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.38f,
                        style = Stroke(width = w * 0.12f)
                    )
                    val tail = Path().apply {
                        moveTo(w * 0.22f, h * 0.74f)
                        lineTo(w * 0.12f, h * 0.90f)
                        lineTo(w * 0.34f, h * 0.84f)
                    }
                    drawPath(tail, color = Color.White, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
                    drawArc(
                        color = Color.White,
                        startAngle = 35f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(w * 0.30f, h * 0.30f),
                        size = Size(w * 0.40f, h * 0.40f),
                        style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
                    )
                }
            }
            PlatformIconKind.X_TWITTER -> {
                Text(
                    text = "𝕏",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            PlatformIconKind.YOUTUBE -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(11.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val play = Path().apply {
                        moveTo(w * 0.32f, h * 0.22f)
                        lineTo(w * 0.80f, h * 0.50f)
                        lineTo(w * 0.32f, h * 0.78f)
                        close()
                    }
                    drawPath(play, color = Color.White)
                }
            }
            PlatformIconKind.FACEBOOK -> {
                Text(
                    text = "f",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PlatformIconKind.LINKEDIN -> {
                Text(
                    text = "in",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PlatformIconKind.TELEGRAM -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val plane = Path().apply {
                        moveTo(w * 0.88f, h * 0.16f)
                        lineTo(w * 0.12f, h * 0.48f)
                        lineTo(w * 0.42f, h * 0.58f)
                        lineTo(w * 0.72f, h * 0.84f)
                        close()
                    }
                    drawPath(plane, color = Color.White)
                }
            }
            PlatformIconKind.THREADS -> {
                Text(
                    text = "@",
                    color = Color(0xFF0A0F1D),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PlatformIconKind.PINTEREST -> {
                Text(
                    text = "P",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PlatformIconKind.REDDIT -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(9.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.14f, h * 0.34f),
                        size = Size(w * 0.72f, h * 0.50f)
                    )
                    drawCircle(
                        color = spec.primaryColor,
                        radius = w * 0.07f,
                        center = Offset(w * 0.38f, h * 0.56f)
                    )
                    drawCircle(
                        color = spec.primaryColor,
                        radius = w * 0.07f,
                        center = Offset(w * 0.62f, h * 0.56f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.50f, h * 0.34f),
                        end = Offset(w * 0.66f, h * 0.16f),
                        strokeWidth = w * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.08f,
                        center = Offset(w * 0.70f, h * 0.16f)
                    )
                }
            }
            PlatformIconKind.DISCORD -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.10f, h * 0.22f),
                        size = Size(w * 0.80f, h * 0.56f),
                        cornerRadius = CornerRadius(w * 0.26f)
                    )
                    drawCircle(
                        color = spec.primaryColor,
                        radius = w * 0.09f,
                        center = Offset(w * 0.36f, h * 0.50f)
                    )
                    drawCircle(
                        color = spec.primaryColor,
                        radius = w * 0.09f,
                        center = Offset(w * 0.64f, h * 0.50f)
                    )
                }
            }
            PlatformIconKind.UNIVERSAL -> {
                Text(
                    text = "✦",
                    color = Color(0xFF070C18),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            PlatformIconKind.SNAPCHAT -> {
                Text(
                    text = "S",
                    color = Color(0xFF0A0F1D),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PlatformIconKind.TWITCH -> {
                Text(
                    text = "Tv",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PlatformIconKind.LINE -> {
                Text(
                    text = "LINE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
