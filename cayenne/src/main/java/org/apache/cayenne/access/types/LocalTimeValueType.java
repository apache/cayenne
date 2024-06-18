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

import java.sql.Time;
import java.time.LocalTime;

/**
 * @since 4.0
 */
public class LocalTimeValueType implements ValueObjectType<LocalTime, Time> {

    @Override
    public Class<Time> getTargetType() {
        return Time.class;
    }

    @Override
    public Class<LocalTime> getValueType() {
        return LocalTime.class;
    }

    @Override
    public LocalTime toJavaObject(Time value) {
        return value.toLocalTime();
    }

    @Override
    public Time fromJavaObject(LocalTime object) {
        return Time.valueOf(object);
    }

    @Override
    public String toCacheKey(LocalTime object) {
        return object.toString();
    }
}
