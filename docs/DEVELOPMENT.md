# Development diary

**English** · [Português (Brasil)](DESENVOLVIMENTO.md)

> This file is the **honest record** of how the mod got where it is: what was
> verified and how, what has **not** been tested yet, the bugs each playtest
> turned up, and the reasoning behind every implementation decision that is not
> obvious from the code alone.
>
> It lives apart from the [README](../README.en.md) on purpose. The README
> answers "what the mod does and how to play it"; this is the "why it is like
> this" and, above all, **what could still break**. If you are going to touch
> the code, start here — several decisions that look crooked are scars from real
> problems, and undoing them without reading why tends to bring the bug back.
>
> A rule that repeats across almost every section below: **an assumed API
> signature has taken this mod down more than once.** Whenever there is a choice
> between an elegant unverified API and an ugly one already proven on both
> versions, this project picks the second — and writes down why.

---

## Verification status

**`./gradlew buildAll` compiles both versions successfully**, last verified at
commit **`28833d8`** (Fabric Loom 1.7.4 + Gradle 8.10.2, JDK 21, on a machine
with network access). That proves the code matches Minecraft's and Fabric's APIs
on both versions — it does **not** prove the mod works in play. Nobody has
placed the Nexus, watched the turret turn, or survived an invasion yet.

That build settled a debt of **nine commits** that had been written and checked
only by structural `javac`. With it, the first-use APIs that had been flagged
round after round leave the risk list and count as proven on both versions:

| API | Where | Was the risk for |
|---|---|---|
| `ArrowEntity#addEffect(StatusEffectInstance)` | `DtbCompat` (both) | Frost and Venom modules |
| `EntityAttributes.GENERIC_ATTACK_DAMAGE` | `InvaderEquipment` | invader damage scaling |
| `NbtComponent.of` / `copyNbt` | `DtbCompat` 1.21 | turret state stored in the item |
| `ItemStack#setSubNbt` / `getSubNbt` | `DtbCompat` 1.20.1 | same, on 1.20.1 |
| `Items.GRINDSTONE`, `BlockPos.Mutable`, `MathHelper.floor` | various | stripping modules, `findCover` |

The bet of containing each of them inside `DtbCompat` (a compile error on one
version, never a mixin crash in play) never had to be cashed in — none failed.
What this build does **not** change: none of it has been executed. Compiling
proves signatures, not behaviour.

How it got here: the code was written in an environment that blocks
`maven.fabricmc.net`, `libraries.minecraft.net` and `piston-meta.mojang.com`, so
Loom could not download Minecraft, mappings or the Fabric API. Before the first
real build, what could be verified there was: every JSON valid, every PNG
generated and inspected, and the Java files passing `javac`'s parser with no
structural error — which proves syntax, not API signatures. The first real
compile, done later, flagged 5 errors, all fixed:

| Error | Version | Fix |
|---|---|---|
| `LookControl.lookAtEntity` does not exist | both | it is `lookAt(Entity, float, float)` |
| `HudRenderCallback` passes `RenderTickCounter`, not `float` | 1.21.1 | lambda with inferred type, works on both |
| `SoundEvents.ITEM_CROSSBOW_LOADING_END` became a `RegistryEntry` | 1.21.1 | `DtbCompat.CROSSBOW_LOADED` constant |
| `PersistentProjectileEntity.setPunch` gone | 1.21.1 | `DtbCompat.applyPunch` |
| `setPierceLevel` became private | 1.21.1 | `DtbCompat.applyPiercing` |

**Known limitation on 1.21:** the turret's Punch and Piercing have **no
effect**. 1.20.5 started deriving those two from the enchantments on the
`ItemStack` of the weapon that fired, and the turret keeps its levels in its own
NBT, not on a weapon. The other four enchantments (Power, Flame, Multishot,
Quick Charge) work normally on both versions, and on 1.20.1 all six work. To fix
it, the turret would have to assemble an enchanted crossbow stack and pass it as
the weapon in the arrow's constructor.

**Still untested in play.** Compiling proves API signatures, not behaviour: what
remains is running `runClient`, placing the Nexus, forcing an invasion with
`/dtb forcewave` and seeing whether the turret really turns and fires. That is
the stage where mixin problems (`MobEntityAccessor` field names) or game-logic
problems would surface, if they exist.

### Changes after the playtest

The sections above already reflect the adjustments requested after playing, over
two rounds. **Neither went through a new `buildAll` or an in-game test yet** —
verification is still structural only (`javac` with no errors), the same
limitation as always.

**Round one:** turret redesign with a texture per tier, variable upgrade cost,
every mob (creeper included) damaging the Nexus, skeleton arrows damaging the
Nexus, bridging when the Nexus is elevated, the turret treated as a ranged
target, and the fix for the shoot-before-aiming delay. Highest-risk points:

- **`ArrowNexusDamageMixin`** mixes into `ProjectileEntity#onBlockHit`. If Loom
  fails to apply that mixin (an error at boot, not at build), it is because the
  method is declared on a different class in the hierarchy on that specific
  version — the Fabric log points exactly where.
- **`ModelTransform.of(...)`** (used to angle the crossbow arms) is an API that
  already existed in the project indirectly, but had never been called with
  rotation arguments here; if the signature is wrong it is a compile error, easy
  to find.
- **Bridging** is deliberately a heuristic (jump + place a block underfoot), not
  a pathfinder. It may look odd on complicated terrain; the goal is only to keep
  the Nexus from becoming impossible to reach.

**Round two:** the Nexus as a real combat priority (player/turret engaged only
when genuinely close), door breaking for every invader except the creeper, and
the mod's own inventory tab. Highest-risk points:

- **`InvaderCombatPriority`** clears `mob.getTarget()` every tick when the target
  is too far away. That can produce a 1-tick "flicker" where a vanilla combat AI
  starts reacting before the target is cleared again — expected, imperceptible,
  not a bug.
- **`FabricItemGroup`** (the mod's own inventory tab) is a Fabric API convenience
  historically stable across versions, unlike the Mojang APIs that caused the
  first compile's errors — low risk, but still untested here.
- **Closed-door detection** uses `DoorBlock`/`DoubleBlockHalf`, an API stable
  since two-half doors were introduced (Minecraft 1.13) — low risk.

**Round three:** the real playtest revealed the eternal turret-focus bug (see
"Player and turret are targets" in the README) and brought the stats screen,
turret repair, post-invasion loot, `/dtb removenexus`, the firestarter zombie and
the ladder-climbing fix. As with previous rounds, **none of it went through
`buildAll` or an in-game test yet**. Risk points, highest to lowest:

- **The turret stats screen is the riskiest area of the project.** It is the
  first time the code uses `Screen`/`DrawContext` outside the HUD (which only
  draws an overlay, never opens a screen) and the first **dedicated S2C**
  networking beyond HUD syncing. That is why it was deliberately simplified:
  **there is no `ScreenHandler`/`Slot`/`ScreenHandlerRegistry`** — an API family
  with no tested precedent in this project, and one that would require
  registering a menu type per version. Instead of a synchronised inventory slot,
  material still goes in the way it already worked (right click the turret), and
  the screen only shows the latest state. That lowers the risk, but
  `TurretStatsScreen`, `TurretStatsPayload` (1.21) and the
  `ClientPlayNetworking`/`ServerPlayNetworking` registration (1.20.1) remain
  untested in play.
- **`TurretEntity#damage(DamageSource, float)`** is the first time the mod
  overrides that method (rather than just calling it, as `WebShotEntity` already
  did). The `boolean damage(DamageSource, float)` signature is one of the most
  stable in the entity API — it did not even change across the 1.20.1 → 1.21
  jump — but "overriding" and "calling" are different things, and this had never
  been tested in a real build here.
- **Reading an ambiguous request:** the playtest asked both for "left click
  opens the screen" and "a punch returns the turret" — since a punch *is* the
  left click in Minecraft, the two statements contradict each other. It was read
  as: **right click with an empty hand** opens the screen (the normal
  interact/open-GUI convention) and **a punch (attack)** returns the turret. If
  that is not what made sense, just ask for the swap.
- **The degenerate-angle predicate** (`TurretEntity#isDegenerateAngle`) excludes
  from target selection any mob almost exactly below or above the turret, fixing
  the root cause of the "inert turret" instead of just treating the symptom. Low
  risk (it is only maths over coordinates), but the resulting behaviour (the
  turret ignoring mobs on its own vertical axis) has not been seen in play yet.
- **Post-invasion loot** uses `ItemEntity` with a fixed list of vanilla items —
  basic entity API, low risk.
- **The firestarter zombie** only places `Blocks.FIRE` above the wooden block
  (letting vanilla fire spread) instead of burning the block directly — simpler
  and cheaper than simulating combustion by hand, but it depends on vanilla fire
  actually catching on the block below, which has not been seen in play yet.

**Round four:** the second playtest (now with the turret and the screen really
in-game) found deeper root causes than the previous round covered. Summary of the
diagnoses:

- **The turret stuck to one target and ignored everything else.** The cause was
  target selection itself: an `ActiveTargetGoal` in the target selector held the
  target until it died or left the *follow range* (40 blocks, far beyond any
  tier's actual firing range), never re-evaluating whether that target could
  still actually be attacked. A mob out of firing range, behind a wall, or simply
  worse positioned than another one walking past remained "the target"
  indefinitely. **The `ActiveTargetGoal` was removed entirely** — now
  `TurretShootGoal` itself picks and re-evaluates the target every 5 ticks,
  switching to a genuinely engageable candidate (in range, visible, off its own
  vertical axis) whenever the current one stops qualifying, or to one much
  closer (a 2-block margin so it does not oscillate between two similar targets).
- **A turret on a tall tower could not hit mobs below it** (even when not
  directly underneath). Range was measured as 3D distance
  (`squaredDistanceTo`), so the tower's height "ate" part of the range budget —
  a wooden-tier turret (12 blocks) sitting 15 blocks up could barely see the
  ground. Switched to **horizontal-plane distance only**
  (`TurretEntity#horizontalSquaredDistanceTo`), the way a defence tower should
  work.
- **Creepers did not blow up on the Nexus, spiders sat on top of it dealing no
  damage, and generally "the turrets got focused on mobs and dealt no damage to
  the Nexus".** This was the root cause behind three different complaints:
  `AttackNexusGoal` (numeric priority 4) lost movement/look control to the mob's
  own **vanilla** combat goals (`ZombieAttackGoal` and friends, typically
  priority 2) whenever a target was nearby — even with the mob standing literally
  on the Nexus. Since the target (a turret) stays "engaged" for up to 60 ticks
  because of the previous round's fix, that priority-loss window became far more
  common. Two changes: (1) `AttackNexusGoal#canStart()` now returns `true`
  unconditionally when the mob is already within striking range of the Nexus, no
  longer yielding to "there is a target nearby" in that case; (2) the whole chain
  of custom goals (breach, climb, bridge/web, attack the Nexus) moved to
  **negative priorities** (-3 to 0), guaranteeing it always beats any vanilla
  combat goal in the fight over Control.MOVE/LOOK, not just nominally but in
  fact. This is probably the most important fix of the round.
- **`/dtb removenexus` left mobs "attacking thin air"** where the Nexus used to
  be, and the new Nexus (placed elsewhere) took damage from those old mobs. Two
  stacked causes: `despawnAllInvaders` only discarded invaders with
  `countsForWave() == true`, leaving out any "attracted" mob (recruited outside
  the official spawn batch) — those survived any reset forever. And even the ones
  discarded in time, `AttackNexusGoal#attackNexus` applied damage to the **current
  global state** of the Nexus without checking whether the position the mob had
  stored still matched — a mob standing where the old Nexus was could hurt a new
  Nexus at another coordinate. Fixed at both points: the despawn now takes down
  every invader (counted or not), and `attackNexus` refuses to hit (and "retires"
  as an invader) if the Nexus position it knows is no longer the current one.
- **The stats screen was "washed out" with text spilling outside the frame.** Two
  real problems: nothing darkened the world behind the screen (low contrast
  against a bright sky), and text was drawn straight into a box of fixed
  height/width with no wrapping — any translation longer than the available space
  simply spilled. Rewritten to: (1) darken the whole screen before drawing the
  panel; (2) wrap all text with `TextRenderer#wrapLines` before drawing; (3)
  compute the box height from the real content (line count after wrapping), never
  a fixed value. The "Close" button stopped being a `ButtonWidget` registered in
  `init()` because the box changes height as text wraps — it is now drawn and
  hit-tested by hand in `render()`/`mouseClicked()`.

As always, **none of it went through `buildAll` or an in-game test yet** — and
this is the round with the most goal-behaviour changes (negative priorities, the
turret picking its own target) since the project started, so it deserves special
attention in the next playtest.

**Round five — the turret's actual root cause.** The next playtest showed the
turret *still* locked onto a target without firing, even after the previous
round's whole target-selection rewrite. The reason was much further down, and had
nothing to do with target selection:

> Vanilla's `LookControl` runs **after** the goals, inside
> `MobEntity#tickNewAi`, and since `shouldStayHorizontal()` is `true` by default
> it executes `setPitch(0)` every tick. In other words: the turret computed the
> right pitch in the goal and vanilla zeroed it right after, every tick. It could
> never aim up or down — and since firing only happens once aim converges within
> tolerance, any target not exactly at its eye level was **never** hit, and the
> focus was never released.

That explains all the remaining symptoms at once: "it stays focused on a mob it
cannot shoot", "it does not shoot anything below", "a turret up high hits nothing
down there" and even "it worked at first and then stopped" (the first target
happened to be at the right height). The fix stores the aim in its own fields
(`aimYaw`/`aimPitch`) and **re-applies them after `super.tick()`**, that is, after
`LookControl` has done its damage — without depending on overriding
`LookControl`, whose API varies across versions.

Other changes in this round:

- **A real vision cone.** The previous patch only excluded mobs exactly on the
  vertical axis. Now the turret has a real vertical aperture
  (`turretVerticalFovDegrees`, 60 degrees by default): it turns 360 degrees
  horizontally but only tilts within that limit, which creates two natural
  **blind spots** (a cone above and one below). A target outside the cone is
  never chosen.
- **An absolute safety net against locking up.** Regardless of any
  cone/range/line-of-sight check, if the turret spends 40 ticks on the same
  target **without managing a single shot**, it drops that target and ignores it
  for 5 seconds. If some unforeseen case shows up in the future, it becomes at
  most a 2-second pause instead of a permanent lock-up.
- **The screen was blurred because of vanilla itself.** It was not a contrast
  problem: `Screen#render` starts by calling `renderBackground`, which on 1.20.5+
  applies **blur** to the entire framebuffer. Since the `super.render(...)` call
  sat at the end of the method, the blur landed on top of the panel and text that
  had just been drawn. Since this screen registers no widgets (the button is
  hand-drawn too), the fix was simply **not to call super** and to darken with a
  `fill` of its own.
- **Creeper with a 5-second deadline.** It used to light up only after managing
  to touch the obstacle (3.2 blocks), which could take a long time or never
  happen. Now it is evaluated before the approach phase: it lights up immediately
  if already close, and has at most `creeperBreachTimeoutTicks` (100 ticks = 5s)
  stuck trying to get there before lighting up anyway. The
  `creeperBreachChance` default also rose to 1.0 — with the creeper's spawn
  weight now low, a creeper that cannot open a path contributes nothing.
- **Skeletons prioritise turrets.** A version of `TargetTurretGoal` came back,
  now **restricted to ranged mobs** (`RangedAttackMob`). The reason it was
  removed before — melee mobs chasing turrets they could not reach — does not
  apply to something that shoots from a standstill. That priority has its own
  range (`rangedTurretPriorityRange`, 20 blocks), and the focus still has a cap:
  `maxTurretEngageTicks` (200 ticks) for any invader, followed by a cooldown
  before it can target another turret.

**Round six — new content and balance tweaks.** This round touched fewer bugs and
more content:

- **The zombie as the horde's backbone, in two ways.** Its roll weight went up
  (36 → 48), which changes the *proportion*; and each rolled zombie now spawns
  with 2 more alongside (`zombieExtraSpawnCount`), which changes the *quantity*.
  Those are deliberately different things — touching only the weight would leave
  the horde at the same density, just more monotonous.
- **More special zombies.** Pickaxe 12% → 18%, ladders 10% → 16%, firestarter
  6% → 10%, TNT 5% → 7%, plus the new builder at 12%. The firestarter zombie was
  already rolled regardless of nearby wood — the scenario only decides whether the
  ability is *useful*, not whether it spawns.
- **Builder zombie** (`BLOCK_BUILDER`): does what any stuck invader eventually
  does, only much better — it starts after 1 stuck cycle instead of 3, places a
  block every 2 ticks instead of 6, and goes up to 128 blocks instead of 48. It
  reuses the existing `BridgeToNexusGoal` rather than duplicating the logic.
- **A suspended Nexus pulls in whoever solves the problem.** The chances for
  ladder and builder zombies are multiplied by `elevatedNexusBuilderBonus` (2.5)
  when the Nexus is floating or well above the ground where the horde spawns.
- **Thrown TNT.** The bomber zombie now carries **a single** TNT and **throws it
  in an arc** up to 16 blocks, at the target it is chasing or at the Nexus
  itself, with the fuse already running. It is a normal `TntEntity` with an
  initial velocity — no new entity needed.
- **Invaders no longer drop items** (`invadersDropLoot`, default false). XP still
  drops. See round eight below for how that was done — the first attempt was
  wrong and crashed the game at boot.
- **Turret range became the attribute that defines its role.** The progression
  was timid (12→32); it is now 14→46. Since range is measured on the horizontal
  plane, those numbers hold equally on the ground or atop a tower.
- **Invader eggs**: five new items (`InvaderEggItem`), one per ability, which
  summon a zombie with that ability guaranteed and already recruited by the
  invasion, but outside the wave tally.
- **Route re-evaluation** (`AttackNexusGoal#tryDetour`): before marking the path
  as blocked, the mob sweeps a radius-4 cube looking for a point closer to the
  Nexus that vanilla pathfinding can genuinely reach (`findPathTo` +
  `Path#reachesTarget`), and heads there. The pathfinding cost is only paid for
  the best candidate, once per stuck cycle — doing it for every position in the
  cube, across dozens of mobs, would be far too expensive.

**Round seven — the horde that froze.** The report was that as the night went on
the mobs went **still** until the whole horde locked up. It was not lag or an
unloaded chunk: it was a bug in `BreachObstacleGoal`, and the previous rounds'
code had made it worse twice.

> `canStart()` asked only "does this mob have any breaching ability?". Since
> `DOOR_BREACHER` is given to **every** invader except the creeper, the answer
> was always yes. The goal then took movement control for any marked obstacle,
> got close, called `navigation.stop()` — and fell through every branch without
> executing any, because the wall was not a door and the mob had no pickaxe, TNT
> or ladders. It stood there, with `shouldContinue()` returning true forever.

The effect was **cumulative and irreversible per mob**: every invader that
bumped once into a wall it did not know how to handle froze for good. Since more
and more mobs bump into something over the course of a night, the horde kept
"stiffening" until everything looked stopped — exactly the reported symptom.

Two of my own changes made it worse: the **negative priorities** from round four
put that goal at the mod's highest priority (so a frozen mob would not even hand
movement back to `AttackNexusGoal`), and cutting the TNT zombie down to **a
single stick** in round six created one more path into the stuck state (after
using the TNT, `placeTnt` starts returning false and the mob falls into the same
hole).

The fix attacks the cause and adds two safety nets:

- **`canHandle(pos)`** replaces the old `hasBreachAbility()`: the question is now
  "can this mob do anything about **this** obstacle?" — a door only counts with
  `DOOR_BREACHER`, TNT/ladders only count while the item is still in the slot,
  the firestarter only counts on wood, the pickaxe only counts up to the hardness
  limit. If the answer is no, the obstacle is dropped immediately and
  `AttackNexusGoal` (with round six's route re-evaluation) takes over to try going
  around.
- **A time cap** (`MAX_GOAL_TICKS`, 400 ticks): no legitimate breach takes that
  long, and without it any unforeseen case would go back to being a permanently
  frozen mob.
- **`stop()` releases the obstacle marking**, so the mob does not re-enter the
  same place on the next tick and loop.

Worth recording that the explanation I had given earlier for this same report —
turrets running out of ammo — was wrong, and wrong in two ways: I had understood
the symptom backwards (mobs becoming *less* inert) and the hypothesis did not
explain a permanent lock-up.

**Round eight — the mixin crash, and the lesson about signatures.** Round six's
drop suppression **crashed the game at boot** on 1.21.1. Mixin went straight to
the point:

```
Invalid descriptor on MobEntityMixin->@Inject::defendtheblock$skipInvaderEquipment
Expected (ServerWorld, DamageSource, boolean, CallbackInfo)
but found (DamageSource, int, boolean, CallbackInfo)
```

I had **assumed** `dropEquipment(DamageSource, int, boolean)` held on both
versions. On 1.21.1 the `int lootingMultiplier` gave way to a leading
`ServerWorld` (part of 1.21's enchantment/loot rework) — meaning the signature
differs between versions, and that injection could never have lived in
`common/`. It was the same kind of error that had already happened with
`ITEM_ARMOR_EQUIP_IRON`: asserting an API signature I had no way to verify here.

The fix reduces the risk surface rather than just swapping the signature:

- **Equipment (armour/weapon) no longer uses any mixin.**
  `mob.setEquipmentDropChance(slot, 0f)` is public, stable API on both versions,
  and solves the part that litters the ground most (and is most valuable: diamond
  armour). That is `InvaderEquipment#dropChance`.
- **The loot table (rotten flesh, bones, arrows…) still needs a mixin**, but it
  now lives **per version**, each with its own real signature:
  `dropLoot(DamageSource, boolean)` on 1.20.1 and
  `dropLoot(ServerWorld, DamageSource, boolean)` on 1.21.1.
- **And that mixin was isolated in its own config**
  (`defendtheblock-drops.mixins.json`) marked **`"required": false`**. If the
  `dropLoot` signature is still wrong on some version, Mixin logs a warning and
  moves on — the mod loads normally and only the loot-table items start dropping
  again, instead of the game refusing to open. No other mixin in the mod (the
  ones that actually hold the invasion up) is exposed to this.

**Round nine — the no-spawn zone.** The two regions around the Nexus (the safe
square + the circular invasion ring, described under "The safe yard and the
invasion ring") moved into a single class, `NexusZones`, used both by invasion
spawning and by natural-spawn suppression. Keeping both rules in one place avoids
the classic error of the invasion spawner and the safe-zone check disagreeing by
half a chunk and a mob spawning inside the yard that was supposed to stay clean.

The `spawnChunkRadiusMin`/`spawnChunkRadiusMax` keys were replaced by
`noSpawnChunkRadius` (safe square radius) and `spawnRingChunks` (ring width),
which describe intent instead of two loose radii. `forcedChunkRadius` and
`attractionChunkRadius` rose to 4, so they keep covering exactly the area where
invasions happen.

**Round ten — cleanup, field of view and the creeper's new role.**

- **Invader blocks are undone at dawn.** Every block the horde places
  (pillar/bridge cobblestone, ladders, webs) goes through `InvaderBlocks`, which
  records the position in the invasion's persistent state. At the end of the wave
  everything is removed **with no item drops**. Before removing, it checks the
  block is still one the invasion knows how to place — if the player mined it and
  built something else there, the cleanup leaves it alone. The only case that
  slips through is the player placing exactly the same block type at exactly the
  recorded position; storing every block's full state in the save would not be
  worth the cost to cover that.
- **A real field of view** (`InvaderVision`). Vanilla only offers `canSee`, which
  is line of sight — it answers "is the path clear?", not "is it looking that
  way?". On its own, it let a mob target something behind its own head. Now the
  player and turrets only become targets inside a 120-degree cone around the
  head's facing, *and* with clear sight.
- **Turrets became everyone's target**, not just ranged attackers'. The fear of
  reopening the "horde glued to turrets" bug is addressed by the cone (far more
  restrictive than the first version's line of sight) plus the time caps already
  present in `InvaderCombatPriority`.
- **Creepers no longer damage the Nexus.** Their role is exclusively opening a
  path by blowing up obstacles.
- **Every invader mines.** The pickaxe stopped being a requirement and became a
  speed advantage (`unarmedMineTicksMultiplier`, 3x). That changes difficulty
  considerably: a wall with no pickaxe carrier nearby stopped being an
  impassable barrier, and now several mobs dig in parallel. The creeper still
  does not dig — its way is exploding.

One side effect needed adjusting: `BreachObstacleGoal`'s time cap was a fixed
number (400 ticks), which would end up shorter than the legitimate time a
pickaxe-less mob needs to dig stone. It is now computed per obstacle (work time +
slack), otherwise the anti-lock-up protection would start interrupting valid work.

**Round twelve — the walled-in Nexus hole, and the ramp.**

- **A covered Nexus took damage through the wall.** A real bug: `attackNexus`
  only checked distance, never whether a block sat in the way. Now
  `NexusPathing#findCover` samples the line from the mob's eye to the block and,
  if it finds something solid in between, that block becomes the obstacle instead
  of the Nexus taking damage. Sampling every 0.25 blocks instead of using
  `World#raycast` was deliberate: it uses only `isSolidBlock`, which this project
  already uses in several places, and avoids depending on `RaycastContext`'s
  signature — the kind of API whose cross-version differences have taken this mod
  down twice.

  A behavioural detail: when covered, the mob does **not** stop navigating. It
  keeps trying to move, which makes it look for an exposed side and triggers
  route re-evaluation. Stopping there would leave the horde pressed against the
  wall never trying to go around.
- **Creeper with a 3s fuse** on touching the obstacle, instead of exploding the
  instant it arrives. Since vanilla's fuse lasts 30 ticks after `ignite()`, the
  custom countdown lights up with exactly that much left — so the total matches
  the config and the second half is the white swelling that warns the player. The
  system also does not waste creepers: if another invader opens the path first,
  `shouldContinue()` drops the goal and it never lights up at all. Lighting up
  away from the obstacle became an emergency case (stuck trying to get there).
- **The difficulty ramp was redone.** Night 1 saturated a cap of 35 live invaders
  within the first seconds. It now starts at 10 and takes ~25 invasions to reach
  the same cap. Extra zombies are also unlocked gradually (none until invasion 4),
  and the armour chance dropped from 18% to 4% per piece on the debut night.
- **Skeletons stop advancing when they have a target.** The cause was an
  inconsistency between two numbers: `InvaderCombatPriority` kept the target up to
  20 blocks for ranged mobs, but `AttackNexusGoal` used 6 for everyone when
  deciding whether to yield movement control. With a turret 15 blocks away it kept
  the target and marched at the same time. Both now call
  `InvaderCombatPriority#engageRange`, the single source of the answer.

**Round eleven — restrictions, grace period and horde coordination.** Part of
this round undoes things from the previous one, as a design decision after seeing
the result:

- **Breaking blocks is a privilege of three abilities again** (pickaxe, TNT and
  creeper). Letting the whole horde dig made any wall irrelevant — the effect on
  difficulty was too large. The `unarmedMineTicksMultiplier` penalty went with it,
  becoming dead config.
- **Melee no longer hunts turrets.** `TargetTurretGoal` went back to ranged
  attackers only. For melee, a turret only becomes a target if all three
  conditions hold together: it hit them first, it is within the short engagement
  radius, and **pathfinding can actually get there** — that last one is new,
  checked once on the tick the target is adopted. Without it the mob kept banging
  its head against a turret on top of a pillar: close in a straight line,
  unreachable in fact.
- **Turrets have no friendly fire** and **block each other's line of sight**. The
  immunity is simple (the arrow has the turret as its attacker). The blocking had
  to be done by hand with a raycast against the other turrets' boxes, because
  vanilla's `canSee` only tests blocks — as far as it is concerned, an entity in
  the way does not exist. Unblocking is automatic and needs no event: a destroyed
  turret leaves the entity list and the shot goes through on the next rescan.
- **A 3-day grace period** after placing the Nexus, with an on-screen warning per
  day and a special one on the debut day. The announced day is persisted, so
  reconnecting does not repeat the message. Old worlds (with no recorded placement
  date) simply do not enter a grace period.
- **Variable zombie speed**, with the fast chance rising per invasion. Applied via
  `setBaseValue` on the attribute rather than `EntityAttributeModifier`, on
  purpose: the modifier's constructor changed between 1.20.1 (UUID) and 1.21
  (Identifier), whereas `setBaseValue` is identical on both — exactly the kind of
  difference that has taken this mod down before.
- **The horde makes room for whoever is working** (`YieldToWorkerGoal`). Details
  that needed care: the goal runs at priority -1, above `AttackNexusGoal`,
  otherwise the drive toward the block would beat the retreat; but it refuses to
  run if the mob is already within striking range of the Nexus, so it never gets
  in the way of the final objective. The "working" flag is cleared unconditionally
  in `stop()` on both goals that raise it — if it leaked, the entire horde would
  keep making room for a mob doing nothing. And there is a 200-tick retreat cap,
  for the usual reason.

A note on control conflicts: three goals coexist at priority level -1, and they
do not fight because they take disjoint controls — `BridgeToNexusGoal` uses JUMP,
`SpiderWebShotGoal` uses LOOK and `YieldToWorkerGoal` uses MOVE.

An implementation note: natural-spawn suppression is done by **discarding the
entity in `ServerEntityEvents.ENTITY_LOAD`**, not by intercepting vanilla's
`SpawnHelper`. That is a Fabric event already used successfully in this project
(it is the same one that recruits attracted mobs), whereas touching `SpawnHelper`
would require a mixin whose signature I cannot verify here — and an assumed
signature has taken this mod down twice. The cost is that the mob is created
before disappearing, rather than the spawn attempt being barred beforehand; in
practice that is invisible in play.

**Round thirteen — an audit of dead code and hot paths.** This round adds no
features: it is a requested sweep over the existing code, looking for
optimisation, simplification and orphaned things. It went out as two commits on
purpose, so the zero-risk one could be separated from the one touching AI.

What the sweep did *not* find, worth recording: of `DtbConfig`'s 53 keys,
**none** is orphaned — all are read somewhere. There were also no unused imports
and no private fields written but never read.

**Commit 1 — dead code removal** (no behaviour change):
`InvaderAbility.VALUES`/`toMask()`/`all()`, `InvaderData.getAbilityMask()`
/`setAbilityMask()`, `InvasionData.removeInvader()`, `TurretEntity.getAmmo()`,
`WaveComposition.unlocked()`. `NexusZones.noSpawnRadius()` became `private`,
which is all the use it has.

The case that required a decision was `countsForWave`: it was written,
**persisted to NBT** and had no reader. The last one disappeared when
`despawnAllInvaders` started clearing every invader rather than only the counted
ones. There were two legitimate ways out — start using it again, or remove the
concept. Removed, because what really expresses "counts toward the wave" is the
`activeInvaders` set: it is fed only by `spawnInvader`'s batch (never by an
attracted mob or an egg) and it is what the HUD reads via `getMobsAlive()`.
Having both was having two sources for the same answer, one of them with no
consumer. Save consequence: the `"Counts"` key stops being written, and old saves
that carry it simply ignore it on load.

**Commit 2 — hot paths and duplication.** The most serious finding was in
`TurretShootGoal`: `blockedByTurret` did its own entity lookup and is called from
inside `isEngageable`, which in turn runs for **every candidate** inside
`findBestTarget`. So a sweep of N mobs cost N entity lookups, per rescan, per
turret — the mod's worst loop, and precisely in a base full of turrets with a
horde on top. Now the list of nearby turrets is fetched once per rescan and
reused; turrets do not move, so being up to 5 ticks stale is harmless, and the
old javadoc already promised unblocking only "on the next rescan".
`findBestTarget` also started measuring distance **before** calling
`isEngageable`, because anything already farther than the current best cannot win
and does not need to pay for cone/sight/raycast.

Allocation inside tick loops, the other two spots: `NexusPathing#findCover` runs
every tick for every invader in range of the Nexus and created a `Vec3d` plus a
`BlockPos` every 0.25 blocks of line; it became scalar arithmetic with a
`BlockPos.Mutable`. `AttackNexusGoal#tryDetour` sweeps 243 positions and
allocated two objects per position just to measure distance; it became scalar,
compared squared, with an object only for the best candidate.

Duplication that now has a single source: the Nexus striking range (2.8) was a
constant in `AttackNexusGoal` and an identical one in `YieldToWorkerGoal` — two
copies opened the possibility of a band where the mob would retreat exactly where
it should attack, which is the class of bug rounds nine and twelve already paid
to learn. `Math.floorDiv(timeOfDay, 24000)` was in three places and became
`NexusManager#currentDay`. And `NexusPathing#center` now delegates to
`Vec3d.ofCenter`, which does exactly the same maths.

Smaller simplifications: `announceCountdown` had three conditions where one
suffices (past the debut day, the grace period is over and the countdown is
already zero by definition); `YieldToWorkerGoal.shouldContinue` called
`clearance()` twice in the same expression; and `InvasionData.getPlacedBlocks()`
returned the live set, which let any caller silently bypass the 20000-position
cap and the `markDirty()` in `addPlacedBlock`/`clearPlacedBlocks` — it is now an
immutable view.

Verification for this round: structural `javac` on both versions, no errors, no
removed symbol still referenced and no orphaned imports. **Like previous rounds,
it did not go through `buildAll` or an in-game test.** Since commit 2 touches AI,
what to check in play is: a turret with another turret in front still does not
shoot through it and starts shooting again when the front one is destroyed; a
stuck mob still goes around walls; a walled-in Nexus still takes no damage; and
the horde still makes room for whoever is working without failing to hit the
block when already in range.

**Round fourteen — the stat ramp.** The request was to replace "a fixed mob count
per wave" with "a cap on live mobs that rises to 100, along with stat modifiers".

Half of that **already existed**, worth recording so it does not get lost: the
invasion never had a fixed count. Ever since the round where spawning became
continuous, it spawns from dusk to dawn limited only by the live cap
(`activeInvaders.size() >= cap`), the wave only closes at dawn, spawning already
happens inside the bounded ring, and `maxConcurrentInvaders` was already **100**,
reached at invasion 24. The table in the "The horde" section had wrong batch
numbers (it said 24 per batch at invasion 25; the real figure is 9) — now
corrected with values computed from the formulas, not from memory.

What was **genuinely missing** was the other axis: there was **no** stat scaling
at all. Nothing touched an invader's `GENERIC_MAX_HEALTH` or
`GENERIC_ATTACK_DAMAGE`; the only attribute touched was zombie speed. In practice
a night-30 zombie was identical to a night-1 one, just in greater numbers — which
is exactly the criticism in the request. Now `InvaderEquipment#scaleStats` scales
health (up to 3x, saturating at invasion 26) and melee damage (up to 2x,
saturating at 21), so the three ramps — quantity, health, damage — finish close
together.

Health rises faster than damage on purpose: extra health lengthens the fight and
the player still has time to react, extra damage simply kills. Doubling the
damage of 100 mobs is far more violent than tripling their health.

An honest limitation of the approach: touching the attribute's base value
(instead of `EntityAttributeModifier`, because of the same 1.20.1/1.21
incompatibility as always) means damage scaling **does not reach skeletons** —
arrow damage comes from the projectile, not the attribute — nor creepers and
ghasts, which have no attack attribute. Health applies to everyone. If skeletons
need to scale too, the way is through arrow damage, not here.

**Performance work alongside, and not out of perfectionism:** 100 simultaneous
mobs is only a usable number if per-tick cost does not explode. Two goals started
their counters at zero, and a whole batch of invaders spawns and starts the goal
on the same tick — meaning the entire horde recomputed its route **on the same
tick**, forever. Pathfinding is by far the most expensive thing a mob does; at
the cap that would be a spike of 100 lookups on one tick and zero on the next 19.
`AttackNexusGoal#start` now randomises the initial offset of `repathTimer` and
`stuckTimer`, and `YieldToWorkerGoal` does the same with `scanCooldown` (each of
its `canStart` calls is an entity lookup). Same total load, spread out instead of
concentrated. That adds to the `findCover` and `TurretShootGoal` optimisations
from the previous round, which had already been done with a large horde in mind.

Verification: structural `javac` on both versions, no errors. The ramp table was
**computed from the code's formulas**, not estimated. `GENERIC_MAX_HEALTH` is
already used in this project (on the turret) and compiles on both versions;
`GENERIC_ATTACK_DAMAGE` is a first appearance, but it belongs to the same family
of constants and only changed name in 1.21.5, out of this mod's reach. **It did
not go through `buildAll` or an in-game test.** What to check: whether a late
night really fills and holds the cap, whether the server copes with 100 alive,
and whether 3x health with 90% diamond armour left the turret unable to kill
anything — that last one is the most likely balance risk, and
`invaderHealthMultiplierMax` is the knob to adjust.

**Round fifteen — turret modules.** The request was to separate *customisation*
from *upgrading*: beyond raising the tier, being able to pick isolated attributes
via a book, with grades and a recipe per type, up to 2 per turret.

The central design is the **cap of 2 types**. Without it the system would become
one more progress bar — an endgame turret would simply have all nine modules.
With it, building a turret becomes a decision, and two emerald turrets side by
side can have different roles.

Two modelling choices that deserved an alternative and do not have one, for good
reasons:

- **A grade is not an item.** Applying the same book again raises the grade. The
  alternative (one item per type+grade combination) would mean 25 items, 25
  recipes and 25 textures to express the same thing. This way it is 9 of each,
  and grade progression reuses the gesture the player already uses to feed
  upgrade material to the turret.
- **The book is not a real `EnchantedBookItem`.** A custom enchantment would
  require registering `Enchantment`s, and registration changed completely between
  1.20.1 (simple registry) and 1.21 (dynamic datapack registry) — two entirely
  different paths for a system the turret can already read on its own. Here the
  book is an ordinary item and the turret is what understands the effect.

A detail that was already right and only got generalised: a shot has **always**
cost exactly one arrow, no matter how many fly — vanilla Multishot already
behaved that way because `consumeAmmo()` is called once per shot, not once per
arrow. The Volley module falls under the same rule, so "fire more than one arrow
without spending more than one" needed no exception at all.

An escape hatch was needed: without one, installing the wrong module would occupy
one of the two slots forever and the only solution would be destroying the
turret. Right clicking with a **grindstone** strips everything (the books do not
come back, same as vanilla).

Risks and mitigations, in order:

- **`ArrowEntity#addEffect` is this round's biggest bet.** It is the first time
  the mod makes an arrow carry an effect, and `StatusEffects.X` is a
  `StatusEffect` on 1.20.1 but a `RegistryEntry<StatusEffect>` on 1.21 — the same
  divergence that took this mod down with the crossbow sounds. That is why the
  call lives in `DtbCompat.applyArrowEffect`, one per version: if the signature is
  wrong it is **a compile error on one version only**, not a mixin crash in play.
  The shared code passes only the effect's name as text and never touches those
  types.
- **The enchanted glint is not forced by code.** `Item#hasGlint` existed on
  1.20.1 and disappeared in 1.20.5, which moved to the
  `enchantment_glint_override` component. Those are two incompatible paths for a
  cosmetic detail, so what makes the book look enchanted is the texture.
- **The grindstone sound was swapped as a precaution.** `BLOCK_GRINDSTONE_USE`
  would be the natural pick, but some `SoundEvents` fields became `RegistryEntry`
  on 1.21 and there is no way to know which without compiling. I used a
  low-pitched `BLOCK_ANVIL_USE`, which is already used in that same file and is
  therefore provably safe on both.
- **The nine textures are script-generated and were inspected** (dark-covered
  book, spine and central crest in the module's colour). They went through two
  revisions: the second version came out worse than the first — it turned into
  coloured stripes instead of a book — and was redone.

Verification: structural `javac` on both versions with no errors, no orphaned
imports, all 45 JSON files valid, and confirmation that the 9 modules have lang
entries (3 keys each), a model, a texture and a recipe on both versions — with
the right result field in each (`item` on 1.20.1, `id` on 1.21.1). `en_us` and
`pt_br` have exactly the same key set. **It did not go through `buildAll` or an
in-game test.**

**Round sixteen — the documentation became two.** The README had grown to 1462
lines, and more than half of them were *this* file: verification status, playtest
history and the reasoning behind every decision. That served whoever touches the
code and got in the way of whoever just wants to play — both audiences were
reading the same wall of text.

Now there are two documents with different contracts:

- **README** answers "what the mod does and how to play it": what it is,
  supported versions, installation, the warning that the save gets deleted,
  getting started, each feature, and the **64 configuration keys** grouped by
  topic in collapsible blocks, with ready-made recipes for common cases.
- **This file** answers "why it is like this and what could still break".

None of the technical content was discarded — the whole log was moved, not
summarised.

About the image placeholders: each one is a visible block with the path and a
description of what the screenshot should show, **rather than an `![](...)`
pointing at a nonexistent file** — which would render a broken-image icon on
GitHub. `docs/images/README.md` repeats the list as a checklist, with tips on how
to take each shot (`/dtb forcewave` to avoid waiting for nightfall,
`/dtb removenexus` to rebuild a scene in the same save).

**A hole closed along the way:** the 9 module book textures had been generated by
a script that stayed outside the repository. The README claimed that "textures
are generated by script and only need redoing if you change the art", and for
those nine that was false — they were committed PNGs with no generator. The
script became `tools/generate_turret_modules.py`, with the root path derived from
the file itself instead of hardcoded, and re-running it produces **zero diff**. It
also does not depend on Pillow, unlike the other two: the PNG is written by hand
with `struct` + `zlib`.

Verification for this round: the 11 internal index links checked against the 31
headings, every file link in all three documents resolves, the README's 9 image
placeholders match the 9 rows in the `docs/images/` table exactly, and **the 64
`DtbConfig` keys are all documented — none extra, none missing**, compared by
script against the code. The module recipe table was checked by counting
ingredients in the generated JSON, and it caught an error: I had written "2x
spyglass" for the Range module when the pattern uses 3.

**Round seventeen — picking up the turret stopped being destructive.** Found
while reviewing the project's state, not reported in a playtest.

`TurretEntity#damage` sends **any** melee attack from a player to `pickUp`, which
returned `new ItemStack(ModItems.ARROW_TURRET)` — a blank item. In other words, an
accidental left click erased tier, enchantments and modules.

It was not a regression: the tier was already lost that way before modules
existed. But round fifteen put an expensive system on top of a leaking container
— a Range III module costs 9 spyglasses and 3 ender pearls, and vanished in one
click. Repositioning a turret is a routine action and cannot cost that.

Now `toItemStack()` stores the tier, the six vanilla enchantments, the modules,
the upgrade progress and the **current health** inside the item, and
`ArrowTurretItem#useOnBlock` calls `applyFromStack` before spawning.

Three decisions worth noting:

- **Health goes into the bundle.** If the item always came back at full health,
  picking up and replacing would be a free repair and `turretRepairHealthPerItem`'s
  material cost would stop meaning anything.
- **A factory-fresh turret returns a blank item.** An item with NBT does not stack
  with one without it; without that check, placing and picking up a
  freshly-crafted turret would break the inventory stack for no reason.
- **Death still wipes everything.** Picking it up by hand is dismantling; being
  taken down by the horde is losing the investment. If death returned the full
  item, defending the turret would lose its weight. It is the only part of the old
  behaviour kept on purpose.

Compatibility: storing data in an `ItemStack` is one more hard divergence — plain
NBT on 1.20.1, the `CUSTOM_DATA` component on 1.21, which no longer has item NBT
at all. It became the `DtbCompat.putStackTag` / `getStackTag` pair, contained as
always: if the signature is wrong it is a compile error on one version, not a
crash in play. `NbtComponent.of` / `copyNbt` are first-use in the project; I
avoided `NbtComponent.DEFAULT` by using `get` + a null check, which is one fewer
symbol that can go wrong.

Along the way, the grade's Roman numeral had **three** copies (the chat message,
the stats screen and now the item tooltip). It became `TurretModifiers.grade`, a
single source — verified by script that only one occurrence of `"III"` remains in
the code.

Verification: structural `javac` on both versions with no errors, no orphaned
imports, `en_us` and `pt_br` identical in key set. **Still without `buildAll` and
without an in-game test.**
