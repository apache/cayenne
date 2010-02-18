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
package org.apache.cayenne.project2.validate;

/**
 * ValidationInfo encapsulates information about a single node validation on the project
 * tree.
 */
public class ValidationInfo {

    public static final int VALID = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    protected Object path;
    protected String message;
    protected int severity;

    /**
     * Constructor for ValidationInfo.
     */
    public ValidationInfo(int severity, String message, Object path) {
        this.severity = severity;
        this.message = message;
        this.path = path;
    }

    public Object getValidatedObject() {
        return path;
    }

    @Override
    public String toString() {
        return getMessage();
    }

    /**
     * Returns the message.
     * 
     * @return String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the severity.
     * 
     * @return int
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Returns the object identifing a location described by this ValidationInfo.
     */
    public Object getPath() {
        return path;
    }
}
