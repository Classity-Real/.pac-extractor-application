package dev.classityreal.pext.locked

/**
 * Editable steps. AI agents may change this list.
 * They may not add flash, unlock, erase, or fastboot.
 * Unknown ids are rejected. The list is used only when Experimental is on.
 * With Experimental off, [LockedConfig] is the only pipeline.
 */
object ExtractSteps {
    val current: List<String> = listOf(
        "copy-in",
        "list",
        "extract",
        "copy-out"
    )
}
