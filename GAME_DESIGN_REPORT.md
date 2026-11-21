# GAME DESIGN REPORT — COMBAT FRONTS (جبهات القتال) v2.1.0

**برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م**

---

## 1. High-Concept Identity
- **Title:** جبهات القتال — COMBAT FRONTS
- **Genre:** Offline 3D Third-Person Military Tactical Action Shooter (TPS)
- **Language:** Arabic Primary (RTL UI with Cairo typography + preserved physical thumb controls) with instant toggle to English (LTR).
- **Platform:** Android (Landscape, Touch-First Multi-Touch Controls, 100% Offline).

---

## 2. Core Gameplay Loop
1. **Prepare & Customize:** Choose a mode (Campaign, Survival, Defense, or Challenges), configure weapon loadout & attachments in the 360° Armory, and customize operator tactical gear in the 3D Character Studio.
2. **Deploy & Engage:** Enter one of four distinct fictional cities under dynamic weather conditions. Use cover, vaulting, shoulder swapping, ADS/Sniper optics, Frag Grenades, Field Medkits, explosive environment barrels, and the drivable Armored Rover.
3. **Complete Objectives & Survive:** Fulfill tactical mission objectives (move, secure zones, collect intel, neutralize squads, defeat armored bosses, defend the relay core, or beat timed challenges).
4. **Earn Progression & Unlock:** Receive star ratings, challenge medals (Bronze/Silver/Gold), XP, career stats, achievements, new weapons, attachments, and character gear — all persisted locally offline.

---

## 3. World & Level Structure (4 Cities × 12 Missions)
| City | Arabic Name | Visual & Tactical Identity | Campaign Missions |
|---|---|---|---|
| **River City** | مدينة النهر | Dense modern urban boulevards, bridges, courtyards, multi-level concrete blocks | Missions 1–3 |
| **Mirage City** | مدينة السراب | Arid desert outpost, sand-drifted roads, industrial storage yards, low-rise adobe cover | Missions 4–6 |
| **Harbor City** | مدينة المرسى | Coastal shipping port, steel cargo containers, dockside warehouses, cranes | Missions 7–9 |
| **Mountain City** | مدينة الجبل | High-altitude fortified ridge, rocky cliffs, radar bunkers, command citadel | Missions 10–12 |

### Dynamic Weather System
- **Clear Day, Rain, Fog, Sandstorm, Night Ops:** Dynamically adjusts sky zenith/horizon gradients, fog density, sun/ambient lighting, 3D precipitation/dust particles, ambient audio pulses, and **Enemy AI vision range scaling** (e.g., Sandstorm reduces AI sight multiplier below 0.6x for tactical stealth).

---

## 4. Playable Game Modes
1. **Campaign (الحملة الرئيسية):** 12 structured missions with checkpoints, cinematic intros, reinforcement waves, interactive devices, and Mission 1 interactive contextual onboarding hints.
2. **Survival (طور البقاء):** Endless escalating enemy waves across any of the 4 cities and weather presets with wave-clear resupplies and high-score tracking.
3. **Defense (الدفاع التكتيكي):** Protect the 3D Command Relay Core (400 HP) across 5 escalating waves with enemies prioritizing the core and triggering HUD alerts when damaged.
4. **Tactical Challenges (التحديات التكتيكية):** Specialized rule-sets (Headshot Precision, Timed Blitz, Ironclad Survival, Elite Gauntlet) with countdown timers, soft-lock bonus target spawning, and Bronze/Silver/Gold medals.

---

## 5. Combat, Weapons & Tactical Equipment
- **8 Distinct Weapons:** `VX-74 Assault`, `K9-Compact SMG`, `M12 Tactical Shotgun`, `DMR-14 Marksman`, `SR-308 Precision Sniper`, `MG-48 Light Machine Gun`, `P9-Tac Sidearm`, `Rail-X1 Heavy`.
- **Weapon Customization:** 5 attachment slots (Optic, Barrel, Magazine, Grip, Stabilizer/Suppressor) modifying real live combat stats (capacity, effective range, recoil, reload speed, and suppressor gunfire acoustics/alert radius).
- **Military Sniper Scope Overlay:** Engaging ADS on `SR-308` (6.0x) or `DMR-14` (4.0x) activates a full-screen mil-dot circular tactical scope with target-lock illumination.
- **Tactical Equipment:**
  - **Frag Grenades (3 carried, resupplied via crates/waves):** Parabolic throw physics, ground/wall bounce, 1.65s blinking fuse, 8.0m radial blast, chain-detonation of fuel barrels, and dynamic point-light explosion flash.
  - **Field Medkits (2 carried, max 3):** Instant +55 HP and +25 Armor tactical recovery with cooldown.
- **Drivable Tactical Armored Rover:** Enter/exit via world interaction, 3D rotating horizontal wheels, hull durability, roof autocannon turret with independent aim, and procedural engine audio.

---

## 6. Enemy AI 2.0 & Battlefield Readability
- **8 Enemy Archetypes:** Standard Infantry, Fast Assault, Armored Heavy, Precision Marksman (with visible red laser warning beam), Shield Defender (frontal ballistic shield), Close Aggressor, Squad Coordinator, and Elite Armored Boss.
- **Tactical FSM + Squad Coordinator:** Patrol, Investigate, Suspicious, Alert, Engage, Flank, Take Cover, Reload, Reposition, Retreat, Defeated.
- **Readability Overlays:**
  - 3D-projected floating enemy health bars (color-coded for standard, sniper, boss).
  - Directional off-screen/on-screen objective arrow with live meter distance.
  - Live Kill Feed with Headshot/Blast badges and `+XP` popups.
  - Low-health crimson edge vignette and directional damage arc.

---

## 7. Mobile UX, Accessibility & Controls
- **Physical LTR Thumb Anchors:** Virtual analog joystick (with auto-sprint outer ring) stays anchored on the left thumb and combat buttons on the right thumb even in Arabic RTL mode (with an explicit Left-Handed swap setting).
- **Control Layout Editor:** Drag-and-drop repositioning, button/joystick scale, opacity, and deadzone sliders.
- **Smart Mobile Auto-Fire (Optional Toggle):** Automatically fires when the center reticle locks onto a visible enemy, paired with crimson reticle target-lock feedback.
- **Accessibility:** Readable text scaling (`0.85x–1.25x`), subtitles toggle, reduce screen shake, reduce intense visual effects, and full audio category sliders (Master, SFX, Music, Ambient).
