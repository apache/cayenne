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

package org.apache.cayenne.access.translator.batch;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DefaultBatchTranslatorIT extends ServerCase {

    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testConstructor() throws Exception {
        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        DefaultBatchTranslator builder = new DefaultBatchTranslator(mock(BatchQuery.class), adapter, null) {
            @Override
            protected String createSql() {
                return null;
            }

            @Override
            protected ParameterBinding[] createBindings() {
                return new ParameterBinding[0];
            }

            @Override
            protected ParameterBinding[] doUpdateBindings(BatchQueryRow row) {
                return new ParameterBinding[0];
            }
        };

        assertSame(adapter, builder.adapter);
    }

    @Test
    public void testAppendDbAttribute1() throws Exception {
        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        String trimFunction = "testTrim";

        DefaultBatchTranslator builder = new DefaultBatchTranslator(mock(BatchQuery.class), adapter, trimFunction) {

            @Override
            protected String createSql() {
                return null;
            }

            @Override
            protected ParameterBinding[] createBindings() {
                return new ParameterBinding[0];
            }

            @Override
            protected ParameterBinding[] doUpdateBindings(BatchQueryRow row) {
                return new ParameterBinding[0];
            }
        };

        StringBuilder buf = new StringBuilder();
        DbEntity entity = new DbEntity("Test");
        DbAttribute attr = new DbAttribute("testAttr", Types.CHAR, null);
        attr.setEntity(entity);
        builder.appendDbAttribute(buf, attr);
        assertEquals("testTrim(testAttr)", buf.toString());

        buf = new StringBuilder();
        attr = new DbAttribute("testAttr", Types.VARCHAR, null);
        attr.setEntity(entity);
        builder.appendDbAttribute(buf, attr);
        assertEquals("testAttr", buf.toString());
    }

    @Test
    public void testAppendDbAttribute2() throws Exception {
        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());

        DefaultBatchTranslator builder = new DefaultBatchTranslator(mock(BatchQuery.class), adapter, null) {
            @Override
            protected String createSql() {
                return null;
            }

            @Override
            protected ParameterBinding[] createBindings() {
                return new ParameterBinding[0];
            }

            @Override
            protected ParameterBinding[] doUpdateBindings(BatchQueryRow row) {
                return new ParameterBinding[0];
            }
        };

        StringBuilder buf = new StringBuilder();
        DbEntity entity = new DbEntity("Test");

        DbAttribute attr = new DbAttribute("testAttr", Types.CHAR, null);
        attr.setEntity(entity);
        builder.appendDbAttribute(buf, attr);
        assertEquals("testAttr", buf.toString());

        buf = new StringBuilder();
        attr = new DbAttribute("testAttr", Types.VARCHAR, null);
        attr.setEntity(entity);

        builder.appendDbAttribute(buf, attr);
        assertEquals("testAttr", buf.toString());
    }
}
