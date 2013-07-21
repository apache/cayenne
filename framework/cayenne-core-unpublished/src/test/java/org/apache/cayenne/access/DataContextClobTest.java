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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.ClobTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextClobTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context2;

    @Inject
    private DataContext context3;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        if (accessStackAdapter.supportsLobs()) {
            dbHelper.deleteAll("CLOB_TEST");
        }
    }

    private boolean skipTests() {
        return !accessStackAdapter.supportsLobs();
    }

    private boolean skipEmptyLOBTests() {
        return !accessStackAdapter.handlesNullVsEmptyLOBs();
    }

    public void testEmptyClob() throws Exception {
        if (skipEmptyLOBTests()) {
            return;
        }
        runWithClobSize(0);
    }

    public void test5ByteClob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithClobSize(5);
    }

    public void test5KByteClob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithClobSize(5 * 1024);
    }

    public void test1MBClob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithClobSize(1024 * 1024);
    }

    public void testNullClob() throws Exception {
        if (skipTests()) {
            return;
        }

        // insert new clob
        context.newObject(ClobTestEntity.class);
        context.commitChanges();

        // read the CLOB in the new context
        List<?> objects2 = context2.performQuery(new SelectQuery(ClobTestEntity.class));
        assertEquals(1, objects2.size());

        ClobTestEntity clobObj2 = (ClobTestEntity) objects2.get(0);
        assertNull("Expected null, got: '" + clobObj2.getClobCol() + "'", clobObj2
                .getClobCol());

        // update and save Clob
        clobObj2.setClobCol("updated rather small clob...");
        context2.commitChanges();

        // read into yet another context and check for changes
        List<?> objects3 = context3.performQuery(new SelectQuery(ClobTestEntity.class));
        assertEquals(1, objects3.size());

        ClobTestEntity clobObj3 = (ClobTestEntity) objects3.get(0);
        assertEquals(clobObj2.getClobCol(), clobObj3.getClobCol());
    }

    protected void runWithClobSize(int sizeBytes) throws Exception {
        // insert new clob
        ClobTestEntity clobObj1 = context.newObject(ClobTestEntity.class);

        // init CLOB of a specified size
        if (sizeBytes == 0) {
            clobObj1.setClobCol("");
        }
        else {
            byte[] bytes = new byte[sizeBytes];
            for (int i = 0; i < sizeBytes; i++) {
                bytes[i] = (byte) (65 + (50 + i) % 50);
            }
            clobObj1.setClobCol(new String(bytes));
        }

        context.commitChanges();

        // read the CLOB in the new context
        List<?> objects2 = context2.performQuery(new SelectQuery(ClobTestEntity.class));
        assertEquals(1, objects2.size());

        ClobTestEntity clobObj2 = (ClobTestEntity) objects2.get(0);
        assertEquals(clobObj1.getClobCol(), clobObj2.getClobCol());

        // update and save Clob
        clobObj2.setClobCol("updated rather small clob...");
        context2.commitChanges();

        // read into yet another context and check for changes
        List<?> objects3 = context3.performQuery(new SelectQuery(ClobTestEntity.class));
        assertEquals(1, objects3.size());

        ClobTestEntity clobObj3 = (ClobTestEntity) objects3.get(0);
        assertEquals(clobObj2.getClobCol(), clobObj3.getClobCol());
    }
}
