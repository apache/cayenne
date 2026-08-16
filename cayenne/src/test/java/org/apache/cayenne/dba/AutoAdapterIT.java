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

import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class AutoAdapterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void getAdapter_Proxy() {
        AutoAdapter adapter = (AutoAdapter) env.dataNode().getAdapter();

        DbAdapter detected = adapter.getAdapter();
        assertNotNull(detected);
        assertFalse(detected instanceof AutoAdapter);

        // the delegate is resolved once and cached
        assertSame(detected, adapter.getAdapter());
        assertSame(detected, adapter.unwrap());
    }

    @Test
    public void createSQLTemplateAction() {

        assertInstanceOf(AutoAdapter.class, env.dataNode().getAdapter());
        AutoAdapter autoAdapter = (AutoAdapter) env.dataNode().getAdapter();

        SQLTemplateAction action = (SQLTemplateAction) autoAdapter
                .getAction(new SQLTemplate(Artist.class, "select * from artist"), env.dataNode());

        // it is important for SQLTemplateAction to be used with unwrapped adapter,
        // as the adapter class name is used as a key to the correct SQL template.
        assertNotNull(action.getAdapter());
        assertFalse(action.getAdapter() instanceof AutoAdapter);
    }

    @Test
    public void correctProxyMethods() {
        AutoAdapter adapter = (AutoAdapter) env.dataNode().getAdapter();
        DbAdapter detected = adapter.getAdapter();

        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class);

        // query related methods
        assertEquals(detected.supportsBatchUpdates(), adapter.supportsBatchUpdates());
        assertEquals(detected.supportsGeneratedKeys(), adapter.supportsGeneratedKeys());
        assertEquals(detected.supportsGeneratedKeysForBatchInserts(), adapter.supportsGeneratedKeysForBatchInserts());
        assertSame(detected.getBatchTerminator(), adapter.getBatchTerminator());
        // returns a new instance for each call
        assertSame(detected.createPkGenerator().getClass(), adapter.createPkGenerator().getClass());
        DbEntity artistDbEntity = env.dataNode().getEntityResolver().getObjEntity(Artist.class).getDbEntity();
        assertSame(detected.getQuotingStrategy(artistDbEntity), adapter.getQuotingStrategy(artistDbEntity));
        // returns a new instance for each call
        assertSame(detected.getSqlTreeProcessor().getClass(), adapter.getSqlTreeProcessor().getClass());
        assertSame(detected.getExtendedTypes(), adapter.getExtendedTypes());
        assertSame(detected.getEjbqlTranslator(), adapter.getEjbqlTranslator());
        // returns a new instance for each call
        assertSame(detected.getSelectTranslator(select, env.dataNode().getEntityResolver()).getClass(),
                adapter.getSelectTranslator(select, env.dataNode().getEntityResolver()).getClass());

        // reverse engineering related methods
        assertEquals(detected.supportsCatalogsOnReverseEngineering(), adapter.supportsCatalogsOnReverseEngineering());
        assertSame(detected.getSystemCatalogs(), adapter.getSystemCatalogs());
        assertSame(detected.getSystemSchemas(), adapter.getSystemSchemas());
        assertSame(detected.tableTypeForTable(), adapter.tableTypeForTable());
        assertSame(detected.tableTypeForView(), adapter.tableTypeForView());

        // db generation related methods
        assertEquals(detected.supportsUniqueConstraints(), adapter.supportsUniqueConstraints());
    }
}
