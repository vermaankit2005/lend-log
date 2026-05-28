# LendLog

> **"What did I lend to whom — and when do I get it back?"**

A fully local Android app that tracks items you've lent out. No backend. No login. No subscription. Just open, log, and never forget again.

![Platform](https://img.shields.io/badge/Platform-Android%206%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-informational)
![Build](https://img.shields.io/github/actions/workflow/status/vermaankit2005/lend-log/release.yml?label=Release%20Build)

---

## What It Does

Most people track loans in a Notes app — a chaotic list with no dates, no reminders, and no way to know what's overdue. LendLog replaces that hack with a purpose-built tool.

**Core flow:** Tap the FAB → snap a photo of the item → pick the borrower from contacts → set a return date → done. The item appears on your feed and you'll get a notification 3 days before it's due and again when it goes overdue.

**Key behaviours:**
- Overdue loans are pinned to the top with a red badge — impossible to miss
- One-tap WhatsApp nudge sends a pre-filled reminder to the borrower
- Marking an item returned frees the slot and moves it to History (nothing is ever deleted)
- Two feed views: flat list by item, or grouped by person — toggleable
- Free tier: 3 active loans. One-time $2.99 unlock for unlimited

---

## Tech Stack

| Concern | Choice | Why |
|---|---|---|
| Language | Kotlin | Concise, null-safe, coroutine-native |
| UI | Jetpack Compose | Declarative, no XML, first-class state handling |
| Database | Room (SQLite) | Type-safe ORM with Flow support for reactive queries |
| Preferences | DataStore | Replaces SharedPreferences; coroutine and Flow-based |
| Async | Kotlin Coroutines + Flow | Structured concurrency, no callback hell |
| State management | ViewModel + StateFlow | Lifecycle-aware, survives configuration changes |
| Navigation | Navigation Compose | Type-safe routes, deep link support |
| Camera | CameraX | Jetpack camera library, handles lifecycle complexity |
| Contacts | ContactsContract | Standard Android content provider |
| Background work | WorkManager | Guaranteed execution for notifications and nightly backup |
| Dependency injection | Hilt | Jetpack-native DI, compile-time verified |
| Image loading | Coil | Kotlin-first, Compose-compatible, async |
| Billing | Google Play Billing v6 | One-time purchase (non-consumable) |

---

## Architecture

Clean unidirectional data flow. One ViewModel per screen. Repository as the single source of truth.

```
UI Layer (Composables)
    └── ViewModel  ←  StateFlow<UiState>  →  recompose on change
          └── Repository
                ├── LoanDao          (Room — local SQLite)
                ├── AppPreferences   (DataStore — unlock state, settings)
                └── BackupManager    (WorkManager — scheduled jobs)
```

**Patterns used:**
- **Repository pattern** — UI never touches the database directly
- **Unidirectional data flow** — UI emits events, ViewModel processes them, StateFlow pushes new state back
- **Single source of truth** — Room is the authoritative store; everything else derives from it via Flow
- **Dependency inversion** — all dependencies injected via Hilt; no singletons accessed directly

---

## Screens

| Screen | Description |
|---|---|
| **Feed / Home** | Active loans in chronological order. Overdue loans pinned at top with red badge. Toggle between flat (by item) and grouped (by person) views. Filter chips for overdue, tag, and person filters. |
| **Add Loan** | Step-through form: optional photo via CameraX, item name and note, borrower from contacts or manual entry, return date, custom tags. Free-tier paywall intercepts at 3 active loans. |
| **Loan Detail** | Full loan view with photo, borrower info, and due date. Actions: WhatsApp nudge, mark as returned, edit, delete. |
| **History** | Archive of all returned loans. Read-only. Permanent — nothing is ever hard-deleted. |
| **Settings** | Nightly backup status, manual export to Downloads, restore from JSON backup, notification preferences. |
| **Paywall Sheet** | Bottom sheet (not full screen) shown when the free tier limit is hit. One-time $2.99 unlock via Google Play Billing. Includes purchase restore flow. |

---

## Data Model

```kotlin
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String,           // UUID
    val itemName: String,
    val notes: String?,
    val photoUri: String?,                // local file URI
    val borrowerName: String,
    val borrowerContactId: String?,       // null = manually entered name
    val borrowerPhone: String?,           // used for WhatsApp nudge
    val returnDate: Long,                 // epoch millis
    val lentDate: Long,
    val isReturned: Boolean,
    val returnedDate: Long?,
    val tags: String,                     // comma-separated
    val createdAt: Long
)
```

DataStore keys: `isUnlocked: Boolean`, `onboardingDone: Boolean`, `lastBackupTimestamp: Long`, `reminderDays: Int`

---

## Backup Strategy

Two automatic layers. The user never has to think about backup.

### Layer 1 — Android Auto Backup
Android's built-in system (Android 6+) silently copies the database, DataStore preferences, and photo files to the user's Google Drive backup quota. Restores automatically on reinstall. Zero extra code beyond a config XML — just works for the 99% of users with a Google account.

### Layer 2 — Nightly local export
A WorkManager periodic job runs nightly (when idle and charging) and exports all loans to `lendlog-backup.json` in the device's `Downloads` folder. This file lives in shared storage so it **survives uninstall**. On reinstall, the user taps "Restore from backup" in Settings and picks the file. 100% offline — nothing leaves the phone.

| Scenario | Auto Backup | Local export |
|---|---|---|
| Accidental uninstall, same Google account | ✅ | ✅ |
| New phone, same Google account | ✅ | ✅ |
| No Google account / de-Googled device | ❌ | ✅ |
| Google account changed | ❌ | ✅ |
| User wants a file they can inspect | ❌ | ✅ |

---

## Notifications

Fully local — no server, no FCM. WorkManager schedules two `OneTimeWorkRequest` jobs per loan at creation time:

- **Pre-warning** — fires 3 days before the return date (configurable in Settings)
- **Overdue alert** — fires on the return date if the item hasn't been marked returned

Both notifications deep-link directly to the Loan Detail screen. If the loan is marked returned before the job fires, the worker checks and exits silently — no ghost notifications.

---

## Monetisation

| Tier | Active loans | Price |
|---|---|---|
| Free | 3 | — |
| Unlimited | No limit | $2.99 one-time |

Implemented via Google Play Billing Library v6 with a non-consumable in-app product (`lendlog_unlimited`). Includes a "Restore purchases" flow for reinstalls. The free tier enforces the limit in the Repository layer — the UI paywall is a bottom sheet, not a full screen interrupt.

"Active" means unreturned only. Returning a loan immediately frees its slot.

---

## Permissions

| Permission | Used for |
|---|---|
| `READ_CONTACTS` | Borrower picker — contact name and phone number |
| `CAMERA` | Optional item photo via CameraX |
| `POST_NOTIFICATIONS` | Due-soon and overdue loan reminders |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule WorkManager jobs after device reboot |

All permissions are runtime-requested with graceful fallback if denied. Camera denial → photo skipped. Contacts denial → manual name entry. Notifications denial → silent (no crash, no nag).

---

## CI / CD

Releases are built and signed entirely in GitHub Actions — no local Android Studio needed.

**Trigger:** push a version tag, e.g. `v1.2.0`

**Pipeline steps:**
1. Decode the upload keystore from an encrypted GitHub Secret
2. Derive `versionCode` and `versionName` from the tag (`v1.2.0` → code `10200`, name `1.2.0`)
3. Build a signed release AAB with R8 minification and resource shrinking
4. Publish the AAB as a GitHub Release artifact

The app signing key is managed by Google Play (Play App Signing). The keystore in CI is the upload key only — replaceable via Google if ever lost.

---

## Project Structure

```
app/src/main/kotlin/com/lendlog/app/
├── billing/
│   └── BillingManager.kt          # Google Play Billing v6 integration
├── data/
│   ├── db/
│   │   ├── Loan.kt                # Room entity
│   │   ├── LoanDao.kt             # DAO with Flow queries
│   │   └── LoanDatabase.kt        # Room database
│   ├── datastore/
│   │   └── AppPreferences.kt      # DataStore keys and accessors
│   └── repository/
│       └── LoanRepository.kt      # Single source of truth
├── di/
│   └── AppModule.kt               # Hilt module
├── navigation/
│   └── AppNavigation.kt           # Nav graph + deep links
├── ui/
│   ├── addloan/                   # Add loan screen + ViewModel
│   ├── components/                # Shared composables (LoanCard, EmptyState, etc.)
│   ├── detail/                    # Loan detail screen + ViewModel
│   ├── history/                   # History tab + ViewModel
│   ├── home/                      # Feed screen + ViewModel
│   ├── paywall/                   # Paywall bottom sheet
│   ├── settings/                  # Settings screen + ViewModel
│   └── theme/                     # Color, typography, shape tokens
├── worker/
│   ├── BootReceiver.kt            # Re-schedules jobs on reboot
│   ├── LoanNotificationWorker.kt  # Posts due-soon / overdue notifications
│   ├── NightlyBackupWorker.kt     # Exports JSON to Downloads
│   └── NotificationScheduler.kt  # Schedules / cancels per-loan work requests
└── LendLogApp.kt                  # Application class, Hilt entry point
```

---

## Building Locally

```bash
git clone https://github.com/vermaankit2005/lend-log.git
cd lend-log
./gradlew assembleDebug
```

The debug build skips signing entirely — no keystore needed. Install directly:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Roadmap

- [ ] Home screen widget (glanceable overdue count)
- [ ] Statistics screen (most borrowed-from person, avg return time)
- [ ] Dark mode
- [ ] iOS version (Kotlin Multiplatform / SwiftUI)
- [ ] SMS fallback for nudge when WhatsApp is not installed
