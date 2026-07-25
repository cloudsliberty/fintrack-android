# Changelog

All notable changes to FinTrack for Android are documented in this file.

## [1.1.7]

### Accounts
- Added a **view toggle** (top-right of the search bar): **Detail** (the existing full card —
  icon, name, type/currency, edit + delete) and **Compact tile** (3 across the screen width, no
  edit/delete actions, just icon, name, currency code, and account type). Tapping either still
  opens that account's transactions.

### Loading Animation
- Replaced the plain spinner everywhere the app shows a loading state with an animated version
  of the FinTrack logo: five translucent bars pulsing at staggered intervals behind a bold white
  "FT", with the F and T themselves gently pulsing too — a direct Compose recreation of the
  supplied CSS keyframe animation (same timings: bars 1.8s staggered by 0.2s each, letters 1.4s,
  T lagging 0.3s behind F).

### Version
- Bumped to **v1.1.7** (versionCode 9).

---

## [1.1.6]

### Transactions
- Date range (From/To) filter fields are now reliably tappable — same "transparent tap-catcher
  overlay" fix already used elsewhere, since a plain `readOnly` field can silently swallow the
  tap before it reaches an outer click handler.
- Added an **Income / Expense / Difference** summary bar above the transaction list: green for
  income, red for expense, orange for the difference. Computed per-currency so amounts are never
  summed across currencies if the filtered set spans more than one.
- Filter panel is more compact: only the **Account** field (label reverted to
  *"Account (all if empty)"*) is visible by default; description search, category, date range,
  and tags all moved into the collapsible "more filters" section, freeing up space for the actual
  transaction list.
- Pull down to refresh.

### Accounts
- Account icon/initial is now 2.5× larger.
- Pull down to refresh.

### Dashboard
- Pull down to refresh.

### Settings
- Removed the old server-side **App Lock** section.
- **PIN Lock** section can now also enable **fingerprint / face unlock** as a shortcut (device
  must support it) — the PIN itself remains the fallback if biometrics fail or aren't set up.

### PIN Unlock Screen
- Now shows the FinTrack logo, app name, and version.
- Prompts for fingerprint/face automatically on screen show when enabled, with the PIN field
  always available underneath as a fallback.

### Top Status Indicator
- Replaced the text pill ("Data refreshing…", etc.) with icons: a spinner while a fetch is in
  flight, a green cloud when the last refresh succeeded, a red cloud when offline/showing cached
  data. A numeric badge still appears when writes are queued for sync; tapping it still opens the
  pending-sync list.

### Layout
- Removed page titles from the four bottom-nav screens (Transactions, Accounts, Dashboard, More)
  to reclaim vertical space — the bottom nav itself already indicates where you are.

### App Icon
- The "FT" glyph now fills ~85% of the icon (was noticeably small before), on both the legacy
  launcher icon and the adaptive icon's foreground layer.

### Version
- Bumped to **v1.1.6** (versionCode 8). Compose BOM bumped to 2024.09.00 (needed for the stable
  `PullToRefreshBox` API). Added `androidx.biometric` and `androidx.fragment` dependencies;
  `MainActivity` now extends `FragmentActivity` instead of `ComponentActivity` (required by
  `BiometricPrompt`, fully Compose-compatible).

---

## [1.1.5] — Crash Fix

### Fixed: app crashed immediately when opening Accounts / Dashboard / More
- **Cause:** `NavHost`'s `startDestination` was set to the plain `"transactions"` route, but the
  Transactions destination is actually registered under the parameterized route
  `"transactions?accountId={accountId}"` (added to support "tap an account → see its
  transactions"). Those two strings don't match, so the graph's `startDestinationId` didn't
  correspond to any real destination. Tapping any other bottom-nav tab ran
  `navController.graph.findStartDestination()` — which found nothing and threw
  `NoSuchElementException: Sequence is empty` — and crashed the app. Transactions itself never hit
  this code path (you start there), which is why only the *other* tabs appeared broken.
- **Fix:** `startDestination` now uses the exact same parameterized route string the destination
  is registered under, and is computed once via `remember` instead of every recomposition. The
  bottom-nav's `popUpTo(...)` also no longer depends on `graph.findStartDestination()` at all —
  it pops to our own known start route directly, so this class of mismatch can't crash things
  again even if routes change in the future.

### Version
- Bumped to **v1.1.5** (versionCode 7).

---

## [1.1.4]

*(No separate entry — version bump only, prior to this crash-fix build.)*

---

## [1.1.4]

- **Reverted application ID / namespace** back to `com.fintrack.android` everywhere (undoes the
  1.1.3 rename to `com.cloudsliberty.fintrack`) — `build.gradle.kts` `namespace`/`applicationId`,
  and the `R`/`BuildConfig` imports in `CommonComponents.kt`, `AboutScreen.kt`, and `ApiClient.kt`
  are all back in sync with the Kotlin source package.
- **Repo links** updated: the Android app's own GitHub link (About screen, README) now points to
  `github.com/cloudsliberty/fintrack-android`, and a separate link to the FinTrack **backend**
  (Nextcloud app) repo, `github.com/cloudsliberty/fintrack`, was added alongside it so the two
  projects aren't conflated.
- **Accounts screen:** added a "Search accounts" box, and accounts are now grouped into
  collapsible sections (Asset / Expenses / Revenue / Liability / Inactive) instead of one flat
  list — each section header shows a count and can be expanded/collapsed; searching auto-expands
  any section with a match.
- **Accounts screen:** account icons are now bigger (40dp → 56dp) for easier scanning.
- **Accounts screen:** tapping an account row now opens **Transactions**, filtered to that
  account (all other filters reset), instead of opening the edit dialog. Edit/Delete are still
  available via their icon buttons on the row.
- **Transactions screen:** added a description search box next to the account selector, both
  always visible above the collapsible "more filters" section (category/date/tags). Matches
  description text case-insensitively, client-side.
- **Dashboard:** removed the "Recent Transactions" list; added an "Upcoming Recurring
  Transactions" section (next 5 active recurring rules by due date) and kept/renamed the budgets
  section to "Budgets vs. Spending" for clarity.

## [Release Configuration — same version, 1.1.3]

⚠️ **Security note:** a keystore password was pasted in plaintext during this session while
requesting these changes. It was **not** written into any file — treat it as compromised and
generate a fresh one when you set up your real keystore.

- **Application ID / namespace** changed to `com.cloudsliberty.fintrack` (was
  `com.fintrack.android`) — this is what Google Play identifies the app by, and can never change
  again after the first release. Kotlin source files keep their existing `com.fintrack.android.*`
  package declarations (that's independent of the app/namespace ID and doesn't need to match); the
  two spots that imported `R`/`BuildConfig` by name were updated to the new namespace.
- **Release signing** added the safe way: credentials are read from a local `keystore.properties`
  file (see `keystore.properties.example` for the template) or equivalent environment variables
  (`FINTRACK_KEYSTORE_PATH`, `FINTRACK_KEYSTORE_PASSWORD`, `FINTRACK_KEY_ALIAS`,
  `FINTRACK_KEY_PASSWORD`) — never hardcoded in `build.gradle.kts`. If `keystore.properties` isn't
  present (e.g. a fresh checkout), the release build type simply skips custom signing instead of
  failing the whole build.
- Added `.gitignore` (was missing) covering `keystore.properties`, `*.jks`, `*.keystore`, and the
  usual Gradle/Android build output — so signing secrets can't be committed by accident.
- **Shrinking & optimization:** `isMinifyEnabled = true` and `isShrinkResources = true` for release
  builds (was `false`/unset). Expanded `proguard-rules.pro` with keep rules for the Gson-serialized
  model/sync classes, `TypeToken`-based generic deserialization, and the Retrofit API interfaces,
  plus `-dontwarn` entries for optional OkHttp platform dependencies R8 otherwise flags.
- **Debug logging:** audited the codebase — there were no `Log.d`/`Log.v`/`Log.i` calls to remove.
  The one thing that *was* logging unconditionally, OkHttp's `HttpLoggingInterceptor`, is now
  gated to debug builds only (`BuildConfig.DEBUG`); release builds log no network activity at all.


---

## [1.1.3]

### UI Refresh — Cache First, Then Live
- Every main list screen (Accounts, Transactions, Dashboard, Categories, Budgets, Recurring,
  Transfers) now shows whatever was last cached **immediately** on load, then silently swaps in
  the live server data once it arrives — no more blank/spinner screen on every visit when we
  already have something to show. A true first-ever load (nothing cached yet) still shows the
  loading state as before.

### Account Management
- Confirmed/retained from 1.1.1: accounts in every selector are grouped into **Asset / Expenses /
  Revenue / Liability / Inactive**, with fuzzy search by name across all groups.

### PIN Lock (New)
- Added a genuine **on-device PIN lock**, separate from the existing server-tracked "App Lock"
  password: Settings → **PIN Lock**.
- Set a 4–6 digit PIN and a background timeout (Immediately / 1 / 5 / 15 / 30 minutes).
- The PIN is **always** required on a fresh app open. If the app was only briefly backgrounded
  (within the configured timeout), reopening it doesn't re-prompt.
- Includes a "Forgot PIN? Log out" escape hatch on the lock screen so no one gets permanently
  locked out of their own device.

### Version
- Bumped to **v1.1.3** (versionCode 5).

---

## [1.1.2]

### Version
- Bumped to **v1.1.2** (versionCode 4). No functional changes beyond the 1.1.1 fixes below —
  this bump exists to mark a fresh, verified build of the package (all 1.1.1 changes confirmed
  present: fuzzy combo box fix, grouped account picker, offline sync, PayPal/Ko-fi donate buttons).

---

## [1.1.1]

### Transaction Filters
- Account filter placeholder text changed from `Account (all if empty)` to `Account (all)`.
- Added a **Clear all filters** button next to the account filter. It only appears/acts while the
  filter panel is expanded — collapsed, the account field stays a quick one-tap switch rather than
  doubling as a reset control.

### Fuzzy Search / Combo Box Fix (App-Wide)
- **Root cause found:** account and category combo fields were rendered `readOnly = true`, which
  silently blocks all keyboard input — so typing to search was never actually possible; the field
  could only ever show whatever text was already there.
- **Fix:** every fuzzy combo box (`FuzzyComboField`, and everything built on it) is now always
  editable. Typing filters the dropdown in real time everywhere: transaction entry, transaction
  filters, transfers, and recurring rules.
- Tapping into a combo box now auto-selects the existing text, so you can just start typing to
  replace it instead of manually clearing it first.
- Added a **Clear (✕)** icon inside every fuzzy combo box to reset it in one tap.

### Account Selection
- New grouped, fuzzy-searchable account picker (`AccountFuzzyComboField`) used everywhere an
  account is chosen. Results are grouped into **Asset / Expenses / Revenue / Liability / Inactive**
  so long account lists stay scannable.
- Search-by-name works across all groups at once.
- Swapped into: transaction entry, transaction filters, Transfers (From/To), and Recurring rules.

### Offline Support & Background Sync
- Creating or editing an **account** or **transaction** while offline now saves instantly to a
  local cache and is queued for later sync — the UI reflects the change immediately instead of
  failing silently.
- A background `SyncWorker` (WorkManager) automatically replays the queued writes against the
  server as soon as connectivity returns (detected via `ConnectivityManager.NetworkCallback`,
  registered at app startup).
- The top-right status pill now also shows **"N items pending sync"** when there's a queue.
  Tapping it opens a sheet listing what's waiting, with a manual **Sync now** action.

### Settings & About
- Replaced the single "Donate" button with two dedicated buttons:
  - **PayPal** → `paypal.me/jaleel1618`
  - **Ko-fi** → `ko-fi.com/jaleel1618` (with a card-payment icon accent)
- About screen developer name updated to **Abdul Jaleel Adenpulan**.

### Version
- Bumped to **v1.1.1** (versionCode 3).

---

## [1.1.0]

### Branding
- New app icon and in-app loading-screen logo: white "FT" on blue, matching the supplied
  Nextcloud-style icon artwork. Generated for every mipmap density plus the adaptive-icon
  foreground/background layers.

### Settings
- Removed **Reset All Data** — no longer exposed in the UI.
- Removed **Set base currency** per-currency action.

### Accounts
- Account types restricted to the four ledger types the backend actually accepts: **Asset,
  Expense, Revenue, Liability** (previously an unrelated preset like Checking/Savings/Credit/etc.).
- Currency picker in the account editor now only offers currencies you've actually created —
  no more hardcoded preset currency list.

### Navigation
- Bottom navigation changed to **Transactions / Accounts / Dashboard / More**.
- **Budgets** moved out of the bottom bar and into the new **More** screen.
- New **Dashboard** screen (replacing Budgets' bottom-bar slot): net worth, total assets/liabilities,
  cash flow, active budget progress, top spending categories, and recent transactions.
- New **About** screen (via More): app + developer info and a donate link.

### Add/Edit Transaction Screen
- Fixed: **accounts** list wasn't refreshing after an account was added/edited elsewhere.
- Fixed: **categories** list wasn't refreshing after a category was added/edited elsewhere.
  (Both fixed by refreshing accounts/categories/tags whenever the Transactions screen resumes.)
- Category field changed from multi-line to **single-line**, and fuzzy filtering now actually
  narrows results as you type.
- **Date** field is now changeable (was effectively stuck — a `readOnly` field was swallowing the
  tap before it reached the date picker).
- Tags input replaced with a **fuzzy multi-select chip picker** instead of a plain existing-tags list.
- Account input converted to a fuzzy combo box.
- Transactions list now refreshes automatically after adding/editing a transaction.
- Filters redesigned: **Account** stays always visible; **Date, Category, Tags** live in a
  collapsible "more filters" section underneath.

### Transfers & Recurring
- From/To account fields (Transfers) and the account field (Recurring) converted to fuzzy combo boxes.

### Offline & Sync (initial version)
- Added an offline cache: every list-shaped GET response (accounts, transactions, categories,
  currencies, budgets, tags, recurring rules) is mirrored locally and used as a fallback if a
  network call fails.
- Added a top-right status pill showing **Connecting to server… / Data refreshing… / Offline —
  showing cached data**.

### Version
- Bumped to **v1.1.0** (versionCode 2).
