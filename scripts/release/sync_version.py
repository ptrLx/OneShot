#!/usr/bin/env python3
"""Derive versionCode and the F-Droid/fastlane changelog from the versionName set by release-please.

release-please only bumps `versionName` (generic updater, `x-release-please-version` marker) and
CHANGELOG.md. F-Droid additionally needs a strictly increasing literal `versionCode` in the app
build file and shows fastlane/metadata/android/en-US/changelogs/<versionCode>.txt (max 500 chars).

versionCode scheme: MAJOR * 10000 + MINOR * 100 + PATCH (1.2.0 -> 10200, always > legacy 111).

Usage: scripts/release/sync_version.py [--check]
  --check  only verify that versionCode and the changelog file match versionName (exit 1 if not)
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CHANGELOGS = ROOT / "fastlane/metadata/android/en-US/changelogs"
FDROID_CHANGELOG_LIMIT = 500


def gradle_file() -> pathlib.Path:
    kts = ROOT / "app/build.gradle.kts"
    return kts if kts.exists() else ROOT / "app/build.gradle"


def version_code_for(name: str) -> int:
    major, minor, patch = (int(p) for p in name.split("."))
    if minor > 99 or patch > 99:
        sys.exit(f"versionName {name}: minor and patch must stay below 100 for the versionCode scheme")
    return major * 10000 + minor * 100 + patch


def release_notes(version: str) -> str:
    """Section of CHANGELOG.md for `version`, flattened to plain text for F-Droid."""
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(rf"^## \[?{re.escape(version)}\b.*?$(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not match:
        sys.exit(f"CHANGELOG.md has no section for {version}")
    lines = []
    for line in match.group(1).strip().splitlines():
        line = re.sub(r"\s*\(\[[0-9a-f]{7,}\]\([^)]*\)\)", "", line)  # commit links
        line = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", line)  # other links -> text
        line = re.sub(r"^#{3,}\s*(.*)$", r"\1:", line)  # sub headings
        line = re.sub(r"^\s*[*-]\s+", "• ", line)
        line = line.replace("**", "")
        if line.strip():
            lines.append(line.rstrip())
    notes = "\n".join(lines) or f"Release {version}"
    if len(notes) > FDROID_CHANGELOG_LIMIT:
        notes = notes[: FDROID_CHANGELOG_LIMIT - 1].rstrip() + "…"
    return notes + "\n"


def main() -> None:
    check_only = "--check" in sys.argv[1:]
    path = gradle_file()
    source = path.read_text(encoding="utf-8")
    name = re.search(r"""^\s*versionName\s*=?\s*["']([0-9]+\.[0-9]+\.[0-9]+)["']""", source, re.M).group(1)
    code_match = re.search(r"^(\s*versionCode\s*=?\s*)([0-9]+)", source, re.M)
    current, wanted = int(code_match.group(2)), version_code_for(name)
    changelog = CHANGELOGS / f"{wanted}.txt"

    if check_only:
        ok = current == wanted and changelog.exists()
        print(f"versionName={name} versionCode={current} expected={wanted} changelog={'ok' if changelog.exists() else 'missing'}")
        sys.exit(0 if ok else 1)

    if wanted < current:
        sys.exit(f"versionCode would decrease ({current} -> {wanted}); existing installs could not update")
    if wanted != current:
        source = source[: code_match.start(2)] + str(wanted) + source[code_match.end(2):]
        path.write_text(source, encoding="utf-8")
    changelog.write_text(release_notes(name), encoding="utf-8")
    print(f"versionName={name} versionCode={wanted} changelog={changelog.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
