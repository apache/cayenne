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

package org.apache.cayenne.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.Util;

/**
 * @author Andrus Adamchik
 */
public class SQLTemplateTest extends TestCase {

    public void testQueryWithParameters() {
        SQLTemplate q1 = new SQLTemplate("E1", "SELECT");
        q1.setName("QName");

        Query q2 = q1.queryWithParameters(Collections.EMPTY_MAP);
        assertNotNull(q2);
        assertNotSame(q1, q2);
        assertTrue(q2 instanceof SQLTemplate);

        assertEquals(q1.getName(), q2.getName());

        Query q3 = q1.queryWithParameters(Collections.singletonMap("a", "b"));
        assertNotNull(q3);
        assertNotSame(q1, q3);
        assertNotNull(q3.getName());
        assertFalse(q1.getName().equals(q3.getName()));

        Query q4 = q1.queryWithParameters(Collections.singletonMap("a", "b"));
        assertNotNull(q4);
        assertNotSame(q3, q4);
        assertEquals(q3.getName(), q4.getName());
    }

    public void testSerializability() throws Exception {
        SQLTemplate o = new SQLTemplate("Test", "DO SQL");
        Object clone = Util.cloneViaSerialization(o);

        assertTrue(clone instanceof SQLTemplate);
        SQLTemplate c1 = (SQLTemplate) clone;

        assertNotSame(o, c1);
        assertEquals(o.getRoot(), c1.getRoot());
        assertEquals(o.getDefaultTemplate(), c1.getDefaultTemplate());
    }

    public void testSerializabilityWithHessian() throws Exception {
        SQLTemplate o = new SQLTemplate("Test", "DO SQL");
        Object clone = HessianUtil.cloneViaClientServerSerialization(
                o,
                new EntityResolver());

        assertTrue(clone instanceof SQLTemplate);
        SQLTemplate c1 = (SQLTemplate) clone;

        assertNotSame(o, c1);
        assertEquals(o.getRoot(), c1.getRoot());
        assertEquals(o.getDefaultTemplate(), c1.getDefaultTemplate());

        // set immutable parameters ... query must recast them to mutable version
        Map[] parameters = new Map[] {
            Collections.EMPTY_MAP
        };
        o.setParameters(parameters);

        HessianUtil.cloneViaClientServerSerialization(o, new EntityResolver());
    }

    public void testGetDefaultTemplate() {
        SQLTemplate query = new SQLTemplate();
        query.setDefaultTemplate("AAA # BBB");
        assertEquals("AAA # BBB", query.getDefaultTemplate());
    }

    public void testGetTemplate() {
        SQLTemplate query = new SQLTemplate();

        // no template for key, no default template... must be null
        assertNull(query.getTemplate("key1"));

        // no template for key, must return default
        query.setDefaultTemplate("AAA # BBB");
        assertEquals("AAA # BBB", query.getTemplate("key1"));

        // must find template
        query.setTemplate("key1", "XYZ");
        assertEquals("XYZ", query.getTemplate("key1"));

        // add another template.. still must find
        query.setTemplate("key2", "123");
        assertEquals("XYZ", query.getTemplate("key1"));
        assertEquals("123", query.getTemplate("key2"));
    }

    public void testSingleParameterSet() throws Exception {
        SQLTemplate query = new SQLTemplate();

        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());

        Map params = new HashMap();
        params.put("a", "b");

        query.setParameters(params);
        assertEquals(params, query.getParameters());
        Iterator it = query.parametersIterator();
        assertTrue(it.hasNext());
        assertEquals(params, it.next());
        assertFalse(it.hasNext());

        query.setParameters((Map) null);
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
        it = query.parametersIterator();
        assertFalse(it.hasNext());
    }

    public void testBatchParameterSet() throws Exception {
        SQLTemplate query = new SQLTemplate();

        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());

        Map params1 = new HashMap();
        params1.put("a", "b");

        Map params2 = new HashMap();
        params2.put("1", "2");

        query.setParameters(new Map[] {
                params1, params2, null
        });
        assertEquals(params1, query.getParameters());
        Iterator it = query.parametersIterator();
        assertTrue(it.hasNext());
        assertEquals(params1, it.next());
        assertTrue(it.hasNext());
        assertEquals(params2, it.next());
        assertTrue(it.hasNext());
        assertTrue(((Map) it.next()).isEmpty());
        assertFalse(it.hasNext());

        query.setParameters((Map[]) null);
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
        it = query.parametersIterator();
        assertFalse(it.hasNext());
    }
}
