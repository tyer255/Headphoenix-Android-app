# 🎵 Android Lossless Music Application

A modern, high-performance Android music streaming and offline playback application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Media3 / ExoPlayer**.

---

## 📱 Releases & UI Screens Showcase

Below is an overview of the core mobile application screens, demonstrating the human-designed, restrained, and production-ready Material 3 interface:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       📱 MOBILE APPLICATION SCREENS                                     │
├───────────────────────────────────┬───────────────────────────────────┬─────────────────────────────────┤
│ 1. NOW PLAYING & LYRICS           │ 2. ANDROID 13+ SYSTEM MEDIA       │ 3. HOME & CURATED FEED          │
├───────────────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
│ ┌───────────────────────────────┐ │ ┌───────────────────────────────┐ │ ┌─────────────────────────────┐ │
│ │ 9:41                      85% │ │ │ 9:41                      85% │ │ │ 9:41                    85% │ │
│ │ ⌄  PLAYING FROM ALBUM       ⋮ │ │ │ ┌───────────────────────────┐ │ │ │ Good evening              ⚙️│ │
│ │                               │ │ │ │ 🎵 Lagda Nahi             │ │ │ │ ┌───────────┐ ┌───────────┐ │ │
│ │        ┌─────────────┐        │ │ │ │    Ammy Gill • Up Next    │ │ │ │ │ Liked     │ │ Daily     │ │ │
│ │        │  [ARTWORK]  │        │ │ │ │ ──●──────────────── 01:12 │ │ │ │ │ Songs     │ │ Mix 1     │ │ │
│ │        └─────────────┘        │ │ │ │   ⏮   ⏸   ⏭   (+)     │ │ │ │ └───────────┘ └───────────┘ │ │
│ │ Lagda Nahi                 ❤️ │ │ │ │ [T1] [T2] [T3] [T4] (Up)  │ │ │ │ Recently Played             │ │
│ │ Ammy Gill                     │ │ │ └───────────────────────────┘ │ │ │ ┌─────┐ ┌─────┐ ┌─────┐   │ │
│ │ ──●──────────────────── 03:11 │ │ │                               │ │ │ │Mix 1│ │Top  │ │Indie│   │ │
│ │    🔀   ⏮   ⏸   ⏭   🔁    │ │ │ Native MediaLibraryService    │ │ │ └─────┘ └─────┘ └─────┘   │ │
│ └───────────────────────────────┘ │ └───────────────────────────────┘ │ └─────────────────────────────┘ │
│ • Synchronized real-time lyrics   │ • 4-thumbnail Up Next carousel    │ • 120 FPS buttery smooth scroll │
│ • Dynamic gradient background     │ • Lock screen & notification      │ • 2x3 Quick Access playlist grid│
│ • Scrub dot & speed controller    │ • Instant background audio start  │ • Curated personalized mixes    │
├───────────────────────────────────┼───────────────────────────────────┴─────────────────────────────────┤
│ 4. SEARCH & INSTANT RESOLVE       │ 5. LIBRARY & OFFLINE DOWNLOADS                                      │
├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────┐ │ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 9:41                      85% │ │ │ 9:41                                                        85% │ │
│ │ Search                        │ │ │ Your Library                                               🔍 ➕ │ │
│ │ ┌───────────────────────────┐ │ │ │ [Playlists]  [Downloaded (✓)]                                   │ │
│ │ │ 🔍 What to listen to?     │ │ │ │ ┌───┐ Liked Songs                    [Downloaded ✓]             │ │
│ │ └───────────────────────────┘ │ │ │ │❤️ │ 240 tracks • Local Offline Storage                        │ │
│ │ Browse all                    │ │ │ └───┘                                                           │ │
│ │ ┌───────────┐ ┌───────────┐ │ │ │ ┌───┐ Offline Punjabi Hits             [Downloaded ✓]             │ │
│ │ │ Podcasts  │ │ Made For U│ │ │ │ │🎵 │ 18 tracks • High Bitrate Audio                              │ │
│ │ └───────────┘ └───────────┘ │ │ │ └───┘                                                           │ │
│ └───────────────────────────────┘ │ └─────────────────────────────────────────────────────────────────┘ │
│ • Real-time autocomplete search   │ • 100% Offline playback without WiFi or Mobile Data                 │
│ • Category genre cards & chips    │ • Direct local storage download manager                             │
└───────────────────────────────────┴─────────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Technical Highlights

| Component | Architecture & Technologies |
| :--- | :--- |
| **UI & Theming** | Jetpack Compose, Material Design 3, Dynamic Theme Extraction, 120 FPS zero-GPU-blur rendering |
| **Media Playback** | Media3 ExoPlayer, MediaLibraryService, ForwardingPlayer, Headless YouTube audio engine fallback |
| **System Integration** | Android 13/14 Media Notification with 4-track Up Next recommendations, Lock screen controls |
| **Offline Storage** | Custom DownloadManager with FileProvider, Local Disk Cache & JSON state persistence |
| **Build & CI/CD** | Automated GitHub Actions workflow, Temurin Java 17, Gradle 9.3.1, AGP 9.1.1, Debug APK publishing |
