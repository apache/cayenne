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

import java.lang.reflect.Constructor;

import org.apache.cayenne.util.Util;

/**
 * ExtendedTypeFactory for handling JDK 1.5 Enums. Gracefully handles JDK 1.4 environment.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EnumTypeFactory implements ExtendedTypeFactory {

    private Constructor enumTypeConstructor;

    EnumTypeFactory() {

        // see if we can support enums
        try {
            Class enumTypeClass = Util
                    .getJavaClass("org.apache.cayenne.access.types.EnumType");
            enumTypeConstructor = enumTypeClass.getConstructor(new Class[] {
                Class.class
            });
        }
        catch (Throwable th) {
            // no enums support... either Java 1.4 or Cayenne 1.5 extensions are absent
        }
    }

    public ExtendedType getType(Class objectClass) {
        if (enumTypeConstructor == null) {
            return null;
        }

        try {
            // load EnumType via reflection as the source has to stay JDK 1.4 compliant
            ExtendedType type = (ExtendedType) enumTypeConstructor
                    .newInstance(new Object[] {
                        objectClass
                    });

            return type;
        }
        catch (Throwable th) {
            // ignore exceptions...
            return null;
        }
    }
}
