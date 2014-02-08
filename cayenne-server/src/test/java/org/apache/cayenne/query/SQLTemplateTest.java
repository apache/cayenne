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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SQLTemplateTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    public void testSQLTemplateForDataMap() {
        DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
        SQLTemplate q1 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", true);
        List<DataRow> result = context.performQuery(q1);
        assertEquals(0, result.size());
    }

    public void testSQLTemplateForDataMapWithInsert() {
        DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
        String sql = "INSERT INTO ARTIST VALUES (15, 'Surikov', null)";
        SQLTemplate q1 = new SQLTemplate(testDataMap, sql, true);
        context.performNonSelectingQuery(q1);

        SQLTemplate q2 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", true);
        List<DataRow> result = context.performQuery(q2);
        assertEquals(1, result.size());
    }

    public void testSQLTemplateForDataMapWithInsertException() {
        DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
        String sql = "INSERT INTO ARTIST VALUES (15, 'Surikov', null)";
        SQLTemplate q1 = new SQLTemplate(testDataMap, sql, true);
        context.performNonSelectingQuery(q1);

        SQLTemplate q2 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", false);
        boolean gotRuntimeException = false;
        try {
            context.performQuery(q2);
        } catch (CayenneRuntimeException e) {
            gotRuntimeException = true;
        }
        assertTrue("If fetchingDataRows is false and ObjectEntity not set, shoulb be thrown exception",
                gotRuntimeException);
    }

    public void testColumnNameCapitalization() {
        SQLTemplate q1 = new SQLTemplate("E1", "SELECT");
        assertSame(CapsStrategy.DEFAULT, q1.getColumnNamesCapitalization());
        q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
        assertEquals(CapsStrategy.UPPER, q1.getColumnNamesCapitalization());
    }

    public void testQueryWithParameters() {
        SQLTemplate q1 = new SQLTemplate("E1", "SELECT");
        q1.setName("QName");

        Query q2 = q1.queryWithParameters(Collections.EMPTY_MAP);
        assertNotNull(q2);
        assertNotSame(q1, q2);
        assertTrue(q2 instanceof SQLTemplate);

        assertNull(q2.getName());

        Query q3 = q1.queryWithParameters(Collections.singletonMap("a", "b"));
        assertNotNull(q3);
        assertNotSame(q1, q3);
        assertNull(q3.getName());
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

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("a", "b");

        query.setParameters(params);
        assertEquals(params, query.getParameters());
        Iterator<?> it = query.parametersIterator();
        assertTrue(it.hasNext());
        assertEquals(params, it.next());
        assertFalse(it.hasNext());

        query.setParameters(null);
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
        it = query.parametersIterator();
        assertFalse(it.hasNext());
    }

    public void testBatchParameterSet() throws Exception {
        SQLTemplate query = new SQLTemplate();

        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());

        Map<String, Object> params1 = new HashMap<String, Object>();
        params1.put("a", "b");

        Map<String, Object> params2 = new HashMap<String, Object>();
        params2.put("1", "2");

        query.setParameters(new Map[] { params1, params2, null });
        assertEquals(params1, query.getParameters());
        Iterator<?> it = query.parametersIterator();
        assertTrue(it.hasNext());
        assertEquals(params1, it.next());
        assertTrue(it.hasNext());
        assertEquals(params2, it.next());
        assertTrue(it.hasNext());
        assertTrue(((Map<String, Object>) it.next()).isEmpty());
        assertFalse(it.hasNext());

        query.setParameters((Map[]) null);
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
        it = query.parametersIterator();
        assertFalse(it.hasNext());
    }
}
