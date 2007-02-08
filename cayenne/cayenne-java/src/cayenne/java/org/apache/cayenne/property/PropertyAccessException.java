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

package org.apache.cayenne.property;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * An unchecked exception thrown on errors during property access.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PropertyAccessException extends CayenneRuntimeException {

    protected PropertyAccessor accessor;
    protected Object source;

    public PropertyAccessException(String message, PropertyAccessor accessor,
            Object source) {
        this(message, accessor, source, null);
    }

    public PropertyAccessException(String message, PropertyAccessor accessor,
            Object source, Throwable cause) {
        super(message, cause);

        this.accessor = accessor;
        this.source = source;
    }

    /**
     * Returns property descriptor that was used to access the property. It may be null.
     */
    public PropertyAccessor getAccessor() {
        return accessor;
    }

    /**
     * Returns an object that caused an error.
     */
    public Object getSource() {
        return source;
    }
}
