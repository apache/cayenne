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
package org.apache.cayenne.access.types;

import org.apache.cayenne.util.IDUtil;

import java.lang.reflect.Array;

/**
 * A factory that dynamically creates ExtendedTypes for Character, Character[], Byte[] and
 * char[] based on adapter configured types for String and byte[].
 * 
 * @since 3.0
 */
class ByteOrCharArrayFactory implements ExtendedTypeFactory {

    private ExtendedTypeMap map;

    ByteOrCharArrayFactory(ExtendedTypeMap map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    public ExtendedType getType(Class<?> objectClass) {

        if (objectClass.isArray()) {
            Class<?> elementType = objectClass.getComponentType();
            if (Character.class.isAssignableFrom(elementType)) {
                // can't use "getRegisteredType" as it causes infinite recursion
                ExtendedType<String> stringType = map.getExplictlyRegisteredType("java.lang.String");
                return new CharacterArrayType(stringType);
            } else if (Character.TYPE.isAssignableFrom(elementType)) {
                // can't use "getRegisteredType" as it causes infinite recursion
                ExtendedType<String> stringType = map.getExplictlyRegisteredType("java.lang.String");
                return new CharArrayType(stringType);
            } else if (Byte.class.isAssignableFrom(elementType)) {
                // can't use "getRegisteredType" as it causes infinite recursion
                ExtendedType<byte[]> bytesType = map.getExplictlyRegisteredType("byte[]");
                return new ByteWrapperArrayType(bytesType);
            }
        } else if (Character.class.isAssignableFrom(objectClass)) {
            // can't use "getRegisteredType" as it causes infinite recursion
            ExtendedType<String> stringType = map.getExplictlyRegisteredType("java.lang.String");
            return new CharacterType(stringType);
        }

        return null;
    }

    final class CharacterType extends ExtendedTypeDecorator<Character, String> {

        CharacterType(ExtendedType<String> stringType) {
            super(stringType);
        }

        @Override
        public String getClassName() {
            return "java.lang.Character";
        }

        @Override
        String fromJavaObject(Character object) {
            return object != null
                    ? String.valueOf(object.charValue())
                    : null;
        }

        @Override
        Character toJavaObject(String string) {
            if (string == null) {
                return null;
            }

            return (string.length() > 0) ? string.charAt(0) : null;
        }
    }

    final class CharArrayType extends ExtendedTypeDecorator<char[], String> {

        CharArrayType(ExtendedType<String> stringType) {
            super(stringType);
        }

        @Override
        public String getClassName() {
            return "char[]";
        }

        @Override
        String fromJavaObject(char[] object) {
            return object != null ? new String(object) : null;
        }

        @Override
        char[] toJavaObject(String object) {
            return object != null ? object.toCharArray() : null;
        }

        @Override
        public String toString(char[] value) {
            if (value == null) {
                return "NULL";
            }

            StringBuilder buffer = new StringBuilder();
            buffer.append("<");

            int len = Array.getLength(value);
            boolean trimming = false;
            if (len > TRIM_VALUES_THRESHOLD) {
                len = TRIM_VALUES_THRESHOLD;
                trimming = true;
            }

            for (int i = 0; i < len; i++) {
                buffer.append(Array.get(value, i));
            }

            if (trimming) {
                buffer.append("...");
            }

            buffer.append('>');
            return buffer.toString();
        }
    }

    final class CharacterArrayType extends ExtendedTypeDecorator<Character[], String> {

        CharacterArrayType(ExtendedType<String> stringType) {
            super(stringType);
        }

        @Override
        public String getClassName() {
            return "java.lang.Character[]";
        }

        @Override
        String fromJavaObject(Character[] object) {
            if (object == null) {
                return null;
            }

            StringBuilder buffer = new StringBuilder(object.length);
            for (Character aChar : object) {
                buffer.append(aChar != null ? aChar : 0);
            }

            return buffer.toString();
        }

        @Override
        Character[] toJavaObject(String object) {
            if (object == null) {
                return null;
            }

            Character[] chars = new Character[object.length()];
            for (int i = 0; i < object.length(); i++) {
                chars[i] = object.charAt(i);
            }

            return chars;
        }

        @Override
        public String toString(Character[] value) {
            if (value == null) {
                return "NULL";
            }

            StringBuilder buffer = new StringBuilder();
            buffer.append("<");

            int len = Array.getLength(value);
            boolean trimming = false;
            if (len > TRIM_VALUES_THRESHOLD) {
                len = TRIM_VALUES_THRESHOLD;
                trimming = true;
            }

            for (int i = 0; i < len; i++) {
                buffer.append(Array.get(value, i));
            }

            if (trimming) {
                buffer.append("...");
            }

            buffer.append('>');
            return buffer.toString();
        }
    }

    final class ByteWrapperArrayType extends ExtendedTypeDecorator<Byte[], byte[]> {

        ByteWrapperArrayType(ExtendedType<byte[]> byteArrayType) {
            super(byteArrayType);
        }

        @Override
        public String getClassName() {
            return "java.lang.Byte[]";
        }

        @Override
        byte[] fromJavaObject(Byte[] bytes) {
            if (bytes == null) {
                return null;
            }

            byte[] buffer = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                buffer[i] = bytes[i] != null ? bytes[i] : 0;
            }

            return buffer;
        }

        @Override
        Byte[] toJavaObject(byte[] bytes) {
            if (bytes == null) {
                return null;
            }

            Byte[] byteWrappers = new Byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                byteWrappers[i] = bytes[i];
            }

            return byteWrappers;
        }

        @Override
        public String toString(Byte[] value) {
            if (value == null) {
                return "NULL";
            }

            StringBuilder buffer = new StringBuilder();
            buffer.append("<");

            int len = value.length;
            boolean trimming = false;
            if (len > TRIM_VALUES_THRESHOLD) {
                len = TRIM_VALUES_THRESHOLD;
                trimming = true;
            }

            for (int i = 0; i < len; i++) {
                IDUtil.appendFormattedByte(buffer, value[i]);
            }

            if (trimming) {
                buffer.append("...");
            }

            buffer.append('>');
            return buffer.toString();
        }
    }
}
