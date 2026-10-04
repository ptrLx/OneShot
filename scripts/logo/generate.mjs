#!/usr/bin/env node
// OneShot logo v2: logo v1 redrawn by script. The brand mark stays the same, a
// circle open to the top right around a monospace "1" with a wide foot, on the
// amber camera body. New: the amber runs into a darker orange (linear gradient).
//
// Single source of truth for the mark. All shapes are SVG path strings on the
// 108x108 adaptive-icon canvas, so the very same geometry is written as
//   - Android VectorDrawables (adaptive foreground + monochrome themed icon),
//   - SVG masters in logo/v2/ (icon, foreground, monochrome, mask preview sheet).
// PNGs (fastlane store icon, preview) are rasterized from the SVGs by
// scripts/logo/render.sh with rsvg-convert. No npm dependencies.
//
// Usage: node scripts/logo/generate.mjs   (from the repository root; `make logo` runs everything)
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

// Palette of logo v1, plus a darker orange as the end of the body gradient.
const AMBER = "#FEBA4B";
const ORANGE = "#DD6200";
const INK = "#442C00"; // circle and "1", as in logo v1
const NIGHT = "#1F1B16"; // adaptive icon background (values/ic_launcher_background.xml)

// Adaptive icon canvas: 108x108, the launcher shows the inner 72x72 and masks
// may cut anything outside a circle of radius 33 around the center (safe zone).
const SIZE = 108;
const C = SIZE / 2;
const SAFE_RADIUS = 33;

const f = (n) => Number(n.toFixed(2));

function roundedRect(x, y, w, h, r) {
  return (
    `M${f(x + r)},${f(y)} H${f(x + w - r)} A${r},${r} 0 0 1 ${f(x + w)},${f(y + r)} ` +
    `V${f(y + h - r)} A${r},${r} 0 0 1 ${f(x + w - r)},${f(y + h)} H${f(x + r)} ` +
    `A${r},${r} 0 0 1 ${f(x)},${f(y + h - r)} V${f(y + r)} A${r},${r} 0 0 1 ${f(x + r)},${f(y)} Z`
  );
}

function circle(cx, cy, r) {
  return `M${f(cx - r)},${f(cy)} A${r},${r} 0 1 0 ${f(cx + r)},${f(cy)} A${r},${r} 0 1 0 ${f(cx - r)},${f(cy)} Z`;
}

// Open arc from angle a1 to a2 (degrees, 0 = right, clockwise because y points down).
function arc(cx, cy, r, a1, a2) {
  const p = (a) => [cx + r * Math.cos((a * Math.PI) / 180), cy + r * Math.sin((a * Math.PI) / 180)];
  const [x1, y1] = p(a1);
  const [x2, y2] = p(a2);
  const large = Math.abs(a2 - a1) > 180 ? 1 : 0;
  return `M${f(x1)},${f(y1)} A${r},${r} 0 ${large} 1 ${f(x2)},${f(y2)}`;
}

// The brand mark around center (cx, cy) with ring radius r, as round-capped strokes:
// the ring runs clockwise from just below 3 o'clock to 12 o'clock, leaving the top
// right open; the "1" has a diagonal flag, a vertical stem and a wide foot.
function mark(cx, cy, r) {
  const x = (u) => f(cx + u * r);
  const y = (v) => f(cy + v * r);
  const stem = 0.07;
  const top = -0.54;
  const foot = 0.47;
  return {
    width: f(0.3 * r),
    paths: [
      arc(cx, cy, r, 6, 270),
      `M${x(-0.3)},${y(-0.27)} L${x(stem)},${y(top)} V${y(foot)}`,
      `M${x(-0.26)},${y(foot)} H${x(0.4)}`,
    ],
  };
}

// ---- Geometry -------------------------------------------------------------
// Camera of logo v1 (squarer body, shutter on the top left). The ring sits in the center
// of the body and of the icon canvas, so the camera leaves more room below than above
// itself to balance the floating shutter.
const body = { x: 29, y: 32, w: 50, h: 44, r: 9 };
const shutter = { x: 37, y: 26.4, w: 13, h: 3.6, r: 1.8 };
const ring = { cx: C, cy: C, r: 13.6 };

const bodyPath = roundedRect(body.x, body.y, body.w, body.h, body.r);
const shutterPath = roundedRect(shutter.x, shutter.y, shutter.w, shutter.h, shutter.r);
const icon = mark(ring.cx, ring.cy, ring.r);

// Diagonal body gradient, top left (light amber) to bottom right (darker orange).
const gradient = {
  id: "amber",
  x1: body.x,
  y1: shutter.y,
  x2: body.x + body.w,
  y2: body.y + body.h,
  stops: [
    [0, AMBER],
    [1, ORANGE],
  ],
};

// Full-color foreground: amber-to-orange camera with the ink mark.
const foreground = [
  { d: shutterPath, gradient },
  { d: bodyPath, gradient },
  ...icon.paths.map((d) => ({ d, stroke: INK, width: icon.width })),
];

// Round variant (android:roundIcon, used by launchers that ask for a round icon): no
// camera and no night background, the amber-to-orange gradient fills the whole icon and
// carries the circled "1" alone. Gradient spans the visible 72x72 center.
const roundGradient = { ...gradient, id: "amberRound", x1: 18, y1: 18, x2: 90, y2: 90 };
const roundBackground = [{ d: `M0,0 H${SIZE} V${SIZE} H0 Z`, gradient: roundGradient }];
const roundMark = mark(C, C, 18);
const roundForeground = roundMark.paths.map((d) => ({ d, stroke: INK, width: roundMark.width }));

// Monochrome (Android 13 themed icons), built from the round icon: the launcher keeps only
// the alpha of this layer and tints it with the theme color, so the amber-to-orange gradient
// becomes a translucent-to-denser gradient of the theme color and the "1" stays solid.
const MONO_ALPHA = [0.18, 0.5];
const monoShapes = (color, id) => [
  { ...roundBackground[0], gradient: { ...roundGradient, id, stops: [[0, color, MONO_ALPHA[0]], [1, color, MONO_ALPHA[1]]] } },
  ...roundMark.paths.map((d) => ({ d, stroke: color, width: roundMark.width })),
];
const monochrome = monoShapes("#000000", "mono");

// ---- Safe-zone check: the camera and the themed mark inside the 33dp circle ----
function assertSafe() {
  const points = [
    [body.x + body.r * 0.3, body.y + body.r * 0.3],
    [body.x + body.w - body.r * 0.3, body.y + body.h - body.r * 0.3],
    [body.x + body.r * 0.3, body.y + body.h - body.r * 0.3],
    [body.x + body.w - body.r * 0.3, body.y + body.r * 0.3],
    [shutter.x, shutter.y],
    [shutter.x + shutter.w, shutter.y],
  ];
  for (const [x, y] of points) {
    const dist = Math.hypot(x - C, y - C);
    if (dist > SAFE_RADIUS) throw new Error(`point ${x},${y} is ${dist.toFixed(1)}dp from center, outside the safe zone`);
  }
  const bodyCenter = [body.x + body.w / 2, body.y + body.h / 2];
  if (bodyCenter[0] !== ring.cx || bodyCenter[1] !== ring.cy) throw new Error(`ring is not at the body center ${bodyCenter}`);
  const ringOuter = 18 + roundMark.width / 2;
  if (ringOuter > SAFE_RADIUS) throw new Error(`round mark reaches ${ringOuter}dp, outside the safe zone`);
}

// ---- Writers ----------------------------------------------------------------
function svgGradients(shapes) {
  const seen = new Map(shapes.filter((s) => s.gradient).map(({ gradient: g }) => [g.id, g]));
  const defs = [...seen.values()].map(
    (g) =>
      `<linearGradient id="${g.id}" gradientUnits="userSpaceOnUse" x1="${g.x1}" y1="${g.y1}" x2="${g.x2}" y2="${g.y2}">` +
      g.stops.map(([o, c, a = 1]) => `<stop offset="${o}" stop-color="${c}"${a < 1 ? ` stop-opacity="${a}"` : ""}/>`).join("") +
      `</linearGradient>`,
  );
  return defs.length ? `  <defs>${defs.join("")}</defs>\n` : "";
}

function svgShapes(shapes) {
  return shapes
    .map(({ d, fill, gradient, stroke, width }) =>
      stroke
        ? `  <path d="${d}" fill="none" stroke="${stroke}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"/>`
        : `  <path d="${d}" fill="${gradient ? `url(#${gradient.id})` : fill}"/>`,
    )
    .join("\n");
}

function svg(shapes, { background, viewBox = `0 0 ${SIZE} ${SIZE}` } = {}) {
  const bg = background ? `  <rect x="0" y="0" width="${SIZE}" height="${SIZE}" fill="${background}"/>\n` : "";
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}">\n${svgGradients(shapes)}${bg}${svgShapes(shapes)}\n</svg>\n`;
}

// VectorDrawable colors carry alpha as #AARRGGBB.
const argb = (hex, a) => (a < 1 ? `#${Math.round(a * 255).toString(16).padStart(2, "0").toUpperCase()}${hex.slice(1)}` : hex);

// Gradients need the aapt inline-resource syntax (API 24+, minSdk is 29).
function vdGradient(g) {
  const items = g.stops.map(([o, c, a = 1]) => `\n                <item android:offset="${o}" android:color="${argb(c, a)}" />`).join("");
  return `
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="${g.x1}"
                android:startY="${g.y1}"
                android:endX="${g.x2}"
                android:endY="${g.y2}">${items}
            </gradient>
        </aapt:attr>
    </path>`;
}

function vectorDrawable(shapes, comment) {
  const paths = shapes
    .map(({ d, fill, gradient, stroke, width }) =>
      stroke
        ? `    <path\n        android:pathData="${d}"\n        android:strokeColor="${stroke}"\n        android:strokeWidth="${width}"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />`
        : gradient
          ? `    <path\n        android:pathData="${d}">${vdGradient(gradient)}`
          : `    <path\n        android:pathData="${d}"\n        android:fillColor="${fill}" />`,
    )
    .join("\n");
  const aapt = shapes.some((s) => s.gradient) ? '\n    xmlns:aapt="http://schemas.android.com/aapt"' : "";
  return `<?xml version="1.0" encoding="utf-8"?>
<!-- ${comment} Generated by scripts/logo/generate.mjs, do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"${aapt}
    android:width="${SIZE}dp"
    android:height="${SIZE}dp"
    android:viewportWidth="${SIZE}"
    android:viewportHeight="${SIZE}">
${paths}
</vector>
`;
}

const adaptiveIcon = (background, foreground) => `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/logo/generate.mjs, do not edit by hand. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="${background}" />
    <foreground android:drawable="@drawable/${foreground}" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
`;

// Preview sheet for review: the icon under common launcher masks plus a themed icon.
function previewSheet() {
  const cell = 140;
  const masks = [
    ["circle", `<circle cx="54" cy="54" r="54"/>`],
    ["squircle", `<rect x="0" y="0" width="108" height="108" rx="34"/>`],
    ["rounded square", `<rect x="0" y="0" width="108" height="108" rx="18"/>`],
  ];
  const tile = (i, inner, clip, label) =>
    `<g transform="translate(${20 + i * cell} 20)"><defs><clipPath id="m${i}">${clip}</clipPath></defs>` +
    `<g clip-path="url(#m${i})" transform="scale(1.111)" >${inner}</g>` +
    `<text x="60" y="140" font-family="sans-serif" font-size="12" text-anchor="middle" fill="#5b5146">${label}</text></g>`;
  // Launchers show the 72x72 center of the 108 canvas scaled to the icon size.
  const full = `<g transform="scale(1.5) translate(-18 -18)"><rect width="108" height="108" fill="${NIGHT}"/>${svgShapes(foreground)}</g>`;
  const round = `<g transform="scale(1.5) translate(-18 -18)">${svgShapes([...roundBackground, ...roundForeground])}</g>`;
  const themed = `<g transform="scale(1.5) translate(-18 -18)"><rect width="108" height="108" fill="#E6DFF7"/>${svgShapes(monoShapes("#4A3D7A", "monoPreview"))}</g>`;
  const tiles = [
    ...masks.map(([label, clip], i) => tile(i, full, clip, label)),
    tile(3, round, masks[0][1], "round icon"),
    tile(4, themed, masks[0][1], "themed (Android 13+)"),
  ];
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${40 + 5 * cell} 190" width="${2 * (40 + 5 * cell)}" height="380">
${svgGradients([...foreground, ...roundBackground, ...monoShapes("#4A3D7A", "monoPreview")])}  <rect width="100%" height="100%" fill="#FBF6EE"/>
  ${tiles.join("\n  ")}
</svg>
`;
}

async function write(rel, content) {
  const file = path.join(ROOT, rel);
  await mkdir(path.dirname(file), { recursive: true });
  await writeFile(file, content);
  console.log("wrote", rel);
}

assertSafe();
const res = "app/src/main/res";
await write(`${res}/drawable/ic_launcher_foreground.xml`, vectorDrawable(foreground, "OneShot logo v2 adaptive icon foreground."));
await write(`${res}/drawable/ic_launcher_round_background.xml`, vectorDrawable(roundBackground, "OneShot logo v2 round icon background."));
await write(`${res}/drawable/ic_launcher_round_foreground.xml`, vectorDrawable(roundForeground, "OneShot logo v2 round icon foreground."));
await write(`${res}/drawable/ic_launcher_monochrome.xml`, vectorDrawable(monochrome, "OneShot logo v2 themed icon (monochrome)."));
await write(`${res}/mipmap-anydpi-v26/ic_launcher.xml`, adaptiveIcon("@color/ic_launcher_background", "ic_launcher_foreground"));
await write(`${res}/mipmap-anydpi-v26/ic_launcher_round.xml`, adaptiveIcon("@drawable/ic_launcher_round_background", "ic_launcher_round_foreground"));
await write("logo/v2/foreground.svg", svg(foreground));
await write("logo/v2/monochrome.svg", svg(monochrome));
await write("logo/v2/round.svg", svg([...roundBackground, ...roundForeground], { viewBox: "18 18 72 72" }));
// Store icon: the visible 72x72 center of the adaptive canvas on the background, full bleed.
await write("logo/v2/logo.svg", svg(foreground, { background: NIGHT, viewBox: "18 18 72 72" }));
await write("logo/v2/preview.svg", previewSheet());
