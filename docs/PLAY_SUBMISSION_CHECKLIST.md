# Play Store submission — step-by-step checklist

The single ordered runbook. Each step says **who** does it and **where**. Items marked 🔒 can only be
done by you (Google account / Console / device); everything else is already prepared in this repo.

> **State as of v1.1.21:** signed AAB builds on every `v*` tag; privacy policy hosted; icon + feature
> graphic generated; listing copy, declarations, and Data Safety answers drafted. What's left is
> Console data entry, screenshots, and (if Drive backup matters) one OAuth SHA-1.

---

## 0. One-time account setup 🔒

- [ ] **Create / open a Google Play Console account** — https://play.google.com/console
  ($25 one-time). Note whether it's a **Personal** or **Organization** account:
  - **Personal** → Google requires a **closed test with 12+ testers for 14 continuous days**
    *before* you can promote to Production. Plan for this (step 7).
  - **Organization** → no 12-tester rule; you can go to Production after review.
- [ ] **Create app** → name **Notification Digest**, language **English (US)**, type **App**, **Free**.

## 1. The artifact to upload (already automated)

- [ ] Confirm the latest release built green and grab the **`.aab`** (not the `.apk`) — Play wants the
      bundle. The CI uploads it to the GitHub Release for each tag.
      Latest: `v1.1.21`. To cut a fresh build, bump `versionCode`/`versionName` in
      `app/build.gradle.kts`, commit, push `main`, then `git tag vX.Y.Z <sha> && git push origin vX.Y.Z`.
- [ ] The **first AAB upload enrolls the app in Play App Signing automatically** — accept it.

## 2. Privacy policy 🔒 (URL is live)

- [ ] In **App content → Privacy policy**, paste:
      **`https://aucksy.github.io/notification-digest/`**
      (Served from `docs/index.html` via GitHub Pages. If it 404s, confirm Pages is enabled:
      repo **Settings → Pages → Source = Deploy from branch → `main` / `/docs`**.)

## 3. Store listing — paste from `docs/STORE_LISTING.md`

In **Grow → Store presence → Main store listing**:
- [ ] **App name:** `Notification Digest`
- [ ] **Short description** (74 chars) and **Full description** — copy verbatim.
- [ ] **App icon:** upload `play/assets/ic_play_512.png` ✅
- [ ] **Feature graphic:** upload `play/assets/feature_graphic_1024x500.png` ✅
- [ ] **Phone screenshots (2–8):** 🔒 capture on a device —
      `adb exec-out screencap -p > play/assets/1-home.png` (Home, Inbox-by-day, Apps toggles,
      a delivered digest, Settings/privacy).
- [ ] **Category:** Productivity. **Tags/contact email:** `aakashpahuja1990@gmail.com`.

## 4. App content / declarations — paste from `docs/PLAY_DECLARATIONS.md`

- [ ] **Data safety:** *No data collected / No data shared.* (Drive backup = user's own Drive, not
      developer collection.) Deletion: on-device "Clear all data" + user-owned Drive file.
- [ ] **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`): declare core-purpose use; be ready
      to record a **30–60s demo video** (enable access → a Digest app collected → digest → inbox).
- [ ] **`QUERY_ALL_PACKAGES`** — *the declaration most likely to draw a question.* Paste the justification.
      **Fallback ready:** if rejected, swap to a manifest `<queries>` element (one-line change — ask me).
- [ ] **Ads:** None. **Content rating:** complete questionnaire → expect Everyone. **Target audience:** 13+.
- [ ] **Government/financial/health:** N/A.

## 5. Google Drive backup — OAuth SHA-1 🔒 (do before relying on backup)

- [ ] Play Console **→ Setup → App signing** → copy the **SHA-1 of the app-signing certificate**.
- [ ] Add it as an **Android OAuth client** (package `com.notdigest.app` + that SHA-1) in the Google
      Cloud project that owns the Sign-In web client (`gmailapi-491903`, shared with the sibling apps).
      *Without this, Google Sign-In returns DEVELOPER_ERROR in the Play build and backup won't connect.*
      (Notification batching/digest/inbox all work without this — it only gates Drive backup.)

## 6. Internal testing (smoke test the real Play build)

- [ ] **Testing → Internal testing → Create release** → upload the AAB → add yourself as a tester →
      install via the opt-in link. Verify: notification access prompt, a digest delivers, inbox groups
      by day, the new unread dots + swipe-right hint, and (if step 5 done) Drive backup connects.

## 7. Closed test (Personal accounts only) 🔒

- [ ] If Personal: **Testing → Closed testing**, recruit **≥12 testers**, keep them opted in for
      **14 continuous days**. Only then does Production unlock.

## 8. Production 🔒

- [ ] **Production → Create release** → upload AAB → release notes (from `STORE_LISTING.md`) →
      set rollout (start at e.g. 20%) → **Send for review**. First review typically a few days,
      longer if the notification-access demo video is requested.

---

## Quick file map

| Need | File |
|------|------|
| Listing copy | `docs/STORE_LISTING.md` |
| Every declaration / Data Safety answer | `docs/PLAY_DECLARATIONS.md` |
| Hosted privacy policy (source) | `docs/index.html` → `https://aucksy.github.io/notification-digest/` |
| Privacy policy (markdown) | `docs/PRIVACY_POLICY.md` |
| Icon + feature graphic | `play/assets/` |
| Build/ship mechanics | `docs/SHIPPING.md`, `docs/PLAY_STORE.md` |

## What I cannot do for you (and why)

Creating the Console account, paying the fee, capturing device screenshots, recording the demo video,
adding the OAuth SHA-1, recruiting testers, and pressing **Send for review** all require your Google
identity and a device. Everything that can be prepared in the repo, is.
