# FinTrack for Android

A native Android client (Kotlin + Jetpack Compose) for the FinTrack Nextcloud app,
with a Retrofit API client covering every endpoint the Nextcloud app exposes.

- **This repo (Android app):** https://github.com/cloudsliberty/fintrack-android
- **FinTrack backend (Nextcloud app, required server-side):** https://github.com/cloudsliberty/fintrack

## Requirements
- Android Studio Koala (2024.1) or newer
- JDK 17 (bundled with recent Android Studio)
- A Nextcloud server running the FinTrack app (v1.16.6+), reachable from your device/emulator

## Opening the project
1. Open Android Studio → **Open** → select the `FinTrackAndroid` folder.
2. Let Gradle sync (Android Studio will generate the Gradle wrapper jar automatically
   the first time; `gradle/wrapper/gradle-wrapper.properties` is already included).
3. Run on a device/emulator with API 26+ (Android 8.0+).

## How login works
The app uses Nextcloud's own **Login Flow v2** (the same mechanism the official
Nextcloud Android/desktop clients use):
1. Enter your server address on the login screen.
2. The app opens your server's login page in the system browser.
3. Sign in and approve access there — your real password never touches this app.
4. The app receives a scoped **app password** and stores it in
   `EncryptedSharedPreferences` (see `data/SessionManager.kt`).
5. Every API call after that uses HTTP Basic Auth with that app password
   (see `data/network/ApiClient.kt`).

## Project structure
```
data/
  model/Models.kt              — every request/response shape, matched field-for-field
                                  against the Nextcloud app's PHP controllers/services
  network/FinTrackApi.kt       — Retrofit interface, one function per API endpoint
  network/ApiClient.kt         — builds the authenticated Retrofit client
  network/NextcloudAuthApi.kt  — Login Flow v2 handshake
  repository/FinTrackRepository.kt — wraps every API call in a Kotlin Result
  SessionManager.kt            — encrypted credential storage

ui/
  login/          — server entry + Login Flow v2 UI
  accounts/       — account list, add/edit/delete
  transactions/   — transaction list, filtering, add/edit/delete
  categories/     — categories & tags (add/edit/delete, tag rename)
  budgets/        — spending limits
  recurring/      — recurring rules, incl. "Post Now"
  transfers/      — inter-account transfers
  settings/       — base currency, currencies, app lock, data reset, logout
  navigation/     — bottom-nav + NavHost wiring
  theme/          — colors matching the web app's dark theme
  common/         — shared loading/error/empty states, dialogs, money formatting
```

## API coverage
`FinTrackApi.kt` implements every route in the Nextcloud app's `appinfo/routes.php`
under the `api#` prefix: accounts, transactions (+ trash), transfers, budgets,
categories (+ export/import/defaults), currencies (+ exchange rate lookup/test),
recurring rules (+ post-now), tags (+ rename), category auto-categorization rules,
app lock, settings, summary, API token, and data reset/restore.

The UI currently exercises most of this surface directly; a few endpoints
(CSV import, category-rules editor, backup restore, API token management) are
wired up in the repository layer but don't yet have dedicated screens — see
"Suggested next steps" below.

## Suggested next steps
- CSV import screen (endpoint + models are ready: `importTransactions`)
- Category auto-categorization rules editor (`getCategoryRules`/`saveCategoryRules`)
- Recycle bin screen (`getTrash`/`restoreFromTrash`/`destroyFromTrash`/`emptyTrash`)
- Offline caching (Room) — everything today is live-network, no local cache
- Widgets / notifications for upcoming recurring transactions
- The app lock (`LockSetupRequest`/etc.) currently uses a plain text prompt;
  consider a proper numeric PIN pad UI for the on-device lock screen itself
  (distinct from the Nextcloud login, which already happened via Login Flow v2)
