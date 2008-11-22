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

import org.apache.art.BlobTestEntity;
import org.apache.cayenne.access.types.ByteArrayTypeTest;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataContextBlobTest extends CayenneCase {

    protected DataContext ctxt;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        ctxt = createDataContext();
    }

    protected boolean skipTests() {
        return !getAccessStackAdapter().supportsLobs();
    }

    protected boolean skipEmptyLOBTests() {
        return !getAccessStackAdapter().handlesNullVsEmptyLOBs();
    }

    public void testEmptyBlob() throws Exception {
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
        ctxt.newObject(BlobTestEntity.class);
        ctxt.commitChanges();

        // read the BLOB in the new context
        DataContext ctxt2 = createDataContext();
        List objects2 = ctxt2.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects2.size());

        BlobTestEntity blobObj2 = (BlobTestEntity) objects2.get(0);
        assertNull(blobObj2.getBlobCol());

        // update and save Blob
        blobObj2.setBlobCol(bytes2);
        ctxt2.commitChanges();

        // read into yet another context and check for changes
        DataContext ctxt3 = createDataContext();
        List objects3 = ctxt3.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects3.size());

        BlobTestEntity blobObj3 = (BlobTestEntity) objects3.get(0);
        ByteArrayTypeTest.assertByteArraysEqual(blobObj2.getBlobCol(), blobObj3
                .getBlobCol());
    }

    protected void runWithBlobSize(int sizeBytes) throws Exception {
        // insert new clob
        BlobTestEntity blobObj1 = ctxt.newObject(BlobTestEntity.class);

        // init BLOB of a specified size
        byte[] bytes = new byte[sizeBytes];
        for (int i = 0; i < sizeBytes; i++) {
            bytes[i] = (byte) (65 + (50 + i) % 50);
        }

        blobObj1.setBlobCol(bytes);
        ctxt.commitChanges();

        // read the CLOB in the new context
        DataContext ctxt2 = createDataContext();
        List objects2 = ctxt2.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects2.size());

        BlobTestEntity blobObj2 = (BlobTestEntity) objects2.get(0);
        ByteArrayTypeTest.assertByteArraysEqual(blobObj1.getBlobCol(), blobObj2
                .getBlobCol());

        // update and save Clob
        blobObj2.setBlobCol(new byte[] {
                '1', '2'
        });
        ctxt2.commitChanges();

        // read into yet another context and check for changes
        DataContext ctxt3 = createDataContext();
        List objects3 = ctxt3.performQuery(new SelectQuery(BlobTestEntity.class));
        assertEquals(1, objects3.size());

        BlobTestEntity blobObj3 = (BlobTestEntity) objects3.get(0);
        ByteArrayTypeTest.assertByteArraysEqual(blobObj2.getBlobCol(), blobObj3
                .getBlobCol());
    }
}
