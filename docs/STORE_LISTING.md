# Play store listing — drafts

Paste these into Play Console → **Grow → Store presence → Main store listing**. Character limits noted.

## App name (≤ 30)

```
Notification Digest
```
(19 chars. Alternatives if taken: `Notification Digest: Batch`, `Digest — Calm Notifications`.)

## Short description (≤ 80)

```
Batch noisy notifications into calm, scheduled digests. Private, on-device.
```
(74 chars.)

## Full description (≤ 4000)

```
Your phone buzzes all day. Notification Digest quietly collects the noisy ones and hands them to you in clean batches at the times YOU choose — so you stay in control instead of reacting to every ping.

HOW IT WORKS
• Put noisy apps in “Digest” — their notifications are collected silently, with nothing popping up.
• Keep the important ones “Real-Time” — messages, OTPs and calls come through instantly, as normal.
• Get a tidy digest at your chosen times, or tap “See all now” whenever you like.
• Browse everything in an inbox grouped by day: Today, Yesterday and Older.

CALM BY DESIGN
• No endless interruptions — batch the noise, keep what matters.
• Two simple modes: Digest and Real-Time. You decide for every app.
• Smart starting point: messaging, authenticator/OTP, dialer and clock apps stay Real-Time by default.

PRIVATE BY DESIGN
• Everything is processed and stored ON YOUR DEVICE.
• No account required. No ads. No analytics. No tracking. We can’t see your notifications — there’s no server.
• Optional backup writes only your settings (never notification content) to YOUR OWN Google Drive, which only you control.

RELIABLE
• Survives reboots and delivers on schedule.
• A clear setup step helps your phone keep the app running in the background.

Notification Digest needs Notification access to read and batch your notifications — that’s its whole purpose. It never uses your notifications for ads and never sells your data.

Take back your attention. Let the noise wait for you.
```

## Graphic assets

The two required graphics are **already generated and committed** — upload them as-is:

| Asset | Spec | File (in repo) | Status |
|-------|------|----------------|--------|
| **App icon** | 512×512 PNG, 32-bit | [`play/assets/ic_play_512.png`](../play/assets/ic_play_512.png) | ✅ Ready — rendered 1:1 from the in-app adaptive icon (plum gradient + card stack + clock badge). |
| **Feature graphic** | 1024×500 PNG, 24-bit (no alpha) | [`play/assets/feature_graphic_1024x500.png`](../play/assets/feature_graphic_1024x500.png) | ✅ Ready — wordmark + tagline on brand plum. |
| **Phone screenshots** | 2–8, ~1080×1920 (you capture) | — | ⏳ Capture on a device: `adb exec-out screencap -p > 1-home.png`. Suggested set: Home (hero + stats), Inbox grouped by day, Apps (Digest/Real-Time toggles), a delivered digest, Settings (privacy section). |
| (optional) Tablet shots, promo video | — | — | Nice-to-have, not required. |

> Editable masters live alongside the PNGs (`play/assets/icon-master.svg`, `feature-master.svg`); see
> [`play/README.md`](../play/README.md) to re-render if you ever want to tweak copy or colours.

## Release notes (first release)

```
First release. Batch noisy notifications into calm, scheduled digests — fully on-device and private.
```

## Categorization

- **App** (not Game). **Category:** Productivity (or Tools). **Free.** No in-app purchases.
- Tags/keywords are derived from the description — emphasize: digest, batch notifications, focus,
  do not disturb alternative, calm, privacy.
