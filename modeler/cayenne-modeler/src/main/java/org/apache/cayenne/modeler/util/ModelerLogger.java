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

import javax.swing.text.AttributeSet;

/**
 * ModelerLogger is a Log implementation, which performs output
 * to the LogConsole. Default behavior is saved since they are delegated
 * to default Log instance. 
 */
public class ModelerLogger implements Logger {
    /**
     * 'Default' Log instance (i.e. Log4jLogger, Jdk14Logger, or other)
     */
    Logger delegate;
    
    /** 
     * Logger name 
     */
    String name;
    
    public ModelerLogger(String name, Logger delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public void debug(String s) {
        delegate.debug(s);
        log("DEBUG", s, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void debug(String s, Object o) {
        delegate.debug(s, o);
        log("DEBUG", s, null, LogConsole.WARN_STYLE, o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        delegate.debug(s, o, o1);
        log("DEBUG", s, null, LogConsole.WARN_STYLE, o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        delegate.debug(s, objects);
        log("DEBUG", s, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        delegate.debug(s, throwable);
        log("DEBUG", s, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        delegate.debug(marker, s);
        log("DEBUG", s, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        delegate.debug(marker, s, o);
        log("DEBUG", s, null, LogConsole.WARN_STYLE, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        delegate.debug(marker, s, o, o1);
        log("DEBUG", s, null, LogConsole.WARN_STYLE, o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        delegate.debug(marker, s, objects);
        log("DEBUG", s, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        delegate.debug(marker, s, throwable);
        log("DEBUG", s, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void error(String s) {
        delegate.error(s);
        log("ERROR", s, null, LogConsole.ERROR_STYLE);
    }

    @Override
    public void error(String s, Object o) {
        delegate.error(s, o);
        log("ERROR", s, null, LogConsole.ERROR_STYLE, o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        delegate.error(s, o, o1);
        log("ERROR", s, null, LogConsole.ERROR_STYLE, o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        delegate.error(s, objects);
        log("ERROR", s, null, LogConsole.ERROR_STYLE, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        delegate.error(s, throwable);
        log("ERROR", s, throwable, LogConsole.ERROR_STYLE);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        delegate.error(marker, s);
        log("ERROR", s, null, LogConsole.ERROR_STYLE);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        delegate.error(marker, s , o);
        log("ERROR", s, null, LogConsole.ERROR_STYLE, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        delegate.error(marker, s , o, o1);
        log("ERROR", s, null, LogConsole.ERROR_STYLE, o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        delegate.error(marker, s, objects);
        log("ERROR", s, null, LogConsole.ERROR_STYLE, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        delegate.error(marker, s, throwable);
        log("ERROR", s, throwable, LogConsole.ERROR_STYLE);
    }

    @Override
    public void info(String s) {
        delegate.info(s);
        log("INFO", s, null, LogConsole.INFO_STYLE);
    }

    @Override
    public void info(String s, Object o) {
        delegate.info(s, o);
        log("INFO", s, null, LogConsole.INFO_STYLE, o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        delegate.info(s, o, o1);
        log("INFO", s, null, LogConsole.INFO_STYLE, o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        delegate.info(s, objects);
        log("INFO", s, null, LogConsole.INFO_STYLE, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        delegate.info(s, throwable);
        log("INFO", s, throwable, LogConsole.INFO_STYLE);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        delegate.info(marker, s);
        log("INFO", s, null, LogConsole.INFO_STYLE);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        delegate.info(marker, s, o);
        log("INFO", s, null, LogConsole.INFO_STYLE, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        delegate.info(marker, s, o, o1);
        log("INFO", s, null, LogConsole.INFO_STYLE, o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        delegate.info(marker, s, objects);
        log("INFO", s, null, LogConsole.INFO_STYLE, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        delegate.info(marker, s, throwable);
        log("INFO", s, throwable, LogConsole.INFO_STYLE);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        delegate.trace(s);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void trace(String s, Object o) {
        delegate.trace(s, o);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        delegate.trace(s, o, o1);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        delegate.trace(s, objects);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        delegate.trace(s, throwable);
        log("TRACE", s, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        delegate.trace(marker, s);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        delegate.trace(marker, s, o);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        delegate.trace(marker, s, o, o1);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        delegate.trace(marker, s, objects);
        log("TRACE", s, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        delegate.trace(marker, s, throwable);
        log("TRACE", s, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isErrorEnabled();
    }

    @Override
    public void warn(String s) {
        delegate.warn(s);
        log("WARN", s, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void warn(String s, Object o) {
        delegate.warn(s, o);
        log("WARN", s, null, LogConsole.WARN_STYLE, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        delegate.warn(s, objects);
        log("WARN", s, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        delegate.warn(s, o, o1);
        log("WARN", s, null, LogConsole.WARN_STYLE, o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        delegate.warn(s, throwable);
        log("WARN", s, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        delegate.warn(marker, s);
        log("WARN", s, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        delegate.warn(marker, s, o);
        log("WARN", s, null, LogConsole.WARN_STYLE, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        delegate.warn(marker, s, o, o1);
        log("WARN", s, null, LogConsole.WARN_STYLE, o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        delegate.warn(marker, s, objects);
        log("WARN", s, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        delegate.warn(marker, s, throwable);
        log("WARN", s, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    /**
     * Prints common message to the console
     */

    private void log(String level, String message, Throwable t, AttributeSet style, Object... parameters) {
        for (Object parameter : parameters) {
            message.replaceFirst("\\{\\}", String.valueOf(parameter));
        }
        getLogConsole().appendMessage(level, message, t, style);
    }

    private void log(String level, Object message, Throwable t, AttributeSet style) {
        getLogConsole().appendMessage(level, String.valueOf(message), t, style);
    }

    private LogConsole getLogConsole() {
        return LogConsole.getInstance();
    }
}
