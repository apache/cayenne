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

import org.junit.Test;

import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LocalTimeValueTypeTest {

    private static final LocalTimeValueType valueType = new LocalTimeValueType();

    @Test
    public void testToJavaObject() {
        // UTC 17:50:23.123
        long utcMillis = 64223123;
        long systemMillis = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .get(ChronoField.MILLI_OF_DAY);

        Time time = new Time(utcMillis);
        LocalTime localTime = valueType.toJavaObject(time);

        assertEquals(systemMillis, localTime.get(ChronoField.MILLI_OF_DAY));
    }

    @Test
    public void testFromJavaObject() {
        // UTC 17:50:23.123
        long utcMillis = 64223123;
        long systemMillis = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .getLong(ChronoField.MILLI_OF_DAY);

        LocalTime localTime = LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(systemMillis));
        Time time = valueType.fromJavaObject(localTime);

        assertEquals(utcMillis, time.getTime());
    }

    @Test
    public void testToJavaObjectFromJavaObject() {
        // UTC 17:50:23.123
        long utcMillis = 64223123;

        Time time = new Time(utcMillis);
        LocalTime localTime = valueType.toJavaObject(time);
        Time newTime = valueType.fromJavaObject(localTime);

        assertEquals(time, newTime);
    }

    @Test
    public void testToJavaObject_isBackwardCompatible() {
        // UTC 17:50:23.123
        long utcMillis = 64223123;

        Time time = new Time(utcMillis);
        LocalTime impreciseLocalTime = time.toLocalTime();
        LocalTime localTime = valueType.toJavaObject(time);

        assertEquals(impreciseLocalTime.toSecondOfDay(), localTime.get(ChronoField.SECOND_OF_DAY));
    }

    @Test
    public void testFromJavaObject_isBackwardCompatible() {
        // UTC 17:50:23.123
        long utcMillis = 64223123;
        long systemMillis = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .getLong(ChronoField.MILLI_OF_DAY);

        LocalTime localTime = LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(systemMillis));
        Time impreciseTime = Time.valueOf(localTime);
        Time time = valueType.fromJavaObject(localTime);

        assertEquals(TimeUnit.MILLISECONDS.toSeconds(impreciseTime.getTime()),
                     TimeUnit.MILLISECONDS.toSeconds(time.getTime()));
    }

    @Test
    public void testToJavaObjectFromJavaObject_changeTimeZone() {
        TimeZone originalTimeZone = TimeZone.getDefault();

        try {
            // UTC 17:50:23.123
            long utcMillis = 64223123;

            Time time = new Time(utcMillis);
            LocalTime localTime = valueType.toJavaObject(time);
            TimeZone.setDefault(getOtherTimeZone(originalTimeZone));
            Time newTime = valueType.fromJavaObject(localTime);

            assertNotEquals(time, newTime);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    private static TimeZone getOtherTimeZone(TimeZone timeZone) {
        List<TimeZone> timeZones = Arrays.stream(TimeZone.getAvailableIDs())
                .map(TimeZone::getTimeZone)
                .distinct()
                .filter(tz -> tz.getRawOffset() != timeZone.getRawOffset())
                .collect(Collectors.toList());
        return timeZones.get(0);
    }
}
