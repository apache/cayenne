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

package org.apache.cayenne.dbsync.filter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NamePatternMatcherTest {

    /**
     * Test tokenizing
     */
    @Test
    public void testTokenizer() {

        String[] nullFilters = NamePatternMatcher.tokenizePattern(null);
        assertEquals(0, nullFilters.length);

        String[] filters = NamePatternMatcher.tokenizePattern("billing_*,user?");
        assertEquals(2, filters.length);
        assertEquals("^billing_.*$", filters[0]);
        assertEquals("^user.?$", filters[1]);
    }

    /**
     * Test tokenizing
     */
    @Test
    public void testTokenizerEntities() {

        String includePattern = "Organization,SecGroup,SecIndividual";

        String[] filters = NamePatternMatcher.tokenizePattern(includePattern);
        assertEquals(3, filters.length);
        assertEquals("^Organization$", filters[0]);
        assertEquals("^SecGroup$", filters[1]);
        assertEquals("^SecIndividual$", filters[2]);
    }
}
