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

/**
 * A factory that dynamically creates ExtendedTypes for Character, Character[], Byte[] and
 * char[] based on adapter configured types for String and byte[].
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class ByteOrCharArrayFactory implements ExtendedTypeFactory {

    private ExtendedTypeMap map;

    ByteOrCharArrayFactory(ExtendedTypeMap map) {
        this.map = map;
    }

    public ExtendedType getType(Class objectClass) {

        if (objectClass.isArray()) {

            Class elementType = objectClass.getComponentType();

            if (Character.class.isAssignableFrom(elementType)) {
                // can't use "getRegisteredType" as it causes infinite recursion
                ExtendedType stringType = map
                        .getExplictlyRegisteredType("java.lang.String");

                return new CharacterArrayType(stringType);
            }
            else if (Character.TYPE.isAssignableFrom(elementType)) {

                // can't use "getRegisteredType" as it causes infinite recursion
                ExtendedType stringType = map
                        .getExplictlyRegisteredType("java.lang.String");

                return new CharArrayType(stringType);
            }
            else if (Byte.class.isAssignableFrom(elementType)) {
                // can't use "getRegisteredType" as it causes infinite recursion
                ExtendedType bytesType = map.getExplictlyRegisteredType("byte[]");
                return new ByteWrapperArrayType(bytesType);
            }
        }
        else if (Character.class.isAssignableFrom(objectClass)) {

            // can't use "getRegisteredType" as it causes infinite recursion
            ExtendedType stringType = map.getExplictlyRegisteredType("java.lang.String");
            return new CharacterType(stringType);
        }

        return null;
    }

    final class CharacterType extends ExtendedTypeDecorator {

        CharacterType(ExtendedType stringType) {
            super(stringType);
        }

        public String getClassName() {
            return "java.lang.Character";
        }

        Object fromJavaObject(Object object) {
            return object != null
                    ? String.valueOf(((Character) object).charValue())
                    : null;
        }

        Object toJavaObject(Object object) {
            if (object == null) {
                return null;
            }

            String string = object.toString();
            return (string.length() > 0) ? new Character(string.charAt(0)) : null;
        }
    }

    final class CharArrayType extends ExtendedTypeDecorator {

        CharArrayType(ExtendedType stringType) {
            super(stringType);
        }

        public String getClassName() {
            return "char[]";
        }

        Object fromJavaObject(Object object) {
            return object != null ? new String((char[]) object) : null;
        }

        Object toJavaObject(Object object) {
            return object != null ? ((String) object).toCharArray() : null;
        }
    }

    final class CharacterArrayType extends ExtendedTypeDecorator {

        CharacterArrayType(ExtendedType stringType) {
            super(stringType);
        }

        public String getClassName() {
            return "java.lang.Character[]";
        }

        Object fromJavaObject(Object object) {
            if (object == null) {
                return null;
            }

            Character[] chars = (Character[]) object;
            StringBuffer buffer = new StringBuffer(chars.length);
            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i] != null ? chars[i].charValue() : 0);
            }

            return buffer.toString();
        }

        Object toJavaObject(Object object) {
            if (object == null) {
                return null;
            }

            String string = object.toString();
            Character[] chars = new Character[string.length()];
            for (int i = 0; i < string.length(); i++) {
                chars[i] = new Character(string.charAt(i));
            }

            return chars;
        }
    }

    final class ByteWrapperArrayType extends ExtendedTypeDecorator {

        ByteWrapperArrayType(ExtendedType byteArrayType) {
            super(byteArrayType);
        }

        public String getClassName() {
            return "java.lang.Byte[]";
        }

        Object fromJavaObject(Object object) {
            if (object == null) {
                return null;
            }

            Byte[] bytes = (Byte[]) object;
            byte[] buffer = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                buffer[i] = bytes[i] != null ? bytes[i].byteValue() : 0;
            }

            return buffer;
        }

        Object toJavaObject(Object object) {
            if (object == null) {
                return null;
            }

            byte[] bytes = (byte[]) object;
            Byte[] byteWrappers = new Byte[bytes.length];

            for (int i = 0; i < bytes.length; i++) {
                byteWrappers[i] = new Byte(bytes[i]);
            }

            return byteWrappers;
        }
    }
}
