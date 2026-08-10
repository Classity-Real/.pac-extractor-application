![codenation](https://github.com/Classity-Real/.pac-extractor-application/blob/main/Images/Banner.jpg?raw=true)

# PExt — Unisoc .pac Extractor (Android)

Android app to inspect and extract partitions (`boot.img`, `vendor_boot.img`,
`super.img`, modem blobs, etc.) out of Unisoc/Spreadtrum `.pac` firmware
packages, built with Kotlin + Jetpack Compose + **Material 3 Expressive**.

## How extraction actually works

The app ships two prebuilt `arm64-v8a` command-line tools —
**`unpac`** and **`pacextractor`** — and shells out to them via
`ProcessBuilder` rather than parsing the `.pac` format itself:

- `native/UnpacRunner.kt` wraps `unpac`, which supports `list` (partition
  names/sizes) and `extract <names...>` (pick a subset). This is the
  primary engine — it's what powers the checklist UI.
- `native/PacExtractorRunner.kt` wraps `pacextractor`, a simpler tool that
  always extracts every partition in one pass. Useful as a fallback if a
  given `.pac` trips up `unpac`.
- `native/SafBridge.kt` copies the SAF-picked input file into app cache
  (the CLIs need a real path, not a `content://` Uri) and copies the
  results back out into the user's chosen SAF output folder afterward.
- `native/NativeTools.kt` locates the binaries under
  `context.applicationInfo.nativeLibraryDir` and runs them.

### Why the binaries are named `libunpac.so` / `libpacextractor.so`

They're real executables (they have a `PT_INTERP` segment pointing at
`/system/bin/linker64`), **not** JNI shared libraries — don't pass them to
`System.loadLibrary()`/`dlopen()`, only run them via `ProcessBuilder` as the
native/ classes do. The `.so` naming and the `jniLibs/arm64-v8a/` placement
are intentional: that's the standard, sanctioned way to get a helper binary
extracted to disk with the executable bit set at install time (the same
trick Termux and various VPN-client apps use for their bundled binaries).

Two build settings make this actually work:
- `app/build.gradle.kts` sets `packaging.jniLibs.useLegacyPackaging = true`
  so the binaries are extracted to `nativeLibraryDir` instead of being
  left mmap'd inside the APK (the AGP default since API 23, which only
  works for real `dlopen()`'d libraries, not `exec()`).
- The manifest sets `android:extractNativeLibs="true"` to match.
- `abiFilters += "arm64-v8a"` restricts the APK to that ABI, since that's
  the only build we have of these two binaries.

### `unpac list` output parsing — needs verification

`UnpacRunner.list()` parses `unpac`'s stdout by regex-matching
`name = "..."`, `id = "..."`, `type = ...`, `size = 0x...` tokens per line,
inferred from the binary's own embedded format strings rather than from a
real run against a device `.pac`. It's written to degrade gracefully
(missing fields just come back blank/zero) rather than throw if the real
line grouping differs slightly — **but test it against an actual `.pac`
file before shipping**, and adjust the regexes in `UnpacRunner.kt` if a
field comes out wrong.

## Project layout

```
app/src/main/jniLibs/arm64-v8a/
  libunpac.so, libpacextractor.so       — the two CLI tools, exec'd not dlopen'd

app/src/main/java/dev/classityreal/pext/
  native/         NativeTools.kt, UnpacRunner.kt, PacExtractorRunner.kt,
                   SafBridge.kt          — subprocess wrappers + SAF<->file bridging
  pac/            PacModels.kt, PacParser.kt
                   — an earlier pure-Kotlin format parser, no longer wired
                   into the UI now that the real tools are bundled. Left in
                   as a reference/fallback if you ever want to drop the
                   native binaries and parse the container yourself; see
                   the caveats in that file's own comments before trusting
                   its offsets.
  ui/             PacViewModel.kt, screens/*    — Compose UI + state machine
  service/        ExtractionService.kt          — foreground-service skeleton (see below)
  MainActivity.kt                               — wires SAF pickers to the ViewModel
```

## Building

Open the `PExt/` folder in Android Studio (Ladybug/Meerkat or newer) and let
it sync, or from the CLI:

```
./gradlew assembleDebug
```

Requires JDK 17. `compileSdk`/`targetSdk` are set to 35; `minSdk` is 26.
The resulting APK only installs on **arm64-v8a** devices — see above.

## Extraction & large files

Neither CLI reports byte-level progress on stdout, so the UI shows an
indeterminate progress bar plus a tail of the tool's own output line (which
still shows which file it's currently on). `PacViewModel.startExtraction`
runs the subprocess and the subsequent SAF copy-out inside a
`viewModelScope` coroutine, which is fine while the app stays in the
foreground.

`ExtractionService.kt` is a stub `dataSync` foreground service — route the
same `UnpacRunner`/`PacExtractorRunner` calls through it (or a
`CoroutineWorker` with `setForeground()`) if you want extraction to survive
the user backgrounding the app mid-run, which matters once you're routinely
pulling multi-GB `super.img`s out of real firmware.

## Not included yet

- Verification of `unpac list`'s real output format against a device `.pac`
  (see caveat above)
- CRC-16 verification of the package (the bundled tools may or may not do
  this internally — not confirmed)
- A background-service extraction path that survives the app being closed
