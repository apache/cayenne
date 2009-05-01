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

package org.apache.cayenne.tools;

import junit.framework.TestCase;

import org.apache.tools.ant.Task;

public class NamePatternMatcherTest extends TestCase {

    /**
     * Test pattern expansion.
     */
    public void testReplaceWildcardInStringWithString() throws Exception {
        assertEquals(null, NamePatternMatcher.replaceWildcardInStringWithString(
                "*",
                null,
                "Entity"));
        assertEquals("*.java", NamePatternMatcher.replaceWildcardInStringWithString(
                null,
                "*.java",
                "Entity"));
        assertEquals("Entity.java", NamePatternMatcher.replaceWildcardInStringWithString(
                "*",
                "*.java",
                "Entity"));
        assertEquals("java.Entity", NamePatternMatcher.replaceWildcardInStringWithString(
                "*",
                "java.*",
                "Entity"));
        assertEquals("Entity.Entity", NamePatternMatcher
                .replaceWildcardInStringWithString("*", "*.*", "Entity"));
        assertEquals("EntityEntity", NamePatternMatcher
                .replaceWildcardInStringWithString("*", "**", "Entity"));
        assertEquals("EditEntityReport.vm", NamePatternMatcher
                .replaceWildcardInStringWithString("*", "Edit*Report.vm", "Entity"));
        assertEquals("Entity", NamePatternMatcher.replaceWildcardInStringWithString(
                "*",
                "*",
                "Entity"));
    }

    /**
     * Test tokenizing
     */
    public void testTokenizer() {
        Task parentTask = new Task() {

            @Override
            public void log(String msg, int msgLevel) {
                System.out.println(String.valueOf(msgLevel) + ": " + msg);
            }
        };

        String includePattern = "billing_*,user?";
        String excludePattern = null;
        NamePatternMatcher namePatternMatcher = new NamePatternMatcher(
                new AntLogger(parentTask),
                includePattern,
                excludePattern);

        String[] nullFilters = namePatternMatcher.tokenizePattern(null);
        assertEquals(0, nullFilters.length);

        String[] filters = namePatternMatcher.tokenizePattern("billing_*,user?");
        assertEquals(2, filters.length);
        assertEquals("^billing_.*$", filters[0]);
        assertEquals("^user.?$", filters[1]);
    }

    /**
     * Test tokenizing
     */
    public void testTokenizerEntities() {
        Task parentTask = new Task() {

            @Override
            public void log(String msg, int msgLevel) {
                System.out.println(String.valueOf(msgLevel) + ": " + msg);
            }
        };

        String includePattern = "Organization,SecGroup,SecIndividual";
        String excludePattern = null;
        NamePatternMatcher namePatternMatcher = new NamePatternMatcher(
                new AntLogger(parentTask),
                includePattern,
                excludePattern);

        String[] filters = namePatternMatcher.tokenizePattern(includePattern);
        assertEquals(3, filters.length);
        assertEquals("^Organization$", filters[0]);
        assertEquals("^SecGroup$", filters[1]);
        assertEquals("^SecIndividual$", filters[2]);
    }
}
