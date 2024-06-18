/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.access.DataNode;
import org.slf4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class BaseSchemaUpdateStrategy_ConcurrencyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSchemaUpdateStrategy_ConcurrencyTest.class);

    private ExecutorService threadPool;
    private DataNode dataNode;

    @Before
    public void before() {
        threadPool = Executors.newFixedThreadPool(2);
        dataNode = mock(DataNode.class);
    }

    @After
    public void after() {
        threadPool.shutdownNow();
    }

    @Test
    public void testUpdateSchema_Concurrency() throws InterruptedException, ExecutionException, TimeoutException {

        final AtomicInteger counter = new AtomicInteger();
        final AtomicBoolean errors = new AtomicBoolean(false);

        final BaseSchemaUpdateStrategy strategy = new BaseSchemaUpdateStrategy() {
            @Override
            protected void processSchemaUpdate(DataNode dataNode) throws SQLException {
                counter.incrementAndGet();
            }
        };

        Collection<Future<?>> tasks = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            tasks.add(threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        strategy.updateSchema(dataNode);
                    } catch (Throwable e) {
                        LOGGER.error("error in test", e);
                        errors.set(true);
                    }
                }
            }));
        }

        for(Future<?> f : tasks) {
            f.get(1, TimeUnit.SECONDS);
        }

        assertFalse(errors.get());
        assertEquals(1, counter.get());
    }
}
