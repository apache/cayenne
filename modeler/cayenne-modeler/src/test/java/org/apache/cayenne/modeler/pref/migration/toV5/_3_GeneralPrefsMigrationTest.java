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

package org.apache.cayenne.modeler.pref.migration.toV5;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;



public class _3_GeneralPrefsMigrationTest {

    @Test
    public void normalizeEncoding_historicalAlias() {
        assertEquals("UTF-8", _3_GeneralPrefsMigration.normalizeEncoding("UTF8"));
    }

    @Test
    public void normalizeEncoding_alreadyCanonical() {
        assertEquals("UTF-8", _3_GeneralPrefsMigration.normalizeEncoding("UTF-8"));
    }

    @Test
    public void normalizeEncoding_caseInsensitive() {
        assertEquals("ISO-8859-1", _3_GeneralPrefsMigration.normalizeEncoding("iso-8859-1"));
    }

    @Test
    public void normalizeEncoding_empty() {
        assertNull(_3_GeneralPrefsMigration.normalizeEncoding(""));
    }

    @Test
    public void normalizeEncoding_null() {
        assertNull(_3_GeneralPrefsMigration.normalizeEncoding(null));
    }

    @Test
    public void normalizeEncoding_unsupportedReturnsNull() {
        assertNull(_3_GeneralPrefsMigration.normalizeEncoding("BogusCharset"));
    }

    @Test
    public void normalizeEncoding_illegalNameReturnsNull() {
        // Charset.forName throws IllegalCharsetNameException for names with illegal chars
        assertNull(_3_GeneralPrefsMigration.normalizeEncoding("@#$"));
    }
}
