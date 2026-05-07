# Cue Working Boundary

## Purpose
This document defines the working boundary between product authorship and technical authorship in Cue so that future sprint planning and implementation stay consistent.

The goal is simple:
- product intent stays coherent
- architecture stays disciplined
- UX language does not leak into core business logic

---

## Ownership Split

### Product Ownership
The product side owns:
- UX philosophy
- product framing
- emotional tone
- wording shown to users
- narrative concepts such as "Quiet Mirror", "Contextual Whisper", and "Stoic Observer"
- how the experience should feel

Primary reference:
- [product.md](/abs/path/docs/Product/product.md)

### Technical Ownership
The technical side owns:
- architecture
- scalability
- layering
- modular boundaries
- data and domain modeling
- repository and use case contracts
- testability
- maintainability
- implementation sequencing
- technical maturity across sprints

---

## Core Rule

Product language is a constraint on presentation, not a driver of domain structure.

This means:
- product concepts may shape UI behavior and copy
- product concepts must not directly determine repository design
- emotional tone must not be embedded inside rule logic
- user-facing phrasing should live near presentation or mapping layers
- domain and data layers should remain explicit, deterministic, and explainable

---

## Layering Policy

### Presentation Layer
Owns:
- screen composition
- UI state
- display models
- user-facing copy
- product framing translation

This is where terms like:
- "Clarity"
- "Pattern Strength"
- "On days like today..."

should be mapped for display.

### Domain Layer
Owns:
- business rules
- pattern aggregation logic
- ranking logic
- correlation logic
- deterministic interpretation rules

This layer should speak in technical terms such as:
- confidence
- recurrence
- frequency
- time bucket
- signal match

The domain layer should not carry emotionally framed product copy.

### Data Layer
Owns:
- persistence
- query strategy
- entity mapping
- repository implementations
- storage efficiency

The data layer should optimize for correctness and future extensibility, not UX naming.

---

## Decision Rules

When making technical decisions:

1. Prefer explicit models over overloaded strings.
2. Prefer aggregation layers over packing more logic into UI code.
3. Prefer evolving contracts over one-off screen-specific shortcuts.
4. Keep detection, interpretation, and rendering separate.
5. Treat history as a first-class system input once patterns depend on time.
6. Add repository methods only when they simplify the domain meaningfully.
7. Keep rule logic explainable and testable.

---

## Practical Translation Rule

If a concept is about:
- how it is detected, ranked, stored, grouped, or tested: technical ownership
- how it is described, framed, softened, or emotionally positioned: product ownership

Examples:
- "confidenceScore" is technical
- "Clarity" is product-facing presentation language
- "high phone usage correlates with delayed starts" is technical logic
- "This pattern is becoming clear" is product copy

---

## Expected Architecture Direction

As Cue matures, the system should evolve toward:
- stable domain contracts
- thin presentation mapping layers
- use cases that compose rather than bloat
- repositories that support history-aware queries cleanly
- test coverage focused on interpretation boundaries
- sprint plans that separate product intent from implementation mechanics

This is especially important from V5 onward, where the app starts interpreting patterns rather than just logging events.

---

## Sprint Planning Rule

Future sprint plans should be split mentally into two tracks:

### Product Track
- philosophy
- framing
- emotional tone
- experience goals

### Technical Track
- system responsibilities
- models
- query needs
- use case boundaries
- state management
- migration path
- scalability risks
- testing strategy

The technical plan should support the product track without rewriting it.

---

## Final Note

Cue should feel calm and reflective at the surface, while remaining structurally rigorous underneath.

That separation is intentional. It is what allows the product to mature without the architecture becoming vague, fragile, or overly coupled to wording decisions.

I want you to be the UX phylosophy, product framing and Emotional tone
of cue, everything else such as
architecture,scalability,layering,technical maturity i will handle it
on my own


i want you to handle the architecture,scalability,layering,technical
maturity of cue. everything else such as UX phylosophy, product framing
and Emotional tone i will handle it on my own. an example of this is in
docs/Product/product.md

