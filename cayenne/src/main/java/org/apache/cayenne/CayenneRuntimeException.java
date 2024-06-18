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

package org.apache.cayenne;

import org.apache.cayenne.util.LocalizedStringsHandler;

/**
 * A generic unchecked exception that may be thrown by Cayenne framework. All runtime
 * exceptions in Cayenne inherit from this class.
 */
public class CayenneRuntimeException extends RuntimeException {

    private static String exceptionLabel;

    static {
        String version = LocalizedStringsHandler.getString("cayenne.version");
        String date = LocalizedStringsHandler.getString("cayenne.build.date");
        exceptionLabel = "[v." + version + " " + date + "] ";
    }

    /**
     * @since 4.1
     */
    public static String getExceptionLabel() {
        return exceptionLabel;
    }

    /**
     * Creates new CayenneRuntimeException without detail message.
     */
    public CayenneRuntimeException() {
    }

    /**
     * Constructs an exception with the specified message and an optional list of message
     * formatting arguments. Message formatting rules follow "String.format(..)"
     * conventions.
     */
    public CayenneRuntimeException(String messageFormat, Object... messageArgs) {
        super(messageFormat == null ? null : String.format(messageFormat, messageArgs));
    }

    /**
     * Constructs an <code>CayenneRuntimeException</code> that wraps
     * <code>exception</code> thrown elsewhere.
     */
    public CayenneRuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an exception with the specified message and exception cause and an
     * optional list of message formatting arguments. Message formatting rules follow
     * "String.format(..)" conventions.
     */
    public CayenneRuntimeException(String messageFormat, Throwable cause, Object... messageArgs) {
        super(messageFormat == null ? null : String.format(messageFormat, messageArgs), cause);
    }

    /**
     * Returns exception message without Cayenne version label.
     * 
     * @since 1.1
     */
    public String getUnlabeledMessage() {
        return super.getMessage();
    }

    /**
     * Returns message that includes Cayenne version label and the actual exception
     * message.
     */
    @Override
    public String getMessage() {
        String message = super.getMessage();
        return (message != null)
                ? exceptionLabel + message
                : exceptionLabel + "(no message)";
    }
}
