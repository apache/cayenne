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


package org.apache.cayenne.jpa;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.jpa.JpaNativeQuery;

import junit.framework.TestCase;

public class JpaNativeQueryTest extends TestCase {

    public void testSetParameter1() {
        JpaNativeQuery q = new JpaNativeQuery(
                new DataContext(),
                "select a from person where name = $name and id = $id ",
                Object.class);
        assertEquals("Should return same query", q, q.setParameter("name", ""));

        /* TODO: uncomment when supported..
        try {
            q.setParameter("unexisting", "");
            fail("Should throw on unexisting parameter");
        }
        catch (IllegalArgumentException e) {
            // ok
        }
        */
    }

    public void testSetParameter2() {
        JpaNativeQuery q = new JpaNativeQuery(
                new DataContext(),
                "select a from person where name = ?1 and id = ?2 and addr = ?123 ",
                Object.class);
        assertEquals("Should return same query", q, q.setParameter(1, ""));
        q.setParameter(2, "");
        q.setParameter(123, "");

        /* TODO: uncomment when supported..
        try {
            q.setParameter(3, "");
            fail("Should throw on unexisting parameter");
        }
        catch (IllegalArgumentException e) {
            // ok
        }
        */
    }
}
