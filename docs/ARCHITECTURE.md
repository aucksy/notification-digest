# Architecture

Notification Digest is a single Gradle module organised into strict Clean‑Architecture layers with
MVVM on the presentation side. This document explains the layers, the runtime data flows, and the
key design decisions.

## Layers & dependency rule

```
        ┌──────────────────────── presentation ────────────────────────┐
        │  ui/ (Compose screens + ViewModels)                           │
        │  service/  notification/  work/   (Android system surfaces)   │
        └───────────────┬───────────────────────────────────────────────┘
                        │ depends on ↓ (interfaces + use cases only)
        ┌───────────────▼──────────────── domain ──────────────────────┐
        │  model/  ·  repository/ (interfaces)  ·  system/ (interfaces) │
        │  usecase/  (pure Kotlin, no Android imports)                   │
        └───────────────▲──────────────────────────────────────────────┘
                        │ implemented by ↑
        ┌───────────────┴──────────────── data ────────────────────────┐
        │  local/ (Room, DataStore)  ·  repository/ (impls)             │
        │  system/ (PackageManager, icons, PendingIntents, deep links)  │
        └───────────────────────────────────────────────────────────────┘
```

- **Domain never imports Android.** System effects are expressed as interfaces
  (`DigestNotifier`, `DeepLinkLauncher`, `DigestScheduler`, `TimeProvider`) and implemented in the
  outer layers, so use cases are deterministic and unit‑testable.
- **ViewModels** depend only on domain (repositories + use cases), exposed via Hilt.
- **Data** implements the repository interfaces and owns all framework details.

## Key components

| Component | Responsibility |
|-----------|----------------|
| `DigestNotificationListenerService` | Receives every posted notification; persists + suppresses Digest‑mode ones; leaves Real‑Time / never‑batch ones alone. |
| `NotificationRepository` (+ Room DAO) | The notification store: inbox queue, search, read/delete, digest assignment, stats queries. |
| `DeliverDigestUseCase` | Snapshots the pending queue → creates a `Digest` → links the notifications (delivered, not deleted) → posts the grouped notification. Powers both scheduled and "Deliver Now". |
| `ComputeNextDigestTimeUseCase` | Pure function: given schedules + now, the next delivery epoch. |
| `DigestScheduler` / `DigestDeliveryWorker` | A self‑re‑arming WorkManager chain: each delivery schedules the next; re‑armed on boot. |
| `PendingIntentStore` + `DeepLinkLauncher` | In‑memory cache of live `PendingIntent`s for exact deep links, with app‑launch fallback. |
| `RetentionCleanupWorker` | Daily purge of notifications/digests beyond the retention window. |
| `StatsRepository` / `RecommendationRepository` | Derived, reactive metrics & volume‑based suggestions (no AI). |

## Runtime flows

### 1) Collecting a notification

```
App posts notification
        │
        ▼
NotificationListenerService.onNotificationPosted
        │  shouldIgnore?  (self / ongoing / FGS / group summary / call / nav / media)──► leave it
        ▼ no
   getMode(pkg)
   ├─ REALTIME ─► do nothing (passes through as normal)
   └─ DIGEST   ─► cache PendingIntents → insert into Room → cancelNotification(key)
                                                            → update quiet status notification
```

Only Digest‑mode notifications are ever stored, which makes the stats exact: **every stored
notification is an interruption that was avoided.**

### 2) Delivering a digest (scheduled or manual)

```
WorkManager (scheduled)  or  "Deliver Now"
        │
        ▼
DeliverDigestUseCase
   pending = NotificationRepository.pendingSnapshot()
   if empty → DeliverResult.Empty
   else:
     groups   = GroupNotificationsUseCase(pending)
     digestId = DigestRepository.createDigest(type, now, counts)
     NotificationRepository.assignToDigest(ids, digestId, now)   // delivered=true, not deleted
     DigestNotifier.postDigest(grouped)                          // single grouped notification
        │
        ▼
(scheduled only) DigestScheduler.reschedule()  // arm the next slot
```

### 3) Opening a notification (deep link)

```
Tap (inbox / digest / history)
        │
        ▼
OpenNotificationUseCase → markRead → DeepLinkLauncher.open
   1) PendingIntentStore.contentIntent(key)?.send()  → DEEP_LINKED  (exact destination)
   2) packageManager.getLaunchIntentForPackage(pkg)  → OPENED_APP   (graceful fallback)
   3) otherwise                                        → FAILED      (graceful message)
```

## Design decisions

- **Single module, layered packages.** Maximises clarity and build speed while keeping strict
  separation. The clean boundaries make a later split into Gradle modules mechanical if desired.
- **Store only Digest‑mode notifications.** Simplifies the model and makes "interruptions avoided"
  trivially correct.
- **Self‑re‑arming one‑time work** (instead of periodic work) gives precise, per‑schedule delivery
  times and survives reboots without a long‑running service.
- **System effects behind interfaces.** Keeps the domain pure and the use cases unit‑testable with
  fakes/mocks; no Robolectric needed for the core logic.
- **DataStore for settings, Room for data.** The right tool for each: typed preferences vs. indexed,
  query‑heavy notification storage (indexed for 10k+ rows).
- **Minimal assumptions.** Only an explicit, small critical set is Real‑Time by default
  (`CriticalDefaults`); no category guessing — the user owns every other decision.

## Testing strategy

The pure use cases and utilities are covered by fast JVM unit tests (JUnit + Truth + MockK +
coroutines‑test). Because the domain has no Android dependencies, these run without an emulator.
Repository implementations can be covered with Room's in‑memory database in instrumented tests
(scaffolding/deps included via `androidx.room:room-testing`).
