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

package org.apache.cayenne.modeler.ui.preferences.general;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class GeneralPreferencesControllerTest {

    @Test
    public void standardEncodingsAreSupportedByJVM() {
        assertTrue(GeneralPreferencesController.STANDARD_ENCODINGS.length > 0,
                "STANDARD_ENCODINGS must not be empty");

        for (String encoding : GeneralPreferencesController.STANDARD_ENCODINGS) {
            assertTrue(Charset.isSupported(encoding),
                    "Charset not supported by JVM: " + encoding);
        }
    }

    @Test
    public void detectPlatformEncodingReturnsCanonicalName() {
        // Must return the canonical Charset name (e.g. "UTF-8"), not a historical
        // alias (e.g. "UTF8"), so it matches entries in STANDARD_ENCODINGS.
        String encoding = GeneralPreferencesController.detectPlatformEncoding();
        assertEquals(Charset.forName(encoding).name(), encoding);
    }
}
