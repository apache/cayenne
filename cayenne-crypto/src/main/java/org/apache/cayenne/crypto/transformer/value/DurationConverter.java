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

package org.apache.cayenne.crypto.transformer.value;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @since 4.2
 */
public class DurationConverter implements BytesConverter<Duration> {

    public static final BytesConverter<Duration> INSTANCE = new DurationConverter(LongConverter.INSTANCE);

    private BytesConverter<Long> longConverter;

    DurationConverter(BytesConverter<Long> longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }

    @Override
    public Duration fromBytes(byte[] bytes) {
        return Duration.of(longConverter.fromBytes(bytes), ChronoUnit.MILLIS);
    }

    @Override
    public byte[] toBytes(Duration value) {
        return longConverter.toBytes(value.toMillis());
    }
}
