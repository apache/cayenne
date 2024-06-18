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

package org.apache.cayenne;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextConcurrencyIT extends RuntimeCase {

    private static final Logger logger = LoggerFactory.getLogger(DataContextConcurrencyIT.class);

    @Inject
    private ObjectContext context;

    /**
     * see https://issues.apache.org/jira/browse/CAY-2382
     *
     * This test is probabilistic as it tries to catch concurrency problem.
     * It can be false-negative (e.g. Travis build not always fails)
     */
    @Test
    public void testCMEInContextSerialization() throws Exception {

        // add some content to context, so it will be serializing slowly
        for(int i=0; i<1000; i++) {
            Artist artist = context.newObject(Artist.class);
            artist.setArtistName("name " + i);
        }

        // add some barriers so threads will try to start synchronously
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch serializationStartSignal = new CountDownLatch(1);

        ExecutorService service = Executors.newFixedThreadPool(2);

        // this task will modify context
        Future<Boolean> resultObjectCreation = service.submit(() -> {
            try {
                // this thread will start when context serialized at least once,
                // as it should be faster (but who knows...)
                serializationStartSignal.await();
            } catch (InterruptedException e) {
                return false;
            }
            for(int i=0; i<1000; i++) {
                Artist artist = context.newObject(Artist.class);
                context.deleteObject(artist);
            }
            return true;
        });

        // this task will serialize context
        Future<Boolean> resultSerialization = service.submit(() -> {
            try {
                // wait for outer thread start this one
                startSignal.await();
            } catch (InterruptedException e) {
                return false;
            }
            for(int i=0; i<100; i++) {
                try {
                    // make one serialization, before starting modifying thread
                    Util.cloneViaSerialization(context);
                    if(i == 0) {
                        serializationStartSignal.countDown();
                    }
                } catch (Exception e) {
                    logger.error("Serialization failed", e);
                    return false;
                }
            }

            return true;
        });

        // make sure that everyone are ready
        Thread.sleep(10);
        startSignal.countDown();

        assertTrue(resultObjectCreation.get(20, TimeUnit.SECONDS));
        assertTrue(resultSerialization.get(20, TimeUnit.SECONDS));
    }
}
