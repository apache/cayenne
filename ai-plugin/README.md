<!--
	Licensed to the Apache Software Foundation (ASF) under one
	or more contributor license agreements.  See the NOTICE file
	distributed with this work for additional information
	regarding copyright ownership.  The ASF licenses this file
	to you under the Apache License, Version 2.0 (the
	"License"); you may not use this file except in compliance
	with the License.  You may obtain a copy of the License at
	
	https://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an
	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, either express or implied.  See the License for the
	specific language governing permissions and limitations
	under the License.   
-->
# apache-cayenne — Claude Code plugin

Apache Cayenne ORM workflows for Claude Code.

This plugin teaches Claude how to:

- Edit Cayenne DataMap (`*.map.xml`) and project descriptor (`cayenne-*.xml`) files a-la-carte (add entities, relationships, queries, embeddables).
- Reverse-engineer a database schema into a DataMap by driving CayenneModeler.
- Polish the reverse-engineered Object-layer names (entities, attributes, relationships) into idiomatic Java, fixing the few cases the deterministic name generator can't.
- Regenerate Java entity classes from a DataMap.
- Bootstrap `CayenneRuntime` in a Java application and write `ObjectSelect` / `SQLSelect` queries.

The plugin assumes downstream **users of Cayenne** writing their own Java apps. It does *not* cover contributor workflows for hacking on Cayenne itself.

## Install

**Cayenne 5.0+ only.** The MCP server ships with Cayenne 5.0. Skills that depend on it (`cayenne-cgen`, `cayenne-modeler`, `cayenne-db-import`) will not work against earlier Cayenne versions. The XML-editing, runtime, and query skills also target 5.0 idioms — for older Cayenne, this plugin is not the right tool.

The plugin is distributed from the Apache Cayenne GitHub repository: **https://github.com/apache/cayenne**. Inside Claude Code:

```
# register the "marketplace"
/plugin marketplace add apache/cayenne

# install the plugin
/plugin install apache-cayenne@apache-cayenne

# Install the MCP server (requires a local CayenneModeler install)
claude mcp add cayenne --scope user -- java -jar /path/to/CayenneMCPServer.jar
```

Full MCP setup instructions: https://cayenne.apache.org/docs/5.0/cayenne-guide/installing-the-cayenne-mcp-server/

That's it. The skills detect the MCP server at runtime; if it isn't connected they point back at this README instead of falling back to Maven or Gradle goals.

## What's in here

```
ai-plugin/
├── .claude-plugin/plugin.json   # manifest
├── README.md                    # this file
├── skills/                      # auto-triggering workflows
│   ├── cayenne-modeling/        # edit *.map.xml and cayenne-*.xml
│   ├── cayenne-db-import/       # import a DB schema (Modeler GUI)
│   ├── cayenne-model-naming/    # polish Obj-layer names after import / on request
│   ├── cayenne-cgen/            # regenerate Java classes via MCP
│   ├── cayenne-modeler/         # open the GUI on a project
│   ├── cayenne-runtime/         # bootstrap CayenneRuntime in an app
│   └── cayenne-query/           # write ObjectSelect / SQLSelect queries
└── references/                  # source-of-truth docs loaded by skills
    ├── project-layout.md
    ├── datamap-schema.md
    ├── project-descriptor-schema.md
    ├── dbimport-config.md
    ├── model-naming-conventions.md
    ├── model-naming-rename-safety.md
    ├── cgen-config.md
    ├── runtime-api.md
    ├── query-api.md
    └── mcp-tools.md
```

Each skill is a thin trigger that loads the right reference docs and walks Claude through the workflow.

ORM workflows go through:

1. Direct XML edits — primary path for a-la-carte model changes.
2. MCP `cgen_run` — class generation.
3. MCP `open_project` → CayenneModeler GUI — full DB sync and visual editing.

and will NOT use Maven or Gradle plugins
