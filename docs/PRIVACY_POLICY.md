# Privacy Policy — Notification Digest

_Last updated: 2026-06-22_

Notification Digest ("the app") is designed to be private by default. This policy explains what the
app does and does not do with your information. **Plain summary: everything stays on your device. We
(the developer) never receive, see, collect, or sell any of your data.**

## Who this applies to

This policy covers the Notification Digest Android app published by **Aakash Pahuja** ("we", "us").
Contact: **aakashpahuja1990@gmail.com**.

## What the app does

Notification Digest reads incoming notifications via Android's Notification Listener service so it can
**batch** notifications from apps you put in "Digest" mode and deliver them on a schedule, instead of
interrupting you one by one. Notifications you keep in "Real-Time" mode are never touched.

## Data the app handles, and where it stays

| Data | Why | Where it is stored | Leaves your device? |
|------|-----|--------------------|---------------------|
| **Notification content** (title, text, app name, time, the tap target) | To show your batched digest and inbox | On-device only, in a local database | **No.** Never uploaded anywhere. |
| **Your app classifications, schedules and settings** | To remember how you want each app handled | On-device; optionally backed up (see below) | Only if **you** turn on a backup — to **your own** Google Drive. |
| **Your Google account email** | Only to display which account a backup is connected to | On-device | No. We never receive it. |
| **A "lifetime avoided" counter and basic on-device stats** | The numbers on the home screen | On-device | No. |

The app contains **no advertising, no analytics, and no third-party tracking SDKs.**

## Optional Google Drive backup

If — and only if — you choose to connect Google Drive, the app saves a **single small configuration
file** (your app classifications, schedules and settings) to **your own** Google Drive, using the
`drive.file` scope, which limits the app to the one file it creates. **Notification content is never
included in this backup.** This file lives in your Drive, under your control; **we have no server and
no access to your Drive.** You can disconnect or delete the backup at any time from Settings, or from
your Google account.

Android's built-in **Auto Backup** may also sync that same single configuration file to your Google
account, per your device's backup settings. The notifications database and app preferences are
excluded from Auto Backup by the app's backup rules.

## Permissions and why they're used

- **Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`)** — the core function: to read and
  batch notifications. Notification data is used only on-device, only for the digest/inbox features.
- **See all installed apps (`QUERY_ALL_PACKAGES`)** — so you can choose which of your apps to batch.
  Used only to list app names/icons on-device; the list is never transmitted.
- **Post notifications** — to deliver your scheduled digest.
- **Run after restart / ignore battery optimizations** — so digests still arrive reliably after a
  reboot or when the system would otherwise stop the app.
- **Internet / network state** — used **only** for the optional Google Drive backup you initiate.
- **Vibrate** — subtle haptic feedback on key actions.

## Data sharing and sale

We do **not** sell your data. We do **not** share your data with third parties. We do **not** use
notification content for advertising or any purpose other than providing the app's features to you.

## Children

The app is not directed at children and collects no personal data.

## Your choices

- Turn any app to Real-Time so it is never read for batching.
- Revoke Notification access at any time in Android Settings → Notification access.
- Disconnect Google Drive and/or delete the backup file at any time.
- Clear all collected notifications and digests from Settings → Clear all data.
- Uninstalling the app removes all on-device data.

## Changes

If this policy changes, we will update the date above and the copy bundled with the app.

## Contact

Questions? Email **aakashpahuja1990@gmail.com**.
