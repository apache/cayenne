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
package org.apache.cayenne.di;

/**
 * An object that encapsulates a key used to store and lookup DI bindings. Key is made of
 * a binding type and an optional binding name.
 * 
 * @since 3.1
 */
public class Key<T> {

    protected Class<T> type;
    protected String typeName;
    protected String bindingName;

    /**
     * Creates a key for a nameless binding of a given type.
     */
    public static <T> Key<T> get(Class<T> type) {
        return new Key<T>(type, null);
    }

    /**
     * Creates a key for a named binding of a given type. 'bindingName' that is an empty
     * String is treated the same way as a null 'bindingName'. In both cases a nameless
     * binding key is created.
     */
    public static <T> Key<T> get(Class<T> type, String bindingName) {
        return new Key<T>(type, bindingName);
    }

    protected Key(Class<T> type, String bindingName) {
        if (type == null) {
            throw new NullPointerException("Null key type");
        }

        this.type = type;

        // will use type name in comparisons to ensure the key works across ClassLoaders.
        this.typeName = type.getName();

        // empty non-null binding names are often passed from annotation defaults and are
        // treated as null
        this.bindingName = bindingName != null && bindingName.length() > 0
                ? bindingName
                : null;
    }

    public Class<T> getType() {
        return type;
    }

    /**
     * Returns an optional name of the binding used to distinguish multiple bindings of
     * the same object type.
     */
    public String getBindingName() {
        return bindingName;
    }

    @Override
    public boolean equals(Object object) {

        if (object == this) {
            return true;
        }

        if (object instanceof Key<?>) {
            Key<?> key = (Key<?>) object;

            // type is guaranteed to be not null, so skip null checking...
            if (!typeName.equals(key.typeName)) {
                return false;
            }

            // bindingName can be null, so take this into account
            if (bindingName != null) {
                return bindingName.equals(key.bindingName);
            }
            else {
                return key.bindingName == null;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {

        int hashCode = 407 + 11 * typeName.hashCode();

        if (bindingName != null) {
            hashCode += bindingName.hashCode();
        }

        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<BindingKey: ");
        buffer.append(typeName);

        if (bindingName != null) {
            buffer.append(", '").append(bindingName).append('\'');
        }

        buffer.append('>');
        return buffer.toString();
    }
}
