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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * This test checks rollback behaviour of different propagation modes.
 *
 * @see TransactionPropagation
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class TransactionPropagationRollbackIT extends RuntimeCase {

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

    /**
     * @see TransactionPropagation#REQUIRES_NEW
     */
    @Test
    public void testPropagationRequiresNew() {
        TransactionDescriptor descriptor = TransactionDescriptor.builder()
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .isolation(Connection.TRANSACTION_SERIALIZABLE)
                .build();

        performInTransaction(descriptor);

        // rollback should be performed and no artist will be in DB
        assertEquals(0L, ObjectSelect.query(Artist.class).selectCount(context));

        // painting should be there
        assertEquals(1L, ObjectSelect.query(Painting.class).selectCount(context));
    }

    /**
     * @see TransactionPropagation#NESTED
     */
    @Test
    public void testPropagationNested() {

        TransactionDescriptor descriptor = TransactionDescriptor.builder()
                .isolation(Connection.TRANSACTION_SERIALIZABLE)
                .propagation(TransactionPropagation.NESTED)
                .build();

        performInTransaction(descriptor);

        // nested rollback shouldn't affect outer transaction
        assertEquals(1L, ObjectSelect.query(Artist.class).selectCount(context));

        // painting should be there
        assertEquals(1L, ObjectSelect.query(Painting.class).selectCount(context));
    }

    /**
     * @see TransactionPropagation#MANDATORY
     */
    @Test
    public void testPropagationMandatory() {

        TransactionDescriptor descriptor = TransactionDescriptor.builder()
                .isolation(Connection.TRANSACTION_SERIALIZABLE)
                .propagation(TransactionPropagation.MANDATORY)
                .build();

        performInTransaction(descriptor);

        // nested rollback shouldn't affect outer transaction
        assertEquals(1L, ObjectSelect.query(Artist.class).selectCount(context));

        // painting should be there
        assertEquals(1L, ObjectSelect.query(Painting.class).selectCount(context));
    }

    private void performInTransaction(TransactionDescriptor descriptor) {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("test");

        manager.performInTransaction(() -> {
            // try to perform illegal operation in nested transaction
            try {
                manager.performInTransaction(() -> {
                    artist.setArtistName("test3");
                    context.commitChanges(); // this should pass

                    artist.setArtistName(null);
                    context.commitChanges(); // this should throw
                    return null;
                }, descriptor);
                fail("Exception should be thrown");
            } catch (Exception ignore) {
            }

            // perform some valid commit
            artist.setArtistName("test2");

            Painting painting = context.newObject(Painting.class);
            painting.setPaintingTitle("painting");

            // Outcome of this will depend on transaction propagation
            // if it's nested or mandatory we'll have here artist committed,
            // if it's new no artist should be in database
            context.commitChanges();
            return null;
        });
    }

}
