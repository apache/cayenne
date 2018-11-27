/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.util;

import org.apache.cayenne.modeler.dialog.LogConsole;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import javax.swing.text.AttributeSet;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ModelerLogger is a Logger implementation, which performs output
 * to the LogConsole.
 */
public class ModelerLogger implements Logger {

    private static final int BUFFER_SIZE = 32;
    private static final byte LOG_LEVEL_INFO = 2;
    private static final byte LOG_LEVEL_DEBUG = 1;
    private static final byte LOG_LEVEL_TRACE = 0;
    private static final byte LOG_LEVEL_WARNING = 3;
    private static final byte LOG_LEVEL_ERROR = 4;

    static final String INFO_LOG_NAME = "INFO";
    private static final String DEBUG_LOG_NAME = "DEBUG";
    private static final String TRACE_LOG_NAME = "TRACE";
    private static final String WARNING_LOG_NAME = "WARNING";
    private static final String ERROR_LOG_NAME = "ERROR";
    private static final String DATE_FORMAT = "yyyy/MM/dd HH.mm.ss";
    
    /** 
     * Logger name 
     */
    private String name;
    private int currentLogLevel = LOG_LEVEL_INFO;

    public ModelerLogger(String name) {
        this.name = name;
    }

    private String getLogLevel(byte level) {
        switch (level) {
            case LOG_LEVEL_INFO:
                return INFO_LOG_NAME;

            case LOG_LEVEL_DEBUG:
                return DEBUG_LOG_NAME;

            case LOG_LEVEL_TRACE:
                return TRACE_LOG_NAME;

            case LOG_LEVEL_WARNING:
                return WARNING_LOG_NAME;

            case LOG_LEVEL_ERROR:
                return ERROR_LOG_NAME;

            default:
                throw new IllegalStateException("Unregistered log level - " + level);

        }
    }

    private void consoleLog(byte level, String message, Throwable throwable) {
        if(this.isLevelEnabled(level)) {
            StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
            buffer.append(this.getFormattedDate());
            buffer.append(' ');

            buffer.append('[');
            buffer.append(Thread.currentThread().getName());
            buffer.append("] ");

            buffer.append('[');
            String levelStr = this.getLogLevel(level);
            buffer.append(levelStr);
            buffer.append(']');

            buffer.append(' ');
            buffer.append(message);
            this.write(buffer, throwable);
        }
    }
    
    private void consoleLog(byte level, String message) {
        consoleLog(level, message, (Throwable) null);
    }

    private void consoleLog(byte level, String format, Object... arguments) {
        if(this.isLevelEnabled(level)) {
            FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
            this.consoleLog(level, tuple.getMessage(), tuple.getThrowable());
        }
    }

    private String getFormattedDate() {
        Date currentDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String dateText = formatter.format(currentDate);
        return dateText;
    }

    private void write(StringBuilder buffer, Throwable throwable) {
        PrintStream targetStream = System.err;
        targetStream.println(buffer.toString());
        this.writeThrowable(throwable, targetStream);
        targetStream.flush();
    }

    private void writeThrowable(Throwable throwable, PrintStream targetStream) {
        if(throwable != null) {
            throwable.printStackTrace(targetStream);
        }

    }

    private boolean isLevelEnabled(int logLevel) {
        return (logLevel >= this.currentLogLevel);
    }

    @Override
    public void debug(String message) {
        consoleLog(LOG_LEVEL_DEBUG, message);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void debug(String message, Object object) {
        consoleLog(LOG_LEVEL_DEBUG, message, object);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE, object);
    }

    @Override
    public void debug(String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_DEBUG, message, object, secondObject);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE, object, secondObject);
    }

    @Override
    public void debug(String message, Object... objects) {
        consoleLog(LOG_LEVEL_DEBUG, message, objects);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_DEBUG, message, throwable);
        log("DEBUG", message, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    @Override
    public void debug(Marker marker, String message) {
        consoleLog(LOG_LEVEL_DEBUG, message);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void debug(Marker marker, String message, Object object) {
        consoleLog(LOG_LEVEL_DEBUG, message, object);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE, object);
    }

    @Override
    public void debug(Marker marker, String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_DEBUG, message, object, secondObject);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE, object, secondObject);
    }

    @Override
    public void debug(Marker marker, String message, Object... objects) {
        consoleLog(LOG_LEVEL_DEBUG, message, objects);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_DEBUG, message, throwable);
        log("DEBUG", message, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    @Override
    public void error(String message) {
        consoleLog(LOG_LEVEL_ERROR, message);
        log("ERROR", message, null, LogConsole.ERROR_STYLE);
    }

    @Override
    public void error(String message, Object object) {
        consoleLog(LOG_LEVEL_ERROR, message, object);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object);
    }

    @Override
    public void error(String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_ERROR, message, object, secondObject);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object, secondObject);
    }

    @Override
    public void error(String message, Object... objects) {
        consoleLog(LOG_LEVEL_ERROR, message, objects);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, objects);
    }

    @Override
    public void error(String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_ERROR, message, throwable);
        log("ERROR", message, throwable, LogConsole.ERROR_STYLE);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    @Override
    public void error(Marker marker, String message) {
        consoleLog(LOG_LEVEL_ERROR, message);
        log("ERROR", message, null, LogConsole.ERROR_STYLE);
    }

    @Override
    public void error(Marker marker, String message, Object object) {
        consoleLog(LOG_LEVEL_ERROR, message, object);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object);
    }

    @Override
    public void error(Marker marker, String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_ERROR, message, object, secondObject);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object, secondObject);
    }

    @Override
    public void error(Marker marker, String message, Object... objects) {
        consoleLog(LOG_LEVEL_ERROR, message, objects);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, objects);
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_ERROR, message, throwable);
        log("ERROR", message, throwable, LogConsole.ERROR_STYLE);
    }

    @Override
    public void info(String message) {
        consoleLog(LOG_LEVEL_INFO, message);
        log("INFO", message, null, LogConsole.INFO_STYLE);
    }

    @Override
    public void info(String message, Object object) {
        consoleLog(LOG_LEVEL_INFO, message, object);
        log("INFO", message, null, LogConsole.INFO_STYLE, object);
    }

    @Override
    public void info(String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_INFO, message, object, secondObject);
        log("INFO", message, null, LogConsole.INFO_STYLE, object, secondObject);
    }

    @Override
    public void info(String message, Object... objects) {
        consoleLog(LOG_LEVEL_INFO, message, objects);
        log("INFO", message, null, LogConsole.INFO_STYLE, objects);
    }

    @Override
    public void info(String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_INFO, message, throwable);
        log("INFO", message, throwable, LogConsole.INFO_STYLE);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    @Override
    public void info(Marker marker, String message) {
        consoleLog(LOG_LEVEL_INFO, message);
        log("INFO", message, null, LogConsole.INFO_STYLE);
    }

    @Override
    public void info(Marker marker, String message, Object object) {
        consoleLog(LOG_LEVEL_INFO, message, object);
        log("INFO", message, null, LogConsole.INFO_STYLE, object);
    }

    @Override
    public void info(Marker marker, String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_INFO, message, object, secondObject);
        log("INFO", message, null, LogConsole.INFO_STYLE, object, secondObject);
    }

    @Override
    public void info(Marker marker, String message, Object... objects) {
        consoleLog(LOG_LEVEL_INFO, message, objects);
        log("INFO", message, null, LogConsole.INFO_STYLE, objects);
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_INFO, message, throwable);
        log("INFO", message, throwable, LogConsole.INFO_STYLE);
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARNING);
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
    public void trace(String message) {
        consoleLog(LOG_LEVEL_TRACE, message);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void trace(String message, Object object) {
        consoleLog(LOG_LEVEL_TRACE, message, object);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object);
    }

    @Override
    public void trace(String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_TRACE, message, secondObject);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object, secondObject);
    }

    @Override
    public void trace(String message, Object... objects) {
        consoleLog(LOG_LEVEL_TRACE, message, objects);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_TRACE, message, throwable);
        log("TRACE", message, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    @Override
    public void trace(Marker marker, String message) {
        consoleLog(LOG_LEVEL_TRACE, message);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void trace(Marker marker, String message, Object object) {
        consoleLog(LOG_LEVEL_TRACE, message, object);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object);
    }

    @Override
    public void trace(Marker marker, String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_TRACE, message, object, secondObject);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object, secondObject);
    }

    @Override
    public void trace(Marker marker, String message, Object... objects) {
        consoleLog(LOG_LEVEL_TRACE, message, objects);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_TRACE, message, throwable);
        log("TRACE", message, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isErrorEnabled();
    }

    @Override
    public void warn(String message) {
        consoleLog(LOG_LEVEL_WARNING, message);
        log("WARN", message, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void warn(String message, Object object) {
        consoleLog(LOG_LEVEL_WARNING, message, object);
        log("WARN", message, null, LogConsole.WARN_STYLE, object);
    }

    @Override
    public void warn(String message, Object... objects) {
        consoleLog(LOG_LEVEL_WARNING, message, objects);
        log("WARN", message, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void warn(String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_WARNING, message, secondObject);
        log("WARN", message, null, LogConsole.WARN_STYLE, object, secondObject);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_WARNING, message, throwable);
        log("WARN", message, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isLevelEnabled(LOG_LEVEL_WARNING);
    }

    @Override
    public void warn(Marker marker, String message) {
        consoleLog(LOG_LEVEL_WARNING, message);
        log("WARN", message, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void warn(Marker marker, String message, Object object) {
        consoleLog(LOG_LEVEL_WARNING, message, object);
        log("WARN", message, null, LogConsole.WARN_STYLE, object);
    }

    @Override
    public void warn(Marker marker, String message, Object object, Object secondObject) {
        consoleLog(LOG_LEVEL_WARNING, message, object, secondObject);
        log("WARN", message, null, LogConsole.WARN_STYLE, object, secondObject);
    }

    @Override
    public void warn(Marker marker, String message, Object... objects) {
        consoleLog(LOG_LEVEL_WARNING, message, objects);
        log("WARN", message, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        consoleLog(LOG_LEVEL_WARNING, message, throwable);
        log("WARN", message, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    /**
     * Prints common message to the modeler console
     */

    void log(String level, String message, Throwable throwable, AttributeSet style, Object... parameters) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, parameters);
        getLogConsole().appendMessage(level, tuple.getMessage(), throwable, style);
    }

    void log(String level, Object message, Throwable throwable, AttributeSet style) {
        getLogConsole().appendMessage(level, String.valueOf(message), throwable, style);
    }

    private LogConsole getLogConsole() {
        return LogConsole.getInstance();
    }
}
