# Notification Digest — current state & handoff

_Snapshot for picking up in a fresh chat. Last updated after **v1.1.28** (versionCode 39)._

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

## Recently shipped this arc (v1.1.22 → v1.1.28)
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

## ▶ OPEN THREAD 1 — keep-alive notification (ACTIVE, awaiting user's on-device check)
Aggressive OEMs (user is on **ColorOS/realme**) unbind the listener in the background → slip-through.
Fix = Pause's `specialUse` foreground service (`service/ListenerKeepAliveService`) that pins the
process so the listener stays bound. See memory `[[notdigest-keepalive]]`.
- **Hard constraint:** an FGS must show a notification, and Android only hides it when the app has NO
  `POST_NOTIFICATIONS`. NotDigest MUST hold it (to post digests), so the keep-alive **cannot be 100%
  invisible** while digest pop-ups exist.
- v1.1.27 removed the toggle — it just runs when notification access is granted (like Pause).
- **v1.1.28 = user chose "hide it" (option B):** channel `IMPORTANCE_MIN` (no status-bar icon) +
  `lockscreenVisibility = VISIBILITY_SECRET` (off lockscreen) + `setVisibility(SECRET)`. Channel id
  bumped `keep_alive`→`keep_alive_quiet` (visibility is immutable once a channel exists) and the old
  channel is deleted in `NotificationChannels.ensureChannels()`.
- **NEXT:** user verifies on ColorOS whether the notice is truly gone from lockscreen/status bar.
  If ColorOS overrides `VISIBILITY_SECRET`, the fallback is **option A = fully notification-free**:
  remove `POST_NOTIFICATIONS` from the manifest + stop posting digest/status notifications (digests
  become in-app only, no "digest ready" pop-up) → the FGS then becomes 100% invisible on Android 13+.

## ▶ OPEN THREAD 2 — Play Store submission (prep done in-repo; owner-only steps remain)
All prep complete (see `docs/PLAY_SUBMISSION_CHECKLIST.md`, `STORE_LISTING.md`, `PLAY_DECLARATIONS.md`).
- Privacy policy LIVE: **https://aucksy.github.io/notification-digest/** (GitHub Pages from `docs/`).
- Store graphics in `play/assets/` (512 icon + 1024×500 feature graphic).
- CI already builds the signed **`.aab`** on each tag = the upload artifact.
- **Owner-only:** create Play Console account, capture 2–8 phone screenshots, record the
  notification-access demo video, add the Play app-signing **SHA-1** to the Google OAuth client
  (project `gmailapi-491903`) or Drive backup breaks in prod, press Send for review.
- **UNANSWERED GATE:** is the Play account **personal** (→ 12-tester / 14-day closed test required
  before Production) or **organization** (→ straight to Production)?
- `QUERY_ALL_PACKAGES` is the declaration most likely to draw a question; `<queries>` fallback ready.

## Where things live
Listener: `service/DigestNotificationListenerService`. Keep-alive: `service/ListenerKeepAliveService`.
Channels: `notification/NotificationChannels`. Prefs: `data/repository/PreferencesRepositoryImpl`.
Inbox dots/hints: `ui/inbox/*` + `core/util/InboxDots.kt`. Theme: `ui/theme/Theme.kt` +
`core/util/ThemeSchedule.kt` + `ui/AppViewModel.kt`. Scheduling: `work/*`.
