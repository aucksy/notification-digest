# Notification Digest — current state & handoff

_Snapshot for picking up in a fresh chat. Last updated **2026-07-24**. Keep-alive hide **verified
working on device**; Play account confirmed **Personal**. Shipped **v1.1.29** (versionCode 40) =
removed the Inbox swipe gesture and moved its actions into the long-press selection top bar._

## What this is
Native Android app (`com.notdigest.app`, Kotlin/Compose/Hilt/Room/WorkManager/DataStore, minSdk 26 /
compileSdk 35) at `D:\Apps\Notifications Digest\NotDigest Codebase`. A `NotificationListenerService`
batches notifications from "Digest" apps and delivers grouped digests at scheduled times; "Real-Time"
apps are untouched. Repo: **github.com/aucksy/notification-digest**.

## Build / ship (unchanged, must follow)
- **Never build locally** (toolchain removed; low-RAM machine crashes). Cloud only via GitHub Actions
  `android-release.yml` (tag-driven).
- To ship: bump `versionCode`+`versionName` in `app/build.gradle.kts` → commit → push `main` →
  `git tag vX.Y.Z <sha> && git push origin vX.Y.Z`. CI builds signed APK+AAB, runs
  `testReleaseUnitTest` (AFTER build, BEFORE publish — a green build still won't publish if a unit
  test fails), then auto-publishes the Release.
- No `gh` CLI. Poll via authenticated REST: token from
  `printf 'protocol=https\nhost=github.com\n\n' | git credential fill | awk -F= '/^password=/{print $2}'`
  — never print the token. After release, paste the direct `.apk` URL
  (`releases/download/<tag>/notification-digest-<tag>.apk`).
- Standing rule: run an adversarial review (subagent/Workflow) BEFORE tagging, not after. Keep unit
  tests in the same commit as any changed public signature.

## Recently shipped this arc (v1.1.22 → v1.1.29)
- **v1.1.22** permanent suggestion dismissal + honor the "Smart suggestions" toggle; tile-shake fix
  (`AppRuleRepository.ensureSeeded` seeds a rule the first time the listener sees a new app so the
  swipe hint can fire); deliberate-swipe threshold; **Scheduled (Auto) dark mode** (window default
  8pm–6am); unread-dot pure fn + tests.
- **v1.1.23** digest-notification tap scrolls Inbox to top; fixed the Auto-theme time-pill wrap.
- **v1.1.24** unread dots survive a freshly-tapped digest: advance the "seen" line on `ON_STOP` (real
  leave) not `ON_PAUSE` (transient), + NavHost guard against re-nav churn.
- **v1.1.25** listener self-heal via `requestRebind` (onListenerDisconnected + a 15-min
  `ListenerRebindWorker` + at each digest delivery); swipe nudge = only the top-most new app, once ever.
- **v1.1.26–28** the **keep-alive foreground service** saga (see below).
- **v1.1.29** removed the **Inbox swipe gesture** (owner: too many accidental triggers).
  `SwipeToDismissBox` + its teaching-nudge are gone; `SwipeableNotificationRow` → plain
  `NotificationRow` (tap opens / long-press selects). Swipe's two actions now live in the long-press
  selection top bar (`SelectionBar`): **Delete** (already there) + new **Make Real-Time** (⚡). Make
  Real-Time is now **batch-capable** — `makeSelectedRealtime(ids)` moves every app in the selection via
  `setModeForAll`, one Undo restores each app's prior mode. Pure `selectedApps()` helper +
  `InboxSelectionTest`. Dead swipe-hint pref removed (`swipeHintShown`/`SWIPE_HINT_SHOWN`).
  Adversarially reviewed (no blockers); hardened the top bar for large font scales. **Tradeoff:**
  Make-Real-Time is now long-press-only (no swipe hint) — a one-time "press & hold to select" tip is an
  easy follow-up if discoverability suffers. See `[[notdigest-inbox-swipe-removed]]`.

## ✅ THREAD 1 — keep-alive notification — RESOLVED (2026-07-24, verified on device)
Aggressive OEMs (user is on **ColorOS/realme**) unbind the listener in the background → slip-through.
Fix = Pause's `specialUse` foreground service (`service/ListenerKeepAliveService`) that pins the
process so the listener stays bound. See memory `[[notdigest-keepalive]]`.
- **v1.1.28's hide (option B) WORKS on the user's ColorOS phone** — the always-on notice is gone from
  **both** the status bar and the lockscreen. Mechanism kept as-is: keep-alive channel `IMPORTANCE_MIN`
  (no status-bar icon) + `lockscreenVisibility = VISIBILITY_SECRET` (off lockscreen) +
  `setVisibility(SECRET)`; channel id bumped `keep_alive`→`keep_alive_quiet` (visibility is immutable
  once a channel exists) and the old channel deleted in `NotificationChannels.ensureChannels()`.
  **No further action; no new build.**
- **Hard constraint (still true, for future reference):** an FGS must show a notification; Android only
  fully hides it when the app has NO `POST_NOTIFICATIONS`. NotDigest holds it (to post digests), so a
  collapsed line can still appear in the pulled-down "Silent" shade — acceptable to the user.
- **Dormant fallback** (only if a future ColorOS update resurfaces the notice): **option A =
  notification-free** — remove `POST_NOTIFICATIONS` from the manifest + stop posting digest/status
  notifications (digests become in-app only, no "digest ready" pop-up) → the FGS becomes 100% invisible
  on Android 13+. Not needed now.

## ▶ OPEN THREAD 2 — Play Store submission (Personal account confirmed; only owner steps remain)
All prep complete (see `docs/PLAY_SUBMISSION_CHECKLIST.md`, `STORE_LISTING.md`, `PLAY_DECLARATIONS.md`).
- **Account type = PERSONAL (confirmed 2026-07-24)** → Google requires a **closed test: ≥12 testers
  opted in for 14 continuous days** before Production unlocks. This is now the locked plan
  (checklist §7). An Organization account would have skipped it — but it's Personal, so we don't.
- Privacy policy LIVE: **https://aucksy.github.io/notification-digest/** (GitHub Pages from `docs/`).
- Store graphics in `play/assets/` (512 icon + 1024×500 feature graphic).
- CI already builds the signed **`.aab`** on each tag = the upload artifact (latest tag **v1.1.28**).
- **Owner-only, in order:** create Console account ($25) → create app → paste listing + privacy URL +
  declarations → capture 2–8 phone screenshots → record the notification-access demo video → Internal
  test (smoke) → **recruit ≥12 testers, hold 14 continuous days (closed test)** → add the Play
  app-signing **SHA-1** to the Google OAuth client (project `gmailapi-491903`) or Drive backup breaks
  in prod → Production → Send for review.
- `QUERY_ALL_PACKAGES` is the declaration most likely to draw a question; `<queries>` fallback ready.

## Where things live
Listener: `service/DigestNotificationListenerService`. Keep-alive: `service/ListenerKeepAliveService`.
Channels: `notification/NotificationChannels`. Prefs: `data/repository/PreferencesRepositoryImpl`.
Inbox dots/hints: `ui/inbox/*` + `core/util/InboxDots.kt`. Theme: `ui/theme/Theme.kt` +
`core/util/ThemeSchedule.kt` + `ui/AppViewModel.kt`. Scheduling: `work/*`.
