---
name: cayenne-modeler
description: "Use this skill when the user explicitly wants to open CayenneModeler (the GUI) on a Cayenne project, or when the modeling task is inherently visual — reverse engineering (delegated to cayenne-reverse-engineer), bulk relationship layout, multi-entity visual refactoring. Trigger on phrases like 'open the Modeler', 'open in CayenneModeler', 'launch the GUI', 'edit visually', 'show me the project in the Modeler'. Do NOT trigger as a fallback for ordinary a-la-carte XML edits — those belong in the cayenne-modeling skill, which is faster and doesn't require the user to context-switch."
---

# cayenne-modeler

Launch CayenneModeler with a Cayenne project file pre-loaded, via the `mcp__cayenne__open_project` MCP tool.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/mcp-tools.md` — `open_project` tool reference and failure modes.
- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate the project descriptor.

## Step 1 — Resolve the project path

The MCP tool needs an **absolute path** to a top-level project descriptor (`cayenne-*.xml`). Use `project-layout.md` to find it. If multiple descriptors exist, ask which one. Cache for the rest of the session.

If no descriptor exists yet, the user is starting from scratch. Two options:

1. Create a minimal descriptor first (use `cayenne-modeling` to scaffold it), then open.
2. Open the Modeler on *any* path it'll accept and use **File → New Project** inside.

For starting fresh, option 2 is simpler — but `open_project` does require an existing readable file.

## Step 2 — Call `open_project`

```
mcp__cayenne__open_project({ "projectPath": "<absolute path to cayenne-*.xml>" })
```

If the tool is not available (MCP server not registered), surface `cayenne-mcp-server/README.md` and stop. Do not attempt to launch CayenneModeler by any other means — the bundled launcher relies on the MCP server's discovery of the Modeler installation.

The tool spawns the Modeler asynchronously, then waits up to ~15 seconds for a startup handshake. Possible return codes:

| Code | Meaning |
|---|---|
| `ok` | Modeler launched and loaded the project. |
| `modeler_not_found` | The MCP jar is not co-located with a Modeler installation. User needs to install CayenneModeler properly. |
| `project_not_found` | The `projectPath` doesn't exist or isn't readable. |
| `handshake_timeout` | The Modeler started but didn't confirm load within the timeout. Often this means it's still loading — the user can check the GUI directly. |
| `launch_failed` | Process spawn failed. Surface the error message. |

## Step 3 — Hand off to the user

Once `open_project` returns `ok`, the user is in the GUI. From here, depending on intent:

- **Reverse engineering**: that's the `cayenne-reverse-engineer` skill's job to walk them through. Do not duplicate that workflow here — just open and step out.
- **Visual layout / bulk editing**: tell the user what tab to navigate to (e.g., DataMap → ObjEntity for entity-level edits, DataMap → Class Generation for cgen config) and let them work. Don't try to script GUI actions.
- **Just wanted to see the project**: nothing more to do.

## When NOT to use this skill

- **A-la-carte XML edits.** Adding one entity, tweaking one attribute, renaming a relationship — all faster as direct XML edits via `cayenne-modeling`. Don't context-switch the user to the GUI for trivial work.
- **Class generation.** That's `cayenne-cgen`'s job, fully scripted via MCP. No GUI needed.

## Anti-patterns

- **Do not** trigger as a generic fallback for "Cayenne tasks." This skill is for explicit user requests or inherently visual workflows. The `cayenne-modeling` skill handles most modeling intent.
- **Do not** retry `open_project` on `handshake_timeout` — the Modeler may still be loading. Tell the user to check the GUI window directly.
- **Do not** attempt to launch CayenneModeler outside the MCP tool (no `java -jar` direct calls, no opening a `.dmg`, no `open -a CayenneModeler`). The MCP tool's launcher knows where the installation lives.
