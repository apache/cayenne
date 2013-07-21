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
package org.apache.cayenne.access.types;

import org.apache.cayenne.util.Util;

/**
 * A {@link ExtendedTypeFactory} that makes a {@link ExtendedType} usable for subclasses
 * of the className in the {@link ExtendedType}.
 * 
 * @since 3.0
 */
class SubclassTypeFactory implements ExtendedTypeFactory {

    private ExtendedType type;
    private Class<?> javaClass;

    SubclassTypeFactory(ExtendedType type) {
        this.type = type;

        try {
            javaClass = Util.getJavaClass(type.getClassName());

            // some classes that should not be handled here..
            if (javaClass.isArray()
                    || javaClass.equals(Object.class)
                    || javaClass.isPrimitive()) {
                javaClass = null;
            }
        }
        catch (ClassNotFoundException e) {
            // ignore.
        }
    }

    public ExtendedType getType(Class<?> objectClass) {

        if ((javaClass != null) && javaClass.isAssignableFrom(objectClass)) {
            return type;
        }

        return null;
    }

}
