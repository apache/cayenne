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
    public void debug(String message) {
        delegate.debug(message);
        log("DEBUG", message, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void debug(String message, Object object) {
        delegate.debug(message, object);
        log("DEBUG", message, null, LogConsole.WARN_STYLE, object);
    }

    @Override
    public void debug(String message, Object object, Object secondObject) {
        delegate.debug(message, object, secondObject);
        log("DEBUG", message, null, LogConsole.WARN_STYLE, object, secondObject);
    }

    @Override
    public void debug(String message, Object... objects) {
        delegate.debug(message, objects);
        log("DEBUG", message, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        delegate.debug(message, throwable);
        log("DEBUG", message, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String message) {
        delegate.debug(marker, message);
        log("DEBUG", message, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void debug(Marker marker, String message, Object object) {
        delegate.debug(marker, message, object);
        log("DEBUG", message, null, LogConsole.WARN_STYLE, object);
    }

    @Override
    public void debug(Marker marker, String message, Object object, Object secondObject) {
        delegate.debug(marker, message, object, secondObject);
        log("DEBUG", message, null, LogConsole.WARN_STYLE, object, secondObject);
    }

    @Override
    public void debug(Marker marker, String message, Object... objects) {
        delegate.debug(marker, message, objects);
        log("DEBUG", message, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        delegate.debug(marker, message, throwable);
        log("DEBUG", message, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void error(String message) {
        delegate.error(message);
        log("ERROR", message, null, LogConsole.ERROR_STYLE);
    }

    @Override
    public void error(String message, Object object) {
        delegate.error(message, object);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object);
    }

    @Override
    public void error(String message, Object object, Object secondObject) {
        delegate.error(message, object, secondObject);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object, secondObject);
    }

    @Override
    public void error(String message, Object... objects) {
        delegate.error(message, objects);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, objects);
    }

    @Override
    public void error(String message, Throwable throwable) {
        delegate.error(message, throwable);
        log("ERROR", message, throwable, LogConsole.ERROR_STYLE);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String message) {
        delegate.error(marker, message);
        log("ERROR", message, null, LogConsole.ERROR_STYLE);
    }

    @Override
    public void error(Marker marker, String message, Object object) {
        delegate.error(marker, message , object);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object);
    }

    @Override
    public void error(Marker marker, String message, Object object, Object secondObject) {
        delegate.error(marker, message , object, secondObject);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, object, secondObject);
    }

    @Override
    public void error(Marker marker, String message, Object... objects) {
        delegate.error(marker, message, objects);
        log("ERROR", message, null, LogConsole.ERROR_STYLE, objects);
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        delegate.error(marker, message, throwable);
        log("ERROR", message, throwable, LogConsole.ERROR_STYLE);
    }

    @Override
    public void info(String message) {
        delegate.info(message);
        log("INFO", message, null, LogConsole.INFO_STYLE);
    }

    @Override
    public void info(String message, Object object) {
        delegate.info(message, object);
        log("INFO", message, null, LogConsole.INFO_STYLE, object);
    }

    @Override
    public void info(String message, Object object, Object secondObject) {
        delegate.info(message, object, secondObject);
        log("INFO", message, null, LogConsole.INFO_STYLE, object, secondObject);
    }

    @Override
    public void info(String message, Object... objects) {
        delegate.info(message, objects);
        log("INFO", message, null, LogConsole.INFO_STYLE, objects);
    }

    @Override
    public void info(String message, Throwable throwable) {
        delegate.info(message, throwable);
        log("INFO", message, throwable, LogConsole.INFO_STYLE);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String message) {
        delegate.info(marker, message);
        log("INFO", message, null, LogConsole.INFO_STYLE);
    }

    @Override
    public void info(Marker marker, String message, Object object) {
        delegate.info(marker, message, object);
        log("INFO", message, null, LogConsole.INFO_STYLE, object);
    }

    @Override
    public void info(Marker marker, String message, Object object, Object secondObject) {
        delegate.info(marker, message, object, secondObject);
        log("INFO", message, null, LogConsole.INFO_STYLE, object, secondObject);
    }

    @Override
    public void info(Marker marker, String message, Object... objects) {
        delegate.info(marker, message, objects);
        log("INFO", message, null, LogConsole.INFO_STYLE, objects);
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        delegate.info(marker, message, throwable);
        log("INFO", message, throwable, LogConsole.INFO_STYLE);
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
    public void trace(String message) {
        delegate.trace(message);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void trace(String message, Object object) {
        delegate.trace(message, object);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object);
    }

    @Override
    public void trace(String message, Object object, Object secondObject) {
        delegate.trace(message, object, secondObject);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object, secondObject);
    }

    @Override
    public void trace(String message, Object... objects) {
        delegate.trace(message, objects);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        delegate.trace(message, throwable);
        log("TRACE", message, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String message) {
        delegate.trace(marker, message);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE);
    }

    @Override
    public void trace(Marker marker, String message, Object object) {
        delegate.trace(marker, message, object);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object);
    }

    @Override
    public void trace(Marker marker, String message, Object object, Object secondObject) {
        delegate.trace(marker, message, object, secondObject);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, object, secondObject);
    }

    @Override
    public void trace(Marker marker, String message, Object... objects) {
        delegate.trace(marker, message, objects);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE, objects);
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        delegate.trace(marker, message, throwable);
        log("TRACE", message, throwable, LogConsole.DEBUG_STYLE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isErrorEnabled();
    }

    @Override
    public void warn(String message) {
        delegate.warn(message);
        log("WARN", message, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void warn(String message, Object object) {
        delegate.warn(message, object);
        log("WARN", message, null, LogConsole.WARN_STYLE, object);
    }

    @Override
    public void warn(String message, Object... objects) {
        delegate.warn(message, objects);
        log("WARN", message, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void warn(String message, Object object, Object secondObject) {
        delegate.warn(message, object, secondObject);
        log("WARN", message, null, LogConsole.WARN_STYLE, object, secondObject);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        delegate.warn(message, throwable);
        log("WARN", message, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String message) {
        delegate.warn(marker, message);
        log("WARN", message, null, LogConsole.WARN_STYLE);
    }

    @Override
    public void warn(Marker marker, String message, Object object) {
        delegate.warn(marker, message, object);
        log("WARN", message, null, LogConsole.WARN_STYLE, object);
    }

    @Override
    public void warn(Marker marker, String message, Object object, Object secondObject) {
        delegate.warn(marker, message, object, secondObject);
        log("WARN", message, null, LogConsole.WARN_STYLE, object, secondObject);
    }

    @Override
    public void warn(Marker marker, String message, Object... objects) {
        delegate.warn(marker, message, objects);
        log("WARN", message, null, LogConsole.WARN_STYLE, objects);
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        delegate.warn(marker, message, throwable);
        log("WARN", message, throwable, LogConsole.WARN_STYLE);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    /**
     * Prints common message to the console
     */

    private void log(String level, String message, Throwable throwable, AttributeSet style, Object... parameters) {
        for (Object parameter : parameters) {
            message.replaceFirst("\\{\\}", String.valueOf(parameter));
        }
        getLogConsole().appendMessage(level, message, throwable, style);
    }

    private void log(String level, Object message, Throwable throwable, AttributeSet style) {
        getLogConsole().appendMessage(level, String.valueOf(message), throwable, style);
    }

    private LogConsole getLogConsole() {
        return LogConsole.getInstance();
    }
}
