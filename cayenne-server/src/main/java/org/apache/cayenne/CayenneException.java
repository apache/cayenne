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

package org.apache.cayenne;


/**
 * A generic checked exception that may be thrown by Cayenne framework. All checked
 * exceptions in Cayenne inherit from this class.
 *
 * @deprecated since 4.1 use {@link CayenneRuntimeException} instead
 */
@Deprecated
public class CayenneException extends Exception {

    /**
     * @deprecated since 4.1 use {@link CayenneRuntimeException#getExceptionLabel()}
     */
    @Deprecated
    public static String getExceptionLabel() {
        return CayenneRuntimeException.getExceptionLabel();
    }

    /**
     * Creates new <code>CayenneException</code> without detail message.
     */
    public CayenneException() {
    }

    /**
     * Constructs an <code>CayenneException</code> with the specified detail message.
     * 
     * @param messageFormat the detail message format string.
     */
    public CayenneException(String messageFormat, Object... messageArgs) {
        super(String.format(messageFormat, messageArgs));
    }

    /**
     * Constructs an <code>CayenneException</code> that wraps a <code>cause</code> thrown
     * elsewhere.
     */
    public CayenneException(Throwable cause) {
        super(cause);
    }

    public CayenneException(String messageFormat, Throwable cause, Object... messageArgs) {
        super(String.format(messageFormat, messageArgs), cause);
    }

    /**
     * Returns exception message without Cayenne version label.
     * 
     * @since 1.1
     */
    public String getUnlabeledMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return (message != null) ? getExceptionLabel() + message : getExceptionLabel()
                + "(no message)";
    }
}
