# Notification Digest

**A calmer phone, without missing what matters.**

Notification Digest intercepts notifications before they interrupt you, stores them privately on
your device, and delivers them as clean, organised digests at the times *you* choose. A small set
of obviously‑critical apps (messages, OTP/authenticators, calls, alarms) stay real‑time by default —
everything else is your call.

> Production‑quality, single‑module Android app built with **Kotlin · Jetpack Compose · Material 3 ·
> Room · WorkManager · Hilt**, following an MVVM + Clean Architecture structure. 100% on‑device.
> No account, no cloud, no analytics, no tracking.

---

## Table of contents

- [Highlights](#highlights)
- [Screens](#screens)
- [How it works](#how-it-works)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Build & run](#build--run)
- [Permissions](#permissions)
- [Privacy](#privacy)
- [Testing](#testing)
- [Known limitations & design notes](#known-limitations--design-notes)
- [Future‑proofing](#future-proofing)
- [License](#license)

---

## Highlights

- **Digest, don't disturb.** Apps in *Digest* mode are collected silently and removed from the
  shade; you receive a single grouped digest at scheduled times.
- **Almost no setup.** Install → grant access → pick a schedule → done. Every app defaults to Digest;
  only a tiny critical set (SMS, authenticators/OTP, dialer, alarms) is Real‑Time by default. No app
  is assumed to be banking/social/shopping — *you* decide.
- **Deliver Now & Review Now.** Never wait for the next slot — generate a digest or open the inbox
  any time, from the dashboard or the inbox.
- **Inbox like email.** Search, filter by app, multi‑select, mark read, delete, and swipe
  (→ open, ← delete) with a one‑tap "make this app Real‑Time".
- **Deep linking.** Tapping a collected notification restores the original destination (exact chat /
  email / thread) whenever Android allows, falling back gracefully to opening the app.
- **Digest history.** Every delivered batch is kept and searchable; opening a notification never
  deletes it — retention is entirely your choice.
- **Exempt‑apps management.** A fast, beautiful screen to move apps between Digest and Real‑Time,
  with search, bulk actions, filters and a "recently changed" row.
- **Smart, non‑intrusive suggestions.** Purely volume‑based ("Instagram sent 72 this week") — no AI,
  always dismissible.
- **Interruption‑savings counter.** A quiet, non‑gamified metric of how many interruptions you've
  avoided today / this week.
- **Calm, premium UI.** Material 3 with dynamic color, a custom violet design language, spring
  animations, expandable cards, and a polished onboarding flow. Light / Dark / System themes.

---

## Screens

Onboarding · Home dashboard · Inbox · Digest detail · History · Apps (Digest vs Real‑Time) ·
Schedules · Settings.

Full annotated wireframes for every screen are in **[docs/SCREENS.md](docs/SCREENS.md)**.

---

## How it works

1. **Collection** — A `NotificationListenerService` receives every posted notification. If the app
   is in **Digest** mode, the notification's content is persisted to Room and **cancelled from the
   shade** (so you're not interrupted). Real‑Time apps — and never‑batch cases like calls, ongoing
   media, navigation and foreground‑service notifications — are left completely untouched.
2. **Queueing** — Collected notifications form your **Inbox**. A quiet, ongoing status notification
   (optional) shows how many are waiting and offers *Deliver now*.
3. **Delivery** — At each scheduled time (via WorkManager), or on demand, a **digest** is created:
   notifications are grouped by app, a single grouped notification is posted, and the items move from
   the inbox into **History** (marked delivered — never deleted).
4. **Acting on it** — Tapping any notification (in the inbox, a digest, or history) restores the
   original destination via the preserved `PendingIntent`, falling back to launching the app.

See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for the full data‑flow and design rationale.

---

## Architecture

Clean Architecture with a strict dependency direction **UI → Domain ← Data**:

```
┌───────────────────────────────────────────────────────────────────┐
│  ui/            Compose screens + ViewModels (MVVM), theme, nav      │
│  notification/  Channels, digest notification builder, deep links    │
│  service/       NotificationListenerService (collection + suppress)  │
│  work/          WorkManager scheduler + delivery/cleanup workers      │
├───────────────────────────────────────────────────────────────────┤
│  domain/        Pure Kotlin: models, repository interfaces,          │
│                 use cases, system abstractions (no Android types)     │
├───────────────────────────────────────────────────────────────────┤
│  data/          Room (entities/DAOs/db), DataStore, repo impls,      │
│                 PackageManager/icon/PendingIntent system sources      │
│  di/            Hilt modules + qualifiers                             │
│  core/          Constants, time + scheduling utilities               │
└───────────────────────────────────────────────────────────────────┘
```

- **Domain** is plain Kotlin and fully unit‑testable. Use cases hold the real logic
  (next‑digest‑time, grouping, recommendations, delivery orchestration).
- **Data** implements the domain repository interfaces over Room + DataStore + the platform.
- **System effects** (posting notifications, launching deep links, scheduling) are behind domain
  interfaces (`DigestNotifier`, `DeepLinkLauncher`, `DigestScheduler`) so use cases stay Android‑free.

---

## Project structure

```
app/src/main/java/com/notdigest/app/
├─ NotDigestApp.kt              # @HiltAndroidApp + WorkManager config + channels
├─ MainActivity.kt              # Compose host, theme, deep-link routing
├─ core/                        # Constants, TimeProvider, TimeFormatter, CriticalDefaults…
├─ domain/
│  ├─ model/                    # AppNotification, Digest, Schedule, AppRule, …
│  ├─ repository/               # Repository interfaces
│  ├─ system/                   # DigestNotifier, DeepLinkLauncher, DigestScheduler
│  └─ usecase/                  # ComputeNextDigestTime, GroupNotifications, DeliverDigest, …
├─ data/
│  ├─ local/                    # Room entities, DAOs, database, converters, mappers
│  ├─ repository/               # Repository implementations
│  └─ system/                   # InstalledApps, AppIconLoader, PendingIntentStore, DeepLinkLauncher
├─ di/                          # Hilt modules + qualifiers
├─ service/                     # DigestNotificationListenerService
├─ notification/                # Channels, DigestNotifier impl, deep-link intents, action receiver
├─ work/                        # DigestScheduler impl, workers, BootReceiver
└─ ui/                          # theme/, navigation/, components/, + one package per screen

app/src/test/java/com/notdigest/app/   # JUnit + Truth + MockK unit tests
```

---

## Tech stack

| Concern              | Choice                                                            |
|----------------------|-------------------------------------------------------------------|
| Language             | Kotlin 2.0.21                                                     |
| UI                   | Jetpack Compose, Material 3, Navigation Compose                  |
| DI                   | Hilt 2.52                                                         |
| Persistence          | Room 2.6.1, DataStore Preferences                                |
| Background work      | WorkManager 2.9.1 (+ Hilt worker factory)                        |
| Async                | Kotlin Coroutines + Flow                                          |
| Build                | AGP 8.7.3, Gradle 8.10.2, KSP, version catalog                   |
| Min / Target SDK     | 26 / 35                                                          |
| Testing              | JUnit4, Google Truth, MockK, coroutines‑test, Turbine            |

---

## Build & run

> **This project was authored without an Android SDK on the machine, so it has not been compiled
> here.** Open it in Android Studio (which bundles a compatible JDK 17) to build and run.

**Requirements**
- Android Studio **Ladybug (2024.2)** or newer
- JDK **17** (bundled with recent Android Studio)
- Android SDK Platform **35**

**Steps**
1. Open the `NotDigest Codebase` folder in Android Studio. It will sync Gradle and download
   dependencies automatically.
2. If you prefer the command line, generate the Gradle wrapper jar first (it is intentionally not
   committed): `gradle wrapper --gradle-version 8.10.2`, then `./gradlew assembleDebug`.
3. Run the `app` configuration on a device/emulator (API 26+).
4. Complete onboarding and **grant Notification Access** when prompted
   (*Settings → Notifications → Device & app notifications / Notification access*).

**Useful commands**
```bash
./gradlew :app:testDebugUnitTest     # run unit tests
./gradlew :app:assembleDebug         # build a debug APK
./gradlew :app:lintDebug             # Android lint
```

### Shipping a release

This is a native Gradle app, so it ships as a **signed APK** (sideload) or **AAB** (Play Store) —
**not** via Expo/EAS (those build React Native apps and can't build this project). Release signing is
already wired into `app/build.gradle.kts` (reads `keystore.properties` locally or env vars in CI),
and a cloud build is provided at `.github/workflows/android-release.yml`. Full instructions —
keystore creation, GitHub Actions, Android Studio, and Play Store — are in
**[docs/SHIPPING.md](docs/SHIPPING.md)**.

---

## Permissions

| Permission | Why |
|-----------|-----|
| `BIND_NOTIFICATION_LISTENER_SERVICE` (granted in system settings) | Core: receive & suppress notifications |
| `POST_NOTIFICATIONS` (Android 13+) | Post the digest & status notifications |
| `QUERY_ALL_PACKAGES` | List installed apps so you can choose Digest vs Real‑Time. A legitimate, declared use for a notification‑management app. |
| `RECEIVE_BOOT_COMPLETED` | Re‑arm scheduled digests after a reboot/update |
| `VIBRATE` | Optional, gentle haptic feedback |

No internet permission is requested — the app **cannot** send your data anywhere.

---

## Privacy

Privacy is the product, not a feature:

- All notification content lives in a local Room database; preferences in local DataStore.
- No `INTERNET` permission, no account, no cloud, no analytics SDKs, no tracking.
- Cloud backup and device‑transfer of the database/preferences are explicitly excluded
  (`data_extraction_rules.xml`, `backup_rules.xml`).
- "Clear all data" and a configurable retention window are one tap away in Settings.

---

## Testing

Unit tests cover the pure business logic (run with `:app:testDebugUnitTest`):

- `ComputeNextDigestTimeUseCaseTest` — next‑delivery computation, roll‑over, disabled schedules.
- `GroupNotificationsUseCaseTest` — grouping & ordering.
- `GenerateRecommendationsUseCaseTest` — volume thresholds, dismissal, capping/ordering.
- `DeliverDigestUseCaseTest` — delivery orchestration (MockK) incl. the empty‑queue path.
- `TimeFormatterTest` — relative/clock/"when" formatting.
- `CriticalDefaultsTest` — the small critical‑apps default set and the "no assumptions" rule.

---

## Known limitations & design notes

- **Deep‑link durability.** Android does not allow a `PendingIntent` to be persisted across process
  death, so exact deep links are restored from an in‑memory cache (`PendingIntentStore`) while the
  process lives; after a cold start we fall back to launching the owning app. This is the strongest
  restoration Android permits and is handled transparently with a graceful message.
- **Never‑batch rules.** Regardless of an app's mode, ongoing/foreground‑service notifications,
  calls, navigation and media are never collected — they pass straight through.
- **OEM background limits.** Aggressive battery managers (some OEMs) can delay WorkManager; the
  delivery chain is self‑re‑arming and re‑armed on boot to stay robust.

---

## Future‑proofing

The architecture intentionally isolates concerns so these can be added without rework:
AI summaries/prioritisation (a new use case over the existing repositories), daily/weekly recaps,
Work vs Personal profiles, Focus modes, Wear OS surface, cross‑device sync, and smart categorisation.

---

## License

Released under the MIT License — see [LICENSE](LICENSE).
