# Play Console declarations — ready-to-paste

Everything you'll be asked to justify in Play Console, with answers tailored to Notification Digest.
Copy/paste the bold blocks. The order roughly follows the Dashboard → **App content** checklist.

---

## 1. Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`)

Google allows this when reading notifications is the app's **core purpose** — which it is. You'll
declare it and likely be asked for a **short demo video** (30–60s screen recording showing: enabling
notification access → a Digest app's notification being collected → the scheduled/See-Now digest →
the inbox). Justification:

> **Notification Digest's core function is to reduce interruptions by batching notifications.** The
> app requires notification access to read incoming notifications from apps the user places in
> "Digest" mode, group them, and deliver them as a scheduled summary instead of one-by-one. The user
> explicitly chooses which apps are batched; "Real-Time" apps are never altered. Notification content
> is processed and stored **only on the device**, is shown only to the user in the app's inbox/digest,
> and is **never** transmitted off-device, used for advertising, or sold. There is no server.

## 2. See all apps (`QUERY_ALL_PACKAGES`) — *the one to plan for*

This is the riskier declaration. Pick **"Apps that need to discover/communicate with apps on the
device"** → the most honest fit is the **device/account search or interoperability** style use; if a
better-matching reason isn't offered, use the free-text justification:

> Notification Digest lets the user choose, from a list of their installed apps, which apps to batch
> ("Digest") vs. keep "Real-Time". To present that complete, searchable list — with each app's name
> and icon — the app enumerates installed packages. The list is displayed **only on-device** and is
> never transmitted. No package data leaves the device and it is not used for analytics or ads.

**If Google rejects `QUERY_ALL_PACKAGES`** (have this ready): the app can fall back to a
`<queries>` element in the manifest instead. You'd lose the ability to enumerate *every* installed
app up front, but the app still works — it learns about an app the first time that app posts a
notification. This is a one-line manifest change if needed; ask me to make it.

## 3. Run in background / `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

If asked (it appears under "App access" / permissions review for some apps):

> Scheduled digests must be delivered at user-chosen times even when the device is idle. Requesting
> exemption from battery optimization keeps the notification listener and the WorkManager delivery
> chain alive so digests are not silently dropped by aggressive power management. The user opts in
> via an in-app prompt and can revoke it any time.

---

## 4. Data safety form (Dashboard → App content → Data safety)

The developer **receives no data**; everything is processed on-device, and the optional backup goes to
the user's **own** Google Drive (which the developer cannot access). Recommended answers:

- **Does your app collect or share any of the required user data types?** → **No.**
  - Rationale: "Collection"/"sharing" in Play's terms means data transmitted **off the device to you
    (the developer) or a third party**. Notification Digest transmits nothing to us. The only network
    transfer is the user-initiated backup of a settings file to the user's own Google Drive
    (`drive.file` scope), which is *user-controlled cloud storage of their own data*, not collection by
    the developer. Notification content is never part of it.
- **Is all user data encrypted in transit?** → The on-device data never transits. The Drive backup
  uses Google's HTTPS APIs → **Yes** for that path.
- **Do you provide a way to request data deletion?** → On-device "Clear all data" in Settings; the
  user owns and can delete the Drive file. → **Yes** (data is user-controlled).

> If you'd rather be maximally conservative, you may instead declare that the app **accesses** "App
> activity / Other (notifications)" but **processes it only on-device and does not collect or share
> it** — Play allows "processed ephemerally / on-device only". Either framing is defensible; "No
> collection/sharing" is the simplest and accurate one for this app.

## 5. Other App-content forms

| Form | Answer |
|------|--------|
| **Privacy policy URL** | **Live:** `https://aucksy.github.io/notification-digest/` (GitHub Pages, served from `docs/index.html`). Paste this exact URL. |
| **Ads** | **No ads.** |
| **App access** | All features available without an account/login. (Note Drive backup is optional and self-service — no reviewer credentials needed.) |
| **Content rating** | Complete the questionnaire honestly → expect **Everyone**. |
| **Target audience** | 13+ (do **not** target children). |
| **Government/financial/health** | Not applicable. |
| **Data deletion** | On-device clear + user-owned Drive file (see above). |

---

## 6. Pre-submit gotchas specific to this app

- **Drive sign-in will break in the Play build unless** you add Play's **app-signing SHA-1** (from
  Play Console → Setup → App signing) to the Google Cloud **OAuth client** used for Google Sign-In.
  Do this before relying on backup in a public release. (Same gotcha that affects the sibling apps.)
- The CI already produces a **signed `.aab`** on each `v*` tag — that's your upload artifact. The
  first upload links **Play App Signing** automatically.
- Bump `versionCode` for every upload (the tag-driven CI does versionName; keep `versionCode`
  incrementing — it already is).
