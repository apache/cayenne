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

package org.apache.cayenne.exp.parser;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.Util;

/**
 * Scalar node that represents constant enumeration value.
 * It resolves actual value at a late stage to be parsable in environment where
 * final Enum class is not known, i.e. in Modeler.
 *
 * @since 4.1
 */
public class ASTEnum extends ASTScalar {

    ASTEnum(int id) {
        super(id);
    }

    public ASTEnum() {
        super(ExpressionParserTreeConstants.JJTENUM);
    }

    public ASTEnum(Object value) {
        super(ExpressionParserTreeConstants.JJTENUM);
        setValue(value);
    }

    @Override
    protected Object evaluateNode(Object o) {
        return getValueAsEnum().resolve();
    }

    @Override
    public Expression shallowCopy() {
        ASTScalar copy = new ASTEnum(id);
        copy.value = value;
        return copy;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        Object scalar = getValueAsEnum().resolve();
        SimpleNode.encodeScalarAsEJBQL(parameterAccumulator, out, scalar);
    }

    @Override
    public Object getValue() {
        return getValueAsEnum().resolve();
    }

    public void setEnumValue(String enumPath) throws ParseException{
        if (enumPath == null) {
            throw new ParseException("Null 'enumPath'");
        }

        int dot = enumPath.lastIndexOf('.');
        if (dot <= 0 || dot == enumPath.length() - 1) {
            throw new ParseException("Invalid enum path: " + enumPath);
        }

        String className = enumPath.substring(0, dot);
        String enumName = enumPath.substring(dot + 1);

        setValue(new ASTEnum.EnumValue(className, enumName));
    }

    EnumValue getValueAsEnum() {
        return (EnumValue)value;
    }

    static final class EnumValue {
        String className;
        String enumName;

        EnumValue(String className, String enumName) {
            this.className = Objects.requireNonNull(className);
            this.enumName = Objects.requireNonNull(enumName);
        }

        Enum<?> resolve() {
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
            if (o == null || EnumValue.class != o.getClass()) return false;

            EnumValue enumValue = (EnumValue) o;
            return className.equals(enumValue.className) && enumName.equals(enumValue.enumName);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + enumName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "enum:" + className + "." + enumName;
        }
    }
}
