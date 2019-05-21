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
package org.apache.cayenne.crypto.transformer.value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * @since 4.1
 */
public class LocalDateTimeConverter implements BytesConverter<LocalDateTime> {

    public static final BytesConverter<LocalDateTime> INSTANCE = new LocalDateTimeConverter(LongConverter.INSTANCE);

    private BytesConverter<Long> longConverter;

    public LocalDateTimeConverter(BytesConverter<Long> longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }


    @Override
    public LocalDateTime fromBytes(byte[] bytes) {
        if (bytes.length < 2) {
            throw new IllegalArgumentException("Unexpected number of bytes: " + bytes.length);
        }

        // long values converted to varying length byte arrays, so first byte is length
        int dateLength = bytes[0];
        if(dateLength <= 0 || dateLength >= bytes.length - 1) {
            throw new CayenneCryptoException("Corrupted data for LocalDateTime: wrong encoded length");
        }
        int timeLength = bytes.length - 1 - dateLength;

        byte[] date = new byte[dateLength];
        byte[] time = new byte[timeLength];

        System.arraycopy(bytes, 1, date, 0, dateLength);
        System.arraycopy(bytes, dateLength + 1, time, 0, timeLength);

        LocalDate localDate = LocalDate.ofEpochDay(longConverter.fromBytes(date));
        LocalTime localTime = LocalTime.ofNanoOfDay(longConverter.fromBytes(time));
        return LocalDateTime.of(localDate, localTime);
    }


    @Override
    public byte[] toBytes(LocalDateTime value) {
        byte[] date = longConverter.toBytes(value.toLocalDate().toEpochDay());
        byte[] time = longConverter.toBytes(value.toLocalTime().toNanoOfDay());

        byte [] datetime = new byte[date.length + time.length  + 1];
        // store date part length as first byte
        datetime[0] = (byte)date.length;
        System.arraycopy(date,0, datetime, 1, date.length);
        System.arraycopy(time,0, datetime, date.length + 1, time.length);

        return datetime;
    }
}
