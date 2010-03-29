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

package org.apache.cayenne.access.jdbc;

import org.apache.art.Artist;
import org.apache.art.GeneratedColumnTestEntity;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class BatchActionTest extends CayenneCase {

    public void testHasGeneratedKeys1() throws Exception {
        EntityResolver resolver = getDomain().getEntityResolver();

        // test with adapter that supports keys
        DbAdapter adapter = buildAdapter(true);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.lookupObjEntity(
                GeneratedColumnTestEntity.class).getDbEntity(), 5);
        assertTrue(new BatchAction(batch1, adapter, resolver).hasGeneratedKeys());

        InsertBatchQuery batch2 = new InsertBatchQuery(resolver.lookupObjEntity(
                Artist.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch2, adapter, resolver).hasGeneratedKeys());
    }

    public void testHasGeneratedKeys2() throws Exception {
        EntityResolver resolver = getDomain().getEntityResolver();

        // test with adapter that does not support keys...
        DbAdapter adapter = buildAdapter(false);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.lookupObjEntity(
                GeneratedColumnTestEntity.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch1, adapter, resolver).hasGeneratedKeys());

        InsertBatchQuery batch2 = new InsertBatchQuery(resolver.lookupObjEntity(
                Artist.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch2, adapter, resolver).hasGeneratedKeys());
    }

    DbAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = new JdbcAdapter();
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }
}
