# Changelog

All notable changes to FinTrack for Android are documented in this file.

## [1.1.4]


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


### Version
- Bumped to **v1.1.1** (versionCode 3).

---

## [1.1.0]


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
