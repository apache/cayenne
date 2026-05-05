---
paths:
  - "modeler/**"
description: CayenneModeler UI components architecture
---

CayenneModeler is a Swing application for visually managing Cayenne OR mapping files.

## CayenneModeler Architecture - Hierarchical Components

Conventions:

1. **Single class per screen** — view and logic merged. The Swing component IS the controller.
2. **Hierarchical** — components compose other components; no separate controller tree.
3. **Bases** (in `org.apache.cayenne.modeler.ui`): extend `AppFrame` for top-level `JFrame` windows, `AppDialog` for modal/modeless dialogs, `AppPanel` for embedded panels that need `Application` access. Plain `JPanel` is fine for self-contained widget panels with no app/session dependency. All three bases hold the `Application` reference and expose it via `app()`. Components that need `ProjectSession` in addition to the Application, have their own `Project*` bases.
4. **Dependencies via constructor** — `Application` (and later `ProjectSession`) are passed in, exposed via `app()` / `session()` accessors. Do not use singletons or parent-walk-on-Swing-tree.
5. **`private final` fields** for every widget, except those reassigned during the component's life (e.g. card-layout content swaps).
6. **Layout vs. bindings vs. reactions** are separate methods on the component:
   - `initLayout()` — pure visual structure, no listeners.
   - `initBindings()` — listener wiring. Lambdas for short cases; **named private inner classes** for non-trivial listener bundles or grouped `Action`s (see `FindDialog.JumpToResultListener`).
   - `onSessionAttached(session)` (when introduced) — subscribe to project events; reactions are methods on the component itself.
7. **If a component grows past ~500 LOC, split it into smaller hierarchical sub-components** — do not split into view + controller.
