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

package org.apache.cayenne.reflect;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * An unchecked exception thrown on errors during property access, either within a
 * Accessor or a Property.
 * 
 * @since 3.0
 */
public class PropertyException extends CayenneRuntimeException {

    protected PropertyDescriptor property;
    protected Accessor accessor;
    protected Object source;

    public PropertyException(String messageFormat, Object... messageArgs) {
        this(messageFormat, (Throwable) null, messageArgs);
    }

    public PropertyException(String messageFormat, Throwable cause, Object... messageArgs) {
        super(messageFormat, cause, messageArgs);
    }

    public PropertyException(String messageFormat, Accessor accessor, Object source,
            Object... messageArgs) {
        this(messageFormat, accessor, source, (Throwable) null, messageArgs);
    }

    public PropertyException(String messageFormat, Accessor accessor, Object source,
            Throwable cause, Object... messageArgs) {
        super(messageFormat, cause, messageArgs);

        this.accessor = accessor;
        this.source = source;
    }

    public PropertyException(String messageFormat, PropertyDescriptor property, Object source,
            Object... messageArgs) {
        this(messageFormat, property, source, (Throwable) null, messageArgs);
    }

    public PropertyException(String messageFormat, PropertyDescriptor property, Object source,
            Throwable cause, Object... messageArgs) {
        super(messageFormat, cause, messageArgs);

        this.property = property;
        this.source = source;
    }

    /**
     * Returns property descriptor that was used to access the property. It may be null.
     */
    public Accessor getAccessor() {
        return accessor;
    }

    public PropertyDescriptor getProperty() {
        return property;
    }

    /**
     * Returns an object that caused an error.
     */
    public Object getSource() {
        return source;
    }
}
