# Design System Specification: The Academic Sanctuary

## 1. Overview & Creative North Star
The "Creative North Star" for this design system is **The Digital Curator**. Unlike standard SaaS platforms that prioritize loud, aggressive call-to-actions and flat "startup" aesthetics, this system is built on the philosophy of an **Academic Sanctuary**. 

The goal is to create a space that feels quiet, premium, and focused. We move away from generic layouts by utilizing intentional asymmetry, expansive negative space, and a "depth-first" architecture. Every element should feel like a curated artifact resting within a high-end physical library—intentional, weighty, and sophisticated.

---

## 2. Color & Tonal Depth
We reject the "flat" web. Our palette is built on the deep, nocturnal foundations of the night, accented by the sharp clarity of glacial light.

### The Palette
- **Base Surface (Nocturnal):** `#111417` (Surface) — The void in which all content exists.
- **Primary Accent (Electric Sapphire):** `#40A9FF` (Primary Container) — Used for high-intensity focal points.
- **Highlight (Glacial):** `#D6EFFF` (Primary Fixed) — Used for the most delicate interactive elements.

### The "No-Line" Rule
Standard 1px solid borders are strictly prohibited for sectioning. Boundaries must be defined through:
1.  **Background Shifts:** Use `surface-container-low` (#191C1F) to define a section against the `surface` (#111417).
2.  **Tonal Transitions:** A container sitting on a background should be distinguished by its elevation in the stack, not a stroke.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. Use the following hierarchy to nest content:
- **Level 0 (Background):** `surface` (#111417)
- **Level 1 (Subtle Inset):** `surface-container-lowest` (#0C0E12)
- **Level 2 (Standard Card):** `surface-container` (#1D2023)
- **Level 3 (Floating/Active):** `surface-bright` (#37393D)

### Glassmorphism & Textures
To achieve the "Sanctuary" feel, use **Glassmorphism** for floating overlays and sidebars. 
- **Effect:** `backdrop-filter: blur(20px); background: rgba(29, 32, 35, 0.6);`
- **Ghost Border:** A `1px` border using `white` at `10%` opacity is the only permitted structural line, creating a "shimmer" on the edge of glass elements.

---

## 3. Typography: The Academic Voice
We utilize **Manrope** not as a utility font, but as an editorial statement.

- **Tracking:** Apply a global `-0.02em` tracking to all weights to create a "dense," premium feel.
- **The Contrast Rule:** Avoid "Regular" (400) weight where possible. Use **Medium (500)** for body copy to ensure it feels authoritative, and **Bold (700)** sparingly for headers.
- **Display Scales:** 
    - `display-lg` (3.5rem): Use for quiet, impactful landing moments.
    - `headline-sm` (1.5rem): The workhorse for section titles, always set in Medium weight.
- **Labels:** `label-sm` (0.6875rem) should always be uppercase with `+0.05em` letter spacing to mimic archival indexing.

---

## 4. Elevation & Depth: The Layering Principle
Depth in this design system is achieved through **Ambient Light** and **Tonal Stacking** rather than traditional drop shadows.

- **Layering:** Place a `surface-container-lowest` card on a `surface-container-low` section to create an "etched" or "recessed" look.
- **The Sapphire Glow:** When a floating element (like a modal) is required, use a shadow with a 40px blur, 4% opacity, tinted with the Primary Sapphire (`#40A9FF`). This creates an "inner light" effect rather than a dark shadow.
- **Inner Glows:** Interactive elements (Primary Buttons) must feature a subtle `0.5px` inner-stroke or inner-shadow to give them a 3D, tactile quality, as if they are backlit glass.

---

## 5. Components

### Primary Buttons (The Sapphire Beacon)
- **Style:** Background `#40A9FF`.
- **Identity Detail:** A subtle inner-glow (`box-shadow: inset 0 1px 2px rgba(255,255,255,0.3)`) to provide a 3D "glowing" quality.
- **Rounding:** `md` (0.375rem).

### Segmented Progress (The Identity Element)
- **Rule:** Never use smooth, continuous progress bars.
- **Style:** Progress must be broken into distinct segments (e.g., 5px wide blocks) using `spacing-0.5` between them.
- **Color:** Active segments use `Electric Sapphire`, inactive segments use `surface-variant`.

### Cards & Lists
- **Rule:** No divider lines. 
- **Spacing:** Use `spacing-6` (2rem) of vertical white space to separate items.
- **Selection State:** Use a subtle background shift to `surface-container-high` and a Glacial Blue left-edge accent (2px wide).

### Input Fields
- **Style:** Understated. Background `surface-container-low`, no border.
- **Focus State:** 1px "Ghost Border" (10% White) and a soft Sapphire outer glow.

---

## 6. Do's and Don'ts

### Do
- **Use Asymmetry:** Place high-contrast typography off-center to create an editorial, "curated" layout.
- **Embrace the Dark:** Allow the `Nocturnal` base to occupy at least 60% of the screen to maintain the "Sanctuary" feel.
- **Leverage the Spacing Scale:** Use `spacing-16` (5.5rem) for major section breaks to let content breathe.

### Don't
- **Don't use 100% Black:** Pure black (#000000) kills the "Sapphire" depth; always stick to the `Nocturnal` base.
- **Don't use Rounded-Full for buttons:** Avoid the "pill" shape; it feels too playful. Use `md` (0.375rem) for a more architectural look.
- **Don't use standard Shadows:** Avoid the "Grey Drop Shadow." If you need lift, use the Sapphire-tinted ambient glow or a Ghost Border.
- **Don't crowd the UI:** If a screen feels busy, increase the spacing values by one tier rather than adding dividers.

---

## 7. Spacing & Grid Philosophy
Break the rigid 12-column grid. Use **Optical Alignment**. Small labels (`label-md`) should be tucked into the margins, while large `display` type should overlap container boundaries slightly to create a sense of layered physical sheets.

- **Standard Gutter:** `spacing-4` (1.4rem).
- **Component Padding:** `spacing-3` (1rem) for internal card padding to keep elements tight and "Academic."