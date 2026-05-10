package org.apache.cayenne.mcp;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class McpStarter {

    public static McpHandle start() {
        try {
            PipedOutputStream clientOut = new PipedOutputStream();
            PipedInputStream serverIn = new PipedInputStream(clientOut);
            PipedOutputStream serverOut = new PipedOutputStream();
            PipedInputStream clientIn = new PipedInputStream(serverOut);

            Thread serverThread = new Thread(
                    () -> new CayenneMcpServer().run("test", serverIn, serverOut),
                    "mcp-server");
            serverThread.setDaemon(true);
            serverThread.start();

            return new McpHandle(clientOut, clientIn, serverThread);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start in-process MCP server", e);
        }
    }
}
