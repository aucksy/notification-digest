# Shipping Notification Digest

This is a **native Android (Kotlin + Gradle)** app, so it ships like any Android app: build a
**signed APK** (for direct install / sideloading) or a **signed AAB** (for the Google Play Store).

> **Why not Expo / EAS?** Expo and EAS Build target **React Native** apps. They cannot build this
> Kotlin/Gradle project, and the app's core — a `NotificationListenerService` that intercepts and
> suppresses notifications — is a privileged native capability that Expo's managed workflow can't
> provide. Your Expo token is not used or needed here.

You have two build paths. Both produce identical, signed artifacts.

---

## Step 1 — Create a signing key (once)

`keytool` ships with any JDK (including the JDK 8 already on this machine):

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias notdigest \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep `release.keystore` and its passwords **safe and backed up** — if you publish to Play with your
own key and lose it, you can't update the app. (Play App Signing, below, removes that risk.)

The keystore is **gitignored** and must never be committed.

---

## Path A — GitHub Actions (cloud build, no local Android SDK)

Best match for "build it in the cloud like Expo did." A workflow is already included at
`.github/workflows/android-release.yml`.

1. Put the project in a GitHub repo (from the project root):
   ```bash
   git init && git add . && git commit -m "Notification Digest"
   # create a repo on github.com, then:
   git remote add origin https://github.com/<you>/notification-digest.git
   git push -u origin main
   ```
   > If `gradle/wrapper/gradle-wrapper.jar` isn't committed, the workflow regenerates it. It's
   > simpler to commit it — Android Studio creates it on first sync (see Path B).

2. Add four repository secrets — **Settings → Secrets and variables → Actions**:

   | Secret | Value |
   |--------|-------|
   | `KEYSTORE_BASE64` | `base64 -w0 release.keystore` (the whole keystore, base64‑encoded) |
   | `KEYSTORE_PASSWORD` | keystore password |
   | `KEY_ALIAS` | `notdigest` |
   | `KEY_PASSWORD` | key password |

   On Windows PowerShell, base64 the keystore with:
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Content keystore.b64
   ```

3. Trigger a build: push a tag `git tag v1.0.0 && git push --tags`, or run the workflow manually
   from the **Actions** tab.

4. Download the signed **APK** and **AAB** from the run's **Artifacts**.

---

## Path B — Android Studio (local)

No CI, no secrets in the cloud. Android Studio bundles JDK 17 and manages the SDK.

1. Open the `NotDigest Codebase` folder in Android Studio (Ladybug 2024.2+). Let it sync.
2. Create `keystore.properties` at the project root (copy `keystore.properties.example`) and fill in
   your keystore path + passwords. It's gitignored.
3. **Build → Generate Signed Bundle / APK…**
   - choose **Android App Bundle** (`.aab`) for Play, or **APK** for direct install,
   - select your `release.keystore`, enter the passwords,
   - build the **release** variant.
4. Output:
   - APK → `app/build/outputs/apk/release/app-release.apk`
   - AAB → `app/build/outputs/bundle/release/app-release.aab`

CLI equivalent (once `keystore.properties` exists and the SDK is installed):
```bash
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # AAB
```

---

## Step 2 — Distribute

### Direct install (sideload / testing)
Send the **APK** to a device and install it (the device must allow installs from your source).
Great for testing the notification‑listener flow on real hardware.

### Google Play Store
1. In the [Play Console](https://play.google.com/console), create the app.
2. Keep **Play App Signing** enabled (recommended): you upload the **AAB** signed with your *upload*
   key; Google holds the real app‑signing key, so a lost key is recoverable.
3. Upload the **AAB** to a testing track (Internal testing is fastest), complete the store listing,
   content rating, data‑safety form, and the **special‑access declaration for notification access**
   (Play requires a justification for `BIND_NOTIFICATION_LISTENER_SERVICE` and
   `QUERY_ALL_PACKAGES` — explain the digest/notification‑management use case).
4. Roll out to testers, then production.

> **Heads‑up for review:** apps using Notification Listener access and `QUERY_ALL_PACKAGES` get extra
> Play scrutiny. The privacy posture here helps: everything is on‑device, no internet permission, no
> analytics. State that clearly in the data‑safety form and the permissions declaration.

---

## Versioning

Bump these in `app/build.gradle.kts` for each release:

```kotlin
versionCode = 1        // integer, must increase every Play upload
versionName = "1.0.0"  // user-visible
```

---

## Optional — automated Play uploads

To publish from CI, add a Google Play **service account** JSON (kept out of git; see `.gitignore`)
and a publish step (e.g. `r0adkll/upload-google-play`) to the workflow, with the AAB path and your
chosen track. Ask and I'll wire it in.
