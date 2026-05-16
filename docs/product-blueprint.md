# LendLog — Product Blueprint

> "What Did I Lend to Whom?"  
> A fully local Android app. No backend. No login. Pure utility.

---

## Elevator Pitch

Open → Snap photo of item → Pick friend from contacts → Set return date.  
A clean feed of outstanding loans with overdue badges and one-tap WhatsApp reminders.

Target market: anyone with friends. Replaces the Notes-app hack everyone currently uses.

---

## Tech Stack

| Concern | Choice | Rationale |
|---|---|---|
| Language | Kotlin | JVM-based, 100% Java-interoperable, reads like modern Java |
| UI | Jetpack Compose | Declarative, modern Android standard, replaces XML layouts |
| Local DB | Room (SQLite ORM) | Feels like JPA/Hibernate — familiar to Spring Boot devs |
| Preferences | DataStore | Replaces SharedPreferences |
| Async | Kotlin Coroutines + Flow | Replaces AsyncTask/callbacks |
| State | ViewModel + StateFlow | Lifecycle-aware reactive state |
| Navigation | Navigation Compose | Type-safe screen routing |
| Camera | CameraX | Jetpack camera library |
| Contacts | ContactsContract (Android API) | Standard contacts ContentProvider |
| Notifications + Backup scheduling | WorkManager + NotificationManager | Scheduled local jobs |
| Billing | Google Play Billing Library v6+ | One-time purchase unlock |
| DI | Hilt | Jetpack-native dependency injection |
| Image loading | Coil | Kotlin-first, Compose-compatible |

### Java → Modern Android equivalents (for reference)

| Old (Java era) | Modern equivalent |
|---|---|
| XML layouts | Jetpack Compose |
| AsyncTask | Kotlin Coroutines |
| SQLiteOpenHelper | Room |
| SharedPreferences | DataStore |
| Loader / callbacks | StateFlow / ViewModel |
| Dagger 2 | Hilt |

---

## Product Decisions

### Loan Entry

| Field | Decision |
|---|---|
| Photo | Optional — app shows a soft nudge prompt but never blocks |
| Item name | Short text label (required) |
| Notes | Free-text note (optional) |
| Borrower | Phone contacts first; manual name entry if permission denied or contact doesn't exist |
| Return date | Required |
| Category / tag | Custom tags created by the user (no preset list) |
| Items per loan | One item per record — one photo, one borrower, one date |

### The Feed (Home Screen)

- Chronological list of all active (unreturned) loans
- **Overdue loans** pinned to the top with a red badge — cannot be missed
- **View toggle:** switch between:
  - **By Item** — flat list, one card per loan record
  - **By Person** — collapsible sections, all loans grouped under each borrower
- **Filter chips** (no full text search bar) — filter by: Overdue · Tag · Person
- No search bar in MVP

### Returned Loans

- Marking returned moves the loan to a **History / Archive tab**
- History is always viewable, never permanently deleted
- Returned loans do **not** count toward the active loan limit

### Notifications (all local, no server)

| Trigger | Timing |
|---|---|
| Pre-warning | 3 days before the return date |
| Overdue alert | Day the return date passes |

### Nudge — WhatsApp Reminder

- Each active loan card has a nudge button
- Opens **WhatsApp** with a pre-filled message to the borrower
- Example: _"Hey! Just a reminder — you still have my [item name]. Would love to get it back soon 😊"_
- Graceful fallback if WhatsApp is not installed (show toast)

### Monetisation

| Tier | Active loan limit | Price |
|---|---|---|
| Free | 3 active loans | Free |
| Unlimited | No limit | $2.99 one-time (Google Play Billing) |

- "Active" = unreturned loans only
- Returning a loan frees up a slot on the free tier
- History is always unlimited regardless of tier
- Paywall sheet appears when a free user tries to add a 4th active loan

---

## Data Persistence & Backup

Two automatic layers. The user never has to think about backup.

### Layer 1 — Android Auto Backup (silent safety net)

- Android's built-in system, available on every Android 6+ device
- Silently copies the Room database, DataStore prefs, and photo files to the user's Google Drive backup quota
- Does **not** count against the user's 15 GB Google Drive storage
- Restores automatically when the app is reinstalled on the same Google account
- Zero extra code — just a config XML in the manifest
- Data goes to Google's servers (acceptable for the 99% of users on a Google account)

```xml
<!-- AndroidManifest.xml -->
android:allowBackup="true"
android:dataExtractionRules="@xml/backup_rules"
```
```xml
<!-- res/xml/backup_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <include domain="database" path="loans.db" />
        <include domain="sharedpref" path="." />
        <include domain="file" path="loan_photos/" />
    </cloud-backup>
</data-extraction-rules>
```

### Layer 2 — Automatic nightly local export (100% offline)

- WorkManager job runs nightly when the phone is idle and charging
- Exports all loans to `lendlog-backup.json` in the device's `Downloads` folder
- `Downloads` is shared storage — the file survives uninstall
- On reinstall, user taps "Restore from backup" in Settings and picks the file
- Nothing leaves the phone. Works with zero Google account.

### Settings screen (backup controls)

| Action | Behaviour |
|---|---|
| Export now | Manual trigger — saves JSON to Downloads immediately |
| Restore from backup | File picker — imports a JSON and repopulates Room DB |

### Why both layers?

| Scenario | Auto Backup | Local export |
|---|---|---|
| Accidental uninstall, same Google account | ✅ | ✅ |
| New phone, same Google account | ✅ | ✅ |
| No Google account / de-Googled phone | ❌ | ✅ |
| Google account changed | ❌ | ✅ |
| User wants a file they can inspect or move | ❌ | ✅ |

---

## Screens (MVP)

| # | Screen | Purpose |
|---|---|---|
| 1 | **Feed / Home** | Active loans, overdue pinned, view toggle, filter chips |
| 2 | **Add Loan** | Photo (nudged) → item name + note → borrower → return date → tags |
| 3 | **Loan Detail** | Full loan view, nudge button, mark returned, edit, delete |
| 4 | **History** | Archive of all returned loans |
| 5 | **Settings** | Export now / Restore from backup |
| 6 | **Paywall Sheet** | Bottom sheet when free user hits 3-loan limit |

---

## Data Model

### `Loan` entity (Room)

```kotlin
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String,           // UUID
    val itemName: String,                  // required
    val notes: String?,                    // optional free text
    val photoUri: String?,                 // local file URI, optional
    val borrowerName: String,              // required
    val borrowerContactId: String?,        // null if manually entered
    val borrowerPhone: String?,            // for WhatsApp nudge
    val returnDate: Long,                  // epoch millis
    val lentDate: Long,                    // epoch millis
    val isReturned: Boolean,
    val returnedDate: Long?,               // null until marked returned
    val tags: String,                      // comma-separated, denormalised for simplicity
    val createdAt: Long
)
```

### `AppPreferences` (DataStore)

```
isUnlocked: Boolean      — true after $2.99 purchase
onboardingDone: Boolean
lastBackupTimestamp: Long — epoch millis of last nightly export
```

---

## Design Language

Inherited in full from the **Travel Pack Pal design blueprint** (see `docs/design-blueprint.md`).

### Key tokens

| Token | Value |
|---|---|
| Primary (teal) | `hsl(183 80% 38%)` / `Color(0xFF0E9AA7)` |
| Background | Warm off-white `hsl(240 5% 96%)` / `Color(0xFFF4F3F5)` |
| Card surface | Pure white `hsl(0 0% 100%)` |
| Overdue / destructive | Red `hsl(0 68% 50%)` / `Color(0xFFD32F2F)` |
| Base radius | `12dp` |
| Body font | DM Sans |
| Display font | Cormorant Garamond |
| Icon stroke | 1.75 (thinner than default 2) |

### Android translation

| Web blueprint token | Android / Compose equivalent |
|---|---|
| Tailwind CSS variables | `MaterialTheme.colorScheme` custom theme |
| `rounded-xl` (12px) | `RoundedCornerShape(12.dp)` |
| `shadow-card` | `Modifier.shadow(elevation = 2.dp, ...)` |
| Lucide icons | `androidx.compose.material.icons` + custom SVGs |
| `animate-fade-in-up` | `AnimatedVisibility` + `slideInVertically` |
| Stagger 70ms per item | `LaunchedEffect` delay per index |

---

## What's Deferred (Post-MVP)

- iOS version
- SMS fallback for the nudge button
- Statistics screen (most borrowed-from person, avg return time)
- Home screen widget (glanceable overdue count)
- Lending share link (for non-contacts)
- Dark mode

---

## Out of Scope (Forever)

- Backend / cloud sync
- User accounts / login
- Social features
- Push via FCM
