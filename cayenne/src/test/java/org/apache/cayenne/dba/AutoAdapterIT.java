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

package org.apache.cayenne.dba;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class AutoAdapterIT extends RuntimeCase {

    @Inject
    private DataNode dataNode;

    @Test
    public void testGetAdapter_Proxy() {
        AutoAdapter adapter = new AutoAdapter(() -> dataNode.getAdapter(), NoopJdbcEventLogger.getInstance());
        DbAdapter detected = adapter.getAdapter();
        assertSame(dataNode.getAdapter(), detected);
    }

    @Test
    public void testCreateSQLTemplateAction() {
        AutoAdapter autoAdapter = new AutoAdapter(() -> dataNode.getAdapter(), NoopJdbcEventLogger.getInstance());

        SQLTemplateAction action = (SQLTemplateAction) autoAdapter.getAction(new SQLTemplate(Artist.class,
                "select * from artist"), dataNode);

        // it is important for SQLTemplateAction to be used with unwrapped adapter,
        // as the adapter class name is used as a key to the correct SQL template.
        assertNotNull(action.getAdapter());
        assertFalse(action.getAdapter() instanceof AutoAdapter);
        assertSame(dataNode.getAdapter(), action.getAdapter());
    }

    @Test
    public void testCorrectProxyMethods() {
        DbAdapter adapter = dataNode.getAdapter();
        AutoAdapter autoAdapter = new AutoAdapter(() -> adapter, NoopJdbcEventLogger.getInstance());

        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class);

        // query related methods
        assertEquals(adapter.supportsBatchUpdates(),
                autoAdapter.supportsBatchUpdates());
        assertEquals(adapter.supportsGeneratedKeys(),
                autoAdapter.supportsGeneratedKeys());
        assertEquals(adapter.supportsGeneratedKeysForBatchInserts(),
                autoAdapter.supportsGeneratedKeysForBatchInserts());
        assertSame(adapter.getBatchTerminator(),
                autoAdapter.getBatchTerminator());
        assertSame(adapter.getPkGenerator(),
                autoAdapter.getPkGenerator());
        assertSame(adapter.getQuotingStrategy(),
                autoAdapter.getQuotingStrategy());
        // returns a new instance for each call
        assertSame(adapter.getSqlTreeProcessor().getClass(),
                autoAdapter.getSqlTreeProcessor().getClass());
        assertSame(adapter.getExtendedTypes(),
                autoAdapter.getExtendedTypes());
        assertSame(adapter.getEjbqlTranslatorFactory(),
                autoAdapter.getEjbqlTranslatorFactory());
        // returns a new instance for each call
        assertSame(adapter.getSelectTranslator(select, dataNode.getEntityResolver()).getClass(),
                autoAdapter.getSelectTranslator(select, dataNode.getEntityResolver()).getClass());


        // reverse engineering related methods
        assertEquals(adapter.supportsCatalogsOnReverseEngineering(),
                autoAdapter.supportsCatalogsOnReverseEngineering());
        assertSame(adapter.getSystemCatalogs(),
                autoAdapter.getSystemCatalogs());
        assertSame(adapter.getSystemSchemas(),
                autoAdapter.getSystemSchemas());
        assertSame(adapter.tableTypeForTable(),
                autoAdapter.tableTypeForTable());
        assertSame(adapter.tableTypeForView(),
                autoAdapter.tableTypeForView());

        // db generation related methods
        assertEquals(adapter.supportsUniqueConstraints(),
                autoAdapter.supportsUniqueConstraints());
    }
}
