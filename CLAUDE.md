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
| Notifications | WorkManager + NotificationManager |
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

---

## Screens

| Screen | Route | Status |
|---|---|---|
| Feed / Home | `home` | Not built |
| Add Loan | `add` | Not built |
| Loan Detail | `detail/{id}` | Not built |
| History | `history` | Not built |
| Paywall Sheet | bottom sheet | Not built |

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
              └── DataStore
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
```

All permissions are **runtime-requested** with graceful fallback if denied.

---

## What Has Been Done

- [x] Product brainstorm + all decisions locked
- [x] Product blueprint written (`docs/product-blueprint.md`)
- [x] Design system documented (`docs/design-blueprint.md` — from Travel Pack Pal)
- [x] Tech stack decided: Kotlin + Jetpack Compose
- [x] This CLAUDE.md created

## What Needs to Be Built

- [ ] Android project scaffold (Gradle, Hilt, Room, Compose setup)
- [ ] Design system / theme (colors, typography, shapes in Compose)
- [ ] Room database + DAO + Repository
- [ ] Home / Feed screen
- [ ] Add Loan screen
- [ ] Loan Detail screen
- [ ] History screen
- [ ] Paywall bottom sheet
- [ ] WorkManager notification scheduling
- [ ] WhatsApp nudge intent
- [ ] Contacts picker + manual fallback
- [ ] CameraX photo capture
- [ ] Google Play Billing integration
