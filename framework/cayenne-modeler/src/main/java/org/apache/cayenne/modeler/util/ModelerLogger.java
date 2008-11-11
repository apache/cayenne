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
import org.apache.commons.logging.Log;

import javax.swing.text.AttributeSet;

/**
 * ModelerLogger is a Log implementation, which performs output
 * to the LogConsole. Default behavior is saved since they are delegated
 * to default Log instance. 
 */
public class ModelerLogger implements Log {
    /**
     * 'Default' Log instance (i.e. Log4jLogger, Jdk14Logger, or other)
     */
    Log delegate;
    
    /** 
     * Logger name 
     */
    String name;
    
    public ModelerLogger(String name, Log delegate) {
        this.name = name;
        this.delegate = delegate;
    }
    
    public void debug(Object message) {
        delegate.debug(message);
        log("DEBUG", message, null, LogConsole.DEBUG_STYLE);
    }

    public void debug(Object message, Throwable t) {
        delegate.debug(message, t);
        log("DEBUG", message, t, LogConsole.DEBUG_STYLE);
    }

    public void error(Object message) {
        delegate.error(message);
        log("ERROR", message, null, LogConsole.ERROR_STYLE);
    }

    public void error(Object message, Throwable t) {
        delegate.error(message, t);
        log("ERROR", message, t, LogConsole.ERROR_STYLE);
    }

    public void fatal(Object message) {
        delegate.fatal(message);
        log("FATAL", message, null, LogConsole.FATAL_STYLE);
    }

    public void fatal(Object message, Throwable t) {
        delegate.fatal(message, t);
        log("FATAL", message, t, LogConsole.FATAL_STYLE);
    }

    public void info(Object message) {
        delegate.info(message);
        log("INFO", message, null, LogConsole.INFO_STYLE);
    }

    public void info(Object message, Throwable t) {
        delegate.info(message);
        log("INFO", message, t, LogConsole.INFO_STYLE);
    }
    
    public void trace(Object message) {
        delegate.trace(message);
        log("TRACE", message, null, LogConsole.DEBUG_STYLE);
    }

    public void trace(Object message, Throwable t) {
        delegate.trace(message, t);
        log("TRACE", message, t, LogConsole.DEBUG_STYLE);
    }

    public void warn(Object message) {
        delegate.warn(message);
        log("WARN", message, null, LogConsole.WARN_STYLE);
    }

    public void warn(Object message, Throwable t) {
        delegate.warn(message, t);
        log("WARN", message, t, LogConsole.WARN_STYLE);
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return delegate.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    /**
     * Prints common message to the console
     */
    private void log(String level, Object message, Throwable t, AttributeSet style) {
        getLogConsole().appendMessage(level, String.valueOf(message), t, style);
    }

    private LogConsole getLogConsole() {
        return LogConsole.getInstance();
    }
}
