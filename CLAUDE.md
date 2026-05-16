# CLAUDE.md — LendLog

This file gives every Claude session full context about LendLog. Read this before doing anything.

---

## What Is LendLog?

A fully **local Android app** that tracks what you've lent to whom. No backend. No login. No cloud.

**Core flow:** Open → snap photo of item → pick friend from contacts → set return date → done.

The main screen is a chronological feed of outstanding loans with overdue badges. Think: the Notes-app hack everyone uses, but built properly.

---

## Repository Layout

```
lend-log/
├── CLAUDE.md                  ← you are here
├── docs/
│   ├── product-blueprint.md   ← full product decisions + data model + design tokens
│   └── design-blueprint.md    ← Travel Pack Pal design system (colors, typography, components)
└── app/                       ← Android source (Kotlin + Jetpack Compose) — not yet created
```

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Local DB | Room (SQLite ORM) |
| Preferences | DataStore |
| Async | Kotlin Coroutines + Flow |
| State | ViewModel + StateFlow |
| Navigation | Navigation Compose |
| Camera | CameraX |
| Contacts | ContactsContract |
| Notifications + Backup scheduling | WorkManager + NotificationManager |
| Billing | Google Play Billing Library v6+ |
| DI | Hilt |
| Image loading | Coil |

---

## Product Rules (non-negotiable)

1. **Fully local** — zero network calls, zero accounts, zero Firebase.
2. **One item per loan record** — no bundling multiple items.
3. **3 active loans** on the free tier. Returning a loan frees a slot.
4. **$2.99 one-time** Google Play Billing unlock for unlimited active loans.
5. **Photo is optional** — app nudges but never blocks without one.
6. **Borrower source:** phone contacts first; manual name entry as fallback.
7. **Overdue loans pinned** to the top of the feed with a red badge.
8. **Two feed views:** By Item (flat) and By Person (grouped) — toggleable.
9. **Filter chips only** — no full text search bar in MVP.
10. **Nudge = WhatsApp** pre-filled message. Graceful fallback if not installed.
11. **Notifications:** 3-day pre-warning + overdue alert. Both local, no server.
12. **Returned loans** go to a History tab — never deleted.
13. **Dual-layer backup** — Android Auto Backup (silent/automatic) + nightly local export to Downloads (fully offline). See Backup Strategy below.

---

## Backup Strategy

Two layers, both automatic. The user never has to think about it.

### Layer 1 — Android Auto Backup (silent safety net)
- Built into Android 6+. Zero extra code beyond a config XML.
- Android quietly copies the database, DataStore, and photo files to the user's Google Drive backup quota (does not count against their 15 GB).
- Restores automatically when the app is reinstalled on the same Google account.
- Requires a Google account — which every Play Store user has.
- Data leaves the phone and goes to Google's servers. Acceptable trade-off for the vast majority of users.

**Config required:**
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

### Layer 2 — Automatic nightly local export (truly offline)
- WorkManager runs a nightly job (when idle + charging) that exports all loans to a `lendlog-backup.json` file in the device's `Downloads` folder.
- This file survives uninstall because it lives in shared storage, not app-private storage.
- On reinstall, user can tap "Restore from backup" in Settings and pick the file.
- 100% local. Nothing leaves the phone. Works with no Google account.

**Settings screen actions:**
- "Restore from backup" — file picker → import JSON → repopulate Room DB
- "Export now" — manual trigger of the same export job (for power users)

### Why both?

| Scenario | Layer 1 handles it | Layer 2 handles it |
|---|---|---|
| Accidental uninstall, same Google account | ✅ | ✅ |
| New phone, same Google account | ✅ | ✅ |
| No Google account / de-Googled phone | ❌ | ✅ |
| Google account changed | ❌ | ✅ |
| User wants a local copy they can inspect | ❌ | ✅ |

---

## Screens

| Screen | Route | Status |
|---|---|---|
| Feed / Home | `home` | Not built |
| Add Loan | `add` | Not built |
| Loan Detail | `detail/{id}` | Not built |
| History | `history` | Not built |
| Paywall Sheet | bottom sheet | Not built |
| Settings | `settings` | Not built |

---

## Data Model

```kotlin
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String,           // UUID
    val itemName: String,
    val notes: String?,
    val photoUri: String?,
    val borrowerName: String,
    val borrowerContactId: String?,       // null = manually entered
    val borrowerPhone: String?,
    val returnDate: Long,                 // epoch millis
    val lentDate: Long,
    val isReturned: Boolean,
    val returnedDate: Long?,
    val tags: String,                     // comma-separated
    val createdAt: Long
)
```

DataStore keys: `isUnlocked: Boolean`, `onboardingDone: Boolean`

---

## Design System

Inherited from Travel Pack Pal. Full spec in `docs/design-blueprint.md`.

**Critical tokens for Android:**

| Token | Value |
|---|---|
| Primary teal | `Color(0xFF0E9AA7)` — `hsl(183 80% 38%)` |
| Background | `Color(0xFFF4F3F5)` — warm off-white |
| Card surface | `Color(0xFFFFFFFF)` |
| Overdue red | `Color(0xFFD32F2F)` |
| Corner radius | `12.dp` (base), `16.dp` (dialogs/sheets) |
| Body font | DM Sans |
| Display font | Cormorant Garamond |

**UI rules:**
- Overdue cards: red left border + red badge chip
- Active cards: teal left accent
- Returned (history): muted/greyed
- Empty state: icon in `primary/10` bubble + heading + subtext + CTA button
- Paywall: bottom sheet, not full screen
- FAB: teal gradient, bottom-right, 56dp circle, opens Add Loan

---

## Architecture Pattern

```
UI (Composables)
  └── ViewModel (StateFlow<UiState>)
        └── Repository
              ├── LoanDao (Room)
              ├── DataStore
              └── BackupManager (WorkManager jobs)
```

- One ViewModel per screen
- Repository is the single source of truth
- Coroutines for all async work (no callbacks)
- Hilt for injection across the graph

---

## Permissions Required

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- FileProvider for photo URIs -->
<!-- No extra permission needed for Downloads folder on Android 10+ -->
```

All permissions are **runtime-requested** with graceful fallback if denied.

---

## What Has Been Done

- [x] Product brainstorm + all decisions locked
- [x] Product blueprint written (`docs/product-blueprint.md`)
- [x] Design system documented (`docs/design-blueprint.md` — from Travel Pack Pal)
- [x] Tech stack decided: Kotlin + Jetpack Compose
- [x] Backup strategy decided: Android Auto Backup + nightly local export
- [x] This CLAUDE.md created

## What Needs to Be Built

- [ ] Android project scaffold (Gradle, Hilt, Room, Compose setup)
- [ ] Design system / theme (colors, typography, shapes in Compose)
- [ ] Room database + DAO + Repository
- [ ] Home / Feed screen
- [ ] Add Loan screen
- [ ] Loan Detail screen
- [ ] History screen
- [ ] Settings screen (Export now / Restore from backup)
- [ ] Paywall bottom sheet
- [ ] WorkManager — notification scheduling (3-day warning + overdue)
- [ ] WorkManager — nightly backup job (export to Downloads)
- [ ] Android Auto Backup config XML
- [ ] WhatsApp nudge intent
- [ ] Contacts picker + manual fallback
- [ ] CameraX photo capture
- [ ] Google Play Billing integration
