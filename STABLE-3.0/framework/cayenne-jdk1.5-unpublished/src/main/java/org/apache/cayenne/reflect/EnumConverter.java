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

package org.apache.cayenne.reflect;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ExtendedEnumeration;
import org.apache.cayenne.util.Util;

/**
 * @since 1.2
 */
class EnumConverter extends Converter {

    @Override
    @SuppressWarnings("unchecked")
    Object convert(Object object, Class type) {

        if (ExtendedEnumeration.class.isAssignableFrom(type)) {
            ExtendedEnumeration[] values;

            try {
                values = (ExtendedEnumeration[]) type.getMethod("values").invoke(null);
            }
            catch (Exception e) {
                // unexpected, all enums should have values
                throw new CayenneRuntimeException(e);
            }

            for (ExtendedEnumeration en : values) {
                if (Util.nullSafeEquals(en.getDatabaseValue(), object)) {
                    return en;
                }
            }

            return null;
        }

        if (object == null) {
            return null;
        }

        return Enum.valueOf(type, object.toString());
    }
}
