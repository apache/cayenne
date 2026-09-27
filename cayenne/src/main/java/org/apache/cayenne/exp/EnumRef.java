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

package org.apache.cayenne.exp;

import java.util.Objects;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;

/**
 * An enum constant referenced by the names of its class and constant, as parsed from the "enum:" literal of an
 * expression String. It is resolved to the constant on access, so that an expression can be parsed and printed in an
 * environment where the enum class is not available, e.g. in the Modeler.
 *
 * @since 5.0
 */
public final class EnumRef {

    private final String className;
    private final String enumName;

    public EnumRef(String className, String enumName) {
        this.className = Objects.requireNonNull(className);
        this.enumName = Objects.requireNonNull(enumName);
    }

    public String getClassName() {
        return className;
    }

    public String getEnumName() {
        return enumName;
    }

    /**
     * Loads the enum class and returns the referenced constant.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Enum<?> resolve() {
        Class enumClass;
        try {
            enumClass = Util.getJavaClass(className);
        } catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Enum class not found: " + className);
        }

        if (!enumClass.isEnum()) {
            throw new CayenneRuntimeException("Specified class is not an enum: " + className);
        }

        try {
            return Enum.valueOf(enumClass, enumName);
        } catch (IllegalArgumentException e) {
            throw new CayenneRuntimeException("Invalid enum path: " + className + "." + enumName);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnumRef other)) return false;
        return className.equals(other.className) && enumName.equals(other.enumName);
    }

    @Override
    public int hashCode() {
        return 31 * className.hashCode() + enumName.hashCode();
    }

    /**
     * Returns the "enum:" literal of the constant, as it appears in an expression String.
     */
    @Override
    public String toString() {
        return "enum:" + className + "." + enumName;
    }
}
