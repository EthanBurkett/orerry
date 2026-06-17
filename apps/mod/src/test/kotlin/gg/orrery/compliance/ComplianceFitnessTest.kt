package gg.orrery.compliance

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * §11 Architectural Fitness Test — build-failing compliance chokepoint enforcement.
 *
 * This test statically scans `src/main/kotlin` and FAILS the build if any of the
 * following violations are found anywhere in the source tree:
 *
 * **Violation 1 — Vanilla clickSlot called outside the chokepoint.**
 * `interactionManager` followed (on the same logical line) by `.clickSlot(` is only
 * permitted in `eclipse/Interaction.kt`. Any other file containing this pattern is a
 * direct §2.3 / §11 violation.
 *
 * **Violation 2 — Raw client-to-server packet construction or send.**
 * Direct construction of `*C2SPacket` types, calls to `networkHandler.send(`, or
 * `sendPacket(` anywhere in main source are forbidden. Orrery never constructs or
 * sends packets manually — vanilla's `clickSlot` emits the identical packet through
 * the sanctioned path.
 *
 * Calls to OUR wrapper `Interaction.clickSlot(` are allowed everywhere and are NOT
 * flagged.
 *
 * Comment lines (lines whose first non-whitespace characters are `//` or `*`) are
 * excluded from scanning to avoid false positives in documentation.
 *
 * This test carries no Minecraft classpath dependency — it is pure JVM / file I/O.
 *
 * References: DESIGN_SPEC §2, §11; ADR 0003 §2, §3.
 */
class ComplianceFitnessTest {

    companion object {
        /**
         * Main source roots scanned, resolved relative to the Gradle working directory
         * (`apps/mod`). Both Kotlin AND Java are scanned: the interception Mixin lives in
         * `src/main/java`, and §11 must cover it too — no source path may bypass the
         * chokepoint. Gradle's `test` task sets the working directory to the project dir,
         * so these relative paths resolve correctly.
         */
        private val SOURCE_ROOTS = listOf(File("src/main/kotlin"), File("src/main/java"))

        /**
         * The one file that legitimately contains the vanilla clickSlot call.
         * Expressed as a suffix so it matches on any OS path separator.
         */
        private const val CHOKEPOINT_FILE = "eclipse/Interaction.kt"

        /**
         * Regex: `interactionManager` followed optionally by whitespace or a safe chain
         * (`?.`, `.`), then `.clickSlot(`. This matches the vanilla call pattern regardless
         * of intermediate safe-call operators.
         */
        private val VANILLA_CLICK_SLOT = Regex("""interactionManager\s*\??\.\s*clickSlot\s*\(""")

        /**
         * Our own wrapper. Lines matching this are explicitly ALLOWED even if they also
         * match vanilla patterns (they don't, but exclude defensively).
         */
        private val OUR_WRAPPER = Regex("""Interaction\s*\.\s*clickSlot\s*\(""")

        /**
         * Patterns forbidden everywhere in main source (raw packet send / construction).
         * Each entry is a display name → regex pair.
         */
        private val FORBIDDEN_PACKET_PATTERNS: List<Pair<String, Regex>> = listOf(
            "C2SPacket construction" to Regex("""new\s+\w*C2SPacket\s*\(|=\s*\w*C2SPacket\s*\(|\w*C2SPacket\s*\("""),
            "networkHandler.send(" to Regex("""networkHandler\s*\??\.\s*send\s*\("""),
            "sendPacket(" to Regex("""sendPacket\s*\("""),
        )

        /** Returns true if the line is a comment and should be excluded from scanning. */
        private fun isCommentLine(line: String): Boolean {
            val trimmed = line.trimStart()
            return trimmed.startsWith("//") || trimmed.startsWith("*")
        }

        /**
         * Normalises an OS path to use forward slashes for comparison against the
         * chokepoint suffix constant.
         */
        private fun File.forwardSlashPath(): String = path.replace('\\', '/')

        /** Collects all Kotlin + Java source files under the existing [SOURCE_ROOTS]. */
        private fun allSourceFiles(): List<File> =
            SOURCE_ROOTS.filter { it.isDirectory }.flatMap { root ->
                root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            }
    }

    @Test
    fun `source root exists`() {
        val kotlinRoot = SOURCE_ROOTS.first()
        assertTrue(kotlinRoot.exists(), "Source root does not exist: ${kotlinRoot.absolutePath}")
        assertTrue(kotlinRoot.isDirectory, "Source root is not a directory: ${kotlinRoot.absolutePath}")
        assertTrue(allSourceFiles().isNotEmpty(), "No source files found under $SOURCE_ROOTS")
    }

    /**
     * Violation 1: the vanilla `interactionManager.clickSlot(` call must appear ONLY in
     * `eclipse/Interaction.kt`.
     */
    @Test
    fun `vanilla clickSlot is only called from the compliance chokepoint`() {
        val violations = mutableListOf<String>()

        for (file in allSourceFiles()) {
            val normalised = file.forwardSlashPath()
            // The chokepoint itself is allowed to contain the vanilla call.
            if (normalised.endsWith(CHOKEPOINT_FILE)) continue

            file.readLines().forEachIndexed { index, line ->
                if (isCommentLine(line)) return@forEachIndexed
                // Our wrapper is allowed; skip lines that only call Interaction.clickSlot.
                if (OUR_WRAPPER.containsMatchIn(line)) return@forEachIndexed
                if (VANILLA_CLICK_SLOT.containsMatchIn(line)) {
                    violations += "${file.absolutePath}:${index + 1}: illegal vanilla clickSlot call — " +
                        "use Interaction.clickSlot() instead (DESIGN_SPEC §2.3/§11)"
                }
            }
        }

        assertTrue(violations.isEmpty(), buildViolationMessage("VANILLA clickSlot outside chokepoint", violations))
    }

    /**
     * Violation 2: no raw C2S packet construction or direct network sends anywhere.
     */
    @Test
    fun `no raw C2S packet construction or direct network sends`() {
        val violations = mutableListOf<String>()

        for (file in allSourceFiles()) {
            file.readLines().forEachIndexed { index, line ->
                if (isCommentLine(line)) return@forEachIndexed
                for ((name, pattern) in FORBIDDEN_PACKET_PATTERNS) {
                    if (pattern.containsMatchIn(line)) {
                        violations += "${file.absolutePath}:${index + 1}: forbidden pattern [$name] — " +
                            "Orrery never constructs or sends packets manually (DESIGN_SPEC §2.3/§11)"
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), buildViolationMessage("Raw C2S packet / network send", violations))
    }

    private fun buildViolationMessage(header: String, violations: List<String>): String =
        buildString {
            appendLine()
            appendLine("=== §11 COMPLIANCE VIOLATION: $header ===")
            violations.forEach { appendLine("  $it") }
            appendLine("Fix: route all menu actions through Interaction.clickSlot() in eclipse/Interaction.kt")
            appendLine("Ref: DESIGN_SPEC §2, §11; ADR 0003 §2, §3")
        }
}
