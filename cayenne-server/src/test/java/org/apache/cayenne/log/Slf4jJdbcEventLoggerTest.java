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
package org.apache.cayenne.log;

import org.apache.cayenne.util.IDUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Slf4jJdbcEventLoggerTest {

    @Test
    public void testAppendFormattedByte() throws Exception {
        assertFormatting((byte) 0, "00");
        assertFormatting((byte) 1, "01");
        assertFormatting((byte) 10, "0A");
        assertFormatting(Byte.MAX_VALUE, "7F");
        assertFormatting((byte) -1, "FF");
        assertFormatting(Byte.MIN_VALUE, "80");
    }

    private void assertFormatting(byte b, String formatted) throws Exception {
        StringBuffer buffer = new StringBuffer();
        IDUtil.appendFormattedByte(buffer, b);
        assertEquals(formatted, buffer.toString());
    }
}
