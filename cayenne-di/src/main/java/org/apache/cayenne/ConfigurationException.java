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
 * A runtime exception thrown on failures in Cayenne configuration.
 */
public class ConfigurationException extends CayenneRuntimeException {

    /**
     * Creates new <code>ConfigurationException</code> without detail message.
     */
    public ConfigurationException() {
    }

    /**
     * Constructs an exception with the specified message with an optional list of message
     * formatting arguments. Message formatting rules follow "String.format(..)"
     * conventions.
     */
    public ConfigurationException(String messageFormat, Object... messageArgs) {
        super(messageFormat, messageArgs);
    }

    /**
     * Constructs an exception wrapping another exception thrown elsewhere.
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    public ConfigurationException(String messageFormat, Throwable cause,
            Object... messageArgs) {
        super(messageFormat, cause, messageArgs);
    }
}
