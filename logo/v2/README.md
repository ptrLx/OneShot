# Logo v2

Logo v1, redrawn by a script. The brand identity stays: the amber camera with a circle open to the top right around a monospace "1" with a wide foot, in dark brown. New: the amber of the camera runs into a darker orange (diagonal linear gradient, top left to bottom right).

![Preview under launcher masks](preview.png)

- Source of truth: `scripts/logo/generate.mjs` (no npm dependencies). Never edit generated files by hand.
- Regenerate everything: `make logo` (Docker, `node:22-alpine` + `rsvg-convert`).
- Outputs: adaptive icon foreground and monochrome layer as VectorDrawables (`app/src/main/res/drawable/ic_launcher_*.xml`, wired in `mipmap-anydpi-v26/`), SVG masters here (`logo.svg` store icon, `foreground.svg`, `monochrome.svg`), `preview.png`, and the 512 px store icon `fastlane/metadata/android/en-US/images/icon.png`.
- Colors: amber `#FEBA4B` to orange `#DD6200` (camera), ink `#442C00` (circle and "1"), night `#1F1B16` (adaptive background, `values/ic_launcher_background.xml`).
- Round icon (`ic_launcher_round`, `android:roundIcon`, `round.svg`): no camera and no dark background, the amber-to-orange gradient fills the whole icon around the circled "1". Only launchers that ask for a round icon use it; most current launchers (e.g. Pixel) mask the regular adaptive icon instead.
- Themed icon (Android 13+, `monochrome.svg`): built from the round icon. The launcher keeps only the alpha of this layer and tints it with the theme color, so the gradient becomes a translucent-to-denser gradient (18 % to 50 % alpha) of the theme color and the circled "1" stays solid.
- Safe zone: the generator fails if the mark leaves the 33 dp radius that every launcher mask keeps.
