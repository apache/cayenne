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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @since 4.2
 */
public class DurationConverterTest {

    @Test
    public void fromBytes() {
        assertEquals(Duration.ofDays(10),
                DurationConverter.INSTANCE.fromBytes(new byte[]{51, 127, -104, 0}));

    }

    @Test
    public void toBytes() {
        assertArrayEquals(new byte[] {51, 127, -104, 0},
                DurationConverter.INSTANCE.toBytes(Duration.ofDays(10)));
    }
}
