# Publishing to the Google Play Store — step by step

Two apps, one process. Play always ingests an **`.aab`** (Android App Bundle). Only the *build* step
differs:
- **Notification Digest** (native Kotlin/Gradle) → `./gradlew bundleRelease`
- **ColorCloset** (Expo/React Native) → `eas build -p android --profile production`

Everything else in Play Console is identical. App‑specific notes are marked ⚠️.

---

## Phase 0 — Developer account (do this first; it can take days)

1. Go to **play.google.com/console** and sign in with the Google account you want to own the apps.
2. Pay the **one‑time US$25** registration fee.
3. Choose account type:
   - **Personal** — fastest; requires identity verification (government ID).
   - **Organization** — requires a **D‑U‑N‑S number** (free, ~1–2 weeks to obtain if you don't have one).
4. Complete **identity/contact verification**. Approval can take a few hours to a few days.

⚠️ **New‑account testing requirement (very important).** For **personal** developer accounts created
after **Nov 13, 2023**, Google requires you to run a **closed test with at least 12 testers, opted in
for 14 continuous days**, *before* you can apply for production access. Plan for this — it adds ~2
weeks before your first public launch. Organization accounts are exempt.

---

## Phase 1 — Build the upload artifact (`.aab`)

### Notification Digest (Gradle)

1. **Create a real upload key** (do NOT ship the throwaway `release-test.keystore`). `keytool` ships
   with any JDK:
   ```bash
   keytool -genkeypair -v -keystore upload-keystore.jks \
     -alias notdigest-upload -keyalg RSA -keysize 2048 -validity 9125
   ```
   (9125 days ≈ 25 years.) **Back this file up** and remember the passwords.
2. Point the build at it via `keystore.properties` at the project root (gitignored):
   ```properties
   storeFile=../upload-keystore.jks
   storePassword=...
   keyAlias=notdigest-upload
   keyPassword=...
   ```
3. Build the bundle:
   ```bash
   ./gradlew bundleRelease
   ```
   Output: **`app/build/outputs/bundle/release/app-release.aab`**.

> **Play App Signing** (default & recommended): Google holds the real *app‑signing* key; the key you
> created above is just your *upload* key. If you ever lose the upload key, Google can reset it — you
> can't lose the ability to update your app. Don't disable this.

### ⚠️ ColorCloset (Expo / EAS)

```bash
eas build -p android --profile production   # produces an .aab; EAS manages signing
```
Download the `.aab` from the EAS build page (or `eas submit` can upload it straight to Play). Note:
the Google Drive backup feature is currently gated on the owner's Web client ID — resolve that before
relying on it in a public build.

---

## Phase 2 — Create the app in Play Console

1. **Play Console → All apps → Create app.**
2. App name, default language, **App** (not Game), **Free**/Paid, accept the declarations.
3. You land on the app **Dashboard** with a checklist. Work through it (Phases 3–5).

---

## Phase 3 — Required "App content" forms (Dashboard → Policy → App content)

| Form | Notification Digest answer |
|------|----------------------------|
| **Privacy policy** (URL, required) | You MUST host a privacy policy and paste its URL. See the draft in `docs/PRIVACY_POLICY.md` — host it on GitHub Pages, your site, or a Google Site. |
| **App access** | "All functionality available without special access" (no login). |
| **Ads** | No ads. |
| **Content rating** | Fill the questionnaire → likely **Everyone**. |
| **Target audience & content** | Choose age groups (13+ is typical; avoid declaring it for children). |
| **Data safety** | **No data collected, no data shared.** Everything is on‑device. This is a genuine selling point — say so. |
| **Government / financial / health** | Not applicable. |

### ⚠️ Sensitive‑permission declarations (Notification Digest only)

Notification Digest uses two permissions Google scrutinizes. Expect to fill **Permissions
declaration** forms (Dashboard → App content):

1. **`BIND_NOTIFICATION_LISTENER_SERVICE` (Notification access).** Allowed when notification access is
   the app's **core function** (it is). Declare it, describe the digest/inbox feature, and be ready to
   provide a **short demo video**. Policy forbids using notification data for ads or selling it — you
   don't, so state that.
2. **`QUERY_ALL_PACKAGES`.** This is the riskier one. Google restricts it to specific use cases. A
   "let the user choose which apps to digest" feature is a reasonable justification, but be prepared
   for pushback. **Mitigation if rejected:** the app already degrades gracefully — you can narrow
   visibility using a `<queries>` element instead of `QUERY_ALL_PACKAGES`, at the cost of not
   enumerating *every* app. Keep this in your back pocket.

> Ready‑to‑paste justification text for both is in `docs/PLAY_DECLARATIONS.md` (ask me to generate it).

---

## Phase 4 — Store listing (Dashboard → Grow → Store presence → Main store listing)

You need:
- **App name** (30 chars), **short description** (80), **full description** (4000). Drafts available —
  ask me.
- **App icon** — 512×512 PNG (32‑bit). Export one from the adaptive icon.
- **Feature graphic** — 1024×500 PNG/JPG.
- **Phone screenshots** — 2–8, e.g. 1080×1920. Capture from the running app:
  ```bash
  adb exec-out screencap -p > home.png
  ```
- (Optional) tablet screenshots, a promo video.

---

## Phase 5 — Create a release & roll out

Tracks, from safest to public: **Internal testing → Closed testing → Open testing → Production.**

1. **Release → Testing → Internal testing → Create new release.**
2. Upload the **`.aab`**. (First upload links Play App Signing.)
3. Add a **release name** and **release notes**.
4. **Save → Review release → Start rollout to Internal testing.** (Internal testing is near‑instant.)
5. Add testers (email list) and share the opt‑in link; confirm it installs and runs on a real device.
6. ⚠️ **Personal accounts:** promote to **Closed testing** and run the **12‑testers / 14‑day** test.
7. When satisfied: **Production → Create new release**, reuse the same `.aab` (or a higher
   `versionCode`), and **roll out** (you can stage to a % of users).

**Review time:** first production reviews can take a few hours to ~7 days; sensitive permissions push
it toward the longer end.

---

## After launch

- Every update needs a **higher `versionCode`** (bump it in `app/build.gradle.kts`, then
  `bundleRelease` again).
- Watch **Android vitals** (crashes/ANRs) and respond to policy emails promptly.
- Keep your **upload keystore** backed up forever.

---

## Quick reference — what each app uploads

| | Build command | Artifact | Signing |
|---|---|---|---|
| Notification Digest | `./gradlew bundleRelease` | `app/build/outputs/bundle/release/app-release.aab` | your upload key + Play App Signing |
| ColorCloset | `eas build -p android --profile production` | `.aab` from EAS | EAS‑managed |
