# Design Blueprint — Reusable Design System

Extracted from Travel Pack Pal. This is the visual language, component patterns, and code
conventions that LendLog inherits.

---

## Stack (Web / Reference)

| Concern | Choice |
|---|---|
| Framework | React + Vite + TypeScript |
| UI library | shadcn/ui (Radix primitives + Tailwind) |
| Styling | Tailwind CSS v3 |
| Icons | Lucide React |
| Fonts | DM Sans (body) · Cormorant Garamond (display) |
| Animations | tailwindcss-animate + custom keyframes |

---

## Color System

```css
/* Surfaces */
--background: 240 5% 96%;        /* warm off-white page bg */
--foreground: 24 18% 10%;        /* near-black warm text */
--card: 0 0% 100%;               /* pure white elevated surfaces */
--card-foreground: 24 18% 10%;

/* Primary — tropical teal */
--primary: 183 80% 38%;
--primary-foreground: 0 0% 100%;

/* Secondary — pale sand */
--secondary: 40 28% 92%;
--secondary-foreground: 24 18% 12%;

/* Neutrals */
--muted: 38 22% 93%;
--muted-foreground: 30 10% 44%;
--accent: 38 30% 91%;
--accent-foreground: 183 80% 28%;
--border: 34 20% 87%;
--input: 34 22% 89%;
--ring: 183 80% 38%;

/* Semantic */
--destructive: 0 68% 50%;
--destructive-foreground: 0 0% 100%;
--success: 155 50% 37%;
--success-foreground: 0 0% 100%;

/* Base radius */
--radius: 0.75rem;   /* 12px */

/* Brand extras */
--brand-teal: 183 80% 38%;
--brand-teal-deep: 183 85% 30%;
--brand-sand: 38 30% 92%;
--brand-cream: 38 42% 97%;
--brand-charcoal: 24 18% 10%;
--brand-coral: 16 90% 55%;
--brand-forest: 155 50% 37%;

/* Shadows */
--shadow-xs: 0 1px 2px hsl(24 18% 8% / 0.05);
--shadow-sm: 0 1px 3px hsl(24 18% 8% / 0.07), 0 1px 2px hsl(24 18% 8% / 0.04);
--shadow-md: 0 4px 14px -2px hsl(24 18% 8% / 0.09), 0 2px 4px -1px hsl(24 18% 8% / 0.05);
--shadow-lg: 0 12px 28px -6px hsl(24 18% 8% / 0.12), 0 4px 8px -2px hsl(24 18% 8% / 0.06);
--shadow-xl: 0 24px 48px -12px hsl(24 18% 8% / 0.16), 0 8px 16px -4px hsl(24 18% 8% / 0.07);
--shadow-glow: 0 0 0 1px hsl(var(--primary) / 0.2), 0 6px 20px -4px hsl(var(--primary) / 0.35);
--shadow-card: 0 1px 3px hsl(24 18% 8% / 0.06), 0 2px 8px hsl(24 18% 8% / 0.04);
--shadow-card-hover: 0 4px 16px hsl(24 18% 8% / 0.10), 0 8px 24px hsl(24 18% 8% / 0.07);

/* Gradients */
--gradient-primary: linear-gradient(135deg, hsl(183 80% 48%) 0%, hsl(183 85% 30%) 100%);
--gradient-hero:    radial-gradient(ellipse 90% 60% at 50% -5%, hsl(183 80% 90% / 0.9) 0%, hsl(16 90% 93% / 0.65) 58%, transparent 76%);
--gradient-card:    linear-gradient(180deg, hsl(0 0% 100%) 0%, hsl(38 22% 99%) 100%);
--gradient-sand:    linear-gradient(135deg, hsl(40 32% 95%) 0%, hsl(36 26% 90%) 100%);

/* Typography */
--font-display: 'Cormorant Garamond', Georgia, serif;
--font-body: 'DM Sans', -apple-system, sans-serif;
```

### Android Color equivalents

```kotlin
val TealPrimary     = Color(0xFF0E9AA7)  // hsl(183 80% 38%)
val TealDeep        = Color(0xFF076E7A)  // hsl(183 85% 30%)
val WarmBackground  = Color(0xFFF4F3F5)  // hsl(240 5% 96%)
val CardSurface     = Color(0xFFFFFFFF)
val SandSecondary   = Color(0xFFEDE9DF)  // hsl(40 28% 92%)
val MutedText       = Color(0xFF8A7F72)  // hsl(30 10% 44%)
val BorderColor     = Color(0xFFDDD4C4)  // hsl(34 20% 87%)
val OverdueRed      = Color(0xFFD32F2F)  // hsl(0 68% 50%)
val SuccessGreen    = Color(0xFF2D7A55)  // hsl(155 50% 37%)
val CharcoalText    = Color(0xFF1E1814)  // hsl(24 18% 10%)
```

---

## Typography

| Role | Family | Weights |
|---|---|---|
| Display / headings | Cormorant Garamond (serif) | 300, 400, 500, 600, 700 |
| Body / UI | DM Sans (sans-serif) | 300, 400, 500, 600, 700 |

### Scale

| Token | Size | Weight | Use |
|---|---|---|---|
| Display | `clamp(1.8rem, 5vw, 2.8rem)` | 500 | Section headings |
| H1 | `1.75rem` / `28sp` | 600 | Page title |
| H2 | `1.25rem` / `20sp` | 600 | Section title |
| H3 | `1rem` / `16sp` | 600 | Card title |
| Body | `1rem` / `16sp` | 400 | Default prose |
| Small | `0.875rem` / `14sp` | 400 | Secondary text |
| Caption | `0.6875rem` / `11sp` | 600 | Uppercase labels |

---

## Spacing & Layout

| Use | Value |
|---|---|
| Card padding | `16dp` to `24dp` |
| Section vertical padding | `48dp` to `80dp` |
| Grid gap | `12dp`, `16dp`, `24dp` |
| Mobile touch target | 48dp min height |
| Icon margin | `8dp` or `12dp` inline with text |

---

## Border Radius

| Use | Value |
|---|---|
| Minor elements | `8dp` |
| Input fields | `10dp` |
| Buttons, standard cards | `12dp` |
| Dialogs, large cards | `16dp` |
| Hero cards | `20dp` |
| Pill badges, circular buttons | `999dp` / `50%` |

Rule: **12dp for interactive elements, 16dp+ for containers, full for pills.**

---

## Component Patterns

### Buttons

- Primary CTA: teal gradient + glow shadow
- Destructive: red
- Ghost: transparent, hover reveals bg
- Pill: full radius
- Standard heights: `36dp` (sm) · `40dp` (default) · `44dp` (lg)

### Cards

- Resting: white bg, `12dp` radius, `shadow-card`
- On press: `shadow-card-hover` + `translateY(-1dp)`
- Loan card: teal left border (active) or red left border (overdue)

### Badges / Pills

```kotlin
// Overdue badge
Surface(color = OverdueRed.copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp)) {
    Text("Overdue", color = OverdueRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

// Active badge  
Surface(color = TealPrimary.copy(alpha = 0.10f), shape = RoundedCornerShape(999.dp)) {
    Text("Active", color = TealPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}
```

### Empty States

Center-aligned, always:
1. Icon in a `primary/10` rounded bubble (`12dp` radius, `48dp` size)
2. Heading (`H3`, semibold)
3. Short explanation in `muted` color
4. CTA button below

### Dialogs

- Shape: `16dp` radius
- Cancel → outline button, destructive action → red button
- Title: concise action. Body: concrete consequence.

---

## Animations

| Duration | Use |
|---|---|
| `150ms` | Very fast hover/press states |
| `200ms` | Standard interactive (color, shadow) |
| `300ms` | Moderate (position, opacity) |
| `500ms` | Noticeable (banners, reveals) |

- Always `ease-out` or `ease-in-out`
- Stagger list items: `70ms` delay per index
- Compose: use `AnimatedVisibility`, `animateContentSize`, `animateFloatAsState`

---

## Icons

- Library: Material Icons (Compose) + custom SVGs where needed
- Stroke equivalent: prefer `Outlined` style (lighter, more refined)
- Sizes: `16dp` (inline), `20dp` (actions), `24dp` (section), `32dp`+ (empty states)

---

## Visual Effects Rules

| Effect | When |
|---|---|
| Teal glow shadow | Primary CTA only — one per screen |
| Card lift on press | All tappable cards |
| Gradient hero blob | Landing / onboarding only |
| Stagger animation | First list render only, not on re-renders |
| Shimmer skeleton | Data loading states |
| Overdue red border | Overdue loan cards only |

---

## Design Decisions & Rationale

| Decision | Rationale |
|---|---|
| Teal `hsl(183 80% 38%)` as primary | Distinct from typical blue SaaS; pairs with warm cream |
| Warm off-white background | Avoids harsh pure white; reduces eye strain |
| `12dp` as base radius | Friendly without being cartoonish |
| DM Sans body + Cormorant Garamond display | High contrast between humanist sans and editorial serif |
| 8-level shadow scale | Enough granularity to express depth without arbitrary one-offs |
| FAB instead of bottom nav | Single primary action; avoids nav clutter for simple apps |
| Outlined icon style (`strokeWidth 1.75`) | Lighter and more refined than filled default |
| Stagger delay 70ms per item | Fast enough not to feel slow; legible as a sequence |
| Overdue pinned to top | Social awkwardness cost of forgetting is high — make it unavoidable |
