# Screens & wireframes

Annotated ASCII wireframes for every screen in the app. They describe layout, key interactions and
the calm, premium visual language (rounded cards, violet accent, spring animations). Actual rendering
uses Material 3 with dynamic color and Light/Dark/System themes.

---

## Onboarding (6 steps, swipeable)

```
┌───────────────────────────────┐   Steps:
│                               │   1. Welcome
│            ◐  (icon)          │   2. "Notifications never stop"
│                               │   3. "We batch them, beautifully"
│   Welcome to Notification     │   4. Grant notification access  ← opens system settings,
│   Digest                      │      reflects granted state live, requests POST_NOTIFICATIONS
│                               │   5. Choose a schedule (Workday / Balanced / Evening)
│   A calmer phone, without     │   6. Two simple modes (Digest vs Real-Time) → "Start being calm"
│   missing what matters.       │
│                               │   Animated page dots; Skip / Next; spring transitions.
│        ● ○ ○ ○ ○ ○            │
│   [ Skip ]          [ Next →] │
└───────────────────────────────┘
```

---

## Home dashboard

```
┌───────────────────────────────┐
│ Good morning                  │
│ Your digest                   │  ← large title
│ ╭───────────────────────────╮ │
│ │  37 notifications waiting │ │  ← gradient hero card (violet)
│ │  Next digest at 3:00 PM   │ │
│ │  [ Review ]  [ Deliver now]│ │  ← Deliver now shows a spinner while running
│ ╰───────────────────────────╯ │
│ ┌──────┐ ┌──────┐ ┌──────┐    │
│ │  47  │ │ 386  │ │  6   │    │  ← animated stat tiles
│ │Batched││Avoided││Real- │    │     (Batched today · Avoided·7d · Real-Time apps)
│ │ today││ ·7d  ││Time  │    │
│ └──────┘ └──────┘ └──────┘    │
│ Suggestions                   │
│ ╭───────────────────────────╮ │  ← dismissible volume-based cards
│ │ ◔ Instagram  72 this week │ │     [ Make Real-Time ]  Dismiss
│ ╰───────────────────────────╯ │
│ Recent            Review all  │
│ ◔ WhatsApp · John: still on?  │
│ ◔ Gmail · Roadmap review      │
└───────────────────────────────┘
   [Home] [Inbox•] [History] [Apps] [Settings]   ← bottom nav, Inbox shows pending badge
```

---

## Inbox ("email for notifications")

```
┌───────────────────────────────┐
│ Inbox  (37)        ✓✓   ⚡Deliver│
│ ┌───────────────────────────┐ │
│ │ 🔍 Search notifications    │ │
│ └───────────────────────────┘ │
│ (WhatsApp 12)(Instagram 8)(…) │  ← horizontal app-filter chips
│ ───────────────────────────── │
│ ◔ WhatsApp                ⚡  │  ← swipe → Open · swipe ← Delete · ⚡ = make app Real-Time
│   John: Are we still meeting? •│     long-press → multi-select mode
│ ◔ Instagram               ⚡  │
│   Alex sent a reel          • │
│ ◔ Gmail                   ⚡  │
│   Product review meeting    • │
└───────────────────────────────┘

Selection mode:  [✕] 3 selected      All  ✓✓  🗑
```

---

## Digest detail

```
┌───────────────────────────────┐
│ ←  Digest                     │
│    5 Jun, 3:00 PM             │
│ 12 notifications · 5 apps     │
│ ╭───────────────────────────╮ │  ← expandable AppGroupCard (spring expand)
│ │ ◔ WhatsApp           (12) ▾│ │
│ │   John: Meeting moved…     │ │
│ │   ───────────────────────  │ │
│ │   • Mom: Call me when free │ │  ← tap any item → restores deep link
│ ╰───────────────────────────╯ │
│ ╭───────────────────────────╮ │
│ │ ◔ Instagram           (8) ▸│ │
│ ╰───────────────────────────╯ │
└───────────────────────────────┘
```

---

## History

```
┌───────────────────────────────┐
│ History                       │
│ ┌───────────────────────────┐ │
│ │ 🔍 Search delivered        │ │  ← searches across delivered notifications
│ └───────────────────────────┘ │
│ ╭───────────────────────────╮ │
│ │ ⚡ 12 notifications · 5 apps│ │  ← swipe ← to delete a whole digest
│ │   5 Jun, 3:00 PM  [Manual]›│ │     tap → Digest detail
│ ╰───────────────────────────╯ │
│ ╭───────────────────────────╮ │
│ │ ⏰ 8 notifications · 4 apps │ │
│ │   5 Jun, 12:00 PM [Sched.]›│ │
│ ╰───────────────────────────╯ │
└───────────────────────────────┘
```

---

## Apps — Digest vs Real‑Time

```
┌───────────────────────────────┐
│ Apps                          │
│ 142 in Digest · 6 Real-Time   │
│ ┌───────────────────────────┐ │
│ │ 🔍 Search apps             │ │
│ └───────────────────────────┘ │
│ [ All ][ Digest ][ Real-Time ]│  ← segmented filter
│ Recently changed              │
│ (◔ Instagram)(◔ LinkedIn)…    │
│ ───────────────────────────── │
│ ◔ Gmail        (Digest|Real-Time)│ ← compact per-row mode toggle (animated)
│ ◔ Instagram    (Digest|Real-Time)│   long-press → bulk select → "Move to Digest /
│ ◔ Messages     (Digest|Real-Time)│   Make Real-Time"
└───────────────────────────────┘
```

---

## Schedules

```
┌───────────────────────────────┐
│ ←  Schedules                  │
│ ╭───────────────────────────╮ │
│ │ 9:00 AM            ▲ ▼ 🗑 ◉ │ │  ← tap time → time picker; ▲▼ reorder; switch enable
│ │ Morning                    │ │
│ ╰───────────────────────────╯ │
│ ╭───────────────────────────╮ │
│ │ 12:00 PM           ▲ ▼ 🗑 ◉ │ │
│ │ Midday                     │ │
│ ╰───────────────────────────╯ │
│                               │
│                  ( + Add time)│  ← extended FAB → Material 3 time picker dialog
└───────────────────────────────┘
```

---

## Settings

```
┌───────────────────────────────┐
│ Settings                      │
│ APPEARANCE                    │
│ ╭───────────────────────────╮ │
│ │ Theme  [System][Light][Dark]│ │
│ │ Dynamic color           ◉ │ │  ← Android 12+
│ ╰───────────────────────────╯ │
│ DELIVERY                      │
│ ╭───────────────────────────╮ │
│ │ ⏰ Schedules             › │ │
│ │ Collection status       ◉ │ │
│ ╰───────────────────────────╯ │
│ BEHAVIOUR                     │
│ │ Smart suggestions  ◉ · Haptics ◉ │
│ DATA & PRIVACY                │
│ │ Keep for [7][14][30][60][90]d   │
│ │ Export notifications      › │
│ │ Clear all data            › │  ← confirm dialog
│ │ "Private by design" card    │
│ Notification Digest v1.0.0    │
└───────────────────────────────┘
```

---

## Notifications (system shade)

**Digest delivery** (channel: *Digest deliveries*, default importance):

```
◔ Notification Digest
  Your digest is ready
  37 notifications · 6 apps
  ▸ WhatsApp (12)  John: Meeting moved to 3 PM
  ▸ Instagram (8)  Alex sent a reel
  ▸ Gmail (4)      Product roadmap review
  [ Review ]
```

**Collection status** (channel: *Collection status*, min importance, silent, ongoing):

```
◔ 37 notifications waiting
  Tap to review, or wait for the next digest
  [ Deliver now ]
```
