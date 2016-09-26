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

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UtilDateConverterTest {

    private Date date(String dateString) throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.parse(dateString);
    }

    @Test
    public void testFromBytes() throws ParseException {
        assertEquals(date("2015-01-07 11:00:02"), UtilDateConverter.INSTANCE.fromBytes(new byte[]{0, 0, 1, 74, -60, 13, 31, 80}));
    }

    @Test
    public void testToBytes() throws ParseException {
        assertArrayEquals(new byte[]{0, 0, 1, 74, -60, 13, 31, 80},
                UtilDateConverter.INSTANCE.toBytes(date("2015-01-07 11:00:02")));
    }
}
