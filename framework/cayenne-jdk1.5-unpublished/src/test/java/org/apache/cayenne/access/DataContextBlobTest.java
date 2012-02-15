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

import org.apache.cayenne.access.types.ByteArrayTypeTest;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.BlobTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextBlobTest extends ServerCase {

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
            dbHelper.deleteAll("BLOB_TEST");
        }
    }

    protected boolean skipTests() {
        return !accessStackAdapter.supportsLobs();
    }

    protected boolean skipEmptyLOBTests() {
        return !accessStackAdapter.handlesNullVsEmptyLOBs();
    }

    public void testEmptyBlob() throws Exception {
        if (skipTests()) {
            return;
        }
        if (skipEmptyLOBTests()) {
            return;
        }
        runWithBlobSize(0);
    }

    public void test5ByteBlob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithBlobSize(5);
    }

    public void test5KByteBlob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithBlobSize(5 * 1024);
    }

    public void test1MBBlob() throws Exception {
        if (skipTests()) {
            return;
        }
        runWithBlobSize(1024 * 1024);
    }

    public void testNullBlob() throws Exception {
        if (skipTests()) {
            return;
        }

        byte[] bytes2 = new byte[] {
                'a', 'b', 'c', 'd'
        };

        // insert new blob
        context.newObject(BlobTestEntity.class);
        context.commitChanges();

        // read the BLOB in the new context

        List<?> objects2 = context2.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects2.size());

        BlobTestEntity blobObj2 = (BlobTestEntity) objects2.get(0);
        assertNull(blobObj2.getBlobCol());

        // update and save Blob
        blobObj2.setBlobCol(bytes2);
        context2.commitChanges();

        // read into yet another context and check for changes
        List<?> objects3 = context3.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects3.size());

        BlobTestEntity blobObj3 = (BlobTestEntity) objects3.get(0);
        ByteArrayTypeTest.assertByteArraysEqual(blobObj2.getBlobCol(), blobObj3
                .getBlobCol());
      
    }

    protected void runWithBlobSize(int sizeBytes) throws Exception {
        // insert new clob
        BlobTestEntity blobObj1 = context.newObject(BlobTestEntity.class);

        // init BLOB of a specified size
        byte[] bytes = new byte[sizeBytes];
        for (int i = 0; i < sizeBytes; i++) {
            bytes[i] = (byte) (65 + (50 + i) % 50);
        }

        blobObj1.setBlobCol(bytes);
        context.commitChanges();

        // read the CLOB in the new context
        List<?> objects2 = context2.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects2.size());

        BlobTestEntity blobObj2 = (BlobTestEntity) objects2.get(0);
        ByteArrayTypeTest.assertByteArraysEqual(blobObj1.getBlobCol(), blobObj2
                .getBlobCol());

        // update and save Blob
        blobObj2.setBlobCol(new byte[] {
                '1', '2'
        });
        context2.commitChanges();

        // read into yet another context and check for changes
        List<?> objects3 = context3.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects3.size());

        BlobTestEntity blobObj3 = (BlobTestEntity) objects3.get(0);
        ByteArrayTypeTest.assertByteArraysEqual(blobObj2.getBlobCol(), blobObj3
                .getBlobCol());
    }
}
