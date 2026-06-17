# ADR 0003 — Eclipse spine architecture (Phase 1)

- **Status:** Accepted
- **Date:** 2026-06-17
- **Phase:** 1 (Eclipse spine — the critical slice)
- **Decider:** Orchestrator

## Context

Phase 1 builds the heart of the client (DESIGN_SPEC §6.3, §12): intercept a
SkyBlock menu, parse it (Atlas), render it in Lumen, and route a click back
through the vanilla interaction path. The §2 compliance contract and the §11
fitness functions are load-bearing and must be encoded now, before any menu
interaction code exists. This ADR fixes the module boundaries and the exact
shared interfaces so subsystems can be built in parallel without drift.

## Decisions

### 1. Atlas core is Minecraft-agnostic (so it is unit-testable)

Parsing/fingerprinting logic operates on plain data, not MC types. A thin
adapter converts MC `ScreenHandler`/`ItemStack` into these models; only the
adapter imports `net.minecraft.*`. Core types (`gg.orrery.atlas`):

```kotlin
enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, DIVINE, SPECIAL, UNKNOWN }

data class ParsedItem(
    val slot: Int,                              // backing ScreenHandler slot index
    val name: String,                           // display name, § codes stripped
    val lore: List<String>,                     // lore lines, § codes stripped
    val rarity: Rarity,
    val skyblockId: String?,                    // ExtraAttributes "id" if present
    val count: Int,
    val extraAttributes: Map<String, String>,   // shallow string view
)

data class ParsedMenu(
    val title: String,                          // § codes stripped
    val size: Int,                              // total slots
    val rows: Int,                              // size / 9
    val items: List<ParsedItem?>,               // indexed by slot; null = empty
)

interface MenuRecognizer {
    val id: String                              // stable, e.g. "skyblock_menu"
    fun matches(menu: ParsedMenu): Boolean      // title pattern + key-slot signature
}

object MenuRegistry {                           // first match wins; null = fall through to vanilla
    fun register(recognizer: MenuRecognizer)
    fun recognize(menu: ParsedMenu): MenuRecognizer?
    val all: List<MenuRecognizer>
}
```

Recognizers match on **title pattern + key-slot items**, not absolute layout, so
they survive minor Hypixel changes (§6.5). First recognizer shipped:
`SkyBlockMenuRecognizer` (title == "SkyBlock Menu"). Recognizers will be
remotely disable-able via Ephemeris later; design the registry so a recognizer
can be flagged off (a bad parse degrades to vanilla, never breaks).

### 2. The single compliance chokepoint (§2.3, §11)

All menu actions route through exactly one function. It is the ONLY place in the
entire codebase permitted to call the vanilla `clickSlot`. It constructs no
packets; `clickSlot` emits the identical `ClickSlotC2SPacket` vanilla would.

```kotlin
// gg.orrery.eclipse — THE chokepoint. Nothing else may call interactionManager.clickSlot.
object Interaction {
    fun clickSlot(syncId: Int, slotIndex: Int, button: Int, action: SlotActionType)
    // body: client.interactionManager?.clickSlot(syncId, slotIndex, button, action, client.player)
}
```

Render and act are separated (§11): Lumen widgets cannot call the network; they
invoke an input-authorized callback that ultimately reaches `Interaction.clickSlot`.

### 3. §11 enforced as a build-failing fitness test

A pure-JVM test (`src/test/kotlin/.../compliance/ComplianceFitnessTest.kt`,
runs in `gradle test`/`check`, no MC classpath) statically scans
`src/main/kotlin` and FAILS the build if:

- a call to `interactionManager(...).clickSlot(` appears **outside** the
  chokepoint file (`eclipse/Interaction.kt`);
- any raw client→server packet send appears anywhere (`*C2SPacket`
  construction, `networkHandler.send`, `sendPacket`);
- (best-effort) a timer/scheduler path reaches the chokepoint.

Calls to our wrapper `Interaction.clickSlot(` are allowed everywhere — they are
how widgets act. This is the architectural fitness function from §11.

### 4. Ownership / interception flow (Eclipse)

On container open, a Mixin into the handled-screen lifecycle calls
`AtlasAdapter.parse(...)` → `MenuRegistry.recognize(...)`. If a recognizer owns
it, Eclipse opens an Orrery Lumen `Screen` reading from the live `ScreenHandler`;
otherwise vanilla is left untouched. Coverage is incremental: ship one menu,
widen.

## Consequences

- Atlas core + the §11 fitness test are buildable and testable now, in parallel,
  without the interception mixin or Lumen.
- The interception Mixin + the first Lumen `Screen` (which depend on both the
  Atlas adapter and the chokepoint) are the next step after these integrate.
- The chokepoint is the one place compliance can be audited; §11 keeps it the
  only one mechanically.
