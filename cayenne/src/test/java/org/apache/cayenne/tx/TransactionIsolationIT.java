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

package org.apache.cayenne.tx;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class TransactionIsolationIT extends RuntimeCase {

    private final Logger logger = LoggerFactory.getLogger(TransactionIsolationIT.class);

    @Inject
    DataContext context;

    @Inject
    CayenneRuntime runtime;

    @Inject
    UnitDbAdapter unitDbAdapter;

    TransactionManager manager;

    @Before
    public void initTransactionManager() {
        // no binding in test container, get it from runtime
        manager = runtime.getInjector().getInstance(TransactionManager.class);
    }

    @Test
    public void testIsolationLevel() throws Exception {

        if(!unitDbAdapter.supportsSerializableTransactionIsolation()) {
            return;
        }

        TransactionDescriptor descriptor = TransactionDescriptor.builder()
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .isolation(Connection.TRANSACTION_SERIALIZABLE)
                .build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch resumeSerializableTransaction = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(2);

        Future<Boolean> thread1Result = service.submit(() -> {
            try {
                return manager.performInTransaction(() -> {
                    long result;
                    try {
                        result = ObjectSelect.query(Artist.class).selectCount(context);
                    } finally {
                        startSignal.countDown();
                    }
                    if(result != 0) {
                        logger.error("First fetch returned " + result);
                        return false;
                    }
                    try {
                        resumeSerializableTransaction.await();
                    } catch (InterruptedException e) {
                        logger.error("Resume signal await failed", e);
                        return false;
                    }

                    result = ObjectSelect.query(Artist.class).selectCount(context);
                    logger.info("Second fetch returned " + result);
                    return result == 0;
                }, descriptor);
            } catch (Exception ex) {
                logger.error("Perform in transaction failed", ex);
                return false;
            }
        });

        Future<Boolean> thread2Result = service.submit(() -> {
            try {
                startSignal.await();
                try {
                    Artist artist = context.newObject(Artist.class);
                    artist.setArtistName("artist");
                    context.commitChanges();
                } finally {
                    resumeSerializableTransaction.countDown();
                }
            } catch (Exception ex) {
                logger.error("Unable to create Artist", ex);
                return false;
            }
            return true;
        });

        assertTrue(thread1Result.get(30, TimeUnit.SECONDS));
        assertTrue(thread2Result.get(30, TimeUnit.SECONDS));
    }

}
