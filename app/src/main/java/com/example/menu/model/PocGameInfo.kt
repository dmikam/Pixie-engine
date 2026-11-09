package com.example.menu.model

import androidx.compose.ui.graphics.Color

enum class GameStatus(val label: String, val badgeColor: Color, val textColor: Color) {
    PLAYABLE("POC CLOSED (PLAYABLE)", Color(0xFF2E7D32), Color(0xFFE8F5E9)),
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
            status = GameStatus.PLAYABLE,
            releaseVersion = "v0.5",
            tagline = "Pilot an interceptor over hostile planetary surfaces against swarms of enemy craft.",
            description = "A classic 90s-style vertical top-down arcade shooter. Pilot an experimental aerospace fighter over sweeping planetary surfaces, dodging bullet hell patterns and unleashing concentrated laser fire.",
            primaryColor = Color(0xFFFF9100),
            secondaryColor = Color(0xFFFF3D00),
            engineHighlights = listOf(
                "Continuous Difficulty (0..100) & Context Property Scaler",
                "Zero-allocation Projectile Pool & Procedural Particle Engine",
                "Self-encapsulated Weapon Definitions (Lasers, Missiles, Plasma)",
                "VerticalScrollerViewMode camera & multi-layer scrolling"
            ),
            plannedMechanics = listOf(
                "Touch-drag / virtual joystick precision steering",
                "Encapsulated primary cannons & homing secondary missiles",
                "Procedural multi-stage explosions & particle sparks",
                "Destructible ground turrets, silos, pickable perks, and multi-phase bosses"
            ),
            subsystemsTested = listOf(
                "Continuous Difficulty (0..MAX_DIFFICULTY)",
                "Zero-Alloc Projectile & Particle Pools",
                "Encapsulated Weapon Architecture",
                "VerticalScrollerViewMode & Multi-Level Biomes"
            ),
            controlsSummary = "Direct Touch Drag / Virtual Analog Stick + Auto/Manual Fire + Mega-Bomb"
        ),
        PocGameInfo(
            id = "arkanoid",
            title = "Arkanoid",
            genre = "Precision Brick Breaker",
            paradigm = "Fixed Screen",
            status = GameStatus.PLAYABLE,
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
            id = "superpixie",
            title = "Super Pixie",
            genre = "NES Classic 2D Platformer",
            paradigm = "Tilemap Platformer",
            status = GameStatus.NEXT_UP,
            releaseVersion = "v0.7",
            tagline = "Run, jump, stomp enemies, bonk question blocks, and hurl crates in a faithful NES platformer homage.",
            description = "A faithful homage to 8-bit NES golden-age platformers (Super Mario Bros & Chip 'n Dale 2). Guide a nimble hero across scrolling worlds, head-bonking mystery blocks to spawn coins and mushrooms, stomping roaming monsters, and picking up crates to hurl at obstacles.",
            primaryColor = Color(0xFFFF5252),
            secondaryColor = Color(0xFFFF1744),
            engineHighlights = listOf(
                "TilemapChunkLayer with bumpable blocks & breakable bricks",
                "PlatformerFollowViewMode camera with lead smoothing",
                "NES-grade physics: momentum, skidding, variable jump cut, coyote time",
                "Interactive object pick-and-throw subsystem (Chip 'n Dale homage)"
            ),
            plannedMechanics = listOf(
                "Head-bonk ? blocks popping coins & mushrooms with vertical recoil",
                "Stomp detection bouncing hero upward upon hitting enemy heads",
                "Crate carrying & projectile throwing against enemies and walls",
                "Flagpole & castle level completion sequence"
            ),
            subsystemsTested = listOf(
                "Tilemap Collision Engine",
                "PlatformerFollow Camera",
                "Momentum & Jump-Cut Physics",
                "Head-Bonk Tile Reaction",
                "Stomp & Recoil Vectors"
            ),
            controlsSummary = "D-Pad (Left/Right/Duck) + Button A (Jump) + Button B (Run / Pick & Throw)"
        )
    )

    fun getById(id: String): PocGameInfo? = games.firstOrNull { it.id == id }
}
