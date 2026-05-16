# LendLog — Product Blueprint

> "What Did I Lend to Whom?"  
> A fully local Android app. No backend. No login. Pure utility.

---

## Elevator Pitch

Open → Snap photo of item → Pick friend from contacts → Set return date.  
A clean feed of outstanding loans with overdue badges and one-tap WhatsApp reminders.

---

## Core Decisions

### Loan Entry

| Field | Decision |
|---|---|
| Photo | Optional, but app nudges user to add one |
| Item name / title | Short text label (required) |
| Notes | Free-text note field (optional) |
| Borrower | Phone contacts first; manual name entry as fallback if permission denied or contact doesn't exist |
| Return date | Required |
| Category / tag | Custom tags created by the user (no preset list) |
| Items per loan | One item per record (one photo, one borrower, one date) |

---

### The Feed

- **Default view:** Chronological list of all active (unreturned) loans
- **Overdue loans:** Pinned to the top of the feed with a red badge — can't be missed
- **View toggle:** Switch between two layouts:
  - **By Item** — flat list, one card per loan
  - **By Person** — collapsible sections grouped under each borrower
- **Filtering:** Filter chips (no full text search) — e.g. filter by overdue, by tag, by person
- **No search bar** for MVP

---

### Returned Loans

- Marking a loan as returned moves it to a dedicated **History / Archive tab**
- History is always viewable; never permanently deleted
- Returned loans do **not** count toward the active loan limit

---

### Notifications

| Trigger | Timing |
|---|---|
| Pre-warning | 3 days before the return date |
| Overdue alert | When the return date passes |

Both are **local push notifications** — no server required.

---

### Nudge (WhatsApp Reminder)

- Each active loan card has a **nudge button**
- Tapping it opens **WhatsApp** with a pre-filled message to the borrower
- Example message: _"Hey! Just a reminder — you still have my [item name]. Would love to get it back soon 😊"_
- Falls back gracefully if WhatsApp is not installed

---

### Monetisation

| Tier | Limit | Price |
|---|---|---|
| Free | 3 active loans at a time | Free |
| Unlimited | No limit on active loans | \$2.99 one-time unlock (Google Play Billing) |

- "Active" = unreturned loans only
- Returning a loan frees up a slot on the free tier
- History is always unlimited regardless of tier

---

## Screens (MVP)

1. **Feed / Home** — active loans, overdue pinned at top, view toggle, filter chips
2. **Add Loan** — photo (optional, nudged) → item name + note → borrower picker → return date → tags
3. **Loan Detail** — full view of a loan, nudge button, mark as returned, edit/delete
4. **History** — archive of all returned loans
5. **Paywall Sheet** — appears when user tries to add a 4th active loan on the free tier

---

## Design Language

Inherited from **Travel Pack Pal** design blueprint:

- **Colors:** Teal primary (`hsl(183 80% 38%)`) + warm cream/sand surfaces
- **Font:** DM Sans (body) · Cormorant Garamond (display headings)
- **Radius:** 12px base (`0.75rem`)
- **Icons:** Lucide (strokeWidth 1.75)
- **Overdue badge:** Red (`hsl(0 68% 50%)`)
- **Shadows:** 8-level scale from the blueprint
- Same card, button, badge, and empty-state patterns

---

## What's Deferred (Post-MVP)

- iCloud / Google Drive backup
- iOS version
- Lending to non-contacts (share link)
- Statistics screen (most borrowed-from person, average return time, etc.)
- Widget (home screen glanceable count of overdue loans)
- SMS fallback for the nudge button

---

## Tech Stack

TBD — to be decided before implementation begins.

Options under consideration:
- **Jetpack Compose** (native Kotlin) — best performance + full Android API access
- **React Native** — closer to the existing web design blueprint
- **Flutter** (Dart) — beautiful defaults, fast, cross-platform ready
