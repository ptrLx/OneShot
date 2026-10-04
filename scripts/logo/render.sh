#!/bin/sh
# Generate the logo v2 vector sources and rasterize the PNGs. Runs in node:22-alpine via `make logo`.
set -eu
cd "$(dirname "$0")/../.."
node scripts/logo/generate.mjs
command -v rsvg-convert >/dev/null || apk add --no-cache rsvg-convert font-dejavu >/dev/null
rsvg-convert -w 512 -h 512 logo/v2/logo.svg -o fastlane/metadata/android/en-US/images/icon.png
rsvg-convert logo/v2/preview.svg -o logo/v2/preview.png
echo "wrote fastlane/metadata/android/en-US/images/icon.png logo/v2/preview.png"
