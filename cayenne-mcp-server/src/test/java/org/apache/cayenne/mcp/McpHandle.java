package org.apache.cayenne.mcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Handle to an in-process MCP server started by {@link McpStarter}.
 * Exposes the piped streams the test uses to send and receive JSON-RPC messages.
 */
public class McpHandle {

    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final Thread serverThread;

    McpHandle(OutputStream outputStream, InputStream inputStream, Thread serverThread) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.serverThread = serverThread;
    }

    /** Stream the test writes JSON-RPC requests to (→ server stdin). */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /** Stream the test reads JSON-RPC responses from (← server stdout). */
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
