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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DataContextClobIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.LOB_PROJECT);

    private DataContext context;
    private DataContext context2;
    private DataContext context3;

    @BeforeEach
    public void setUp() {
        context  = env.dataContext();
        context2 = (DataContext) env.runtime().newContext();
        context3 = (DataContext) env.runtime().newContext();
    }

    private boolean skipTests() {
        return !env.getInstance(UnitDbAdapter.class).supportsLobs();
    }

    private boolean skipEmptyLOBTests() {
        return !env.getInstance(UnitDbAdapter.class).handlesNullVsEmptyLOBs();
    }

    @Test
    public void emptyClob() throws Exception {
        if (skipEmptyLOBTests()) {
            return;
        }
        runWithClobSize(0);
    }

    @Test
    public void fiveByteClob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithClobSize(5);
    }

    @Test
    public void fiveKByteClob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithClobSize(5 * 1024);
    }

    @Test
    public void oneMBClob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithClobSize(1024 * 1024);
    }

    @Test
    public void nullClob() throws Exception {
        if (skipTests()) {
            return;
        }

        // insert new clob
        context.newObject(ClobTestEntity.class);
        context.commitChanges();

        // read the CLOB in the new context
        List<ClobTestEntity> objects2 = ObjectSelect.query(ClobTestEntity.class).select(context2);
        assertEquals(1, objects2.size());

        ClobTestEntity clobObj2 = objects2.get(0);
        assertNull(clobObj2.getClobCol(), "Expected null, got: '" + clobObj2.getClobCol() + "'");

        // update and save Clob
        clobObj2.setClobCol("updated rather small clob...");
        context2.commitChanges();

        // read into yet another context and check for changes
        List<ClobTestEntity> objects3 = ObjectSelect.query(ClobTestEntity.class).select(context3);
        assertEquals(1, objects3.size());

        ClobTestEntity clobObj3 = objects3.get(0);
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
        List<ClobTestEntity> objects2 = ObjectSelect.query(ClobTestEntity.class).select(context2);
        assertEquals(1, objects2.size());

        ClobTestEntity clobObj2 = objects2.get(0);
        assertEquals(clobObj1.getClobCol(), clobObj2.getClobCol());

        // update and save Clob
        clobObj2.setClobCol("updated rather small clob...");
        context2.commitChanges();

        // read into yet another context and check for changes
        List<ClobTestEntity> objects3 = ObjectSelect.query(ClobTestEntity.class).select(context3);
        assertEquals(1, objects3.size());

        ClobTestEntity clobObj3 = objects3.get(0);
        assertEquals(clobObj2.getClobCol(), clobObj3.getClobCol());
    }
}
