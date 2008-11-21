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

import org.apache.cayenne.cache.OSQueryCacheFactory;

public class DataContextQueryCachingOSCacheTest extends DataContextQueryCachingTest {

    @Override
    public void testLocalCacheDataObjectsRefresh() throws Exception {
        runTest(new TestRun() {

            public void execute() throws Exception {
                DataContextQueryCachingOSCacheTest.super
                        .testLocalCacheDataObjectsRefresh();
            }
        });
    }

    @Override
    public void testLocalCacheDataRowsRefresh() throws Exception {
        runTest(new TestRun() {

            public void execute() throws Exception {
                DataContextQueryCachingOSCacheTest.super.testLocalCacheDataRowsRefresh();
            }
        });
    }

    @Override
    public void testLocalCacheRefreshObjectsRefresh() throws Exception {
        runTest(new TestRun() {

            public void execute() throws Exception {
                DataContextQueryCachingOSCacheTest.super
                        .testLocalCacheRefreshObjectsRefresh();
            }
        });
    }

    @Override
    public void testSharedCacheDataRowsRefresh() throws Exception {
        runTest(new TestRun() {

            public void execute() throws Exception {
                DataContextQueryCachingOSCacheTest.super.testSharedCacheDataRowsRefresh();
            }
        });
    }

    private void runTest(TestRun test) throws Exception {
        context.setQueryCache(null);
        getDomain().setQueryCacheFactory(new OSQueryCacheFactory());
        getDomain().queryCache = null;
        try {
            test.execute();
        }
        finally {
            getDomain().setQueryCacheFactory(null);
            getDomain().queryCache = null;
        }
    }

    interface TestRun {

        void execute() throws Exception;
    }

}
