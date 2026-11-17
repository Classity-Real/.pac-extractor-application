# Agent rules

You may edit UI, workflow steps, and `app/src/main/java/dev/classityreal/pext/locked/ExtractSteps.kt`.

Do not edit `app/src/main/java/dev/classityreal/pext/locked/LockedConfig.kt`.
Do not add bootloader unlock, fastboot, oem unlock, flashing, or partition erase.
This app only extracts `.pac` files to storage. People unlock bootloaders on their own; a modified extract tool must not grow a write-to-device path.

Experimental mode in Settings is opt-in. Leave it off by default.
