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

package org.apache.cayenne.dba.oracle;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OracleUtilDateTypeTest {

    @Test
    public void testNormalizeDate() throws Exception {
        Date date = new Date();
        Date timeNormalized = new OracleUtilDateType().normalizeDate(date);

        assertNotNull(timeNormalized);

        Calendar raw = Calendar.getInstance();
        raw.setTime(date);

        Calendar normalized = null;

        normalized = Calendar.getInstance();
        normalized.setTime(timeNormalized);

        assertEquals(1970, normalized.get(Calendar.YEAR));
        assertEquals(0, normalized.get(Calendar.MONTH));
        assertEquals(1, normalized.get(Calendar.DAY_OF_MONTH));

        assertEquals(raw.get(Calendar.HOUR_OF_DAY), normalized.get(Calendar.HOUR_OF_DAY));
        assertEquals(raw.get(Calendar.MINUTE), normalized.get(Calendar.MINUTE));
        assertEquals(raw.get(Calendar.SECOND), normalized.get(Calendar.SECOND));
    }
}
