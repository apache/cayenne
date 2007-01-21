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
 * A generic unchecked exception that may be thrown by Cayenne framework. All runtime
 * exceptions in Cayenne inherit from this class.
 * 
 * @author Andrus Adamchik
 */
public class CayenneRuntimeException extends RuntimeException {

    /**
     * Creates new CayenneRuntimeException without detail message.
     */
    public CayenneRuntimeException() {
    }

    /**
     * Constructs an <code>CayenneRuntimeException</code> with the specified detail
     * message.
     * 
     * @param message the detail message.
     */
    public CayenneRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs an <code>CayenneRuntimeException</code> that wraps
     * <code>exception</code> thrown elsewhere.
     */
    public CayenneRuntimeException(Throwable cause) {
        super(cause);
    }

    public CayenneRuntimeException(String message, Throwable cause) {
        super(message, cause);
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
    public String getMessage() {
        String message = super.getMessage();
        return (message != null)
                ? CayenneException.getExceptionLabel() + message
                : CayenneException.getExceptionLabel() + "(no message)";
    }
}
