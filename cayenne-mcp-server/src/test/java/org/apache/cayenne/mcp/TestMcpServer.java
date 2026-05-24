package org.apache.cayenne.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Handles the lifecycle of a test in-process MCP server.
 */
public class TestMcpServer {

    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final Thread serverThread;

    public static TestMcpServer start() {
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

            return new TestMcpServer(clientOut, clientIn, serverThread);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start in-process MCP server", e);
        }
    }

    private TestMcpServer(OutputStream outputStream, InputStream inputStream, Thread serverThread) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.serverThread = serverThread;
    }

    /**
     * Stream the test writes JSON-RPC requests to (→ server stdin).
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Stream the test reads JSON-RPC responses from (← server stdout).
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Waits for the server thread to finish, returning {@code true} if it stops
     * within {@code timeout}. Closing {@link #getOutputStream()} signals EOF to
     * the server and causes it to shut down.
     */
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        serverThread.join(unit.toMillis(timeout));
        return !serverThread.isAlive();
    }
}
