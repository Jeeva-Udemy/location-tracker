# Family Locator

A small Android app for tracking a family member's phone location live and sharing it with
others — built for one purpose: your dad keeps his phone with the app running, and you always
know where he is without him having to fiddle with Google Maps.

One APK, two roles, chosen once on first launch:

- **Dad's phone ("Share My Location")** — runs a foreground service that pushes GPS location to
  Firebase every ~30 seconds. Shows a persistent notification the whole time it's active, so it's
  never hidden from him.
- **Your phone ("Track Family Member")** — shows his live location on a map, and has a
  "Share This Location" button that opens Android's normal share sheet (WhatsApp, SMS, etc.) with
  a Google Maps link, so you can forward it to a friend to do the pickup instead.

The two phones are linked by a short **Family Code** (e.g. `7XQK2M`) you create once on your
phone and type into his.

## 1. Create a Firebase project (free)

1. Go to https://console.firebase.google.com and create a new project.
2. In the project, click **Build → Realtime Database → Create Database**. Start in
   **locked mode** (we'll paste our own rules next).
3. In the Realtime Database's **Rules** tab, paste the contents of
   [`database.rules.json`](database.rules.json) from this repo and click **Publish**.
   This makes read/write require a signed-in (anonymous) user — treat the Family Code itself
   like a password and don't post it publicly.
4. Click **Build → Authentication → Get started**, then enable the **Anonymous** sign-in
   provider. The app uses this only to satisfy the rule above — no email/password/account is
   ever created or shown to either of you.
5. Click the gear icon → **Project settings → Your apps → Add app → Android**.
   - Package name: `com.jeeva.locationtracker`
   - Download the generated **`google-services.json`**. Keep it somewhere safe on your computer —
     you'll upload its contents as a GitHub secret below, not commit it to the repo.

## 2. Get a Google Maps API key

1. Go to https://console.cloud.google.com/google/maps-apis/credentials (same Google account
   is fine).
2. Create an API key, then click into it and under **API restrictions** enable
   **Maps SDK for Android**. Leave **Application restrictions** unrestricted for now — you can
   lock it to the app's package name + SHA-1 later once you have a signed build.
3. Copy the key value — you'll paste it as a GitHub secret below.

## 3. Build the APK on GitHub (no local install needed)

This repo has a GitHub Actions workflow ([`.github/workflows/android-build.yml`](.github/workflows/android-build.yml))
that builds the APK entirely in the cloud — you never need Android Studio or an SDK on your own
machine. It needs the two things from steps 1–2 above as **repository secrets** so it can build
without them ever being committed to the (public) repo.

**Add the secrets** — on GitHub, go to your repo → **Settings → Secrets and variables → Actions →
New repository secret**, and add both:

- `GOOGLE_SERVICES_JSON` — the **base64-encoded** contents of the `google-services.json` you
  downloaded in step 1. Generate it with:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Downloads\google-services.json")) | Set-Clipboard
  ```
  (adjust the path to wherever you saved the file) then paste the clipboard contents as the
  secret value.
- `MAPS_API_KEY` — the raw Maps API key from step 2, pasted as-is.

**Run the build** — once both secrets are added, go to the repo's **Actions** tab → select
**Android Build** → **Run workflow** (or just push a commit; it also runs automatically on every
push to `main`). When it finishes, open the completed run and download the **`family-locator-debug-apk`**
artifact — that zip contains `app-debug.apk`.

Until both secrets are added, the workflow will fail on purpose at the "Write google-services.json
from secret" step with a clear error, rather than a confusing Gradle failure — that's expected on
the very first run.

**Installing the APK:** copy `app-debug.apk` to each phone (email it to yourself, use a USB cable,
Google Drive, etc.) and open it from the phone's file manager to install. You'll need to allow
"install unknown apps" for whichever app you open it from — Android will prompt for this
automatically the first time.

Repeat the install on both phones — it's the same APK; each phone picks its own role on first
launch.

<details>
<summary>Prefer building locally in Android Studio instead?</summary>

1. Install **Android Studio** (https://developer.android.com/studio).
2. Place the downloaded `google-services.json` at `app/google-services.json`.
3. Open [`gradle.properties`](gradle.properties) and replace `REPLACE_WITH_YOUR_MAPS_API_KEY`
   with your real key.
4. Open this folder as a project in Android Studio, let it sync, then **Run** (with a phone
   connected over USB) or **Build → Generate Signed Bundle / APK**.

</details>

## 4. Set it up

**On your phone:** open the app → "This is my phone" → **Create New Family Code** → tap
**Share Code** and send it to your dad's phone (text, WhatsApp, or just read it to him) → tap
**Continue to Map**.

**On dad's phone:** open the app → "This is Dad's phone" → type in the Family Code you sent him
→ **Start Sharing My Location**. Grant the location permission when asked, and when Android
prompts about battery optimization, choose to allow the app to ignore it — this stops the phone
from killing the tracking in the background. He'll see a permanent notification confirming
sharing is on; that's expected and by design, so he always knows it's active.

That's it — his location now updates on your map automatically, with no further action needed
from him. Use **Share This Location** any time to forward his current spot to a friend for pickup.

## Notes on reliability & privacy

- Location only leaves his phone while the "Sharing your location" notification is showing —
  there's no hidden or silent mode, by design, since covert tracking of a person is not something
  this app is meant to do.
- Some phone brands (Xiaomi, Oppo, Samsung, etc.) aggressively kill background apps regardless of
  Android's own rules. If updates stop coming in, check the phone's battery-saver / "auto-start"
  settings for the app and make sure it's allowed to run in the background.
- The Family Code is the only thing protecting the data — anyone who has it and opens the app
  (or calls the Firebase REST API directly) can read or write that family's location. Don't post
  it publicly; if it ever leaks, create a new code and re-enter it on both phones.
- This is a personal/hobby-scale setup (Firebase's free "Spark" tier is far more than enough for
  two phones polling every 30 seconds).
