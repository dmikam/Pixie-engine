package com.example.menu.model

import androidx.compose.ui.graphics.Color

enum class GameStatus(val label: String, val badgeColor: Color, val textColor: Color) {
    PLAYABLE("PLAYABLE NOW", Color(0xFF2E7D32), Color(0xFFE8F5E9)),
    NEXT_UP("NEXT IN QUEUE", Color(0xFFE65100), Color(0xFFFFF3E0)),
    PLANNED("PLANNED", Color(0xFF37474F), Color(0xFFECEFF1))
}

data class PocGameInfo(
    val id: String,
    val title: String,
    val genre: String,
    val paradigm: String,
    val status: GameStatus,
    val releaseVersion: String,
    val tagline: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val engineHighlights: List<String>,
    val plannedMechanics: List<String>,
    val subsystemsTested: List<String>,
    val controlsSummary: String
)

object PocGameRegistry {
    val games: List<PocGameInfo> = listOf(
        PocGameInfo(
            id = "dinorun",
            title = "Dinorun",
            genre = "Prehistoric Endless Runner",
            paradigm = "Horizontal Scroller",
            status = GameStatus.PLAYABLE,
            releaseVersion = "v0.3",
            tagline = "Race through ash lands, leap hazards, and restore color to the jurassic world.",
            description = "A fast-paced horizontal endless runner where an agile dinosaur dashes through prehistoric ash plains. Collecting primal color sparks progressively restores vibrant RGB colors across the grayscale world.",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF00B0FF),
            engineHighlights = listOf(
                "5-Layer Parallax Stack with unified celestial horizon",
                "ColorMatrix post-processing grayscale-to-color recovery",
                "Procedural ChunkManager recycling & pooling",
                "Dual-mode controls: On-screen buttons & Swipe gestures"
            ),
            plannedMechanics = listOf(
                "Variable-height jump & duck slide",
                "Dynamic Pterodactyl altitude flight & Volcanic Meteors",
                "Color recovery thresholds & day/night celestial loop",
                "Real-time FPS, distance, and score telemetry"
            ),
            subsystemsTested = listOf(
                "HorizontalScrollerViewMode",
                "AABB Collision System",
                "ChunkManager Recycling",
                "Android ColorMatrix Adapter",
                "Layer Inspector & Pixel Grid Toggles"
            ),
            controlsSummary = "Dual-Mode: Virtual Pad (Duck/Jump) or Swipe/Tap Gestures"
        ),
        PocGameInfo(
            id = "raptor",
            title = "Raptor",
            genre = "Arcade Sci-Fi Shmup",
            paradigm = "Vertical Scroller",
            status = GameStatus.NEXT_UP,
            releaseVersion = "v0.5 (Next)",
            tagline = "Pilot an interceptor over hostile planetary surfaces against swarms of enemy craft.",
            description = "A classic 90s-style vertical top-down arcade shooter. Pilot an experimental aerospace fighter over sweeping planetary surfaces, dodging bullet hell patterns and unleashing concentrated laser fire.",
            primaryColor = Color(0xFFFF9100),
            secondaryColor = Color(0xFFFF3D00),
            engineHighlights = listOf(
                "VerticalScrollerViewMode camera tracking",
                "Zero-allocation high-frequency projectile pool (100+ bullets)",
                "Parametric wave spawner (sine waves, swooping formations)",
                "Rotated sprite headings & screen boundary clamping"
            ),
            plannedMechanics = listOf(
                "Touch-drag / virtual joystick precision steering",
                "Auto-fire pulse lasers & charge secondary missiles",
                "Energy shield depletion & boss phase transitions",
                "Multi-layer atmospheric cloud drift & planetary flyover"
            ),
            subsystemsTested = listOf(
                "VerticalScrollerViewMode",
                "Zero-Alloc Projectile Pool",
                "Parametric Wave Timeline",
                "Radial & Spatial Partition Collision",
                "Dynamic Surface Parallax"
            ),
            controlsSummary = "Direct Touch Drag / Virtual Analog Stick + Weapon Trigger"
        ),
        PocGameInfo(
            id = "arkanoid",
            title = "Arkanoid",
            genre = "Precision Brick Breaker",
            paradigm = "Fixed Screen",
            status = GameStatus.PLANNED,
            releaseVersion = "v0.6",
            tagline = "Deflect hyper-velocity energy spheres to shatter defensive cyber-grids.",
            description = "An intense fixed-arena block breaker testing exact reflection physics, paddle impact angle modulation, sub-step continuous collision detection, and multi-tier destructible brick matrices.",
            primaryColor = Color(0xFFD500F9),
            secondaryColor = Color(0xFFAA00FF),
            engineHighlights = listOf(
                "FixedScreenViewMode arena with coordinate letterboxing",
                "Sub-step Continuous Collision Detection (CCD) preventing tunneling",
                "2D destructible tile matrix with immediate dirty-rect redraws",
                "ColorMatrix per-tier brick recovery progression"
            ),
            plannedMechanics = listOf(
                "Paddle curvature deflection (impact offset alters bounce vector)",
                "Silver armor, explosive blocks, and speed boost cells",
                "Power capsule drops (Multi-ball, Wide paddle, Plasma laser)",
                "Dynamic ball velocity escalation on consecutive rebounds"
            ),
            subsystemsTested = listOf(
                "FixedScreenViewMode",
                "Continuous Collision Detection (CCD)",
                "Destructible Matrix Layer",
                "Per-Layer Palette Grading"
            ),
            controlsSummary = "Horizontal Touch Slide / Paddle Left-Right Steer"
        ),
        PocGameInfo(
            id = "chiki",
            title = "Chiki",
            genre = "Classic Side-Scrolling Platformer",
            paradigm = "Classic Platformer",
            status = GameStatus.PLANNED,
            releaseVersion = "v0.7",
            tagline = "Explore hand-crafted fantasy stages with acrobatic jumping, climbing, and melee combat.",
            description = "Inspired by golden-age 80s/90s arcade platformers like Chiki Chiki Boys / Wonder Boy. Guide an agile hero through treacherous ruins with multi-tier platforms, climbing ladders, and roaming creatures.",
            primaryColor = Color(0xFF00E676),
            secondaryColor = Color(0xFF00C853),
            engineHighlights = listOf(
                "Bidirectional follow camera with configurable deadzones",
                "Tilemap chunk parser with solid, one-way, and hazard flags",
                "Coyote time & jump-buffer platformer physics controller",
                "Multi-frame sprite state machine (Idle, Run, Jump, Climb, Attack)"
            ),
            plannedMechanics = listOf(
                "Precision jump arcs with variable height cutoff",
                "Ladder climbing & drop-down through one-way ledges",
                "Melee slash attack with knockback physics",
                "Hidden treasure chests, gem pickups, and level goal gate"
            ),
            subsystemsTested = listOf(
                "Bidirectional Deadzone Camera",
                "Tilemap Collision Matrix",
                "Platformer Physics Arc",
                "Sprite Frame Sequencer",
                "One-Way Platform Resolver"
            ),
            controlsSummary = "Arcade D-Pad + Jump + Melee Slash + Climb Up/Down"
        )
    )

    fun getById(id: String): PocGameInfo? = games.firstOrNull { it.id == id }
}
