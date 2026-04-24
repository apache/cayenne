---
paths:
  - "modeler/**"
description: CayenneModeler UI components architecture
---

CayenneModeler is a Swing application for visually managing Cayenne OR mapping files. 

## CayenneModeler Architecture

The architecture follows the MVP (Model-View-Presenter) pattern with own specifics:

1. MVP instead of MVC ("presenter", not "controller")
2. Not using interfaces for views (no goal to uncouple controllers from Swing yet)
3. Views take their presenters in constructor to perform action bindings to the view controls. This keeps presenter
   at least nominally Swing-agnostic, and in many cases removes the need for getters for the view parts.
4. Each Presenter creates its own View. Parent Presenter can access that View to add to its own view.
5. We are NOT yet using Presenter-level listeners for parent sake. We have a fixed hierarchy of Presenters, so this is an overkill.


