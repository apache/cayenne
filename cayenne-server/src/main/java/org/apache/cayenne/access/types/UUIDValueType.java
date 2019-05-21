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

import java.util.UUID;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * @since 4.0
 */
public class UUIDValueType implements ValueObjectType<UUID, String> {

    @Override
    public Class<String> getTargetType() {
        return String.class;
    }

    @Override
    public Class<UUID> getValueType() {
        return UUID.class;
    }

    @Override
    public UUID toJavaObject(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new CayenneRuntimeException("Invalid UUID value: " + value, e);
        }
    }

    @Override
    public String fromJavaObject(UUID object) {
        return object.toString();
    }

    @Override
    public String toCacheKey(UUID object) {
        return object.toString();
    }
}
