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
package org.apache.cayenne.modeler.log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ModelerLogger is a Logger implementation, which performs output to the LogConsole.
 */
// TODO: log channels and log level checking is all pver the place. Requires cleanup
public class ModelerLogger implements Logger {

    private static final byte LOG_LEVEL_TRACE = 0;
    private static final byte LOG_LEVEL_DEBUG = 1;
    private static final byte LOG_LEVEL_INFO = 2;
    private static final byte LOG_LEVEL_WARN = 3;
    private static final byte LOG_LEVEL_ERROR = 4;

    static final String INFO_LOG_NAME = "INFO";
    private static final String DEBUG_LOG_NAME = "DEBUG";
    private static final String TRACE_LOG_NAME = "TRACE";
    private static final String WARN_LOG_NAME = "WARN";
    private static final String ERROR_LOG_NAME = "ERROR";

    // fixed width: 4 fractional digits = tenth-of-millisecond precision
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS");

    private final String name;
    private final int currentLogLevel;

    public ModelerLogger(String name) {
        this.name = name;
        this.currentLogLevel = LOG_LEVEL_DEBUG;
    }

    private String getLevelName(byte level) {
        switch (level) {
            case LOG_LEVEL_INFO:  return INFO_LOG_NAME;
            case LOG_LEVEL_DEBUG: return DEBUG_LOG_NAME;
            case LOG_LEVEL_TRACE: return TRACE_LOG_NAME;
            case LOG_LEVEL_WARN:  return WARN_LOG_NAME;
            case LOG_LEVEL_ERROR: return ERROR_LOG_NAME;
            default: throw new IllegalStateException("Unregistered log level - " + level);
        }
    }

    private boolean isLevelEnabled(int logLevel) {
        return logLevel >= this.currentLogLevel;
    }

    private void output(byte level, String format, Object... arguments) {
        if (!isLevelEnabled(level)) {
            return;
        }
        FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
        String levelName = getLevelName(level);
        String formatted = formatLine(levelName, tuple.getMessage(), tuple.getThrowable());
        ModelerLogFactory.getLogAppender().appendMessage(levelName, formatted);
    }

    private static String formatLine(String level, String message, Throwable throwable) {
        // padded to 6 to fit the longest level name plus colon ("DEBUG", "ERROR", "TRACE")
        StringBuilder text = new StringBuilder()
                .append(TIMESTAMP.format(LocalDateTime.now()))
                .append(' ').append(String.format("%-5s ", level))
                .append(Thread.currentThread().getName()).append(" ");
        if (message != null) {
            text.append(message);
        }
        text.append(System.lineSeparator());
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.flush();
            text.append(sw).append(System.lineSeparator());
        }
        return text.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    @Override
    public void trace(String message) {
        output(LOG_LEVEL_TRACE, message);
    }

    @Override
    public void trace(String message, Object object) {
        output(LOG_LEVEL_TRACE, message, object);
    }

    @Override
    public void trace(String message, Object object, Object secondObject) {
        output(LOG_LEVEL_TRACE, message, object, secondObject);
    }

    @Override
    public void trace(String message, Object... objects) {
        output(LOG_LEVEL_TRACE, message, objects);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        output(LOG_LEVEL_TRACE, message, throwable);
    }

    @Override
    public void trace(Marker marker, String message) {
        output(LOG_LEVEL_TRACE, message);
    }

    @Override
    public void trace(Marker marker, String message, Object object) {
        output(LOG_LEVEL_TRACE, message, object);
    }

    @Override
    public void trace(Marker marker, String message, Object object, Object secondObject) {
        output(LOG_LEVEL_TRACE, message, object, secondObject);
    }

    @Override
    public void trace(Marker marker, String message, Object... objects) {
        output(LOG_LEVEL_TRACE, message, objects);
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        output(LOG_LEVEL_TRACE, message, throwable);
    }

    @Override
    public void debug(String message) {
        output(LOG_LEVEL_DEBUG, message);
    }

    @Override
    public void debug(String message, Object object) {
        output(LOG_LEVEL_DEBUG, message, object);
    }

    @Override
    public void debug(String message, Object object, Object secondObject) {
        output(LOG_LEVEL_DEBUG, message, object, secondObject);
    }

    @Override
    public void debug(String message, Object... objects) {
        output(LOG_LEVEL_DEBUG, message, objects);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        output(LOG_LEVEL_DEBUG, message, throwable);
    }

    @Override
    public void debug(Marker marker, String message) {
        output(LOG_LEVEL_DEBUG, message);
    }

    @Override
    public void debug(Marker marker, String message, Object object) {
        output(LOG_LEVEL_DEBUG, message, object);
    }

    @Override
    public void debug(Marker marker, String message, Object object, Object secondObject) {
        output(LOG_LEVEL_DEBUG, message, object, secondObject);
    }

    @Override
    public void debug(Marker marker, String message, Object... objects) {
        output(LOG_LEVEL_DEBUG, message, objects);
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        output(LOG_LEVEL_DEBUG, message, throwable);
    }

    @Override
    public void info(String message) {
        output(LOG_LEVEL_INFO, message);
    }

    @Override
    public void info(String message, Object object) {
        output(LOG_LEVEL_INFO, message, object);
    }

    @Override
    public void info(String message, Object object, Object secondObject) {
        output(LOG_LEVEL_INFO, message, object, secondObject);
    }

    @Override
    public void info(String message, Object... objects) {
        output(LOG_LEVEL_INFO, message, objects);
    }

    @Override
    public void info(String message, Throwable throwable) {
        output(LOG_LEVEL_INFO, message, throwable);
    }

    @Override
    public void info(Marker marker, String message) {
        output(LOG_LEVEL_INFO, message);
    }

    @Override
    public void info(Marker marker, String message, Object object) {
        output(LOG_LEVEL_INFO, message, object);
    }

    @Override
    public void info(Marker marker, String message, Object object, Object secondObject) {
        output(LOG_LEVEL_INFO, message, object, secondObject);
    }

    @Override
    public void info(Marker marker, String message, Object... objects) {
        output(LOG_LEVEL_INFO, message, objects);
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        output(LOG_LEVEL_INFO, message, throwable);
    }

    @Override
    public void warn(String message) {
        output(LOG_LEVEL_WARN, message);
    }

    @Override
    public void warn(String message, Object object) {
        output(LOG_LEVEL_WARN, message, object);
    }

    @Override
    public void warn(String message, Object... objects) {
        output(LOG_LEVEL_WARN, message, objects);
    }

    @Override
    public void warn(String message, Object object, Object secondObject) {
        output(LOG_LEVEL_WARN, message, object, secondObject);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        output(LOG_LEVEL_WARN, message, throwable);
    }

    @Override
    public void warn(Marker marker, String message) {
        output(LOG_LEVEL_WARN, message);
    }

    @Override
    public void warn(Marker marker, String message, Object object) {
        output(LOG_LEVEL_WARN, message, object);
    }

    @Override
    public void warn(Marker marker, String message, Object object, Object secondObject) {
        output(LOG_LEVEL_WARN, message, object, secondObject);
    }

    @Override
    public void warn(Marker marker, String message, Object... objects) {
        output(LOG_LEVEL_WARN, message, objects);
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        output(LOG_LEVEL_WARN, message, throwable);
    }

    @Override
    public void error(String message) {
        output(LOG_LEVEL_ERROR, message);
    }

    @Override
    public void error(String message, Object object) {
        output(LOG_LEVEL_ERROR, message, object);
    }

    @Override
    public void error(String message, Object object, Object secondObject) {
        output(LOG_LEVEL_ERROR, message, object, secondObject);
    }

    @Override
    public void error(String message, Object... objects) {
        output(LOG_LEVEL_ERROR, message, objects);
    }

    @Override
    public void error(String message, Throwable throwable) {
        output(LOG_LEVEL_ERROR, message, throwable);
    }

    @Override
    public void error(Marker marker, String message) {
        output(LOG_LEVEL_ERROR, message);
    }

    @Override
    public void error(Marker marker, String message, Object object) {
        output(LOG_LEVEL_ERROR, message, object);
    }

    @Override
    public void error(Marker marker, String message, Object object, Object secondObject) {
        output(LOG_LEVEL_ERROR, message, object, secondObject);
    }

    @Override
    public void error(Marker marker, String message, Object... objects) {
        output(LOG_LEVEL_ERROR, message, objects);
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        output(LOG_LEVEL_ERROR, message, throwable);
    }
}
