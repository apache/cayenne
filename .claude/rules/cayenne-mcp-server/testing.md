---
paths:
  - "cayenne-mcp-server/**"
description: cayenne-mcp-server testing conventions
---

## Integration Test Conventions

### `TestMcpClient` — shared test extension

`TestMcpClient` is a JUnit 5 extension (`BeforeEachCallback` + `AfterEachCallback`). Declare it as
an instance field annotated with `@RegisterExtension`; JUnit starts the server before each test
and shuts it down after:

```java
@RegisterExtension
TestMcpClient client = new TestMcpClient();
```

Key methods:

| Method                                            | Purpose |
|---------------------------------------------------|---|
| `client.send(json)`                               | Send a raw JSON-RPC message |
| `client.sendToolCall(id, tool, project, dataMap)` | Send a `tools/call` request for tools that take `projectPath` + `dataMap` args |
| `client.readLine(ms)`                             | Read next non-blank response line, fail after timeout |
| `client.readLineAsJson(ms)`                       | Read next response line and return pretty-printed tool-result JSON |

### Full JSON comparisons

Prefer comparing the entire pretty-printed JSON payload as a string rather than asserting
individual fields. Variable parts (file paths, JDBC URLs) are formatted in with `%s`; everything
else is written literally. This makes assertions self-documenting and catches unexpected field
additions or serialization regressions that field-by-field checks miss:

```java
assertEquals("""
        {
          "status" : "up_to_date",
          ...
        }""".formatted(varPath, varUrl, DRIVER, ADAPTER),
        client.readLineAsJson(15_000));
```
