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

/**
 * This is char and Character type mapped to zero or one char String.
 * @since 4.1
 */
public class CharacterValueType implements ValueObjectType<Character, String> {
    @Override
    public Class<String> getTargetType() {
        return String.class;
    }

    @Override
    public Class<Character> getValueType() {
        return Character.class;
    }

    @Override
    public Character toJavaObject(String value) {
        if(value == null) {
            return null;
        }
        if(value.isEmpty()) {
            return 0;
        }
        if(value.length() > 1) {
            throw new IllegalArgumentException("Only one char String can be used, " + value + " used");
        }
        return value.charAt(0);
    }

    @Override
    public String fromJavaObject(Character object) {
        return String.valueOf(object);
    }

    @Override
    public String toCacheKey(Character object) {
        return String.valueOf(object);
    }
}
