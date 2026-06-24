# Play store assets

Ready-to-upload graphics for the Google Play listing, plus their editable masters.

| File | Use | Spec |
|------|-----|------|
| `assets/ic_play_512.png` | **Hi-res app icon** (Store listing → Graphics) | 512×512 PNG, 32-bit |
| `assets/feature_graphic_1024x500.png` | **Feature graphic** (Store listing → Graphics) | 1024×500 PNG, 24-bit, no alpha |
| `assets/icon-master.svg` | Editable source for the icon PNG | SVG |
| `assets/feature-master.svg` | Editable source for the feature graphic | SVG |

Both PNGs are generated **1:1 from the app's own adaptive icon** (`app/src/main/res/drawable/ic_launcher_*.xml`)
— the plum gradient, the card-off-a-stack, and the clock badge — so the store icon matches the installed
launcher icon exactly.

## Re-rendering

The PNGs are produced from the SVG masters with [sharp](https://sharp.pixelplumbing.com/):

```bash
npm install sharp
# icon
node -e "require('sharp')('assets/icon-master.svg').png().toFile('assets/ic_play_512.png')"
# feature graphic (flatten removes alpha → 24-bit, as Play requires)
node -e "require('sharp')('assets/feature-master.svg').flatten({background:'#2E2168'}).png().toFile('assets/feature_graphic_1024x500.png')"
```

If you change the launcher icon, re-export the masters so the store icon stays in sync.

## Still your job (per-device / Console-only)

- **Screenshots** — 2–8 phone screenshots (~1080×1920). Capture with
  `adb exec-out screencap -p > play/assets/1-home.png`. Suggested order in
  [`docs/STORE_LISTING.md`](../docs/STORE_LISTING.md).
- Everything else listing-related is drafted in `docs/STORE_LISTING.md`,
  `docs/PLAY_DECLARATIONS.md`, and the step-by-step in
  [`docs/PLAY_SUBMISSION_CHECKLIST.md`](../docs/PLAY_SUBMISSION_CHECKLIST.md).
