/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.mcp.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Configures logging for the MCP server: routes everything to stderr so stdout
 * stays clean for the MCP stdio transport, and provides per-invocation warning capture
 * for tools that surface Cayenne log output in their response payloads.
 */
public class McpLoggingHandler {

    private static final String PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    public static void routeLogsToStderr() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(PATTERN);
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(context);
        appender.setName("STDERR");
        // "System.err" is the target name Logback recognises for stderr
        appender.setTarget("System.err");
        appender.setEncoder(encoder);
        appender.start();

        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        root.addAppender(appender);
    }

    /**
     * Attaches a transient in-memory appender to {@code loggerName} and returns a
     * {@code Supplier} that, when called, detaches it and returns all WARN-or-above
     * messages collected since this call. Intended for use in a {@code finally} block:
     * <pre>
     *   var stopCapture = StderrLogging.startCapture("org.apache.cayenne.gen");
     *   try { ... } finally { warnings = stopCapture.get(); }
     * </pre>
     * Returns an empty-list supplier if Logback is not the active SLF4J backend.
     */
    public static Supplier<List<String>> startCapture(String loggerName) {
        try {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = ctx.getLogger(loggerName);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            return () -> {
                ctx.getLogger(loggerName).detachAppender(appender);
                return appender.list.stream()
                        .filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN))
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(Collectors.toList());
            };
        } catch (ClassCastException ignored) {
            return List::of;
        }
    }
}
