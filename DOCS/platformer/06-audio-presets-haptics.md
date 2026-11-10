# 06 — 8-Bit NES Audio Presets & Tactile Haptics

> **Module Scope:** `:pixie-android` (`com.pixie.android.audio.*`, `com.pixie.android.haptic.*`)  
> **Target Release:** v0.7.0 (Step 6)  
> **Status:** COMPLETE  
> **Compliance:** Low-latency PCM DSP synthesis, zero runtime allocation, AGENTS.md compliance.

---

## 📋 Objectives

Expand the native procedural audio synthesizer (`RetroAudioSynthesizer` & `RetroAudioPresets`) with authentic 8-bit NES frequency sweeps, triangle bass jumps, noise bursts for stomps and brick breaks, and coordinated tactile micro-haptics.

---

## 🗺️ Tasks & Checklist

- [x] **Task 6.1: Extend `AudioEvent` with NES Platformer Events**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/ports/EnginePorts.kt`
  - **Action:**
    - Add platformer audio event constants:
      - `PLATFORMER_JUMP`: Rapid rising pitch frequency chirp (~150Hz to 600Hz in 120ms).
      - `PLATFORMER_JUMP_SUPER`: Deeper, fuller rising sweep for powered-up hero.
      - `COIN_DING`: Iconic two-tone dual chime (B5 987Hz for 50ms followed immediately by E6 1318Hz for 280ms).
      - `BLOCK_BUMP`: Low square wave thud with fast decay (~120Hz).
      - `BRICK_BREAK`: Modulated noise burst simulating crumbling masonry.
      - `POWERUP_SPAWN`: Multi-tone rising arpeggio (C-E-G-C-E-G).
      - `POWERUP_COLLECT`: Rapid celebratory musical flourish.
      - `ENEMY_STOMP`: Short punchy pitch drop with fast noise crackle.
      - `CRATE_THROW`: Whoosh frequency sweep.
      - `STAGE_CLEAR`: Triumphant 8-bit fanfare arpeggio upon touching flagpole.

- [x] **Task 6.2: Implement 8-Bit Waveform Synthesizers in `RetroAudioPresets.kt`**
  - **Target File:** `pixie-android/src/main/kotlin/com/pixie/android/audio/RetroAudioPresets.kt`
  - **Action:**
    - Implement programmatic PCM generation for the new audio events using square, triangle, and white noise algorithms:
      - `generateCoinChime()`: Pure dual-square wave with natural exponential decay.
      - `generateJumpChirp()`: 8-bit frequency slide equation.
      - `generateStompThud()`: Low triangle wave coupled with high-frequency noise tap.
      - `generateBrickBreak()`: Band-pass filtered noise burst.
      - `generateStageClearFanfare()`: Multi-note melody encoded via frequency-duration note sequence.

- [x] **Task 6.3: Add Tactile Micro-Haptic Patterns**
  - **Target File:** `pixie-android/src/main/kotlin/com/pixie/android/haptic/AndroidHapticAdapter.kt`
  - **Action:**
    - Provide distinct feedback signatures:
      - Jump: Crisp tick (10ms, light amplitude).
      - Coin collect: Light double-tap click.
      - Stomp / Crate throw: Medium punchy pulse (30ms).
      - Brick shatter / Player damage: Heavy tactile jolt (50ms).

- [x] **Task 6.4: Audio & Haptic Integration Verification**
  - **Target File:** `pixie-android/src/test/kotlin/com/pixie/android/audio/RetroAudioSynthesizerTest.kt`
  - **Action:**
    - Verify that all newly declared `AudioEvent` keys have corresponding generation recipes in `RetroAudioPresets` and generate valid PCM WAV buffers without exceptions or NaN samples.

---

## 🔍 Verification

1. Run `gradle :pixie-android:test` ensuring `RetroAudioSynthesizerTest` passes for all new audio events.
2. Confirm zero audio thread latency and clean disposal on exit.
