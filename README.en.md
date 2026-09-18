<div align="center">

# 🛡️ Defend The Block

**Place the Nexus. Survive every night.**
**If it falls, the world ends — literally.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1-62B47A?style=for-the-badge)](#-supported-versions)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-DBD0B4?style=for-the-badge)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-4A90D9?style=for-the-badge)](LICENSE)

**English** · [Português (Brasil)](README.md)

</div>

> 🖼️ **`docs/images/hero.png`** — cover image: the Nexus glowing at the centre of
> a base ringed with turrets, the horde closing in at night. Landscape, ~1280x480.

---

## 📖 Table of contents

| | |
|---|---|
| [🎯 What this mod is](#-what-this-mod-is) | [🏗️ What you build](#️-what-you-build) |
| [🎮 Supported versions](#-supported-versions) | [🧟 What comes for you](#-what-comes-for-you) |
| [📥 Installation](#-installation) | [📊 HUD and commands](#-hud-and-commands) |
| [⚠️ Before you play: read this](#️-before-you-play-read-this) | [⚙️ Configuration](#️-configuration) |
| [🚀 Getting started](#-getting-started) | [🔧 Building from source](#-building-from-source) |

---

## 🎯 What this mod is

**Defend The Block** turns Minecraft into a wave survival mode without taking
you out of the open world you already know.

You craft and place a single block — the **Nexus**. From that moment on,
**every night** a horde of hostile mobs spawns around it and marches in with one
goal: tearing it down. These are not the usual dumb mobs. They **mine** through
your wall, **blow up** whatever blocks the way, **build ladders** to climb what
you built, and **bridge** across gaps if you hide the Nexus up high.

The days are yours to prepare: raise walls, dig moats, place automatic
**arrow turrets** and customise them. The nights are yours to hold the line.

If the Nexus falls, **the mod deletes the world folder**. There is no second
chance — though you can turn that off in the config and keep just the defeat
message.

### ✨ What it offers

| | Feature |
|---|---|
| 🔷 | **The Nexus** — an indestructible block only invaders can hurt. Once placed, it stays. |
| 🌙 | **An invasion every night**, with no fixed mob count: as long as night lasts, one dies, another spawns. |
| 📈 | **Difficulty that actually grows** — more mobs alive, more health, more damage, better gear. |
| 🏹 | **Automatic arrow turret** with 5 material tiers. |
| 📚 | **9 turret modules** to customise attributes — but only 2 per turret. |
| 🧠 | **Siege AI**: mining, blowing up, climbing, bridging, breaking doors, setting fires. |
| 🛡️ | **Walling in the Nexus works** — real cover genuinely blocks damage. |
| 🕐 | **A 3-day grace period** before the first invasion, with an on-screen warning. |
| 🧹 | **The terrain resets at dawn**: everything the horde built is cleaned up. |
| 💎 | **Loot** scattered around the Nexus for every night survived. |
| ⚙️ | **64 configuration options** — from Nexus health to every ability chance. |

---

## 🎮 Supported versions

| Minecraft | Mod loader | Java | Status |
|---|---|---|---|
| **1.20.1** | Fabric Loader + Fabric API | 17+ | ✅ Supported |
| **1.21.1** | Fabric Loader + Fabric API | 21+ | ✅ Supported |

The same codebase runs on both. Each version has its own jar — download the one
matching your installation.

> ℹ️ **Known limitation on 1.21:** the **Punch** and **Piercing** enchantments
> applied to a turret have no effect, because 1.20.5 started deriving them from
> the weapon item that fired the arrow. The other four (Power, Flame, Multishot,
> Quick Charge) work normally, and on 1.20.1 all six work. **The mod's own
> modules are not affected** by this.

---

## 📥 Installation

1. Install **[Fabric Loader](https://fabricmc.net/use/)** for 1.20.1 or 1.21.1.
2. Download the matching **[Fabric API](https://modrinth.com/mod/fabric-api)**.
3. Drop both jars (Fabric API + Defend The Block) into your `mods/` folder.
4. Launch the game. On first run the mod creates `config/defendtheblock.json`.

Works for **single player** and **dedicated servers** alike.

---

## ⚠️ Before you play: read this

<table>
<tr><td>

### 💀 This mod deletes your save

When the Nexus falls, the mod kicks every player, shuts the server down and
**deletes the world folder**. That is the challenge's game over, and it is
**irreversible**.

To keep just the defeat message without losing the world, edit
`config/defendtheblock.json`:

```json
{ "deleteWorldOnNexusDestroyed": false }
```

**Try the mod on a throwaway world before committing to a real one.**

</td></tr>
</table>

**A consequence worth knowing:** the chunks around the Nexus stay loaded at all
times, so **invasions happen even with nobody online**. Waves start at nightfall
whether or not a player is present, and the Nexus takes damage with no one
defending. A few nights like that and it falls. If you are stepping away from a
server for a while, consider turning `keepNexusChunksLoaded` off.

---

## 🚀 Getting started

<table>
<tr><td width="60"><h3 align="center">1</h3></td><td>

**Gather materials and craft the Nexus.** You will need obsidian, diamonds and
an eye of ender. Take your time — the next step is permanent.

</td></tr>
<tr><td><h3 align="center">2</h3></td><td>

**Pick the spot carefully and place the Nexus.** After this it never comes back
out. Prefer flat ground with room to build around it. Remember that everything
within 1 chunk becomes **free of natural spawning** — your yard is safe by
default.

</td></tr>
<tr><td><h3 align="center">3</h3></td><td>

**Use the 3-day grace period.** A warning appears on screen each dawn telling
you how many days are left. Spend that time raising a first wall and crafting at
least one turret.

</td></tr>
<tr><td><h3 align="center">4</h3></td><td>

**Set up your turrets.** Place them, load arrows, feed iron to raise their tier.
One well-chosen module book is worth more than one extra turret.

</td></tr>
<tr><td><h3 align="center">5</h3></td><td>

**Hold the first night.** Invasion 1 is almost a tutorial: 10 mobs alive at
most, one every ~7 seconds, no stat bonuses. The next ones will not be like that.

</td></tr>
<tr><td><h3 align="center">6</h3></td><td>

**Collect the loot at dawn** and rebuild. The rubble the horde left cleans
itself up. The next night brings more mobs, tougher and better armed.

</td></tr>
</table>

> 🖼️ **`docs/images/primeira-noite.png`** — the first invasion arriving: the
> horde seen from atop the wall, HUD visible in the corner.

---

## 🏗️ What you build

Every item lives in the mod's **own creative inventory tab**, "Defend The
Block" — not scattered across the vanilla ones.

![Mod blocks and items](docs/images/blocos_e_itens.png)

<sub>Mod textures blown up, not in-game screenshots.</sub>

### 🔷 Nexus Block

```
O D O      O = Obsidian
D E D      D = Diamond
O D O      E = Eye of Ender
```

- Works **only in the Overworld**.
- **Only the first one counts.** Place a second and it returns to your inventory.
- Once placed **it never comes out**: it does not break by hand, does not break
  in creative, resists explosions, and pistons cannot push it. Only invaders can
  take its health down.
- **400 health** by default. Reach zero and it is over.

> 🖼️ **`docs/images/nexus.png`** — the Nexus just placed, with its activation
> particles and the HUD showing a full health bar.

### 🏹 Arrow Turret

![Turret texture](docs/images/textura_torreta.png)

```
 C         C = Crossbow
I R I      R = Block of Redstone
I I I      I = Iron Ingot
```

A crossbow on a tripod that **turns to its target on its own** and fires. It is
an entity, not a block. It **only fires once actually aimed** — it turns first
and shoots after, never the other way around.

| Right click with... | What happens |
|---|---|
| 🖐️ Empty hand | Opens the **stats screen** |
| 👊 A punch (attack) | Picks the turret up **with its tier, enchantments and modules stored in the item**, plus the arrows |
| 🏹 Arrows | Loads ammo (accepts tipped and spectral arrows) |
| ⛏️ Current tier's material, turret damaged | **Repairs health** |
| 💎 Next tier's material, at full health | Contributes to the upgrade |
| 📗 Enchanted book | Applies Power, Punch, Flame, Piercing, Multishot or Quick Charge |
| 📚 **Module book** | Installs a module or raises its grade |
| 🪨 **Grindstone** | Strips every module (the grindstone is not consumed) |

**The 5 tiers:**

| Tier | Damage | Range | Reload | Health | Magazine | Upgrade cost |
|---|---|---|---|---|---|---|
| Wooden | 2.0 | 14 | 2.0s | 20 | 64 | — (starting tier) |
| Iron | 3.0 | 21 | 1.6s | 30 | 96 | 20× iron ingot |
| Golden | 4.0 | 28 | 1.2s | 40 | 128 | 7× gold ingot |
| Diamond | 5.5 | 36 | 0.8s | 55 | 192 | 4× diamond |
| Emerald | 7.0 | 46 | 0.5s | 75 | 256 | 2× emerald |

Progress is **per unit**: every correct material you click in counts as one
point, and the turret levels up on its own once it reaches the total. You do not
need it all in hand at once. The current tier's material also **repairs** the
turret when it is damaged.

**Picking a turret up does not destroy your investment.** Punching it returns an
item carrying tier, enchantments, modules, upgrade progress and current health —
the tooltip shows all of it. Placing it back restores the turret as it was.
Repositioning an emerald turret is cheap; **but letting the horde destroy it
still costs everything**, and the item dropped on death comes back blank.

**Range climbs steeply per tier** on purpose: it is the attribute that changes
the turret's *role*. A wooden one covers only the Nexus surroundings; an emerald
one is artillery. Turrets **never hit each other**, and a turret standing in
front of another **blocks its line of fire** — destroy the one in front and the
shot goes through again.

> 🖼️ **`docs/images/torreta-niveis.png`** — the five turrets side by side,
> showing the texture difference per tier.

> 🖼️ **`docs/images/aba-torreta.png`** — the stats screen open, with health,
> ammo, damage/range/reload and installed modules.

### 📚 Turret modules

**Tier** raises everything at once, so it offers no choice: two emerald turrets
are identical. **Modules** exist for the opposite reason — each touches **one**
attribute, and **every turret accepts at most 2 types**.

That cap is what turns the system into a decision: range + rate of fire makes
support artillery; damage + venom makes an execution post; quiver + scavenger
barely ever needs resupplying.

| Module | Grades | Effect per grade | Recipe (all take 1 book in the centre) |
|---|---|---|---|
| 🔵 **Range** | 3 | +15% range | 3× spyglass + ender pearl |
| 🔴 **Damage** | 3 | +20% arrow damage | 2× flint + 2× diamond |
| 🟢 **Fortitude** | 3 | +25% turret max health | 3× block of iron + obsidian |
| 🟡 **Rate of Fire** | 3 | −15% reload time | 3× block of redstone + block of quartz |
| 🟠 **Quiver** | 3 | +50% magazine capacity | leather + 2× arrow + chest |
| 🩷 **Volley** | 2 | +1 arrow/shot, **at no extra ammo cost** | 5× arrow + dispenser |
| 🟤 **Scavenger** | 3 | +20% chance to **spend no arrow** | hopper + 2× emerald + feather |
| 🩵 **Frost** | 3 | Arrows apply **Slowness** | 2× blue ice + 2× packed ice |
| 💚 **Venom** | 3 | Arrows apply **Poison** | 2× spider eye + 2× fermented spider eye |

**How grades work:** a grade is **not a separate item**. Applying the same book
again to the same turret raises its grade up to that module's maximum. One
recipe per type, rather than one per type-and-grade combination.

Details worth knowing:

- 🎯 **Volley and Scavenger do not cancel out.** A shot has always cost exactly
  one arrow, no matter how many fly. Volley raises how many fly; Scavenger
  sometimes does not charge even that one.
- ❄️ **Frost and Venom stack with the loaded arrow.** Loaded tipped arrows? Their
  effect still applies and the module's goes on top.
- 🪨 **A wrong module is not permanent.** Right click with a **grindstone** to
  strip everything. The books do not come back (same as the vanilla grindstone),
  but the slots are freed.
- 💾 **The book is only consumed when something changes.** Refusal for a full
  slot or a maxed grade returns the item.
- ⚡ **Rate of Fire stacks with Quick Charge**, it does not replace it.

> 🖼️ **`docs/images/modulos.png`** — the nine module books side by side in the
> creative tab, showing their distinct colours.

### 🔮 Gathering Totem

```
G E G      G = Gold Ingot
G P G      E = Emerald
 G         P = Ender Pearl
```

Used with the **same gesture as placing a block** — right click, in the air or
on a surface — but it places nothing. It pulls **every player carrying a totem**
to the Nexus. Handy for regrouping the team when night falls. 30s cooldown.

### 🥚 Invader eggs

The mod's tab has **one egg per special zombie** — ladder, TNT, builder,
firestarter and miner. The mob spawns with that ability guaranteed, already
recruited by the invasion, and marches to the Nexus normally. It **does not
count toward the wave's official tally**, so spawning several will not mess up
the HUD — they exist to test each behaviour in isolation.

---

## 🧟 What comes for you

### 🌙 The horde

An invasion has **no fixed mob count** — there is no "kill 10 and it's over". It
spawns continuously from dusk to dawn, always inside the invasion ring, and the
only brake is the **cap on invaders alive at once**: one dies, another spawns.
The night only ends when the sun comes up.

That cap rises with every invasion, and **each mob gets tougher alongside it**:

| Invasion | Alive cap | Batch | Interval | Health | Damage | Armour | Material |
|---|---|---|---|---|---|---|---|
| 1 | 10 | 1 | 6.7s | 1.00x | 1.00x | 9% | leather |
| 3 | 18 | 2 | 6.1s | 1.16x | 1.10x | 19% | leather |
| 5 | 26 | 2 | 5.5s | 1.32x | 1.20x | 29% | gold |
| 10 | 46 | 4 | 4.0s | 1.72x | 1.45x | 54% | iron |
| 15 | 66 | 6 | 2.5s | 2.12x | 1.70x | 79% | diamond |
| 20 | 86 | 7 | 1.0s | 2.52x | 1.95x | 90% | diamond |
| **24+** | **100** | 8+ | 0.8s | 2.84x | 2.00x | 90% | diamond |
| **26+** | 100 | 9+ | 0.8s | **3.00x** | 2.00x | 90% | diamond |

The three ramps finish close together on purpose: **damage** saturates at
invasion 21, **quantity** at 24 and **health** at 26. After that the invasion
stops growing — the cap exists so the server can cope, not to make the game
impossible.

For scale: on invasion 1 the horde takes ~67 seconds to reach 10 alive, out of a
~500 second night. On invasion 24 it fills all 100 in **~5 seconds** and spends
the rest of the night topping them up.

> ℹ️ **About the damage ramp:** it does not affect **skeletons** (arrow damage
> comes from the projectile) nor **creepers and ghasts** (their damage is the
> explosion and the fireball). The **health** ramp applies to everyone.

**Who shows up:** zombie, skeleton and creeper from the first night; spider,
husk, stray, cave spider, witch, zombie villager, drowned and vindicator phasing
in later; plus **Nether** reinforcements — magma cube, wither skeleton, blaze,
zombified piglin, piglin brute, hoglin and ghast. Endermen are deliberately left
out.

> 🖼️ **`docs/images/horda.png`** — a late-game night arriving in force, showing
> the mob density at the high cap.

### 🕐 The 3-day grace period

Placing the Nexus does **not** throw an invasion at you that same night. You get
3 in-game days to prepare. Each dawn an on-screen warning tells you how many days
remain, and on the debut day it announces the invasion is that very night — raid
horn included.

The already-announced day is saved, so reconnecting or restarting the server does
not repeat the message.

### 🏡 The safe yard and the invasion ring

Two concentric regions around the Nexus, with different rules:

- **No-spawn zone** — the Nexus chunk **plus every adjacent one** (3x3 by
  default). **No hostile mob spawns there**, neither the invasion nor vanilla
  natural spawning. Anything near the Nexus marched in from outside. This does
  **not** affect players or passive mobs — animals spawn normally.
- **Invasion ring** — a circular band starting where the safe square ends,
  extending 3 chunks out. With the defaults, invaders spawn between chunk 2 and
  chunk 4 from the Nexus.

### 🧠 They are not stupid

Zombies and skeletons come with rolled armour and weapons, and the quality climbs
every night. **From invasion 4 onward pieces start coming enchanted**, and the
enchantment power grows alongside.

Some invaders also carry abilities that change the game:

| Mob | Ability | Chance |
|---|---|---|
| All but creeper | 🪜 **Climb ladders** — including ones built by other mobs | 100% |
| All but creeper | 🚪 **Break down closed doors** instead of just opening them | 100% |
| 🕷️ Spider | Climbs walls and **shoots web** that pins the target | 25% |
| 💥 Creeper | **Blows itself up on the obstacle** blocking the horde, 3s fuse after touching it. **Does not damage the Nexus** | 100% |
| 🧟 Zombie | ⛏️ **Pickaxe**: mines through the wall in the way | 18% |
| 🧟 Zombie | 🪜 **Ladders**: builds a ladder column up the obstacle | 16% |
| 🧟 Zombie | 🧱 **Builder**: raises a path or pillar to the Nexus, far faster than a regular invader | 12% |
| 🧟 Zombie | 🔥 **Firestarter**: sets wooden obstacles alight instead of breaking them | 10% |
| 🧟 Zombie | 💣 **TNT**: **throws** a single TNT in an arc, like a projectile | 7% |

Each zombie gets **at most one** of these. When the **Nexus is suspended in the
air**, the chances for ladder and builder zombies are multiplied by 2.5 — those
are precisely the two abilities that solve that scenario.

> 🖼️ **`docs/images/habilidades.png`** — a collage showing a zombie mining the
> wall, another building a ladder and a creeper about to blow up an obstacle.

### 🛡️ Walling in the Nexus works

Being near the Nexus is not enough to hit it: there has to be a **clear line** to
the block. A mob outside a wall does not push damage through it — the cover
becomes the obstacle to break instead. Covering the Nexus, which ought to be the
most obvious defence in the game, **is a real defence**.

It is not permanent, though: the horde will mine, blow up or walk around the
wall. It buys time, not immunity.

### 🎯 Combat priorities

The Nexus is always the endgame, but that does not mean mobs ignore you:

- An invader **fights you or a turret** when genuinely close by — it will not
  cross the map chasing you.
- Once **in striking range of the Nexus**, nothing else takes priority: it hits
  the block even with you right beside it.
- **Skeletons keep their distance** and shoot instead of marching, as long as
  they have a target in sight. With no target, they advance on the Nexus.
- An invader **only targets what it can see** (120° field of view) — no magical
  detection from behind.
- **Whoever is working gets room.** If a zombie is mining or building a ladder,
  the others back off so they do not shove it out of place — unless they can
  already reach the Nexus.

### 🧹 The terrain resets at dawn

Every block an invader placed during the night — cobblestone pillars and bridges,
ladders, spider webs — is **removed when the invasion ends**, with no drops. The
world does not accumulate rubble invasion after invasion.

The cleanup only removes blocks **the mobs** placed that are still of a type they
know how to place: if you mined the pillar and built something else there, the
sweep leaves it alone.

### 💎 After every night: loot

Each invasion survived scatters valuables around the Nexus — ores, food, blocks,
bottles of experience. The number of rolls grows (with a cap) as invasions go on.
Since invaders **do not drop items** by default, this is the night's reward.

---

## 📊 HUD and commands

A panel in the top-right corner tracks the campaign: invasions survived, current
invasion, mobs alive and spawned tonight, multiplier, and the Nexus health bar.

> 🖼️ **`docs/images/hud.png`** — a crop of the top-right corner with the panel
> during an active invasion.

| Command | Level | What it does |
|---|---|---|
| `/dtb status` | everyone | Campaign state |
| `/dtb multiplier <0.1–20>` | 2 | Spawn rate multiplier (saved to config) |
| `/dtb forcewave` | 2 | Starts the next invasion right now — great for testing |
| `/dtb stopwave` | 2 | Cancels the current invasion |
| `/dtb removenexus` | 2 | Removes the Nexus **without deleting the world** |

---

## ⚙️ Configuration

Everything lives in **`config/defendtheblock.json`**, created on first boot. It
is plain JSON: edit, save and restart the world/server.

<details open>
<summary><b>🔷 Nexus and defeat</b></summary>

| Key | Default | What it does |
|---|---|---|
| `nexusMaxHealth` | `400` | Nexus health |
| `nexusDamagePerHit` | `3` | Damage per mob hit |
| `nexusHitCooldown` | `20` | Ticks between two hits from the same mob (20 = 1s) |
| `deleteWorldOnNexusDestroyed` | `true` | **Deletes the world on defeat.** Set `false` to only show the message |
| `keepNexusChunksLoaded` | `true` | Keeps the Nexus chunks loaded (invasions run offline) |
| `forcedChunkRadius` | `4` | Loaded radius, in chunks (9x9) |

</details>

<details>
<summary><b>🌙 Invasion pace and scale</b></summary>

| Key | Default | What it does |
|---|---|---|
| `gracePeriodDays` | `3` | Grace days before the first invasion. `0` disables it |
| `mobMultiplier` | `1.0` | Global spawn rate multiplier |
| `baseSpawnInterval` | `140` | Ticks between spawn batches on invasion 1 |
| `spawnIntervalStepPerWave` | `6` | How much the interval shrinks per invasion |
| `minSpawnInterval` | `15` | Smallest possible interval, in ticks |
| `baseSpawnBatch` | `1.0` | Invaders per batch on invasion 1 |
| `spawnBatchGrowthPerWave` | `0.30` | How much the batch grows per invasion |
| `baseConcurrentInvaders` | `6` | Cap on invaders alive on invasion 1 |
| `concurrentInvadersPerWave` | `4` | How much that cap rises per invasion |
| `maxConcurrentInvaders` | `100` | **Absolute cap on invaders alive.** Lower it if your server struggles |
| `zombieExtraSpawnCount` | `2` | Cap on extra zombies per roll |
| `zombieExtraSpawnWavesPerStep` | `4` | Invasions needed to unlock one more extra zombie |
| `invadersDropLoot` | `false` | Invaders drop items on death |

</details>

<details>
<summary><b>📈 Invader stat scaling</b></summary>

| Key | Default | What it does |
|---|---|---|
| `invaderHealthPerWave` | `0.08` | Extra health per invasion (+8%) |
| `invaderHealthMultiplierMax` | `3.0` | Health multiplier cap. `1.0` disables it |
| `invaderDamagePerWave` | `0.05` | Extra melee damage per invasion (+5%) |
| `invaderDamageMultiplierMax` | `2.0` | Damage multiplier cap. `1.0` disables it |

</details>

<details>
<summary><b>🏡 Zones around the Nexus</b></summary>

| Key | Default | What it does |
|---|---|---|
| `noSpawnChunkRadius` | `1` | Radius (chunks) of the no-spawn square. `1` = the 3x3 |
| `spawnRingChunks` | `3` | Width (chunks) of the circular invasion ring |
| `attractionChunkRadius` | `4` | Recruitment radius for mobs that spawned on their own |

</details>

<details>
<summary><b>🧟 Invader abilities</b></summary>

| Key | Default | What it does |
|---|---|---|
| `creeperBreachChance` | `1.0` | Chance of a breaching creeper |
| `creeperObstacleFuseTicks` | `60` | Creeper fuse after touching the obstacle (3s) |
| `creeperBreachTimeoutTicks` | `100` | Time spent trying to **reach** the obstacle before lighting up anyway |
| `spiderWebChance` | `0.25` | Chance of a web-shooting spider |
| `zombiePickaxeChance` | `0.18` | Chance of a miner zombie |
| `zombieLadderChance` | `0.16` | Chance of a ladder zombie |
| `zombieBuilderChance` | `0.12` | Chance of a builder zombie |
| `zombieFireStarterChance` | `0.10` | Chance of a firestarter zombie |
| `zombieTntChance` | `0.07` | Chance of a TNT zombie |
| `elevatedNexusBuilderBonus` | `2.5` | Ladder/builder multiplier when the Nexus is suspended |
| `invadersCanBridge` | `true` | Mobs build block paths when the Nexus is elevated |
| `maxMineHardness` | `30.0` | Hardest block an invader can mine |
| `mineTicksPerHardness` | `14` | Mining pace (higher = slower) |
| `doorBreakTicksPerHardness` | `10` | Door-breaking pace |

</details>

<details>
<summary><b>🏃 Variable zombie speed</b></summary>

| Key | Default | What it does |
|---|---|---|
| `zombieSlowChance` | `0.25` | Chance of a slower zombie |
| `zombieSlowFactor` | `0.75` | How slow (0.75 = 75% of normal speed) |
| `zombieFastChanceBase` | `0.08` | Chance of a faster zombie on invasion 1 |
| `zombieFastChancePerWave` | `0.035` | How much that chance rises per invasion |
| `zombieFastChanceMax` | `0.55` | Cap on the fast-zombie chance |
| `zombieFastFactor` | `1.3` | How fast (1.3 = 130% of normal speed) |

</details>

<details>
<summary><b>🎯 Targeting behaviour</b></summary>

| Key | Default | What it does |
|---|---|---|
| `invaderFieldOfViewDegrees` | `120.0` | Invader field of view for picking targets |
| `nexusPriorityEngageRange` | `6.0` | Radius (blocks) to fight a player/turret before returning to the Nexus |
| `rangedTurretPriorityRange` | `20.0` | Radius within which skeletons prioritise shooting turrets |
| `maxTurretEngageTicks` | `200` | Time focused on one turret before giving up |
| `turretIgnoreTicksAfterGiveUp` | `120` | Cooldown before targeting another turret |
| `workerClearanceRadius` | `3.0` | Space the horde clears around whoever is mining/building |

</details>

<details>
<summary><b>🏹 Turret</b></summary>

| Key | Default | What it does |
|---|---|---|
| `turretDamageMultiplier` | `1.0` | Global turret damage multiplier |
| `turretConsumesAmmo` | `true` | Turrets spend ammo. `false` = infinite ammo |
| `turretRepairHealthPerItem` | `8.0` | Health restored per material unit when repairing |
| `turretVerticalFovDegrees` | `60.0` | Vertical aperture of the vision cone (defines the blind spots) |

</details>

<details>
<summary><b>📚 Turret modules</b></summary>

Each value is the gain **per grade**. Since a turret only accepts 2 types, the
real ceiling for any one of them is "value × max grade", not the sum of all.

| Key | Default | What it does |
|---|---|---|
| `turretModuleRangePerGrade` | `0.15` | Extra range per grade of the Range module |
| `turretModuleDamagePerGrade` | `0.20` | Extra damage per grade of the Damage module |
| `turretModuleHealthPerGrade` | `0.25` | Extra health per grade of the Fortitude module |
| `turretModuleAmmoPerGrade` | `0.50` | Extra ammo per grade of the Quiver module |
| `turretModuleReloadPerGrade` | `0.15` | Reload reduction per grade of the Rate of Fire module |
| `turretModuleSavePerGrade` | `0.20` | Chance per grade for Scavenger to spare the arrow (capped at 0.9) |
| `turretModuleEffectSeconds` | `2.0` | Seconds of effect per grade of the Frost and Venom modules |

</details>

<details>
<summary><b>🔮 Totem</b></summary>

| Key | Default | What it does |
|---|---|---|
| `totemCooldown` | `600` | Gathering Totem cooldown, in ticks (600 = 30s) |

</details>

### 💡 Config recipes

**"I want to play without risking my world"**
```json
{ "deleteWorldOnNexusDestroyed": false }
```

**"My server cannot handle the late nights"**
```json
{ "maxConcurrentInvaders": 40, "concurrentInvadersPerWave": 2 }
```

**"I want more difficulty from the start"**
```json
{ "gracePeriodDays": 0, "baseConcurrentInvaders": 20, "baseSpawnInterval": 60 }
```

**"Just more mobs, without them getting stronger"**
```json
{ "invaderHealthMultiplierMax": 1.0, "invaderDamageMultiplierMax": 1.0 }
```

---

## 🔧 Building from source

```bash
./gradlew buildAll                # builds both jars
./gradlew :fabric-1.20.1:build    # 1.20.1 only
./gradlew :fabric-1.21.1:build    # 1.21.1 only
./gradlew :fabric-1.21.1:runClient   # test in development
```

Jars land in `fabric-<version>/build/libs/`.

> ⚠️ **Always use the repository's `./gradlew`, never a Gradle installed on your
> machine.** The wrapper is pinned to **Gradle 8.10.2** because **Fabric Loom
> 1.7** uses the incubating `Problems.forNamespace(String)` API, removed in
> Gradle 8.11. Running 8.11+ fails right at plugin application. To move Gradle
> up, Loom has to move up with it (1.9+) in `build.gradle`.

A single **JDK 21 compiles both versions** — the build uses `options.release`,
not Java toolchains. If you want to `runClient` on 1.20.1, use JDK 17: that
Minecraft version expects it.

### Asset generators

Textures are generated by script, **deterministically** — the PNGs are already
committed and only need regenerating if you change the art:

```bash
pip install pillow                          # only for the first two
python3 tools/generate_textures.py          # block, item and turret textures
python3 tools/generate_showcase.py          # showcase images for this documentation
python3 tools/generate_turret_modules.py    # module books (no external dependency)
```

The last one does more than textures: from **a single table**, it writes the
texture, the model, the recipe **for both versions** and the three translation
keys in `en_us` and `pt_br` for each module. Adding a new module means adding an
entry there, adding the constant in `TurretModifier.java` and running the
script — `ModItems` and `ModItemGroups` create and register the items straight
from the enum.

### Project layout

```
common/                       shared code and assets
  src/main/java/              all the game logic
  src/main/resources/         textures, models, blockstates, lang
fabric-1.20.1/                build + 1.20.1 compat + recipes
fabric-1.21.1/                build + 1.21.1 compat + recipes
tools/                        texture generators
docs/images/                  documentation images
docs/DEVELOPMENT.md           technical diary and verification status
docs/DESENVOLVIMENTO.md       the same diary, in Portuguese
```

Each subproject compiles `common/` together with its own
`com.defendtheblock.compat` package. **Every API that changed between 1.20.1 and
1.21 is isolated there** — `Identifier`, `EntityType.Builder`, `PersistentState`,
enchantments, item components, networking and status effects — and the shared
code never knows which version it is running on. Recipes are per version because
the JSON `result` field changed from `item` to `id` in 1.21.

---

## 📋 Project status

✅ **`./gradlew buildAll` compiles both versions** — last verified at commit
`28833d8`, on JDK 21.

⚠️ **It has not been through a full playtest yet.** Compiling proves the code
matches Minecraft's APIs; it does not prove the game behaves as described above.
Treat the balance numbers on this page as intent, not as measured results.

The detailed history of what was verified, what could still break and the
reasoning behind each implementation decision lives in:

### 👉 **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)**

If you are going to touch the code, start there — several decisions that look
crooked are scars from real problems.

---

## 📄 License

Apache 2.0 — see [LICENSE](LICENSE).
