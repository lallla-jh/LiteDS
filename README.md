# LiteDS — Nintendo DS Emulator for Android

A clean, lightweight Nintendo DS emulator for Android, forked from [melonDS-android](https://github.com/rafaelvcaetano/melonDS-android) by rafaelvcaetano.

LiteDS strips away complexity and focuses on what matters: playing your DS games with a smooth, modern experience.

[![Ko-fi](https://img.shields.io/badge/Support-Ko--fi-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/lallalaaa51)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---

## What's Different from melonDS-android

| Feature | melonDS-android | LiteDS |
|---|---|---|
| D-pad input | Traditional D-pad | Analog joystick UX |
| Fast-forward | Capped on debug builds | Optimized native build (-O2) |
| Settings | Full-featured | Streamlined (essentials only) |
| Language support | Partial | English / Korean / Japanese + 7 more |
| Donation | Multiple platforms | Ko-fi only |

### Joystick Input
The on-screen D-pad has been replaced with a virtual analog stick. A fixed-center base ring displays on screen, and a draggable nub maps your thumb position to 4-directional input with a configurable dead zone.

- Dead zone: 25% of the view radius (no accidental input at rest)
- Direction mapping: 45° boundary sectors (up / down / left / right)
- Haptic feedback on press and release

### Fast-Forward Fix
The upstream debug build compiles native code with `-O0`, which makes the NDS JIT core 2–3× slower and caps fast-forward speed regardless of the chosen multiplier. LiteDS applies `-DCMAKE_BUILD_TYPE=RelWithDebInfo` to the debug build type so the native core runs at `-O2` while Java remains fully debuggable.

### Streamlined Settings
- Removed: Rewind, Check for updates, sustained-performance clutter in General
- Kept: Theme, fast-forward multiplier, backup/restore, JIT, sustained performance (moved to System)
- About screen: Ko-fi donation, melonDS (GPLv3) attribution

---

## Performance

Performance is solid on 64-bit devices with JIT and thread rendering enabled. Flagship devices should run at full speed. 32-bit devices have limited performance due to lack of JIT support.

**Fast-forward** is implemented by raising the frame-rate cap. The actual speedup depends on your device's CPU performance.

---

## Language Support

| Locale | Coverage |
|---|---|
| English | Full (default) |
| Korean (ko) | Full — 521 strings |
| Japanese (ja) | Full — 521 strings |
| Chinese Simplified, Russian, Portuguese (BR), Indonesian, Italian, French, Spanish | Partial (LiteDS-specific keys + upstream base) |

---

## Building

### Requirements
- Android SDK, NDK, CMake
- JDK 17+

### Steps

```bash
# Clone with submodules
git clone --recurse-submodules https://github.com/lallla-jh/LiteDS.git

# Debug build (native code optimized with RelWithDebInfo)
./gradlew :app:assembleGitHubProdDebug
# Windows: gradlew.bat :app:assembleGitHubProdDebug
```

The generated APK is at `app/gitHubProd/debug/`.

### Release Build

Add the following to your `local.properties`:

```
MELONDS_KEYSTORE=<path_to_your_keystore>
MELONDS_KEYSTORE_PASSWORD=<keystore_password>
MELONDS_KEY_ALIAS=<key_alias>
MELONDS_KEY_PASSWORD=<key_alias_password>
```

Then build:
```bash
./gradlew :app:assembleGitHubProdRelease
```

---

## Third-Party Frontend Integration

LiteDS can be launched from third-party frontends (e.g., Pegasus):

- **Package name:** `me.magnum.melonds`
- **Activity:** `me.magnum.melonds.ui.emulator.EmulatorActivity`
- **Intent data:** SAF URI of the NDS ROM (ZIP and 7z supported)

---

## Credits

- **melonDS** — core NDS emulator engine by StapleButter et al. ([melonds.kuribo64.net](https://melonds.kuribo64.net/)) — GPL-3.0
- **melonDS-android** — Android port by rafaelvcaetano ([GitHub](https://github.com/rafaelvcaetano/melonDS-android)) — GPL-3.0
- **LiteDS** — this fork, same GPL-3.0 license

---

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.
