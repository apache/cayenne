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

package org.apache.cayenne.validation;

import java.io.Serializable;

/**
 * Defines a single failure during the validation process. Implementing classes may
 * store any extra information to help callers to identify the source and reasons 
 * for the failure.
 *
 * @see BeanValidationFailure
 * @since 1.1
 */
public interface ValidationFailure extends Serializable {

    /**
     * Returns the object that has generated the failure. For example, if a <code>Person</code>
     * must have a name and a <code>ValidationFailure</code> is created when the
     * user attempts to save it, the <code>Person</code> object would be the failure source.
     *
     * @return the failure's source or null in case a source cannot be defined.
     */
    public Object getSource();

    /**
     * Returns an user defined error object.
     */
    public Object getError();

    /**
     * Returns a String representation of the error object.
     * This is used in log messages and exceptions.
     */
    public String getDescription();

}
