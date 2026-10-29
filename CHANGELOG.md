# Changelog

## 0.3.0 — Unreleased

- Configure line width and color separately for each of the five reactions from the settings screen.
- Add line widths from 1 to 15 in 0.5 steps, RGB sliders, hex color input, a preview and per-reaction appearance reset.
- Preserve draft edits while moving between settings screens; cancel without changing the saved configuration.
- Migrate the legacy global width and action colors without losing visibility or duration settings.
- Check independent appearance, invalid values, JSON migration and save/reload behavior.
- Add a newest-first interaction log with timestamps, selectable details, block coordinates, world/dimension and before/after properties.
- Control logging independently for each reaction, including while highlights are disabled.
- Retain the latest 1,000 events locally across restarts with background writes and corrupt-file recovery.
- Mark reverted placement/removal predictions on the corresponding log entry.

## 0.2.1 — First public release · 2026-10-29

- Add MIT license and copyright notice for RUOK0214.
- Add the original block-outline icon and project/support links.
- Add Korean and English Mod Menu summaries and descriptions.
- Support the optional Mod Menu configuration button, returning to the mod list when closed.
- Package the install JAR, source JAR, installation notes, license and checksums locally.
- Verify actual JAR metadata, translations, icon, license and absence of bundled dependencies.
- Publish versioned GitHub release assets with SHA-256 checksums after build and download verification.

## 0.2.0 — Settings

- Replace the dedicated H key with `/showinteraction`.
- Add overall visibility and transparency controls.
- Add separate visibility and duration controls for place, break, adjust, open and inspect.
- Add save/apply and cancel behavior; preserve legacy settings.
- Avoid briefly classifying a disabled open/adjust reaction as inspect.

## 0.1.0 — Initial implementation

- Highlight recent block placement, removal, state changes, container opening and inspection.
- Use distinct colors, a pulse and fade-out, with the latest action emphasized.
- Keep container highlights visible while open and briefly after closing.
- Preserve removed block shapes; clear history when changing worlds.

0.1.0 and 0.2.0 describe development milestones. 0.2.1 is the first public release and includes those features.
